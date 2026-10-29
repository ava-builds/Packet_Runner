package com.avabuilds.packetrunner.network

/**
 * One router (or the destination) on a path. [linkMs] is the one-way delay of the link that leads to it.
 * A router with [answersIcmp] false stays silent when a packet's TTL runs out on it. A router with
 * [forwards] false swallows every packet that reaches it (a firewall black hole).
 */
data class PathHop(
    val name: String,
    val ip: String,
    val linkMs: Int,
    val answersIcmp: Boolean = true,
    val forwards: Boolean = true,
)

/**
 * The hops a packet really visits, in order. For a routing loop the same routers simply repeat.
 * [destination] answers pings; null means the packet never reaches any destination (a loop).
 */
data class Path(val hops: List<PathHop>, val destination: PathHop?) {
    /** Hops are numbered from 1 for the first router; the destination counts as the last hop. */
    val route: List<PathHop> get() = hops + listOfNotNull(destination)
}

/** What came back after one probe packet was sent. */
sealed interface ProbeReply {
    /** A router dropped the packet because its TTL reached 0 and said so (ICMP "time exceeded"). */
    data class TimeExceeded(val from: PathHop, val rttMs: Int) : ProbeReply

    /** The destination answered (ICMP "echo reply"). */
    data class EchoReply(val from: PathHop, val rttMs: Int) : ProbeReply

    /** Nothing came back: a silent router, a black hole, or a packet that ran out of path. */
    data object Timeout : ProbeReply
}

/** One printed line of a traceroute. [hop] is null for a line of stars. */
data class TraceLine(val ttl: Int, val reply: ProbeReply)

object Icmp {
    const val DEFAULT_TTL = 64

    /**
     * Sends one packet with the given [ttl] along [path]. Every router lowers the TTL by one; the router that
     * makes it 0 drops the packet and (if it answers ICMP) sends "time exceeded" back. A packet that survives
     * every router reaches the destination, which answers with an echo reply.
     */
    fun probe(path: Path, ttl: Int): ProbeReply {
        var left = ttl
        var delay = 0
        val route = path.route
        for ((i, hop) in route.withIndex()) {
            delay += hop.linkMs
            left--
            val isDestination = path.destination != null && i == route.lastIndex
            if (isDestination) return if (left >= 0) ProbeReply.EchoReply(hop, 2 * delay) else ProbeReply.Timeout
            if (left <= 0) return if (hop.answersIcmp) ProbeReply.TimeExceeded(hop, 2 * delay) else ProbeReply.Timeout
            if (!hop.forwards) return ProbeReply.Timeout
        }
        return ProbeReply.Timeout // the path ended without a destination
    }

    /** A ping: one packet with the normal starting TTL. */
    fun ping(path: Path): ProbeReply = probe(path, DEFAULT_TTL)

    /** Traceroute: probes with TTL 1, 2, 3, ... each router tells who it is. Stops at the destination or after [maxHops]. */
    fun traceroute(path: Path, maxHops: Int = 30): List<TraceLine> {
        val lines = mutableListOf<TraceLine>()
        for (ttl in 1..maxHops) {
            val reply = probe(path, ttl)
            lines += TraceLine(ttl, reply)
            if (reply is ProbeReply.EchoReply) break
        }
        return lines
    }

    /** True when the same router shows up twice in the answers of a traceroute: the packets are going round in a circle. */
    fun hasLoop(lines: List<TraceLine>): Boolean {
        val seen = lines.mapNotNull { (it.reply as? ProbeReply.TimeExceeded)?.from?.ip }
        return seen.size != seen.toSet().size
    }
}
