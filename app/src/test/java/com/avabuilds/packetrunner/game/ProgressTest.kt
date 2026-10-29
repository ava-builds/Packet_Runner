package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(2026L)
    private fun fresh() = GameEngine.newGame(neighbors, generator)

    @Test fun everyoneStartsAtLevelOneWithNoXp() {
        assertEquals(0, fresh().xp)
        assertEquals(1, Progress.levelOf(0))
    }

    @Test fun levelsComeAtGrowingDistances() {
        assertEquals(listOf(0, 100, 300, 600, 1000), (1..5).map { Progress.xpToReach(it) })
        for (l in 1..9) assertTrue(Progress.xpToReach(l + 2) - Progress.xpToReach(l + 1) > Progress.xpToReach(l + 1) - Progress.xpToReach(l))
    }

    @Test fun theLevelChangesExactlyAtTheThreshold() {
        assertEquals(1, Progress.levelOf(99))
        assertEquals(2, Progress.levelOf(100))
        assertEquals(2, Progress.levelOf(299))
        assertEquals(3, Progress.levelOf(300))
    }

    @Test fun theBarShowsHowFarIntoTheLevelYouAre() {
        val p = Progress.progress(150)
        assertEquals(2, p.level)
        assertEquals(50, p.xpIntoLevel)
        assertEquals(200, p.xpForLevel)
        assertEquals(0.25f, p.fraction, 0.001f)
        assertEquals(0f, Progress.progress(100).fraction, 0.001f)
        assertEquals(0f, Progress.progress(0).fraction, 0.001f)
    }

    @Test fun negativeXpIsTreatedAsZero() {
        assertEquals(1, Progress.progress(-50).level)
    }

    @Test fun everyKindOfJobGivesXp() {
        RequestType.entries.forEach { assertTrue(Progress.xpForJob(it) > 0) }
    }

    @Test fun solvingAJobGivesItsXpAndAGreatJobGivesMore() {
        var s = fresh()
        while (s.ringing == null) s = GameEngine.advance(s, 1f)
        val job = checkNotNull(s.ringing)
        val ordinary = GameEngine.complete(GameEngine.answer(s), solved = true)
        assertEquals(Progress.xpForJob(job.type), ordinary.xp)
        val great = GameEngine.complete(GameEngine.answer(s), solved = true, bonusHearts = 1)
        assertEquals(Progress.xpForJob(job.type) + Progress.GREAT_WORK_XP, great.xp)
    }

    @Test fun aFailedJobGivesNoXp() {
        var s = fresh()
        while (s.ringing == null) s = GameEngine.advance(s, 1f)
        assertEquals(0, GameEngine.complete(GameEngine.answer(s), solved = false).xp)
    }

    @Test fun aLabGameGivesXpOnlyTheFirstTimeEachDay() {
        val once = GameEngine.completeLab(fresh(), "crc", 60)
        assertEquals(Progress.LAB_XP, once.xp)
        assertEquals(Progress.LAB_XP, GameEngine.completeLab(once, "crc", 60).xp)
    }

    @Test fun catchingAProblemEarlyOnTheMonitorGivesXp() {
        val call = Request(1, RequestType.FIX, "hana", "cafe", "help", 150, 600f)
        val s = fresh().copy(upcoming = listOf(call), minute = 600f - NetworkMonitor.WARNING_LEAD + 1f, xp = Progress.xpToReach(2)) // the cafe lives in town from level 2
        val result = NetworkMonitor.report(s, neighbors.first { it.id == "hana" })
        assertEquals(Progress.xpToReach(2) + Progress.EARLY_CATCH_XP, result.state.xp)
    }

    @Test fun xpSurvivesTheSaveAndTheNextDay() {
        val s = fresh().copy(xp = 345)
        assertEquals(345, SaveData.fromMap(SaveData.from(s).toMap()).xp)
        assertEquals(345, GameEngine.restore(SaveData.from(s), neighbors, generator).xp)
        assertEquals(345, GameEngine.nextDay(s, neighbors, generator).xp)
    }

    @Test fun anOldSaveWithoutXpStartsFromZeroAndACorruptNegativeValueIsFixed() {
        assertEquals(0, SaveData.fromMap(mapOf("day" to 2)).xp)
        assertEquals(0, SaveData(1, 100, emptyMap(), emptySet(), emptyList(), xp = -40).sanitized(neighbors).xp)
    }
}
