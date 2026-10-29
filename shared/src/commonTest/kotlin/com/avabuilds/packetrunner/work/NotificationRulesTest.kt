package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class NotificationRulesTest {
    private val s = WorkSchedule()
    private val none = WorkLog(-1)
    private fun at(d: Int, h: Int, m: Int = 0) = WallTime.week(d, h, m)
    private fun call(minute: Int, status: CallStatus = CallStatus.PENDING) =
        PlannedCall(Request(1, RequestType.FIX, "hana", "cafe", "hi", 150, minute.toFloat()), status)

    @Test fun duringWorkHoursACallMayNotifyEvenIfYouNeverClockedIn() {
        assertTrue(NotificationRules.isWorkingTime(s, none, at(0, 10)))
        assertTrue(NotificationRules.isWorkingTime(s, none, at(2, 17, 59)))
    }

    @Test fun neverAtLunchBeforeWorkAfterWorkOrOnDaysOff() {
        assertFalse(NotificationRules.isWorkingTime(s, none, at(0, 12, 30)))
        assertFalse(NotificationRules.isWorkingTime(s, none, at(0, 8, 59)))
        assertFalse(NotificationRules.isWorkingTime(s, none, at(0, 18)))
        assertFalse(NotificationRules.isWorkingTime(s, none, at(0, 23)))
        assertFalse(NotificationRules.isWorkingTime(s, none, at(5, 10)))
        assertFalse(NotificationRules.isWorkingTime(s, none, at(6, 15)))
    }

    @Test fun afterClockingOutEarlyThereAreNoNotifications() {
        val out = WorkLog(at(0, 9).epochDay, clockInMinute = 9 * 60, clockOutMinute = 15 * 60)
        assertTrue(NotificationRules.isWorkingTime(s, out, at(0, 14, 59)))
        assertFalse(NotificationRules.isWorkingTime(s, out, at(0, 15, 1)))
        assertTrue(NotificationRules.isWorkingTime(s, out, at(1, 15, 1))) // yesterday's clock-out does not count
    }

    @Test fun onlyAPendingCallWithTheAppClosedAtWorkingTimeNotifies() {
        val now = at(0, 10)
        assertTrue(NotificationRules.shouldNotify(call(600), s, none, now, appInForeground = false))
        assertFalse(NotificationRules.shouldNotify(call(600), s, none, now, appInForeground = true))
        assertFalse(NotificationRules.shouldNotify(call(600, CallStatus.RANG), s, none, now, false))
        assertFalse(NotificationRules.shouldNotify(call(600, CallStatus.MISSED), s, none, now, false))
        assertFalse(NotificationRules.shouldNotify(null, s, none, now, false))
        assertFalse(NotificationRules.shouldNotify(call(600), s, none, at(0, 12, 30), false))
    }

    @Test fun theDelayIsTheTimeUntilTheCall() {
        assertEquals(30L * 60, NotificationRules.delaySeconds(at(0, 0).epochDay, 10 * 60, at(0, 9, 30)))
        assertEquals(24L * 3600 + 3600, NotificationRules.delaySeconds(at(1, 0).epochDay, 10 * 60, at(0, 9)))
    }

    @Test fun aCallInThePastIsDueNowAndNeverNegative() {
        assertEquals(0L, NotificationRules.delaySeconds(at(0, 0).epochDay, 9 * 60, at(0, 10)))
    }

    @Test fun inDemoModeTheDelayIsSixtyTimesShorter() {
        assertEquals(30L, NotificationRules.delaySeconds(at(0, 0).epochDay, 10 * 60, at(0, 9, 30), factor = 60))
    }

    @Test fun onlyCallsStillToComeGetAnAlarm() {
        val today = CallBook(at(0, 0).epochDay, listOf(call(540), call(660), call(700, CallStatus.RANG), call(800, CallStatus.MISSED)))
        val tomorrow = CallBook(at(1, 0).epochDay, listOf(call(600)))
        val result = NotificationRules.toSchedule(listOf(today, tomorrow), at(0, 10))
        assertEquals(listOf(660, 600), result.map { it.second.minute })
        assertEquals(listOf(today.epochDay, tomorrow.epochDay), result.map { it.first.epochDay })
    }

    @Test fun theNextWorkdaySkipsDaysOff() {
        assertEquals(Weekday.MON, s.nextWorkdayAfter(at(4, 18))!!.weekday) // Friday evening -> Monday
        assertEquals(Weekday.TUE, s.nextWorkdayAfter(at(0, 8))!!.weekday)
        assertEquals(at(1, 0).epochDay, s.nextWorkdayAfter(at(0, 20))!!.epochDay)
        assertEquals(Weekday.MON, s.nextWorkdayAfter(at(5, 9))!!.weekday)
    }

    @Test fun withNoWorkdaysThereIsNoNextOne() {
        assertNull(WorkSchedule(daysOff = Weekday.entries.toSet()).nextWorkdayAfter(at(0, 9)))
    }
}
