package com.avabuilds.packetrunner.network

import kotlin.math.roundToInt

/**
 * One IP fragment of a datagram. [offsetUnits] is where its data starts in the original data, counted in
 * 8-byte units (that is how the IP header stores it), and [moreFragments] is false only for the last one.
 */
data class IpFragment(val datagramId: Int, val offsetUnits: Int, val dataLength: Int, val moreFragments: Boolean) {
    val byteOffset: Int get() = offsetUnits * 8
    val totalLength: Int get() = dataLength + Fragmentation.HEADER
    val lastByte: Int get() = byteOffset + dataLength - 1
}

/** What the receiver can say about a datagram after some fragments arrived. */
sealed interface Reassembly {
    data class Complete(val dataLength: Int) : Reassembly

    /** [gaps] are the byte ranges that never arrived; [lastSeen] says whether the final fragment is among those that did. */
    data class Incomplete(val gaps: List<IntRange>, val lastSeen: Boolean) : Reassembly
}

/** The sender's own answer to a path that cannot carry a packet whole. */
sealed interface Probe {
    data object Delivered : Probe

    /** The packet was too big, "don't fragment" was off, so the router cut it into [pieces] fragments. */
    data class Fragmented(val pieces: Int) : Probe

    /** Too big with "don't fragment" on: the router drops it and should send an ICMP "fragmentation needed" back. */
    data object TooBigDropped : Probe
}

object Fragmentation {
    const val HEADER = 20

    /** How much data one fragment can carry on a link with this MTU: what fits after the header, rounded down to a multiple of 8. */
    fun maxData(mtu: Int): Int {
        require(mtu >= HEADER + 8) { "MTU $mtu is too small" }
        return (mtu - HEADER) / 8 * 8
    }

    /** Cuts [dataLength] bytes of data into the fragments a link with [mtu] needs. Data that fits goes as one fragment. */
    fun fragment(datagramId: Int, dataLength: Int, mtu: Int): List<IpFragment> {
        require(dataLength >= 0) { "negative data length" }
        if (dataLength + HEADER <= mtu) return listOf(IpFragment(datagramId, 0, dataLength, moreFragments = false))
        val chunk = maxData(mtu)
        val pieces = (dataLength + chunk - 1) / chunk
        return (0 until pieces).map { i ->
            val start = i * chunk
            val length = minOf(chunk, dataLength - start)
            IpFragment(datagramId, start / 8, length, moreFragments = i < pieces - 1)
        }
    }

    /** Puts the [received] fragments of one datagram back together, or says which bytes are missing. */
    fun reassemble(received: List<IpFragment>): Reassembly {
        val sorted = received.distinctBy { it.byteOffset }.sortedBy { it.byteOffset }
        val gaps = mutableListOf<IntRange>()
        var next = 0
        for (f in sorted) {
            if (f.byteOffset > next) gaps += next until f.byteOffset
            next = maxOf(next, f.byteOffset + f.dataLength)
        }
        val last = sorted.firstOrNull { !it.moreFragments }
        if (last == null) return Reassembly.Incomplete(gaps, lastSeen = false)
        return if (gaps.isEmpty()) Reassembly.Complete(last.byteOffset + last.dataLength) else Reassembly.Incomplete(gaps, lastSeen = true)
    }

    /** The datagram is lost as a whole when any one fragment is lost: the chance it survives is the fragment survival chance to the power of the fragment count. */
    fun survivalPercent(fragments: Int, fragmentSurvivalPercent: Int): Int {
        var p = 1.0
        repeat(fragments) { p *= fragmentSurvivalPercent / 100.0 }
        return (p * 100).roundToInt()
    }

    /** What happens to a ping with [dataLength] bytes of data on a path whose smallest MTU is [pathMtu]. */
    fun probe(dataLength: Int, pathMtu: Int, dontFragment: Boolean): Probe = when {
        dataLength + HEADER <= pathMtu -> Probe.Delivered
        dontFragment -> Probe.TooBigDropped
        else -> Probe.Fragmented(fragment(0, dataLength, pathMtu).size)
    }
}
