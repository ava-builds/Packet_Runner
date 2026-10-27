package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(2026L)
    private fun fresh() = GameEngine.newGame(neighbors, generator)

    /** Runs the clock in small steps until [done] is true (or the day is over). */
    private fun runUntil(start: GameState, done: (GameState) -> Boolean): GameState {
        var s = start
        while (!done(s) && !s.dayOver) s = GameEngine.advance(s, 1f)
        return s
    }

    private fun rung(): GameState = runUntil(fresh()) { it.ringing != null }

    @Test fun aNewGameStartsOnDayOneAtEightWithMoneyAndNoHearts() {
        val s = fresh()
        assertEquals(1, s.day)
        assertEquals(Clock.DAY_START, s.minute, 0.001f)
        assertEquals(START_MONEY, s.money)
        assertTrue(s.hearts.values.all { it == 0 })
        assertEquals(setOf("hana"), s.connected)
        assertEquals(2, s.upcoming.size)
        assertNull(s.ringing)
    }

    @Test fun thePhoneRingsWhenTheFirstCallIsDue() {
        val s = rung()
        val call = s.ringing!!
        assertTrue(s.minute >= call.callMinute)
        assertEquals(1, s.upcoming.size)
    }

    @Test fun nothingRingsBeforeTheFirstCallTime() {
        var s = fresh()
        val firstCall = s.upcoming.first().callMinute
        while (s.minute + Clock.MINUTES_PER_SECOND < firstCall) s = GameEngine.advance(s, 1f)
        assertNull(s.ringing)
    }

    @Test fun answeringMakesTheCallTheActiveRequestAndSilencesThePhone() {
        val s = GameEngine.answer(rung())
        assertNull(s.ringing)
        assertNotNull(s.active)
    }

    @Test fun theSecondCallWaitsWhileTheFirstOneIsBeingWorkedOn() {
        var s = GameEngine.answer(rung())
        val upcoming = s.upcoming.size
        repeat(60) { s = GameEngine.advance(s, 1f) } // 360 game minutes pass
        assertNull(s.ringing)
        assertEquals(upcoming, s.upcoming.size)
    }

    @Test fun ignoringACallCostsAHeartButNeverGoesBelowZero() {
        val ringing = rung()
        val id = ringing.ringing!!.neighborId
        val noHearts = GameEngine.ignore(ringing)
        assertEquals(0, noHearts.hearts.getValue(id))
        assertEquals(1, noHearts.todayMissed)
        assertNull(noHearts.ringing)

        val withHearts = GameEngine.ignore(ringing.copy(hearts = ringing.hearts + (id to 3)))
        assertEquals(2, withHearts.hearts.getValue(id))
    }

    @Test fun aCallThatRingsTooLongIsMissed() {
        val s = runUntil(rung()) { it.ringing == null }
        assertNull(s.ringing)
        assertEquals(1, s.todayMissed)
    }

    @Test fun solvingPaysAddsAHeartAndAnInstallConnectsTheNeighbor() {
        val answered = GameEngine.answer(runUntil(fresh()) { it.ringing?.type == RequestType.INSTALL })
        val job = answered.active!!
        val done = GameEngine.complete(answered, solved = true)
        assertEquals(START_MONEY + job.reward, done.money)
        assertEquals(1, done.hearts.getValue(job.neighborId))
        assertTrue(job.neighborId in done.connected)
        assertEquals(1, done.todaySolved)
        assertNull(done.active)
    }

    @Test fun heartsStopAtTheMaximum() {
        val answered = GameEngine.answer(rung())
        val id = answered.active!!.neighborId
        val full = answered.copy(hearts = answered.hearts + (id to MAX_HEARTS))
        assertEquals(MAX_HEARTS, GameEngine.complete(full, solved = true).hearts.getValue(id))
    }

    @Test fun failingCostsMoneyAndAHeartButNeverGoesNegative() {
        val answered = GameEngine.answer(rung())
        val id = answered.active!!.neighborId
        val failed = GameEngine.complete(answered.copy(hearts = answered.hearts + (id to 2)), solved = false)
        assertEquals(START_MONEY - FAIL_PENALTY, failed.money)
        assertEquals(1, failed.hearts.getValue(id))
        assertEquals(1, failed.todayFailed)

        val broke = GameEngine.complete(answered.copy(money = 10), solved = false)
        assertEquals(0, broke.money)
    }

    @Test fun theDayEndsAtTwentyAndUnfinishedWorkFails() {
        var s = GameEngine.answer(rung())
        while (!s.dayOver) s = GameEngine.advance(s, 1f)
        assertEquals(Clock.DAY_END, s.minute, 0.001f)
        assertNull(s.active)
        assertEquals(1, s.todayFailed)
        // time stands still once the day is over
        assertEquals(s, GameEngine.advance(s, 10f))
    }

    @Test fun nextDayResetsTimeAndCountersButKeepsMoneyAndHearts() {
        val answered = GameEngine.answer(rung())
        val solved = GameEngine.complete(answered, solved = true)
        val over = solved.copy(minute = Clock.DAY_END)
        val day2 = GameEngine.nextDay(over, neighbors, generator)
        assertEquals(2, day2.day)
        assertEquals(Clock.DAY_START, day2.minute, 0.001f)
        assertEquals(solved.money, day2.money)
        assertEquals(solved.hearts, day2.hearts)
        assertEquals(0, day2.todaySolved)
        assertFalse(day2.upcoming.isEmpty())
        assertTrue(day2.upcoming.any { it.neighborId == "joon" }) // Joon moves in on day 2
    }

    @Test fun aPayoutCanBeLowerThanTheRewardBecauseMaterialsCostMoney() {
        val answered = GameEngine.answer(rung())
        val job = answered.active!!
        val done = GameEngine.complete(answered, solved = true, payout = job.reward - 95)
        assertEquals(START_MONEY + job.reward - 95, done.money)
        assertEquals(job.reward - 95, done.todayEarned)
    }

    @Test fun aPayoutNeverGoesBelowZero() {
        val answered = GameEngine.answer(rung())
        val done = GameEngine.complete(answered, solved = true, payout = -40)
        assertEquals(START_MONEY, done.money)
        assertEquals(0, done.todayEarned)
        assertEquals(1, done.todaySolved)
    }

    @Test fun greatWorkEarnsBonusHearts() {
        val answered = GameEngine.answer(rung())
        val id = answered.active!!.neighborId
        val done = GameEngine.complete(answered, solved = true, bonusHearts = 1)
        assertEquals(2, done.hearts.getValue(id))
        // still capped at the maximum
        val almostFull = answered.copy(hearts = answered.hearts + (id to MAX_HEARTS - 1))
        assertEquals(MAX_HEARTS, GameEngine.complete(almostFull, solved = true, bonusHearts = 1).hearts.getValue(id))
    }

    @Test fun skippingToEveningEndsTheDayAndHandlesUnfinishedWork() {
        val answered = GameEngine.answer(rung())
        val evening = GameEngine.skipToEvening(answered)
        assertTrue(evening.dayOver)
        assertNull(evening.active)
        assertEquals(1, evening.todayFailed)
    }
}
