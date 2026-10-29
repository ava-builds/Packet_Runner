package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class RoutingPuzzleTest {
    private val town = RoutingPuzzles.all[0]
    private val campus = RoutingPuzzles.all[1]

    @Test fun everyPuzzleStartsBroken() {
        RoutingPuzzles.all.forEach { assertFalse(it.id, it.isSolved(it.startPrefixes())) }
    }

    @Test fun theIntendedMasksSolveTheTownRouter() {
        assertTrue(town.isSolved(listOf(24, 25, 24)))
    }

    @Test fun theIntendedMasksSolveTheCampusRouter() {
        assertTrue(campus.isSolved(listOf(22, 21, 26)))
    }

    @Test fun theStartingTownTableSendsPrinterMailToTheInternetAndCafeMailToTheInternetToo() {
        // /24 for the printer room never matches (its network has host bits), /28 for the library only reaches .0-.15
        val results = town.send(town.startPrefixes())
        assertEquals("Internet", results.first { it.packet.dest == Ipv4.parse("192.168.10.141") }.actual)
        assertEquals("Internet", results.first { it.packet.dest == Ipv4.parse("192.168.20.77") }.actual)
    }

    @Test fun aLongerPrefixBeatsAShorterOneSoThePrinterRoomWinsInsideTheLibraryNetwork() {
        val packet = Ipv4.parse("192.168.10.141")
        val table = town.table(listOf(24, 25, 24))
        assertEquals("Printer room", Forwarding.lookup(table, packet)!!.nextHop)
        // the library row matches the same packet, it just loses
        assertTrue(table[0].matches(packet))
    }

    @Test fun aMaskThatIsTooLongNeverMatches() {
        // 192.168.20.0 with a /16 mask: the destination AND the mask is 192.168.0.0, never the row's network
        val results = town.send(listOf(24, 25, 16))
        assertEquals("Internet", results.first { it.packet.dest == Ipv4.parse("192.168.20.77") }.actual)
        assertNull(results.first { it.packet.dest == Ipv4.parse("192.168.20.77") }.row)
    }

    @Test fun anAddressThatMatchesNothingGoesOutTheDefaultRoute() {
        val results = campus.send(listOf(22, 21, 26))
        val outside = results.first { it.packet.dest == Ipv4.parse("172.16.20.1") }
        assertEquals("Internet", outside.actual)
        assertTrue(outside.ok)
    }

    @Test fun everyExpectedHopIsARealRowOrTheInternet() {
        RoutingPuzzles.all.forEach { p ->
            val hops = p.rows.map { it.nextHop } + p.defaultHop
            assertTrue(p.id, p.packets.all { it.expected in hops })
        }
    }

    @Test fun everyPuzzleHasAtLeastOnePacketPerRowAndOneForTheInternet() {
        RoutingPuzzles.all.forEach { p ->
            p.rows.forEach { row -> assertTrue("${p.id} ${row.label}", p.packets.any { it.expected == row.nextHop }) }
            assertTrue(p.packets.any { it.expected == p.defaultHop })
        }
    }

    @Test fun nothingBetweenTheMaskLimitsAcceptsAWrongCombinationBySilentLuck() {
        // brute force: every combination of masks that solves the campus router is the intended one
        val range = RoutingPuzzle.MIN_PREFIX..RoutingPuzzle.MAX_PREFIX
        val solutions = range.flatMap { a -> range.flatMap { b -> range.map { c -> listOf(a, b, c) } } }.filter { campus.isSolved(it) }
        assertEquals(listOf(listOf(22, 21, 26)), solutions)
    }

    @Test fun theTownRouterCanBeSolvedSeveralWaysButTheTargetMasksStayInKnownBounds() {
        val range = RoutingPuzzle.MIN_PREFIX..RoutingPuzzle.MAX_PREFIX
        val solutions = range.flatMap { a -> range.flatMap { b -> range.map { c -> listOf(a, b, c) } } }.filter { town.isSolved(it) }
        assertTrue(solutions.isNotEmpty())
        // the printer room needs exactly /25 (its addresses run .128 to .255); the library and cafe have room to move
        assertTrue(solutions.all { it[1] == 25 && it[0] in 23..27 && it[2] in 22..24 })
        assertTrue(listOf(24, 25, 24) in solutions)
    }

    @Test fun starsDependOnHowManyTimesYouTested() {
        assertEquals(3, town.stars(1))
        assertEquals(3, town.stars(2))
        assertEquals(2, town.stars(3))
        assertEquals(2, town.stars(4))
        assertEquals(1, town.stars(5))
        assertEquals(20, town.tip(3))
        assertEquals(0, town.tip(1))
    }

    @Test fun theBinaryExplanationShowsTheAndOperation() {
        val route = Route(Ipv4.parse("192.168.10.128"), 25, "Printer room")
        val e = explainLookup(Ipv4.parse("192.168.10.141"), route)
        assertEquals("11000000.10101000.00001010.10001101", e.dest)
        assertEquals("11111111.11111111.11111111.10000000", e.mask)
        assertEquals("11000000.10101000.00001010.10000000", e.and)
        assertEquals(e.and, e.network)
        assertTrue(e.matches)
    }

    @Test fun differentJobsGetDifferentRouters() {
        assertTrue(RoutingPuzzles.forJob(0).id != RoutingPuzzles.forJob(1).id)
        assertEquals(RoutingPuzzles.forJob(0).id, RoutingPuzzles.forJob(RoutingPuzzles.all.size).id)
    }

    @Test fun routingJobsTeachTheForwardingConcepts() {
        town.concepts.let {
            assertTrue(Concepts.FORWARDING_TABLE in it && Concepts.SUBNET_MASK in it && Concepts.LONGEST_PREFIX in it && Concepts.DEFAULT_ROUTE in it)
        }
    }
}
