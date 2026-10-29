package com.avabuilds.packetrunner.network

/** The 32 bits of an address as text, with a dot after every 8: "11000000.10101000.00001010.10001101". */
fun Ipv4.toBitString(): String = toBinaryString()

/**
 * Everything a subnet mask tells about an address: its network, the broadcast address and how many hosts fit.
 * The first [prefix] bits are the network part; the rest are for hosts.
 */
data class SubnetInfo(val address: Ipv4, val prefix: Int) {
    init { require(prefix in 0..32) }

    val mask: Ipv4 get() = Ipv4.maskFromPrefix(prefix)
    val network: Ipv4 get() = address and mask

    /** The network number with every host bit set to 1: the "everybody here" address. */
    val broadcast: Ipv4 get() = Ipv4(network.octets.zip(mask.octets) { n, m -> n or (m xor 255) })

    val hostBits: Int get() = 32 - prefix

    /** How many addresses can be given to devices: all but the network and broadcast addresses (a /31 or /32 is special). */
    val usableHosts: Long get() = when (prefix) {
        32 -> 1L
        31 -> 2L
        else -> (1L shl hostBits) - 2
    }

    val firstHost: Ipv4 get() = if (prefix >= 31) network else plusOne(network)
    val lastHost: Ipv4 get() = if (prefix >= 31) broadcast else minusOne(broadcast)

    fun contains(other: Ipv4): Boolean = (other and mask) == network

    private fun plusOne(ip: Ipv4): Ipv4 = step(ip, +1)
    private fun minusOne(ip: Ipv4): Ipv4 = step(ip, -1)

    private fun step(ip: Ipv4, delta: Int): Ipv4 {
        var value = ip.octets.fold(0L) { acc, o -> acc * 256 + o } + delta
        val out = MutableList(4) { 0 }
        for (i in 3 downTo 0) { out[i] = (value % 256).toInt(); value /= 256 }
        return Ipv4(out)
    }
}
