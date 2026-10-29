package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcmpLogicTest {
    private val r1 = PathHop("R1", "10.0.0.1", 1)
    private val r2 = PathHop("R2", "10.0.1.1", 5)
    private val r3 = PathHop("R3", "10.0.2.1", 10)
    private val dst = PathHop("Server", "10.0.3.9", 2)
    private val path = Path(listOf(r1, r2, r3), dst)

    @Test fun ttlOneIsDroppedByTheFirstRouterWhichSaysSo() {
        assertEquals(ProbeReply.TimeExceeded(r1, 2), Icmp.probe(path, 1))
    }

    @Test fun eachBiggerTtlReachesOneRouterFurtherAndTheRoundTripAddsUp() {
        assertEquals(ProbeReply.TimeExceeded(r2, 12), Icmp.probe(path, 2))
        assertEquals(ProbeReply.TimeExceeded(r3, 32), Icmp.probe(path, 3))
        assertEquals(ProbeReply.EchoReply(dst, 36), Icmp.probe(path, 4))
    }

    @Test fun aTtlBiggerThanThePathStillGetsAnEchoReply() {
        assertEquals(ProbeReply.EchoReply(dst, 36), Icmp.probe(path, 64))
        assertEquals(Icmp.probe(path, 64), Icmp.ping(path))
    }

    @Test fun tracerouteListsTheRoutersInOrderAndStopsAtTheDestination() {
        val lines = Icmp.traceroute(path)
        assertEquals(listOf(1, 2, 3, 4), lines.map { it.ttl })
        assertEquals(listOf("R1", "R2", "R3", "Server"), lines.map { (it.reply as? ProbeReply.TimeExceeded)?.from?.name ?: (it.reply as ProbeReply.EchoReply).from.name })
        assertTrue(lines.last().reply is ProbeReply.EchoReply)
        assertFalse(Icmp.hasLoop(lines))
    }

    @Test fun aSilentRouterShowsAsAStarButTheTraceGoesOn() {
        val silent = path.copy(hops = listOf(r1, r2.copy(answersIcmp = false), r3))
        val lines = Icmp.traceroute(silent)
        assertEquals(ProbeReply.Timeout, lines[1].reply)
        assertTrue(lines[2].reply is ProbeReply.TimeExceeded)
        assertTrue(lines.last().reply is ProbeReply.EchoReply)
        assertTrue(Icmp.ping(silent) is ProbeReply.EchoReply)
    }

    @Test fun aBlackHoleSwallowsEverythingBeyondItWhileEarlierRoutersStillAnswer() {
        val blocked = path.copy(hops = listOf(r1, r2.copy(forwards = false), r3))
        assertTrue(Icmp.probe(blocked, 1) is ProbeReply.TimeExceeded)
        assertTrue(Icmp.probe(blocked, 2) is ProbeReply.TimeExceeded) // it answers when the TTL runs out on it
        assertEquals(ProbeReply.Timeout, Icmp.probe(blocked, 3))
        assertEquals(ProbeReply.Timeout, Icmp.ping(blocked))
    }

    @Test fun aRoutingLoopAnswersWithTheSameRoutersAndPingEndsInTimeExceeded() {
        val a = PathHop("A", "1.1.1.1", 1)
        val b = PathHop("B", "2.2.2.2", 1)
        val loop = Path(List(70) { if (it % 2 == 0) a else b }, destination = null)
        val lines = Icmp.traceroute(loop, maxHops = 6)
        assertEquals(6, lines.size)
        assertTrue(Icmp.hasLoop(lines))
        assertTrue(Icmp.ping(loop) is ProbeReply.TimeExceeded) // the TTL of 64 runs out inside the loop
    }

    @Test fun aPathThatEndsWithoutADestinationTimesOut() {
        assertEquals(ProbeReply.Timeout, Icmp.probe(Path(listOf(r1), destination = null), 5))
    }

    @Test fun theCaseScansMatchTheLogic() {
        val slow = IcmpCases.slowHop
        assertEquals(5, slow.scans[0].rows.size.coerceAtMost(5))
        val rtts = IcmpCases.slowPath.route.indices.map { (Icmp.probe(IcmpCases.slowPath, it + 1) as? ProbeReply.TimeExceeded)?.rttMs ?: (Icmp.probe(IcmpCases.slowPath, it + 1) as ProbeReply.EchoReply).rttMs }
        assertTrue(rtts.zipWithNext().all { (x, y) -> y >= x })
        assertTrue(rtts[3] - rtts[2] > 150) // the overseas link is where the time jumps
        val loop = IcmpCases.loop
        assertTrue(loop.scans[0].rows.map { it[1] }.toSet().size < loop.scans[0].rows.size)
        assertEquals("* * *", IcmpCases.silent.scans[0].rows[2][1])
        assertTrue(IcmpCases.silent.scans[0].rows.last()[2].endsWith("reached"))
        assertTrue(IcmpCases.loop.afterFix.rows.last()[2].endsWith("reached"))
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("icmp_slow", "icmp_loop", "icmp_silent")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
