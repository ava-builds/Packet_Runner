package com.avabuilds.packetrunner

import kotlin.math.abs

/**
 * Plain-Kotlin logic for worlds 1, 2, 4 and 5 (no Android), so it can be unit-tested.
 * The level screens compute every answer from these functions.
 */

// ---------------------------------------------------------------------------
// World 1 · Wi-Fi fog (CSMA/CA, hidden terminal, RTS/CTS)
// ---------------------------------------------------------------------------

/** A Wi-Fi node on a straight line at position [x] (meters). */
data class WifiNode(val name: String, val x: Int)

object Wifi {
    /** Frames of one protected transmission, in order. */
    val rtsCtsSequence = listOf("RTS", "CTS", "DATA", "ACK")

    /** A node hears another if it is inside radio range. */
    fun canHear(a: WifiNode, b: WifiNode, range: Int): Boolean = abs(a.x - b.x) <= range

    /** [a] and [c] are hidden terminals for receiver [b]: both reach B, but not each other. */
    fun isHiddenPair(a: WifiNode, c: WifiNode, b: WifiNode, range: Int): Boolean =
        canHear(a, b, range) && canHear(c, b, range) && !canHear(a, c, range)

    /** A node that overhears RTS/CTS stays quiet (NAV) until start + duration. */
    fun navEnd(heardAtUs: Int, durationUs: Int): Int = heardAtUs + durationUs
}

// ---------------------------------------------------------------------------
// World 2 · Switch city (learning bridge)
// ---------------------------------------------------------------------------

sealed interface BridgeAction {
    data class Forward(val port: Int) : BridgeAction
    data class Flood(val ports: List<Int>) : BridgeAction
    data object Filter : BridgeAction
}

/** The ports a bridge sends the frame out of (empty = dropped). */
fun BridgeAction.outPorts(): Set<Int> = when (this) {
    is BridgeAction.Forward -> setOf(port)
    is BridgeAction.Flood -> ports.toSet()
    BridgeAction.Filter -> emptySet()
}

/** A learning bridge / switch: learns source MACs, then forwards, floods or filters. */
class LearningBridge(private val portCount: Int) {
    private val table = linkedMapOf<String, Int>()

    val learned: Map<String, Int> get() = table

    fun receive(src: String, dst: String, inPort: Int): BridgeAction {
        table[src] = inPort
        val out = table[dst]
        return when {
            out == null -> BridgeAction.Flood((1..portCount).filter { it != inPort })
            out == inPort -> BridgeAction.Filter
            else -> BridgeAction.Forward(out)
        }
    }
}

// ---------------------------------------------------------------------------
// World 4 · Mom moved! (Mobile IP tunneling)
// ---------------------------------------------------------------------------

/** An IP packet, optionally carrying another IP packet (IP-in-IP tunneling). */
data class IpPacket(val src: Ipv4, val dst: Ipv4, val ttl: Int = 64, val inner: IpPacket? = null)

object MobileIp {
    /** The home agent wraps the packet in a new IP header addressed to the care-of address. */
    fun encapsulate(packet: IpPacket, homeAgent: Ipv4, careOf: Ipv4): IpPacket =
        IpPacket(src = homeAgent, dst = careOf, inner = packet)

    /** The foreign agent removes the outer header. */
    fun decapsulate(packet: IpPacket): IpPacket = packet.inner ?: packet
}

// ---------------------------------------------------------------------------
// World 5 · Mom's phone (TCP / UDP)
// ---------------------------------------------------------------------------

data class TcpSegment(val seq: Long, val len: Int)

object Tcp {
    /** SYN-ACK acknowledges the client's SYN, which consumes one sequence number. */
    fun synAckAckNumber(clientIsn: Long): Long = clientIsn + 1

    /** The final ACK of the handshake acknowledges the server's SYN. */
    fun finalAckNumber(serverIsn: Long): Long = serverIsn + 1

    /** ACK number = next byte expected = seq + len. */
    fun ackFor(segment: TcpSegment): Long = segment.seq + segment.len

    /** Cumulative ACK: advance through contiguous segments only. */
    fun cumulativeAck(startSeq: Long, received: List<TcpSegment>): Long {
        var expected = startSeq
        val pending = received.sortedBy { it.seq }
        for (s in pending) {
            if (s.seq == expected) expected += s.len
        }
        return expected
    }
}
