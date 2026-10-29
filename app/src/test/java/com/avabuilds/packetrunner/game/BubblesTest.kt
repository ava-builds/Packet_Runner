package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BubblesTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(2026L)
    /** A new game at the last level, so everyone lives in town. */
    private fun fresh() = GameEngine.newGame(neighbors, generator).copy(xp = Progress.xpToReach(Curriculum.maxLevel))
    private fun bubble(s: GameState, building: String) = Bubbles.of(s, neighbors).firstOrNull { it.buildingId == building }

    @Test fun onlyNeighborsWhoHaveMovedInGetABubble() {
        val level1 = Bubbles.of(fresh().copy(xp = 0), neighbors).map { it.buildingId }.toSet()
        assertEquals(setOf("house1", "house2", "house3"), level1)
        val level4 = Bubbles.of(fresh().copy(xp = Progress.xpToReach(4)), neighbors).map { it.buildingId }.toSet()
        assertTrue("cityhall" in level4 && "cafe" in level4 && "apartments" !in level4)
        val level5 = Bubbles.of(fresh().copy(xp = Progress.xpToReach(5)), neighbors).map { it.buildingId }.toSet()
        assertTrue("apartments" in level5)
    }

    @Test fun theBubbleShowsTheHeartsOfThatNeighbor() {
        val s = fresh().copy(hearts = mapOf("mina" to 3, "hana" to 5, "joon" to 0, "sora" to 0, "dae" to 0, "rin" to 0))
        assertEquals(3, bubble(s, "house1")!!.hearts)
        assertEquals(5, bubble(s, "cafe")!!.hearts)
    }

    @Test fun moodFollowsTheHeartsWhenAllIsWell() {
        assertEquals(Mood.NEUTRAL, Mood.of(0, hasProblem = false))
        assertEquals(Mood.NEUTRAL, Mood.of(1, hasProblem = false))
        assertEquals(Mood.HAPPY, Mood.of(2, hasProblem = false))
        assertEquals(Mood.HAPPY, Mood.of(3, hasProblem = false))
        assertEquals(Mood.DELIGHTED, Mood.of(4, hasProblem = false))
        assertEquals(Mood.DELIGHTED, Mood.of(5, hasProblem = false))
    }

    @Test fun aProblemMakesAnyoneWorriedWhateverTheirHearts() {
        assertEquals(Mood.WORRIED, Mood.of(5, hasProblem = true))
    }

    @Test fun theBubbleOfARingingPhoneIsMarked() {
        val call = Request(1, RequestType.FIX, "hana", "cafe", "help", 150, 600f)
        val s = fresh().copy(ringing = call, minute = 610f)
        assertTrue(bubble(s, "cafe")!!.ringing)
        assertFalse(bubble(s, "house1")!!.ringing)
    }

    @Test fun aBrokenNetworkIsAProblemAndWorriesTheNeighbor() {
        val call = Request(1, RequestType.FIX, "hana", "cafe", "help", 150, 600f)
        val s = fresh().copy(ringing = call, minute = 610f)
        val b = bubble(s, "cafe")!!
        assertEquals(Alert.PROBLEM, b.alert)
        assertEquals(Mood.WORRIED, b.mood)
    }

    @Test fun anEarlyWarningDoesNotShowUpOnTheMapBecauseThatIsWhatTheMonitorIsFor() {
        val call = Request(1, RequestType.FIX, "hana", "cafe", "help", 150, 700f)
        val s = fresh().copy(upcoming = listOf(call), minute = 700f - NetworkMonitor.WARNING_LEAD + 5f)
        assertEquals(LinkStatus.DEGRADED, NetworkMonitor.status(s, neighbors.first { it.id == "hana" }))
        assertEquals(Alert.NONE, bubble(s, "cafe")!!.alert)
    }

    @Test fun aJobYouTookOnIsMarkedEvenWhenTheNetworkIsNew() {
        val job = Request(2, RequestType.INSTALL, "mina", "house1", "hi", 200, 560f)
        val s = fresh().copy(active = job)
        assertEquals(Alert.JOB, bubble(s, "house1")!!.alert)
        assertEquals(Alert.NONE, bubble(s, "cafe")!!.alert)
    }

    @Test fun everythingIsCalmAtTheStartOfTheDay() {
        Bubbles.of(fresh(), neighbors).forEach {
            assertFalse(it.ringing)
            assertEquals(Alert.NONE, it.alert)
        }
        assertNull(bubble(fresh(), "office"))
    }
}
