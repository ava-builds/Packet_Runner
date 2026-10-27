package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogicTest {

    @Test
    fun prefixToMask() {
        assertEquals("255.255.255.128", Ipv4.maskFromPrefix(25).toString())
        assertEquals("255.255.255.0", Ipv4.maskFromPrefix(24).toString())
        assertEquals("255.255.0.0", Ipv4.maskFromPrefix(16).toString())
        assertEquals("0.0.0.0", Ipv4.maskFromPrefix(0).toString())
    }

    @Test
    fun andWithMask() {
        val d = Ipv4.parse("128.96.34.139")
        assertEquals("128.96.34.128", (d and Ipv4.maskFromPrefix(25)).toString())
    }

    @Test
    fun routeMatching() {
        val r = Route(Ipv4.parse("128.96.34.128"), 25, "if1")
        assertTrue(r.matches(Ipv4.parse("128.96.34.139")))
        assertFalse(r.matches(Ipv4.parse("128.96.34.15")))
    }

    @Test
    fun forwardingLookupForEveryRound() {
        // R2's forwarding table from the textbook example
        val table = listOf(
            Route(Ipv4.parse("128.96.34.0"), 25, "if0"),
            Route(Ipv4.parse("128.96.34.128"), 25, "if1"),
            Route(Ipv4.parse("128.96.33.0"), 24, "R3"),
        )
        assertEquals("if1", Forwarding.lookup(table, Ipv4.parse("128.96.34.139"))?.nextHop)
        assertEquals("if0", Forwarding.lookup(table, Ipv4.parse("128.96.34.15"))?.nextHop)
        assertEquals("R3", Forwarding.lookup(table, Ipv4.parse("128.96.33.200"))?.nextHop)
        assertEquals(null, Forwarding.lookup(table, Ipv4.parse("10.0.0.1")))
    }

    @Test
    fun binary() {
        assertEquals("10001011", 139.toBinary8())
    }
}
