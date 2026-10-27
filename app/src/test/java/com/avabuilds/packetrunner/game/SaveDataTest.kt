package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDataTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(2026L)

    private val sample = SaveData(
        day = 3, money = 740,
        hearts = mapOf("mina" to 2, "hana" to 4),
        connected = setOf("hana", "mina"),
        solved = listOf(SolvedRecord(1, RequestType.INSTALL, "house1", 180), SolvedRecord(2, RequestType.FIX, "cafe", 160)),
    )

    @Test fun aSaveSurvivesTheTripToAMapAndBack() {
        val back = SaveData.fromMap(sample.toMap())
        assertEquals(sample, back)
    }

    @Test fun firestoreNumbersComeBackAsLongsAndStillWork() {
        val stored = sample.toMap().toMutableMap()
        stored["day"] = 3L
        stored["money"] = 740L
        stored["hearts"] = mapOf("mina" to 2L, "hana" to 4L)
        assertEquals(sample, SaveData.fromMap(stored))
    }

    @Test fun aMissingOrBrokenMapGivesAFreshStartInsteadOfACrash() {
        val fresh = SaveData.fromMap(emptyMap())
        assertEquals(1, fresh.day)
        assertEquals(START_MONEY, fresh.money)
        assertTrue(fresh.connected.isEmpty() && fresh.solved.isEmpty())
        val odd = SaveData.fromMap(mapOf("day" to "three", "solved" to listOf("x", mapOf("type" to "NOPE"))))
        assertEquals(1, odd.day)
        assertTrue(odd.solved.isEmpty())
    }

    @Test fun sanitizingKeepsNumbersInRangeAndDropsUnknownNeighbors() {
        val messy = SaveData(day = -4, money = -50, hearts = mapOf("mina" to 99, "ghost" to 3), connected = setOf("hana", "ghost"), solved = emptyList())
        val clean = messy.sanitized(neighbors)
        assertEquals(1, clean.day)
        assertEquals(0, clean.money)
        assertEquals(mapOf("mina" to MAX_HEARTS), clean.hearts)
        assertEquals(setOf("hana"), clean.connected)
    }

    @Test fun gameStateToSaveAndBackKeepsTheLastingParts() {
        var s = GameEngine.newGame(neighbors, generator).copy(money = 900, day = 2, connected = setOf("hana", "mina"), hearts = mapOf("mina" to 3, "hana" to 1, "joon" to 0, "sora" to 0))
        s = s.copy(solved = listOf(SolvedRecord(1, RequestType.INSTALL, "house1", 200)))
        val restored = GameEngine.restore(SaveData.from(s), neighbors, generator)
        assertEquals(2, restored.day)
        assertEquals(900, restored.money)
        assertEquals(s.connected, restored.connected)
        assertEquals(3, restored.hearts["mina"])
        assertEquals(s.solved, restored.solved)
        assertEquals(Clock.DAY_START, restored.minute, 0.001f)
        assertTrue(restored.upcoming.isNotEmpty())
    }

    @Test fun aFinishedDayIsSavedAsTheNextDay() {
        val over = GameEngine.newGame(neighbors, generator).copy(minute = Clock.DAY_END, day = 4)
        assertEquals(5, SaveData.from(over).day)
    }

    /** A game where the player has just answered the first call of the day. */
    private fun answeredFirstCall(): GameState {
        var s = GameEngine.newGame(neighbors, generator)
        while (s.ringing == null) s = GameEngine.advance(s, 1f)
        return GameEngine.answer(s)
    }

    @Test fun solvingAJobAddsItToTheSolvedLog() {
        var s = answeredFirstCall()
        val job = s.active!!
        s = GameEngine.complete(s, solved = true, payout = 123)
        assertEquals(listOf(SolvedRecord(1, job.type, job.buildingId, 123)), s.solved)
    }

    @Test fun aFailedJobIsNotInTheSolvedLog() {
        val s = answeredFirstCall()
        val after = GameEngine.complete(s, solved = false)
        assertTrue(after.solved.isEmpty())
        assertEquals(null, after.active)
    }

    @Test fun aRestoredGameFillsInNeighborsMissingFromTheSave() {
        val restored = GameEngine.restore(SaveData(2, 100, mapOf("mina" to 1), emptySet(), emptyList()), neighbors, generator)
        assertEquals(neighbors.map { it.id }.toSet(), restored.hearts.keys)
    }
}
