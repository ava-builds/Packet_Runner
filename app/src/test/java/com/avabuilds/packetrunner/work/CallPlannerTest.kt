package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.game.RequestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CallPlannerTest {
    private val s = WorkSchedule()
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(99L)
    private val everyone = neighbors.map { it.id }.toSet()
    private fun plan(day: Int, game: Int = 6, connected: Set<String> = everyone, schedule: WorkSchedule = s) =
        CallPlanner.plan(schedule, WallTime.week(day, 9), game, neighbors, connected, generator)

    @Test fun aWorkdayGetsThreeToFiveCalls() {
        for (d in 0 until 5) assertTrue("day $d", plan(d).calls.size in 3..5)
        assertTrue((0 until 40).map { plan(it % 5 + (it / 5) * 7).calls.size }.toSet().size >= 2)
    }

    @Test fun daysOffGetNoCalls() {
        assertTrue(plan(5).calls.isEmpty())
        assertTrue(plan(6).calls.isEmpty())
        assertTrue(plan(1, schedule = s.copy(daysOff = setOf(Weekday.TUE, Weekday.WED))).calls.isEmpty())
    }

    @Test fun callsNeverFallAtLunchOrOutsideWorkingHours() {
        for (d in listOf(0, 1, 2, 3, 4, 7, 8, 9, 10, 11, 14, 15)) {
            plan(d).calls.forEach {
                val m = it.minute
                assertTrue("$m", m >= s.startMinute + 10 && m < s.endMinute - 30)
                assertFalse("$m at lunch", m >= s.lunchStartMinute - 10 && m < s.lunchEndMinute)
            }
        }
    }

    @Test fun aPlanMadeInTheMiddleOfTheDayHasNoCallsInThePast() {
        val noon = CallPlanner.plan(s, WallTime.week(0, 14), 6, neighbors, everyone, generator, fromMinute = 14 * 60)
        assertTrue(noon.calls.isNotEmpty())
        noon.calls.forEach { assertTrue(it.minute > 14 * 60) }
    }

    @Test fun aPlanMadeAfterTheLastCallTimeIsEmpty() {
        assertTrue(CallPlanner.plan(s, WallTime.week(0, 18), 6, neighbors, everyone, generator, fromMinute = 18 * 60).calls.isEmpty())
    }

    @Test fun callsAreSpacedOutAndInOrder() {
        for (d in 0 until 5) {
            val minutes = plan(d).calls.map { it.minute }
            assertEquals(minutes.sorted(), minutes)
            minutes.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= CallPlanner.MIN_GAP) }
        }
    }

    @Test fun customHoursAreRespected() {
        val late = s.copy(startMinute = 10 * 60, endMinute = 18 * 60)
        plan(0, schedule = late).calls.forEach { assertTrue(it.minute >= 10 * 60 + 10) }
    }

    @Test fun thePlanIsTheSameForTheSameDayAndSeedAndDifferentOtherwise() {
        assertEquals(plan(2), plan(2))
        assertTrue((0 until 10).any { plan(it) != CallPlanner.plan(s, WallTime.week(it, 9), 6, neighbors, everyone, RequestGenerator(5L)) })
    }

    @Test fun onlyNeighborsWhoHaveMovedInCallAndAnUnconnectedPlaceCallsOnce() {
        val day1 = plan(0, game = 1, connected = setOf("hana"))
        day1.calls.forEach { assertTrue(it.request.neighborId in setOf("mina", "hana")) }
        assertTrue(day1.calls.count { it.request.neighborId == "mina" } <= 1)
        assertTrue(day1.calls.size >= 2)
    }

    @Test fun everyRequestMatchesTheNeighborsPlace() {
        plan(0).calls.forEach { c ->
            val n = neighbors.first { it.id == c.request.neighborId }
            assertEquals(n.buildingId, c.request.buildingId)
            assertTrue(c.request.message.isNotBlank())
            assertTrue(c.request.reward > 0)
        }
        // an unconnected place asks for an install
        val install = plan(0, game = 3, connected = setOf("hana", "dae", "rin")).calls.filter { it.request.neighborId == "mina" }
        install.forEach { assertEquals(RequestType.INSTALL, it.request.type) }
    }

    @Test fun callIdsAreUniqueWithinAndAcrossDays() {
        val ids = (0 until 5).flatMap { plan(it).calls.map { c -> c.request.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ---- the call book ----
    @Test fun markingACallAsRangRemovesItFromThePending() {
        val book = plan(0)
        val first = book.pending().first()
        val after = book.markRang(first.id)
        assertEquals(book.pending().size - 1, after.pending().size)
        assertFalse(first in after.pending())
    }

    @Test fun aCallNotRungForTooLongIsMissed() {
        val book = plan(0)
        val first = book.calls.first()
        assertEquals(CallStatus.PENDING, book.expire(first.minute + CallPlanner.LATE_MINUTES).calls.first().status)
        assertEquals(CallStatus.MISSED, book.expire(first.minute + CallPlanner.LATE_MINUTES + 1).calls.first().status)
        assertTrue(book.expire(first.minute + 100).pending().none { it.callMinute <= first.minute })
    }

    @Test fun aCallThatAlreadyRangIsNotMissedLater() {
        val book = plan(0)
        val id = book.calls.first().request.id
        val after = book.markRang(id).expire(24 * 60)
        assertEquals(CallStatus.RANG, after.calls.first { it.request.id == id }.status)
    }

    @Test fun theBookSurvivesBeingWrittenAndRead() {
        val book = plan(1).markRang(plan(1).calls.first().request.id).let { it.copy(calls = it.calls.mapIndexed { i, c -> if (i == 1) c.copy(status = CallStatus.MISSED) else c }) }
        assertEquals(book, CallBook.decode(book.encode()))
        assertEquals(CallBook(7), CallBook.decode(CallBook(7).encode()))
    }

    @Test fun brokenSavedTextNeverCrashes() {
        assertNull(CallBook.decode(null))
        assertNull(CallBook.decode(""))
        assertNull(CallBook.decode("not a day"))
        val partly = CallBook.decode("12\nbroken line\n1\tPENDING\tFIX\thana\tcafe\t150\t600\tThe wifi is slow")
        assertNotNull(partly)
        assertEquals(1, partly!!.calls.size)
        assertEquals("The wifi is slow", partly.calls[0].request.message)
    }

    @Test fun pickMinutesWorksEvenWhenTheDayIsVeryShort() {
        val m = CallPlanner.pickMinutes((600..620).toList(), 3, Random(1))
        assertTrue(m.isNotEmpty() && m.all { it in 600..620 })
        assertTrue(CallPlanner.pickMinutes(emptyList(), 3, Random(1)).isEmpty())
    }

    // ---- the game follows the work clock ----
    private fun call(id: Int, minute: Float) = Request(id, RequestType.FIX, "hana", "cafe", "hi", 150, minute)
    private fun fresh() = GameEngine.startWorkDay(GameEngine.newGame(neighbors, generator), listOf(call(1, 600f), call(2, 700f)), newDay = false)

    @Test fun theGameFollowsTheMinuteOfTheDayAndRingsADueCallWhileWorking() {
        val s1 = GameEngine.syncTo(fresh(), 599f, canRing = true)
        assertNull(s1.ringing)
        val s2 = GameEngine.syncTo(s1, 601f, canRing = true)
        assertEquals(1, s2.ringing?.id)
    }

    @Test fun nothingRingsWhileNotWorking() {
        val lunch = GameEngine.syncTo(fresh(), 650f, canRing = false)
        assertNull(lunch.ringing)
        assertEquals(2, lunch.upcoming.size)
        assertEquals(1, GameEngine.syncTo(lunch, 651f, canRing = true).ringing?.id)
    }

    @Test fun aCallNobodyAnswersBecomesAVoicemailAfterAWhile() {
        val ringing = GameEngine.syncTo(fresh(), 601f, canRing = true)
        val missed = GameEngine.syncTo(ringing, 601f + GameEngine.WORK_RING_TIMEOUT_MINUTES, canRing = true)
        assertNull(missed.ringing)
        assertEquals(listOf(1), missed.voicemails.map { it.id })
    }

    @Test fun theDayDoesNotEndByItself() {
        assertFalse(GameEngine.syncTo(fresh(), 23 * 60f, canRing = true).dayOver)
    }

    @Test fun aNewWorkDayCountsUpAndStartsClean() {
        val before = fresh().copy(todayEarned = 50, todaySolved = 2)
        val next = GameEngine.startWorkDay(before, listOf(call(9, 700f)), newDay = true)
        assertEquals(before.day + 1, next.day)
        assertEquals(0, next.todayEarned)
        assertEquals(listOf(9), next.upcoming.map { it.id })
    }

    @Test fun aCallCanBeRungRightNow() {
        val s2 = GameEngine.addCallNow(GameEngine.syncTo(fresh(), 560f, canRing = true), call(77, 0f))
        assertEquals(77, GameEngine.syncTo(s2, 560f, canRing = true).ringing?.id)
    }
}

class OpenCallTest {
    private fun call(id: Int, minute: Float) = Request(id, RequestType.FIX, "hana", "cafe", "hi", 150, minute)
    private val neighbors = Neighbors.default()
    private val gen = RequestGenerator(1L)

    @Test fun aCallOpenedFromANotificationMovesToTheFrontAndRingsAtOnce() {
        val s = GameEngine.startWorkDay(GameEngine.newGame(neighbors, gen), listOf(call(1, 600f), call(2, 700f), call(3, 800f)), newDay = false)
        val at = GameEngine.syncTo(s, 590f, canRing = true)
        val opened = GameEngine.bringCallForward(at, 3)
        assertEquals(listOf(3, 1, 2), opened.upcoming.map { it.id })
        assertEquals(3, GameEngine.syncTo(opened, 590f, canRing = true).ringing?.id)
    }

    @Test fun openingACallThatIsNotWaitingChangesNothing() {
        val s = GameEngine.startWorkDay(GameEngine.newGame(neighbors, gen), listOf(call(1, 600f)), newDay = false)
        assertEquals(s, GameEngine.bringCallForward(s, 42))
    }
}

class VoicemailBookTest {
    private fun call(id: Int, minute: Int, status: CallStatus) = PlannedCall(Request(id, RequestType.FIX, "hana", "cafe", "hi", 150, minute.toFloat()), status)
    private val book = CallBook(1, listOf(call(1, 600, CallStatus.RANG), call(2, 640, CallStatus.MISSED), call(3, 700, CallStatus.PENDING)))

    @Test fun theInboxIsTheMissedCalls() {
        assertEquals(listOf(2), book.voicemails().map { it.id })
    }

    @Test fun aCallThatRangAndIsNowInTheInboxBecomesAVoicemail() {
        val synced = book.syncVoicemails(setOf(1, 2))
        assertEquals(CallStatus.MISSED, synced.calls.first { it.request.id == 1 }.status)
        assertEquals(CallStatus.MISSED, synced.calls.first { it.request.id == 2 }.status)
    }

    @Test fun aVoicemailThatLeftTheInboxIsNoLongerMissed() {
        val synced = book.syncVoicemails(emptySet())
        assertEquals(CallStatus.RANG, synced.calls.first { it.request.id == 2 }.status)
        assertEquals(CallStatus.PENDING, synced.calls.first { it.request.id == 3 }.status)
    }

    @Test fun syncingTwiceChangesNothingMore() {
        val once = book.syncVoicemails(setOf(2))
        assertEquals(once, once.syncVoicemails(setOf(2)))
    }

    @Test fun unreturnedVoicemailsExpireAtTheEndOfTheShift() {
        val after = book.expireVoicemails()
        assertTrue(after.voicemails().isEmpty())
        assertEquals(CallStatus.EXPIRED, after.calls.first { it.request.id == 2 }.status)
        assertEquals(CallStatus.RANG, after.calls.first { it.request.id == 1 }.status)
    }

    @Test fun anExpiredVoicemailNeverComesBackAndSurvivesSaving() {
        val after = book.expireVoicemails()
        assertEquals(after, CallBook.decode(after.encode()))
        assertTrue(after.syncVoicemails(emptySet()).voicemails().isEmpty())
    }
}
