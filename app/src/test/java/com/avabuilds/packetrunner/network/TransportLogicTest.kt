package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportLogicTest {

    // ---- Mobile IP ----

    @Test fun beforeTheDriverRegistersTheHomeAgentThinksTheDriverIsHome() {
        val s = MobileIpSession.driver()
        s.moveTo(0)
        assertNull(s.binding)
        assertEquals(HomeAgentAction.DeliverLocally, s.correctAction())
    }

    @Test fun afterRegisteringAtTheCafePacketsAreTunnelledToTheCareOfAddress() {
        val s = MobileIpSession.driver()
        s.moveTo(0); s.register()
        assertEquals(Ipv4.parse("10.5.0.77"), s.binding)
        assertEquals(HomeAgentAction.Tunnel(Ipv4.parse("10.5.0.77")), s.correctAction())
    }

    @Test fun movingOnAndRegisteringAgainReplacesTheBinding() {
        val s = MobileIpSession.driver()
        s.moveTo(0); s.register()
        s.moveTo(1); s.register()
        assertEquals(HomeAgentAction.Tunnel(Ipv4.parse("172.20.3.21")), s.correctAction())
    }

    @Test fun backHomeTheBindingIsRemovedAndPacketsAreDeliveredNormally() {
        val s = MobileIpSession.driver()
        s.moveTo(0); s.register()
        s.moveTo(2); s.register()
        assertNull(s.binding)
        assertEquals(HomeAgentAction.DeliverLocally, s.correctAction())
        assertNull(s.tunnelled(Ipv4.parse("203.0.113.9")))
    }

    @Test fun theTunnelPacketWrapsTheOriginalInANewHeaderToTheCareOfAddress() {
        val s = MobileIpSession.driver()
        s.moveTo(0); s.register()
        val friend = Ipv4.parse("203.0.113.9")
        val outer = s.tunnelled(friend)!!
        assertEquals(s.homeAgent, outer.src)
        assertEquals(Ipv4.parse("10.5.0.77"), outer.dst)
        assertEquals(IpPacket(friend, s.homeAddress), outer.inner)
    }

    @Test fun theForeignAgentHandsTheOriginalPacketToTheDriver() {
        val s = MobileIpSession.driver()
        s.moveTo(1); s.register()
        val friend = Ipv4.parse("203.0.113.9")
        assertEquals(IpPacket(friend, s.homeAddress), s.delivered(friend))
    }

    @Test fun onlyTheCurrentTunnelIsRight() {
        val s = MobileIpSession.driver()
        s.moveTo(1); s.register()
        assertTrue(s.isRight(HomeAgentAction.Tunnel(Ipv4.parse("172.20.3.21"))))
        assertFalse(s.isRight(HomeAgentAction.Tunnel(Ipv4.parse("10.5.0.77"))))
        assertFalse(s.isRight(HomeAgentAction.DeliverLocally))
    }

    // ---- UDP and TCP ----

    @Test fun udpDeliversEverySegmentOnTimeExceptTheLostOnes() {
        val run = TransportSim.run(Transport.UDP, segments = 6, lost = setOf(2))
        assertEquals(1, run.missing)
        assertNull(run.fates[2].deliveredAt)
        assertEquals(1, run.worstLag)
    }

    @Test fun tcpDeliversEverythingButAlossMakesEverythingBehindItWait() {
        val run = TransportSim.run(Transport.TCP, segments = 6, lost = setOf(2))
        assertEquals(0, run.missing)
        assertTrue(run.fates[2].retransmitted)
        // segment 3 had arrived at tick 4 but waits for segment 2, which arrives at tick 2 + 4 + 1 = 7
        assertEquals(7, run.fates[3].deliveredAt)
        assertEquals(4, run.fates[3].lag)
    }

    @Test fun withoutLossTcpAndUdpBehaveTheSame() {
        val udp = TransportSim.run(Transport.UDP, 5, emptySet())
        val tcp = TransportSim.run(Transport.TCP, 5, emptySet())
        assertEquals(udp.fates.map { it.deliveredAt }, tcp.fates.map { it.deliveredAt })
    }

    @Test fun tcpDeliversInOrder() {
        val run = TransportSim.run(Transport.TCP, 10, setOf(1, 6))
        val times = run.fates.map { it.deliveredAt!! }
        assertEquals(times.sorted(), times)
    }

    @Test fun everyAppIsHappyWithItsBestTransportAndNotWithTheOther() {
        TransportApps.all.forEach { app ->
            val other = if (app.bestTransport == Transport.UDP) Transport.TCP else Transport.UDP
            assertEquals(app.name, TransportApp.Verdict.GOOD, app.judge(app.bestTransport))
            assertFalse(app.name, app.judge(other) == TransportApp.Verdict.GOOD)
        }
    }

    @Test fun theStreamStuttersOnTcpAndTheBankLosesDataOnUdp() {
        val stream = TransportApps.all.first { it.name == "Live stream" }
        val bank = TransportApps.all.first { it.name == "Bank transfer" }
        assertEquals(TransportApp.Verdict.STUTTERS, stream.judge(Transport.TCP))
        assertEquals(TransportApp.Verdict.DATA_MISSING, bank.judge(Transport.UDP))
    }

    @Test fun theGamesAreInTheLabMenu() {
        assertTrue(LabGame.MOBILE in LabGame.entries && LabGame.TRANSPORT in LabGame.entries)
        assertTrue(Concepts.MOBILE_IP in LabGame.MOBILE.concepts)
        assertTrue(Concepts.UDP_TCP in LabGame.TRANSPORT.concepts)
        assertNotNull(Concepts.UDP_TCP.book)
    }
}
