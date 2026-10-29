package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class MobileIpLogicTest {
    private fun ip(t: String) = Ipv4.parse(t)
    private val home = ip("10.1.0.77")
    private val careOf = ip("192.0.2.45")
    private val ha = ip("10.1.0.1")
    private val peer = ip("203.0.113.10")
    private val message = IpPacket(peer, home)

    @Test fun aBindingIsActiveUntilItsLifetimeRunsOut() {
        val b = Binding(home, careOf, registeredAt = 100, lifetimeSec = 600)
        assertTrue(b.isActive(100))
        assertTrue(b.isActive(699))
        assertFalse(b.isActive(700))
        assertEquals(50, b.ageSec(150))
    }

    @Test fun theHomeAgentTunnelsPacketsForARegisteredDeviceToItsCareOfAddress() {
        val agent = HomeAgent(ha).also { it.register(Binding(home, careOf, 0, 600)) }
        val result = agent.deliver(message, now = 10) as MobileDelivery.Tunneled
        assertEquals(IpPacket(ha, careOf, inner = message), result.outer)
        assertEquals(message, MobileIp.decapsulate(result.outer)) // the foreign agent gets the original back
    }

    @Test fun aPacketForAnUnregisteredOrExpiredDeviceIsLostForTheRightReason() {
        val agent = HomeAgent(ha)
        assertEquals(MobileDelivery.Lost(LostReason.NEVER_REGISTERED), agent.deliver(message, 0))
        agent.register(Binding(home, careOf, 0, 600))
        assertEquals(MobileDelivery.Lost(LostReason.REGISTRATION_EXPIRED), agent.deliver(message, 900))
        agent.deregister(home)
        assertEquals(MobileDelivery.Lost(LostReason.NEVER_REGISTERED), agent.deliver(message, 10))
    }

    @Test fun registeringAgainReplacesTheOldBinding() {
        val agent = HomeAgent(ha)
        agent.register(Binding(home, careOf, 0, 600))
        agent.register(Binding(home, ip("198.51.100.8"), 700, 600))
        assertEquals(1, agent.bindings.size)
        assertTrue(agent.deliver(message, 800) is MobileDelivery.Tunneled)
    }

    private val foreign = SubnetInfo(ip("192.0.2.1"), 24)

    @Test fun aNetworkLetsOnlyItsOwnAddressesLeave() {
        assertTrue(IngressFilter.allowsLeaving(IpPacket(careOf, peer), foreign))
        assertFalse(IngressFilter.allowsLeaving(IpPacket(home, peer), foreign))
    }

    @Test fun aDirectReplyWithTheHomeAddressIsDroppedButAReverseTunnelPasses() {
        val direct = MobileReply.send(home, careOf, peer, ha, foreign, reverseTunnel = false) as ReplyRoute.Direct
        assertEquals(IpPacket(home, peer), direct.packet)
        assertFalse(direct.passed)
        val reverse = MobileReply.send(home, careOf, peer, ha, foreign, reverseTunnel = true) as ReplyRoute.ReverseTunnel
        assertTrue(reverse.passed)
        assertEquals(careOf, reverse.outer.src)
        assertEquals(ha, reverse.outer.dst)
        assertEquals(IpPacket(home, peer), MobileIp.decapsulate(reverse.outer)) // the home agent forwards the original
    }

    @Test fun theCaseScansMatchTheLogic() {
        val e = MobileIpCases.expired
        assertEquals("expired", e.scans[0].rows[0][3])
        assertTrue(e.scans[1].rows[1][1].contains("expired"))
        assertEquals("10.1.0.1 → 192.0.2.45 (carries the message)", e.afterFix.rows[1][1])
        assertEquals("203.0.113.10 → 10.1.0.77", e.afterFix.rows[2][1])
        val f = MobileIpCases.filtered
        assertEquals("dropped", f.scans[1].rows[0][2])
        assertEquals("passes", f.afterFix.rows[0][2])
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("van_expired", "van_filtered")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
