package com.avabuilds.packetrunner.network

/**
 * A DHCP server's pool of IP addresses.
 *
 * A device that joins gets an address for [leaseMinutes]. Devices that left still hold theirs
 * until the lease runs out, so a long lease on a busy network wastes addresses.
 * [staleLeaseAges] says, for each device that has gone, how many minutes ago it last talked.
 */
class DhcpPool(val size: Int, val leaseMinutes: Int, private val staleLeaseAges: List<Int> = emptyList()) {

    /** Addresses still held by devices that have already left. */
    val heldByLeftDevices: Int get() = staleLeaseAges.count { it < leaseMinutes }

    /** Addresses a newcomer can still get. */
    val free: Int get() = (size - heldByLeftDevices).coerceAtLeast(0)

    companion object {
        /** Lease times the router's settings offer, shortest first. */
        val LEASE_CHOICES = listOf(15, 30, 60, 120)
    }
}
