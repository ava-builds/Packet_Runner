package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.game.SolvedRecord
import com.avabuilds.packetrunner.game.Upgrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekRulesTest {
    private val s = WorkSchedule()
    private fun rec(payout: Int) = SolvedRecord(1, RequestType.FIX, "cafe", payout)
    private val week0 = WeeklyTopics.weekOf(WallTime.week(0, 9))
    private val start = WeekTracker(week0, WeekSnapshot(solvedCount = 2, heartsTotal = 3, xp = 100))
    private val solved = listOf(rec(100), rec(100), rec(150), rec(200))

    @Test fun theSummaryCountsOnlyWhatHappenedThisWeek() {
        val p = WeekRules.summary(start, solved, WeekSnapshot(4, 7, 230))
        assertEquals(2, p.callsSolved)
        assertEquals(350, p.moneyEarned)
        assertEquals(4, p.heartsGained)
        assertEquals(130, p.xpGained)
        assertEquals(week0, p.week)
    }

    @Test fun theBonusGrowsWithTheCallsAndIsZeroForAnEmptyWeek() {
        assertEquals(0, WeekRules.bonusFor(0))
        assertEquals(120, WeekRules.bonusFor(2))
        assertTrue(WeekRules.bonusFor(5) > WeekRules.bonusFor(2))
    }

    @Test fun fridayIsTheLastWorkdayWithTheDefaultWeek() {
        assertTrue(WeekRules.isLastWorkday(s, WallTime.week(4, 18)))
        assertFalse(WeekRules.isLastWorkday(s, WallTime.week(3, 18)))
        assertFalse(WeekRules.isLastWorkday(s, WallTime.week(5, 12))) // Saturday is a day off
    }

    @Test fun theLastWorkdayMovesWithTheDaysOff() {
        val thuFri = s.copy(daysOff = setOf(Weekday.THU, Weekday.FRI))
        assertTrue(WeekRules.isLastWorkday(thuFri, WallTime.week(6, 18))) // Sunday is now the last workday
        assertFalse(WeekRules.isLastWorkday(thuFri, WallTime.week(2, 18)))
        val sunMon = s.copy(daysOff = setOf(Weekday.SUN, Weekday.MON))
        assertTrue(WeekRules.isLastWorkday(sunMon, WallTime.week(5, 18))) // Saturday
    }

    @Test fun paydayComesOnceAtTheEndOfTheFridayShift() {
        val p = WeekRules.paydayAtClockOut(start, s, WallTime.week(4, 18), solved, WeekSnapshot(4, 7, 230))
        assertNotNull(p)
        val paid = start.copy(paidWeek = week0)
        assertNull(WeekRules.paydayAtClockOut(paid, s, WallTime.week(4, 18), solved, WeekSnapshot(4, 7, 230)))
    }

    @Test fun thereIsNoPaydayOnOtherDaysOrInAnotherWeek() {
        assertNull(WeekRules.paydayAtClockOut(start, s, WallTime.week(3, 18), solved, WeekSnapshot(4, 7, 230)))
        assertNull(WeekRules.paydayAtClockOut(start, s, WallTime.week(11, 18), solved, WeekSnapshot(4, 7, 230)))
    }

    @Test fun aPaydayMissedBecauseTheAppWasClosedComesOnTheNextWeek() {
        val late = WeekRules.missedPayday(start, week0 + 1, solved, WeekSnapshot(4, 7, 230))
        assertNotNull(late)
        assertEquals(2, late!!.callsSolved)
    }

    @Test fun nothingIsDueForAPaidWeekAnEmptyWeekOrTheSameWeek() {
        assertNull(WeekRules.missedPayday(start.copy(paidWeek = week0), week0 + 1, solved, WeekSnapshot(4, 7, 230)))
        assertNull(WeekRules.missedPayday(start, week0 + 1, solved.take(2), WeekSnapshot(2, 3, 100)))
        assertNull(WeekRules.missedPayday(start, week0, solved, WeekSnapshot(4, 7, 230)))
        assertNull(WeekRules.missedPayday(WeekTracker(), week0, solved, WeekSnapshot(4, 7, 230)))
    }

    @Test fun startingAWeekKeepsTheLastPaidWeek() {
        val next = WeekRules.startWeek(start.copy(paidWeek = week0), week0 + 1, WeekSnapshot(9, 9, 9))
        assertEquals(week0 + 1, next.week)
        assertEquals(week0, next.paidWeek)
        assertEquals(WeekSnapshot(9, 9, 9), next.start)
    }
}

class PaydayAndGiftTest {
    private val neighbors = Neighbors.default()
    private fun fresh() = GameEngine.newGame(neighbors, RequestGenerator(1L))

    @Test fun collectingPaydayAddsTheBonus() {
        val s = GameEngine.collectPayday(fresh(), 140)
        assertEquals(fresh().money + 140, s.money)
        assertEquals(fresh().money, GameEngine.collectPayday(fresh(), -50).money)
    }

    @Test fun aGiftCostsMoneyAndGivesAHeartOncePerNeighborPerDay() {
        val once = GameEngine.giveGift(fresh(), "hana")
        assertEquals(fresh().money - GameEngine.GIFT_COST, once.money)
        assertEquals(1, once.hearts.getValue("hana"))
        assertEquals(once, GameEngine.giveGift(once, "hana"))
        assertEquals(1, GameEngine.giveGift(once, "mina").hearts.getValue("mina"))
    }

    @Test fun youCannotGiveAGiftWithoutMoneyOrToAStranger() {
        val poor = fresh().copy(money = GameEngine.GIFT_COST - 1)
        assertEquals(poor, GameEngine.giveGift(poor, "hana"))
        assertEquals(fresh(), GameEngine.giveGift(fresh(), "nobody"))
    }

    @Test fun giftsComeBackEveryNewDay() {
        val after = GameEngine.startWorkDay(GameEngine.giveGift(fresh(), "hana"), emptyList(), newDay = true)
        assertEquals(2, GameEngine.giveGift(after, "hana").hearts.getValue("hana"))
    }

    @Test fun officeDecorationsAreCheapOneOffPurchases() {
        Upgrades.decor.forEach { id ->
            val u = Upgrades.byId(id)!!
            assertEquals(1, u.maxLevel)
            assertTrue(u.prices[0] in 50..200)
        }
        val bought = GameEngine.buy(fresh().copy(money = 500), Upgrades.DECOR_PLANT)
        assertEquals(1, bought.upgrades[Upgrades.DECOR_PLANT])
        assertEquals(bought, GameEngine.buy(bought, Upgrades.DECOR_PLANT))
    }
}

class FreeTimeTest {
    @Test fun daysOffAndTheEveningAreFreeTime() {
        assertTrue(FreeTime.isFreeTime(WorkState.DAY_OFF, canClockIn = false))
        assertTrue(FreeTime.isFreeTime(WorkState.OFF_DUTY, canClockIn = false))
    }

    @Test fun workLunchAndTheMorningBeforeClockInAreNot() {
        assertFalse(FreeTime.isFreeTime(WorkState.WORKING, canClockIn = false))
        assertFalse(FreeTime.isFreeTime(WorkState.LUNCH, canClockIn = false))
        assertFalse(FreeTime.isFreeTime(WorkState.OFF_DUTY, canClockIn = true))
    }

    @Test fun theMarketIsOnSaturdaysOnly() {
        assertTrue(FreeTime.marketOpen(WallTime.week(5, 11)))
        assertFalse(FreeTime.marketOpen(WallTime.week(4, 11)))
        assertFalse(FreeTime.marketOpen(WallTime.week(6, 11)))
    }
}
