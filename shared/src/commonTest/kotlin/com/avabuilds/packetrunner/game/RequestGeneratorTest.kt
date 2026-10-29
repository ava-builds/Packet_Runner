package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class RequestGeneratorTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(seed = 2026L)
    private val startConnected = neighbors.filter { it.hasInternetAtStart }.map { it.id }.toSet()

    /** The calls of [day] at the player's [level]; by default the last level, when everyone lives in town. */
    private fun calls(day: Int, connected: Set<String> = startConnected, level: Int = Curriculum.maxLevel) =
        generator.callsFor(day, level, neighbors, connected)

    @Test fun sameSeedAndDayGiveTheSameCalls() {
        assertEquals(calls(3), RequestGenerator(2026L).callsFor(3, Curriculum.maxLevel, neighbors, startConnected))
    }

    @Test fun differentDaysGiveDifferentCalls() {
        assertTrue(calls(1) != calls(2))
    }

    @Test fun dayOneAtLevelOneIsTwoInstallsForTheHouses() {
        val day1 = calls(1, level = 1)
        assertEquals(2, day1.size)
        assertTrue(day1.all { it.type == RequestType.INSTALL && it.buildingId.startsWith("house") })
    }

    @Test fun onlyNeighborsWhoMovedInCall() {
        for (level in 1..Curriculum.maxLevel) for (day in 1..6) {
            calls(day, level = level).forEach { call ->
                val neighbor = neighbors.first { it.id == call.neighborId }
                assertTrue("${neighbor.name} calls at level $level", neighbor.hasMovedIn(level))
            }
        }
    }

    @Test fun theCafeStartsCallingAtLevelTwoCityHallAtFourAndTheApartmentsAtFive() {
        val all = neighbors.map { it.id }.toSet()
        fun callers(level: Int) = (1..20).flatMap { calls(it, all, level) }.map { it.buildingId }.toSet()
        assertTrue("cafe" !in callers(1))
        assertTrue("cafe" in callers(2))
        assertTrue("cityhall" !in callers(3) && "cityhall" in callers(4))
        assertTrue("apartments" !in callers(4) && "apartments" in callers(5))
    }

    @Test fun placesWithoutInternetCallBeforeConnectedOnes() {
        for (day in 1..8) {
            val types = calls(day, level = 2).map { it.type }
            // once a connected place calls, every place that still needs an install has already been called
            val firstOther = types.indexOfFirst { it != RequestType.INSTALL }
            if (firstOther >= 0) assertTrue(types.drop(firstOther).none { it == RequestType.INSTALL })
        }
    }

    @Test fun helpAboutIpAddressesOnlyComesOnceDhcpIsPartOfTheCourse() {
        val all = neighbors.map { it.id }.toSet()
        fun types(level: Int) = (2..60).flatMap { calls(it, all, level) }.map { it.type }.toSet()
        for (level in 1 until RequestGenerator.HELP_LEVEL) assertTrue("level $level", RequestType.HELP !in types(level))
        assertTrue(RequestType.HELP in types(RequestGenerator.HELP_LEVEL))
    }

    @Test fun caseJobsOnlyComeOnceACaseTopicIsOpen() {
        val all = neighbors.map { it.id }.toSet()
        fun types(level: Int) = (2..60).flatMap { calls(it, all, level) }.map { it.type }.toSet()
        assertTrue(RequestType.CASE !in types(1))
        assertTrue(RequestType.CASE in types(2))
        assertTrue(RequestType.CASE in types(Curriculum.maxLevel))
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
            calls(day, connected = emptySet()).filter { it.buildingId != "cityhall" && it.buildingId != "apartments" }.forEach { assertEquals(RequestType.INSTALL, it.type) }
        }
    }

    @Test fun theApartmentsAlwaysAskForANatJob() {
        val all = neighbors.map { it.id }.toSet()
        val calls = (1..10).flatMap { day -> calls(day, connected = all) }.filter { it.buildingId == "apartments" }
        assertTrue(calls.isNotEmpty())
        assertTrue(calls.all { it.type == RequestType.NAT })
        assertTrue(calls.all { it.callMinute >= Clock.CALLS_FROM })
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
        assertEquals(210, calls(2).first { it.type == RequestType.INSTALL }.reward)
    }
}
