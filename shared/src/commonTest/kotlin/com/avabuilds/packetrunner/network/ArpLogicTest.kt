package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class ArpLogicTest {
    private val a = Iface("10.0.0.2", "aa:01")
    private val r1 = Iface("10.0.0.1", "rr:01")
    private val r2 = Iface("10.1.0.1", "rr:02")
    private val b = Iface("10.1.0.9", "bb:01")

    @Test fun theIpAddressesStayAndTheMacAddressesChangeAtEveryHop() {
        val frames = Arp.frames(a, listOf(r1 to r2), b)
        assertEquals(2, frames.size)
        assertTrue(frames.all { it.srcIp == "10.0.0.2" && it.dstIp == "10.1.0.9" })
        assertEquals(Frame(1, a.ip, b.ip, "aa:01", "rr:01"), frames[0])
        assertEquals(Frame(2, a.ip, b.ip, "rr:02", "bb:01"), frames[1])
        assertTrue(Arp.macChangesIpStays(frames))
    }

    @Test fun moreRoutersMeanMoreHopsAndStillOneIpPair() {
        val r3 = Iface("10.1.0.2", "rr:03")
        val r4 = Iface("10.2.0.1", "rr:04")
        val frames = Arp.frames(a, listOf(r1 to r2, r3 to r4), b)
        assertEquals(3, frames.size)
        assertEquals(listOf("aa:01", "rr:02", "rr:04"), frames.map { it.srcMac })
        assertEquals(listOf("rr:01", "rr:03", "bb:01"), frames.map { it.dstMac })
        assertTrue(Arp.macChangesIpStays(frames))
    }

    @Test fun framesWithTheSameMacOnTwoHopsDoNotPassTheCheck() {
        val bad = listOf(Frame(1, "x", "y", "m1", "m2"), Frame(2, "x", "y", "m1", "m3"))
        assertFalse(Arp.macChangesIpStays(bad))
        assertFalse(Arp.macChangesIpStays(listOf(Frame(1, "x", "y", "m1", "m2"), Frame(2, "x", "z", "m3", "m4"))))
    }

    @Test fun arpAsksTheNetworkOnceAndThenUsesTheCache() {
        val cache = ArpCache()
        val lan = listOf(a, r1)
        assertEquals(ArpOutcome.Learned("rr:01"), cache.resolve("10.0.0.1", lan))
        assertEquals(ArpOutcome.Cached("rr:01"), cache.resolve("10.0.0.1", lan))
        assertEquals(mapOf("10.0.0.1" to "rr:01"), cache.table)
    }

    @Test fun nobodyAnswersForAnIpThatNobodyOwns() {
        val cache = ArpCache()
        assertEquals(ArpOutcome.NoAnswer, cache.resolve("10.0.0.254", listOf(a, r1)))
        assertTrue(cache.table.isEmpty())
    }

    @Test fun aStaleEntryKeepsPointingAtTheOldMacUntilTheCacheIsCleared() {
        val cache = ArpCache(mapOf("10.0.0.1" to "old:mac"))
        val lan = listOf(a, r1)
        assertEquals(ArpOutcome.Cached("old:mac"), cache.resolve("10.0.0.1", lan))
        cache.clear()
        assertEquals(ArpOutcome.Learned("rr:01"), cache.resolve("10.0.0.1", lan))
    }

    @Test fun theHealthyTripOfTheArpJobsFollowsTheRule() {
        assertTrue(Arp.macChangesIpStays(ArpCases.healthyFrames))
        assertEquals(Arp.BROADCAST, "ff:ff:ff:ff:ff:ff")
    }
}

class CaseSessionTest {
    private val case = ArpCases.staleCache
    private val right = case.actions.indexOfFirst { it.correct }
    private val wrong = case.actions.indexOfFirst { !it.correct }

    @Test fun everyCaseHasExactlyOneRightActionConceptsAndScans() {
        CaseFiles.all.forEach {
            assertEquals(1, it.actions.count { a -> a.correct })
            assertTrue(it.scans.isNotEmpty() && it.concepts.isNotEmpty() && it.symptoms.isNotEmpty())
            assertTrue(it.concepts.all { c -> c.en.isNotBlank() && c.ko.isNotBlank() && c.explanation.isNotBlank() })
            assertTrue(it.actions.filter { a -> !a.correct }.all { a -> a.consequence.isNotBlank() })
            it.scans.forEach { s -> assertTrue(s.rows.all { row -> row.size == s.columns.size }) }
        }
    }

    @Test fun theRightActionSolvesItWithThreeStarsAndTheFullReward() {
        val s = CaseSession(case)
        assertTrue(s.choose(right)!!.solved)
        assertTrue(s.solved)
        assertEquals(3, s.stars())
        assertEquals(170 + 20, s.payout(170))
    }

    @Test fun aWrongActionCostsMoneyAndAStarButDoesNotSolve() {
        val s = CaseSession(case)
        val result = s.choose(wrong)!!
        assertFalse(result.solved)
        assertFalse(s.solved)
        assertEquals(case.actions[wrong].cost, s.spent)
        s.choose(right)
        assertEquals(2, s.stars())
        assertEquals(170 + 10 - case.actions[wrong].cost, s.payout(170))
    }

    @Test fun thePayoutNeverGoesBelowZero() {
        val s = CaseSession(case)
        repeat(5) { s.choose(wrong) }
        s.choose(right)
        assertEquals(1, s.stars())
        assertEquals(0, s.payout(10))
    }

    @Test fun anUnknownActionDoesNothingAndScansAreCounted() {
        val s = CaseSession(case)
        assertNull(s.choose(99))
        s.openScan(0); s.openScan(0); s.openScan(7)
        assertEquals(1, s.scansOpened)
    }

    @Test fun theSameCaseDoesNotComeTwiceInARow() {
        assertTrue(CaseFiles.pick(7, 0)!!.id != CaseFiles.pick(7, 1)!!.id)
        assertEquals(CaseFiles.pick(7, 0), CaseFiles.pick(7, CaseFiles.all.size * 2))
    }

    @Test fun noCaseComesBeforeACaseTopicIsOpen() {
        assertNull(CaseFiles.pick(1, 0))
        assertTrue(CaseFiles.pick(2, 0) != null)
    }
}
