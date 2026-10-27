package com.avabuilds.packetrunner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldLogicTest {
    // ---- World 1 ----
    @Test fun hiddenTerminalWhenSendersOutOfEachOthersRange() {
        val a = WifiNode("A", 0); val b = WifiNode("B", 80); val c = WifiNode("C", 160)
        assertTrue(Wifi.isHiddenPair(a, c, b, 100))
    }

    @Test fun notHiddenWhenSendersHearEachOther() {
        val a = WifiNode("A", 0); val b = WifiNode("B", 50); val c = WifiNode("C", 90)
        assertFalse(Wifi.isHiddenPair(a, c, b, 100))
    }

    @Test fun navAndSequence() {
        assertEquals(700, Wifi.navEnd(200, 500))
        assertEquals("CTS", Wifi.rtsCtsSequence[1])
    }

    // ---- World 2 ----
    @Test fun bridgeFloodsLearnsForwardsAndFilters() {
        val br = LearningBridge(3)
        assertEquals(BridgeAction.Flood(listOf(2, 3)), br.receive("A", "C", 1))
        assertEquals(BridgeAction.Forward(1), br.receive("C", "A", 2))
        assertEquals(BridgeAction.Filter, br.receive("B", "A", 1))
        assertEquals(BridgeAction.Forward(2), br.receive("D", "C", 3))
        assertEquals(BridgeAction.Forward(3), br.receive("A", "D", 1))
        assertEquals(mapOf("A" to 1, "C" to 2, "B" to 1, "D" to 3), br.learned)
    }

    @Test fun outPortsMatchActions() {
        assertEquals(setOf(2, 3), BridgeAction.Flood(listOf(2, 3)).outPorts())
        assertEquals(setOf(1), BridgeAction.Forward(1).outPorts())
        assertEquals(emptySet<Int>(), BridgeAction.Filter.outPorts())
    }

    @Test fun switchCityFramesFollowTopology() {
        val bridge = LearningBridge(SwitchCityLevel.PORT_COUNT)
        val actions = SwitchCityLevel.frames.map { bridge.receive(it.src, it.dst, SwitchCityLevel.portOf(it.src)) }
        assertEquals(
            listOf(
                BridgeAction.Flood(listOf(2, 3)), BridgeAction.Forward(1), BridgeAction.Filter,
                BridgeAction.Forward(2), BridgeAction.Forward(3), BridgeAction.Forward(1),
            ),
            actions,
        )
    }

    // ---- World 4 ----
    @Test fun tunnelWrapsAndUnwraps() {
        val pkt = IpPacket(Ipv4.parse("1.1.1.1"), Ipv4.parse("128.96.5.9"))
        val ha = Ipv4.parse("128.96.5.1")
        val coa = Ipv4.parse("45.64.20.77")
        val tunnel = MobileIp.encapsulate(pkt, ha, coa)
        assertEquals(ha, tunnel.src)
        assertEquals(coa, tunnel.dst)
        assertEquals(pkt, MobileIp.decapsulate(tunnel))
    }

    // ---- World 5 ----
    @Test fun handshakeNumbers() {
        assertEquals(1001L, Tcp.synAckAckNumber(1000))
        assertEquals(5001L, Tcp.finalAckNumber(5000))
    }

    @Test fun ackIsCumulative() {
        val s1 = TcpSegment(1001, 500)
        val s3 = TcpSegment(2001, 500)
        assertEquals(1501L, Tcp.ackFor(s1))
        assertEquals(1501L, Tcp.cumulativeAck(1001, listOf(s1, s3)))
        assertEquals(2501L, Tcp.cumulativeAck(1001, listOf(s3, s1, TcpSegment(1501, 500))))
    }

    // ---- Levels: the hands-on puzzles must be solvable with the logic ----
    @Test fun wifiPuzzleHasHiddenAndAudiblePositions() {
        val a = WifiFogLevel.a; val b = WifiFogLevel.b
        val spots = (0..WifiFogLevel.TRACK).filter { Wifi.isHiddenPair(a, WifiNode("C", it), b, WifiFogLevel.RANGE) }
        assertTrue(spots.isNotEmpty())
        assertTrue(WifiFogLevel.HIDDEN_C in spots)
        assertTrue((0..WifiFogLevel.TRACK).any { Wifi.canHear(a, WifiNode("C", it), WifiFogLevel.RANGE) })
    }

    @Test fun transportPuzzleStallsThenRecovers() {
        val segs = TransportLevel.segments
        val arrived = segs.filterIndexed { i, _ -> i != TransportLevel.LOST_INDEX }
        val stalled = Tcp.cumulativeAck(TransportLevel.FIRST_SEQ, arrived)
        assertEquals(segs[TransportLevel.LOST_INDEX].seq, stalled)
        assertEquals(
            segs.last().seq + segs.last().len,
            Tcp.cumulativeAck(TransportLevel.FIRST_SEQ, segs),
        )
    }

    @Test fun mobileIpPuzzleAddressesAreDistinct() {
        val all = listOf(MobileIpLevel.pip, MobileIpLevel.home, MobileIpLevel.homeAgent, MobileIpLevel.careOf)
        assertEquals(4, all.toSet().size)
    }
}
