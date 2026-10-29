package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import kotlin.test.Test

class DhcpPoolTest {

    @Test fun aFreshPoolHasEveryAddressFree() {
        assertEquals(10, DhcpPool(size = 10, leaseMinutes = 60).free)
    }

    @Test fun devicesThatLeftKeepTheirAddressUntilTheLeaseEnds() {
        val pool = DhcpPool(size = 8, leaseMinutes = 120, staleLeaseAges = listOf(20, 45, 70))
        assertEquals(3, pool.heldByLeftDevices)
        assertEquals(5, pool.free)
    }

    @Test fun aShorterLeaseGivesAddressesBackSooner() {
        val ages = listOf(20, 45, 70, 90, 110)
        assertEquals(3, DhcpPool(8, 120, ages).free)
        assertEquals(6, DhcpPool(8, 60, ages).free)
        assertEquals(7, DhcpPool(8, 30, ages).free)
        assertEquals(8, DhcpPool(8, 15, ages).free)
    }

    @Test fun aLeaseEndsExactlyWhenItsAgeReachesTheLeaseTime() {
        assertEquals(1, DhcpPool(2, 60, listOf(59)).heldByLeftDevices)
        assertEquals(0, DhcpPool(2, 60, listOf(60)).heldByLeftDevices)
    }

    @Test fun theFreeCountNeverGoesBelowZero() {
        assertEquals(0, DhcpPool(size = 2, leaseMinutes = 120, staleLeaseAges = listOf(1, 2, 3, 4)).free)
    }

    @Test fun leaseChoicesAreSortedFromShortToLong() {
        assertEquals(DhcpPool.LEASE_CHOICES.sorted(), DhcpPool.LEASE_CHOICES)
    }
}
