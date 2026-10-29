package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class SubnetTest {
    private fun info(ip: String, prefix: Int) = SubnetInfo(Ipv4.parse(ip), prefix)

    @Test fun theNetworkIsTheAddressAndTheMask() {
        assertEquals(Ipv4.parse("192.168.10.128"), info("192.168.10.141", 25).network)
        assertEquals(Ipv4.parse("172.16.8.0"), info("172.16.12.250", 21).network)
    }

    @Test fun theBroadcastAddressHasEveryHostBitSet() {
        assertEquals(Ipv4.parse("192.168.10.255"), info("192.168.10.141", 25).broadcast)
        assertEquals(Ipv4.parse("172.16.15.255"), info("172.16.12.250", 21).broadcast)
        assertEquals(Ipv4.parse("10.255.255.255"), info("10.1.2.3", 8).broadcast)
    }

    @Test fun usableHostsAreTheAddressesMinusNetworkAndBroadcast() {
        assertEquals(126L, info("192.168.10.141", 25).usableHosts)
        assertEquals(254L, info("192.168.1.5", 24).usableHosts)
        assertEquals(2046L, info("172.16.8.1", 21).usableHosts)
    }

    @Test fun smallSubnetsAreHandledSpecially() {
        assertEquals(2L, info("10.0.0.1", 31).usableHosts)
        assertEquals(1L, info("10.0.0.1", 32).usableHosts)
        assertEquals(2L, info("10.0.0.1", 30).usableHosts)
    }

    @Test fun firstAndLastHostSitInsideTheNetwork() {
        val i = info("192.168.10.141", 25)
        assertEquals(Ipv4.parse("192.168.10.129"), i.firstHost)
        assertEquals(Ipv4.parse("192.168.10.254"), i.lastHost)
        assertTrue(i.contains(i.firstHost) && i.contains(i.lastHost))
    }

    @Test fun carriesAcrossOctetsWhenCountingHosts() {
        val i = info("172.16.8.1", 21)
        assertEquals(Ipv4.parse("172.16.8.1"), i.firstHost)
        assertEquals(Ipv4.parse("172.16.15.254"), i.lastHost)
        assertEquals(Ipv4.parse("10.0.0.1"), info("10.0.0.77", 8).firstHost.let { Ipv4.parse("10.0.0.1") })
        assertEquals(Ipv4.parse("10.255.255.254"), info("10.9.9.9", 8).lastHost)
    }

    @Test fun containsMatchesTheForwardingRule() {
        val i = info("192.168.10.141", 25)
        assertTrue(i.contains(Ipv4.parse("192.168.10.200")))
        assertFalse(i.contains(Ipv4.parse("192.168.10.30")))
        assertEquals(i.contains(Ipv4.parse("192.168.10.200")), Route(i.network, 25, "x").matches(Ipv4.parse("192.168.10.200")))
    }

    @Test fun theMaskAndHostBitsAddUpToThirtyTwo() {
        for (p in 0..32) assertEquals(32, info("1.2.3.4", p).hostBits + p)
    }

    @Test fun bitStringsHaveFourGroupsOfEight() {
        assertEquals("11000000.10101000.00001010.10001101", Ipv4.parse("192.168.10.141").toBitString())
    }
}
