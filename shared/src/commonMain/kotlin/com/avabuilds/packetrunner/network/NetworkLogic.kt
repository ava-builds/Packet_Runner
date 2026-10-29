package com.avabuilds.packetrunner.network

/**
 * Core networking logic for Packet Runner.
 * Everything here is plain Kotlin (no Android), so it can be unit-tested.
 */

/** An IPv4 address stored as four octets, e.g. 128.96.34.139 */
data class Ipv4(val octets: List<Int>) {
    init {
        require(octets.size == 4) { "An IPv4 address has exactly 4 octets" }
        require(octets.all { it in 0..255 }) { "Each octet must be between 0 and 255" }
    }

    /** Bitwise AND with a subnet mask (D & mask), octet by octet. */
    infix fun and(mask: Ipv4): Ipv4 = Ipv4(octets.zip(mask.octets) { a, m -> a and m })

    override fun toString(): String = octets.joinToString(".")

    companion object {
        fun parse(text: String): Ipv4 = Ipv4(text.trim().split(".").map { it.toInt() })

        /** /25 -> 255.255.255.128 */
        fun maskFromPrefix(prefix: Int): Ipv4 {
            require(prefix in 0..32)
            val bits = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
            return Ipv4(listOf(24, 16, 8, 0).map { shift -> ((bits shr shift) and 0xFF).toInt() })
        }
    }
}

/** 139 -> "10001011" */
fun Int.toBinary8(): String = toString(2).padStart(8, '0')

/** One row of a router's forwarding table. */
data class Route(val network: Ipv4, val prefix: Int, val nextHop: String) {
    val mask: Ipv4 get() = Ipv4.maskFromPrefix(prefix)

    /** The forwarding rule: (D & mask) == network number? */
    fun matches(dest: Ipv4): Boolean = (dest and mask) == network
}

object Forwarding {
    /**
     * Find the row whose network matches the destination.
     * If several rows match, the longest prefix (most specific) wins.
     */
    fun lookup(table: List<Route>, dest: Ipv4): Route? =
        table.filter { it.matches(dest) }.maxByOrNull { it.prefix }
}

/** Index of the first octet the mask actually "cuts" (not 255). */
fun Ipv4.firstPartialOctet(): Int = octets.indexOfFirst { it != 255 }.let { if (it == -1) 3 else it }
