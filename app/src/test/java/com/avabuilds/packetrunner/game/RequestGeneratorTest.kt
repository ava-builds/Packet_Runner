package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestGeneratorTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(seed = 2026L)
    private val startConnected = neighbors.filter { it.hasInternetAtStart }.map { it.id }.toSet()

    private fun calls(day: Int, connected: Set<String> = startConnected) =
        generator.callsFor(day, neighbors, connected)

    @Test fun sameSeedAndDayGiveTheSameCalls() {
        assertEquals(calls(3), RequestGenerator(2026L).callsFor(3, neighbors, startConnected))
    }

    @Test fun differentDaysGiveDifferentCalls() {
        assertTrue(calls(1) != calls(2))
    }

    @Test fun dayOneIsScriptedAnInstallForMinaAndAFixForTheCafe() {
        val day1 = calls(1)
        assertEquals(setOf("mina", "hana"), day1.map { it.neighborId }.toSet())
        assertEquals(RequestType.INSTALL, day1.first { it.neighborId == "mina" }.type)
        assertEquals(RequestType.FIX, day1.first { it.neighborId == "hana" }.type)
    }

    @Test fun onlyNeighborsWhoMovedInCall() {
        for (day in 1..6) {
            calls(day).forEach { call ->
                val neighbor = neighbors.first { it.id == call.neighborId }
                assertTrue("${neighbor.name} calls on day $day", neighbor.movesInDay <= day)
            }
        }
    }

    @Test fun aNewNeighborAlwaysCallsOnTheirFirstDay() {
        assertTrue(calls(2).any { it.neighborId == "joon" && it.type == RequestType.INSTALL })
        assertTrue(calls(3).any { it.neighborId == "sora" && it.type == RequestType.INSTALL })
    }

    @Test fun nobodyCallsTwiceInADay() {
        for (day in 1..8) {
            val ids = calls(day).map { it.neighborId }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test fun placesWithoutInternetAskForAnInstallAndConnectedOnesNever() {
        val all = neighbors.map { it.id }.toSet()
        for (day in 1..8) {
            calls(day, connected = all).forEach { assertTrue(it.type != RequestType.INSTALL) }
            calls(day, connected = emptySet()).filter { it.buildingId != "cityhall" }.forEach { assertEquals(RequestType.INSTALL, it.type) }
        }
    }

    @Test fun cityHallAlwaysAsksForARoutingJob() {
        val all = neighbors.map { it.id }.toSet()
        val cityHallCalls = (1..8).flatMap { day -> calls(day, connected = all) }.filter { it.buildingId == "cityhall" }
        assertTrue(cityHallCalls.isNotEmpty())
        assertTrue(cityHallCalls.all { it.type == RequestType.ROUTE })
        assertTrue(cityHallCalls.all { it.reward >= RequestGenerator.REWARDS.getValue(RequestType.ROUTE) })
    }

    @Test fun numberOfCallsGrowsButStaysAtMostFour() {
        assertEquals(2, calls(1).size)
        assertEquals(2, calls(2).size)
        assertEquals(3, calls(3).size)
        assertTrue((1..12).all { calls(it).size <= 4 })
    }

    @Test fun callsAreSortedAndInsideTheCallingHours() {
        for (day in 1..8) {
            val minutes = calls(day).map { it.callMinute }
            assertEquals(minutes.sorted(), minutes)
            minutes.forEach { assertTrue(it >= Clock.CALLS_FROM && it <= Clock.CALLS_UNTIL) }
        }
    }

    @Test fun everyRequestPointsAtARealBuildingOfTheTown() {
        val town = Town.default()
        neighbors.forEach { n ->
            assertEquals(n.kind, town.building(n.buildingId).type)
        }
        for (day in 1..5) calls(day).forEach { town.building(it.buildingId) }
    }

    @Test fun rewardsFollowTheTypeAndGrowWithTheDay() {
        val install = calls(1).first { it.type == RequestType.INSTALL }
        assertEquals(200, install.reward)
        assertEquals(210, calls(2).first { it.neighborId == "joon" }.reward)
    }
}
