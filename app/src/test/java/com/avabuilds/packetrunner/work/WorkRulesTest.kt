package com.avabuilds.packetrunner.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkRulesTest {
    private val s = WorkSchedule() // 09:00-18:00, lunch 12:00-13:00, Sat and Sun off
    private fun at(day: Int, h: Int, m: Int = 0) = WallTime.week(day, h, m)
    private fun clockedIn(day: Int, h: Int = 9) = WorkLog(at(day, h).epochDay, clockInMinute = h * 60)

    // ---- time ----
    @Test fun theWeekStartsOnMonday() {
        assertEquals(Weekday.MON, WallTime.week(0, 9).weekday)
        assertEquals(Weekday.FRI, WallTime.week(4, 9).weekday)
        assertEquals(Weekday.SUN, WallTime.week(6, 9).weekday)
        assertEquals(Weekday.MON, WallTime.week(7, 9).weekday)
    }

    @Test fun epochDayZeroWasAThursday() {
        assertEquals(Weekday.THU, WallTime(0, 0).weekday)
    }

    @Test fun addingSecondsCrossesMidnight() {
        val t = WallTime.week(0, 23, 59).plusSeconds(120)
        assertEquals(WallTime.week(1, 0, 1), t)
        assertEquals(WallTime.week(0, 23, 59), t.plusSeconds(-120))
    }

    @Test fun secondsSinceCountsAcrossDays() {
        assertEquals(86_400L + 3600, WallTime.week(1, 10).secondsSince(WallTime.week(0, 9)))
    }

    // ---- state ----
    @Test fun beforeClockInYouAreOffDutyEvenDuringWorkHours() {
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, WorkLog(at(0, 10).epochDay), at(0, 10)))
    }

    @Test fun afterClockInDuringHoursYouAreWorking() {
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, clockedIn(0), at(0, 10)))
    }

    @Test fun lunchIsLunchAndTheBreakEndsAtOneOClock() {
        val log = clockedIn(0)
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, log, at(0, 11, 59)))
        assertEquals(WorkState.LUNCH, WorkRules.stateAt(s, log, at(0, 12)))
        assertEquals(WorkState.LUNCH, WorkRules.stateAt(s, log, at(0, 12, 59)))
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, log, at(0, 13)))
    }

    @Test fun afterTheEndOfWorkYouAreOffDuty() {
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, clockedIn(0), at(0, 17, 59)))
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, clockedIn(0), at(0, 18)))
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, clockedIn(0), at(0, 23)))
    }

    @Test fun clockingOutEndsTheShiftEarly() {
        val log = clockedIn(0).copy(clockOutMinute = 15 * 60)
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, log, at(0, 14, 59)))
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, log, at(0, 15)))
    }

    @Test fun weekendsAreDaysOffWhateverTheLog() {
        assertEquals(WorkState.DAY_OFF, WorkRules.stateAt(s, clockedIn(5, 10), at(5, 10)))
        assertEquals(WorkState.DAY_OFF, WorkRules.stateAt(s, clockedIn(6, 10), at(6, 14)))
    }

    @Test fun otherDaysOffWork() {
        val tue = WorkSchedule(daysOff = setOf(Weekday.TUE, Weekday.WED))
        assertEquals(WorkState.DAY_OFF, WorkRules.stateAt(tue, clockedIn(1), at(1, 10)))
        assertEquals(WorkState.WORKING, WorkRules.stateAt(tue, clockedIn(5, 10), at(5, 11)))
    }

    @Test fun yesterdaysLogDoesNotCountToday() {
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, clockedIn(0), at(1, 10)))
    }

    @Test fun customHoursWork() {
        val late = WorkSchedule(startMinute = 10 * 60, endMinute = 18 * 60)
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(late, clockedIn(0, 9), at(0, 9, 30)))
        assertEquals(WorkState.WORKING, WorkRules.stateAt(late, clockedIn(0, 10), at(0, 10, 30)))
    }

    // ---- clocking in and out ----
    @Test fun openingTheAppDuringWorkHoursClocksYouInAtThatMoment() {
        val log = WorkRules.autoClockIn(s, WorkLog(0), at(0, 10, 30))
        assertEquals(10 * 60 + 30, log.clockInMinute)
        assertEquals(at(0, 0).epochDay, log.epochDay)
    }

    @Test fun openingTheAppOutsideHoursOrOnADayOffDoesNotClockYouIn() {
        assertNull(WorkRules.autoClockIn(s, WorkLog(0), at(0, 8, 30)).clockInMinute)
        assertNull(WorkRules.autoClockIn(s, WorkLog(0), at(0, 18, 0)).clockInMinute)
        assertNull(WorkRules.autoClockIn(s, WorkLog(0), at(5, 10)).clockInMinute)
    }

    @Test fun autoClockInDoesNotOverrideAnEarlierClockInOrACockOut() {
        val log = clockedIn(0, 9)
        assertEquals(9 * 60, WorkRules.autoClockIn(s, log, at(0, 11)).clockInMinute)
        val out = log.copy(clockOutMinute = 12 * 60 + 30)
        assertEquals(out, WorkRules.autoClockIn(s, out, at(0, 13)))
    }

    @Test fun autoClockInStartsAFreshLogOnANewDay() {
        val log = WorkRules.autoClockIn(s, clockedIn(0), at(1, 9, 5))
        assertEquals(at(1, 0).epochDay, log.epochDay)
        assertEquals(9 * 60 + 5, log.clockInMinute)
    }

    @Test fun clockInAndOutByHand() {
        val inLog = WorkRules.clockIn(s, WorkLog(0), at(0, 9, 10))
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, inLog, at(0, 9, 11)))
        val outLog = WorkRules.clockOut(inLog, at(0, 16))
        assertEquals(WorkState.OFF_DUTY, WorkRules.stateAt(s, outLog, at(0, 16)))
        // back to work after clocking out
        val again = WorkRules.clockIn(s, outLog, at(0, 16, 30))
        assertNull(again.clockOutMinute)
        assertEquals(WorkState.WORKING, WorkRules.stateAt(s, again, at(0, 16, 31)))
    }

    @Test fun youCannotClockInOnADayOffOrClockOutWithoutClockingIn() {
        assertEquals(WorkLog(0), WorkRules.clockIn(s, WorkLog(0), at(5, 10)))
        assertNull(WorkRules.clockOut(WorkLog(at(0, 9).epochDay), at(0, 10)).clockOutMinute)
    }

    @Test fun theButtonsAppearOnlyWhenTheyMakeSense() {
        val fresh = WorkLog(at(0, 9).epochDay)
        assertTrue(WorkRules.canClockIn(s, fresh, at(0, 8)))
        assertFalse(WorkRules.canClockOut(s, fresh, at(0, 8)))
        assertTrue(WorkRules.canClockOut(s, clockedIn(0), at(0, 10)))
        assertFalse(WorkRules.canClockIn(s, clockedIn(0), at(0, 10)))
        assertFalse(WorkRules.canClockIn(s, fresh, at(5, 10)))
        assertFalse(WorkRules.canClockIn(s, fresh, at(0, 19)))
    }

    // ---- next up and helpers ----
    @Test fun nextUpTellsWhatComesNext() {
        assertEquals("Lunch at 12:00", WorkRules.nextUp(s, clockedIn(0), at(0, 10)))
        assertEquals("Back to work at 13:00", WorkRules.nextUp(s, clockedIn(0), at(0, 12, 20)))
        assertEquals("Work ends at 18:00", WorkRules.nextUp(s, clockedIn(0), at(0, 14)))
        assertEquals("Work starts at 09:00", WorkRules.nextUp(s, WorkLog(0), at(0, 8)))
        assertEquals("Back to work on Mon", WorkRules.nextUp(s, WorkLog(0), at(5, 10)))
        assertEquals("Back to work on Tue", WorkRules.nextUp(s, WorkLog(0), at(0, 20)))
        assertEquals("Back to work on Mon", WorkRules.nextUp(s, WorkLog(0), at(4, 20)))
    }

    @Test fun daylightIsFromSixToSeven() {
        assertFalse(WorkRules.isDaylight(5 * 60 + 59))
        assertTrue(WorkRules.isDaylight(6 * 60))
        assertTrue(WorkRules.isDaylight(18 * 60 + 59))
        assertFalse(WorkRules.isDaylight(19 * 60))
    }

    // ---- schedule validation ----
    @Test fun theDefaultScheduleIsValid() {
        assertNull(s.problem())
        assertEquals(8 * 60, s.workMinutesPerDay)
    }

    @Test fun badSchedulesAreRefusedWithAMessage() {
        assertNotNull(s.copy(endMinute = 8 * 60).problem())
        assertNotNull(s.copy(endMinute = 11 * 60).problem())
        assertNotNull(s.copy(lunchStartMinute = 9 * 60 + 30).problem())
        assertNotNull(s.copy(lunchStartMinute = 17 * 60).problem())
        assertNotNull(s.copy(daysOff = setOf(Weekday.SUN)).problem())
        assertNotNull(s.copy(daysOff = setOf(Weekday.MON, Weekday.TUE, Weekday.WED)).problem())
        assertNull(s.copy(startMinute = 10 * 60).problem())
    }

    // ---- demo mode ----
    @Test fun demoTimeRunsSixtyTimesFaster() {
        val real = FakeTimeSource(WallTime.week(0, 9))
        val demo = ScaledTimeSource(real, anchorReal = real.now(), anchorGame = WallTime.week(0, 9))
        real.time = real.time.plusSeconds(60) // one real minute
        assertEquals(WallTime.week(0, 10), demo.now()) // one demo hour
        real.time = real.time.plusSeconds(60 * 9) // nine more real minutes
        assertEquals(WallTime.week(0, 19), demo.now())
    }

    @Test fun demoStartsExactlyWhereItWasToldAndCanCrossDays() {
        val real = FakeTimeSource(WallTime.week(2, 14))
        val demo = ScaledTimeSource(real, real.now(), WallTime.week(4, 17, 30))
        assertEquals(WallTime.week(4, 17, 30), demo.now())
        real.time = real.time.plusSeconds(60 * 60) // an hour of real time is 60 demo hours
        assertEquals(WallTime.week(4, 17, 30).plusSeconds(60L * 3600), demo.now())
    }

    @Test fun theSystemClockGivesAPlausibleTime() {
        val t = SystemTimeSource().now()
        assertTrue(t.epochDay > 20_000)
        assertTrue(t.secondOfDay in 0 until 86_400)
    }

    @Test fun minutesAreWrittenAsClockTimes() {
        assertEquals("09:05", formatMinuteOfDay(9 * 60 + 5))
        assertEquals("18:00", formatMinuteOfDay(18 * 60))
    }
}
