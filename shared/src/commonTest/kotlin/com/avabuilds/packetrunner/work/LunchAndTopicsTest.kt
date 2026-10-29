package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.LunchChat
import com.avabuilds.packetrunner.game.MAX_HEARTS
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertNotEquals
import com.avabuilds.packetrunner.testing.assertNotNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class LunchAndTopicsTest {
    private val neighbors = Neighbors.default()
    private fun fresh() = GameEngine.newGame(neighbors, RequestGenerator(1L))

    // ---- lunch ----
    @Test fun lunchWithTheCafeOwnerGivesAHeartAndXpOnceADay() {
        val once = GameEngine.haveLunch(fresh(), "hana")
        assertEquals(1, once.hearts.getValue("hana"))
        assertEquals(GameEngine.LUNCH_XP, once.xp)
        val twice = GameEngine.haveLunch(once, "hana")
        assertEquals(once, twice)
    }

    @Test fun lunchCannotPushHeartsPastTheMaximum() {
        val full = fresh().let { it.copy(hearts = it.hearts + ("hana" to MAX_HEARTS)) }
        assertEquals(MAX_HEARTS, GameEngine.haveLunch(full, "hana").hearts.getValue("hana"))
    }

    @Test fun lunchComesBackTheNextWorkDay() {
        val after = GameEngine.startWorkDay(GameEngine.haveLunch(fresh(), "hana"), emptyList(), newDay = true)
        assertEquals(2, GameEngine.haveLunch(after, "hana").hearts.getValue("hana"))
    }

    @Test fun theCafeOwnerIsTheLunchPartnerAndHasSeveralThingsToSay() {
        val hana = LunchChat.partner(neighbors)!!
        assertEquals("hana", hana.id)
        assertTrue(LunchChat.linesFor(hana).size >= 3)
        assertTrue(LunchChat.linesFor(neighbors.first { it.id == "mina" }).isNotEmpty())
    }

    // ---- weekly topic ----
    @Test fun everyDayOfAWeekHasTheSameTopicAndTheNextWeekHasAnother() {
        val monday = WallTime.week(0, 9)
        for (d in 0..6) assertEquals(WeeklyTopics.forWeek(monday), WeeklyTopics.forWeek(WallTime.week(d, 20)))
        assertNotEquals(WeeklyTopics.forWeek(monday), WeeklyTopics.forWeek(WallTime.week(7, 9)))
    }

    @Test fun theTopicsFollowTheCourseInOrderAndThenStartAgain() {
        val n = WeeklyTopics.all.size
        val first = WeeklyTopics.forWeek(WallTime.week(0, 9))
        assertEquals(first, WeeklyTopics.forWeek(WallTime.week(7 * n, 9)))
        assertEquals(WeeklyTopics.all[(WeeklyTopics.all.indexOf(first) + 1) % n], WeeklyTopics.forWeek(WallTime.week(7, 9)))
    }

    @Test fun theWeekChangesOnMonday() {
        assertNotEquals(WeeklyTopics.weekOf(WallTime.week(6, 23)), WeeklyTopics.weekOf(WallTime.week(7, 0)))
        assertEquals(WeeklyTopics.weekOf(WallTime.week(0, 0)), WeeklyTopics.weekOf(WallTime.week(6, 23)))
    }

    @Test fun everyTopicIsCompleteAndBilingual() {
        WeeklyTopics.all.forEach { t ->
            assertTrue(t.title, t.title.isNotBlank() && t.korean.isNotBlank() && t.summary.isNotBlank() && t.book.isNotBlank())
            assertEquals(t.title, 3, t.points.size)
            assertNotNull(t.practice)
        }
        assertEquals(WeeklyTopics.all.size, WeeklyTopics.all.map { it.title }.toSet().size)
    }

    @Test fun theTopicsSpreadAcrossThePracticeKinds() {
        assertTrue(WeeklyTopics.all.any { it.practice is Practice.InLab })
        assertTrue(WeeklyTopics.all.any { it.practice is Practice.OnWorkbench })
        assertTrue(WeeklyTopics.all.any { it.practice is Practice.OnTheJob })
    }

    @Test fun readingTheWeeklyTopicGivesXpOnceADay() {
        val once = GameEngine.readWeeklyTopic(fresh())
        assertEquals(WeeklyTopics.CLAIM_XP, once.xp)
        assertEquals(once, GameEngine.readWeeklyTopic(once))
        val both = GameEngine.readWeeklyTopic(GameEngine.haveLunch(fresh(), "hana"))
        assertEquals(WeeklyTopics.CLAIM_XP + GameEngine.LUNCH_XP, both.xp)
    }
}
