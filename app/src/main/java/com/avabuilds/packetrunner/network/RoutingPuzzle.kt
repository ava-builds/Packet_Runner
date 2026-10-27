package com.avabuilds.packetrunner.network

/** One row of the forwarding table the player edits: the network and next hop are fixed, the mask is theirs to set. */
data class PuzzleRow(val label: String, val network: Ipv4, val nextHop: String, val startPrefix: Int)

/** A packet sent through the router to test the table, and where it should go. */
data class TestPacket(val dest: Ipv4, val expected: String)

/** What happened to a test packet. [row] is the table row that caught it, or null when the default route did. */
data class PacketResult(val packet: TestPacket, val actual: String, val row: PuzzleRow?) {
    val ok: Boolean get() = actual == packet.expected
}

/**
 * A router whose forwarding table has the wrong subnet masks. The player changes each row's mask
 * (the prefix length) until every test packet leaves by the right next hop. The lookup is the one in
 * [Forwarding]: a row matches when (destination AND mask) equals its network number, and the longest prefix wins.
 */
class RoutingPuzzle(
    val id: String,
    val title: String,
    val story: String,
    val rows: List<PuzzleRow>,
    val packets: List<TestPacket>,
    val hints: List<String>,
    val concepts: List<Concept>,
) {
    /** Where a packet goes when no row matches. */
    val defaultHop = "Internet"

    fun startPrefixes(): List<Int> = rows.map { it.startPrefix }

    /** The table with the given masks, plus the default route 0.0.0.0/0 at the end. */
    fun table(prefixes: List<Int>): List<Route> =
        rows.zip(prefixes) { row, prefix -> Route(row.network, prefix, row.nextHop) } + Route(Ipv4.parse("0.0.0.0"), 0, defaultHop)

    /** Sends every test packet through the table. */
    fun send(prefixes: List<Int>): List<PacketResult> {
        val table = table(prefixes)
        return packets.map { packet ->
            val route = Forwarding.lookup(table, packet.dest)!! // the default route matches everything
            val index = table.indexOf(route)
            PacketResult(packet, route.nextHop, rows.getOrNull(index))
        }
    }

    fun isSolved(prefixes: List<Int>) = send(prefixes).all { it.ok }

    /** 3 stars for testing at most twice, 2 for at most four tests, 1 otherwise. */
    fun stars(tests: Int) = when {
        tests <= 2 -> 3
        tests <= 4 -> 2
        else -> 1
    }

    /** A tip for good work, in won. */
    fun tip(stars: Int) = (stars - 1) * 10

    companion object {
        const val MIN_PREFIX = 8
        const val MAX_PREFIX = 30
    }
}

/** The packet's address, the mask and their AND, written in binary, so the player can see the rule at work. */
data class BinaryLookup(val dest: String, val mask: String, val and: String, val network: String, val matches: Boolean)

fun Ipv4.toBinaryString(): String = octets.joinToString(".") { it.toBinary8() }

fun explainLookup(dest: Ipv4, route: Route): BinaryLookup {
    val masked = dest and route.mask
    return BinaryLookup(dest.toBinaryString(), route.mask.toBinaryString(), masked.toBinaryString(), route.network.toBinaryString(), route.matches(dest))
}

object RoutingPuzzles {
    private val hints = listOf(
        "Nana on the radio. Send the test packets first and see where each one goes.",
        "A row catches a packet when (destination AND mask) gives the row's network number. A mask that is too long or too short never matches.",
        "A packet that matches no row falls through to the default route, the Internet. Look at the binary: which bits does the mask keep?",
        "If two rows match, the longer prefix wins. That is how a small network can sit inside a bigger one.",
    )

    private val concepts = listOf(Concepts.FORWARDING_TABLE, Concepts.SUBNET_MASK, Concepts.LONGEST_PREFIX, Concepts.DEFAULT_ROUTE)

    private fun ip(text: String) = Ipv4.parse(text)

    /** The town's router: a library, a cafe, and a printer room that sits inside the library's network. */
    private val townRouter = RoutingPuzzle(
        id = "route_town", title = "Fix the city router",
        story = "Packets for the library, the cafe and the printer room are going to the wrong places.",
        rows = listOf(
            PuzzleRow("Library", ip("192.168.10.0"), "Library", startPrefix = 28),
            PuzzleRow("Printer room", ip("192.168.10.128"), "Printer room", startPrefix = 24),
            PuzzleRow("Cafe", ip("192.168.20.0"), "Cafe", startPrefix = 16),
        ),
        packets = listOf(
            TestPacket(ip("192.168.10.5"), "Library"),
            TestPacket(ip("192.168.10.30"), "Library"),
            TestPacket(ip("192.168.10.141"), "Printer room"),
            TestPacket(ip("192.168.10.200"), "Printer room"),
            TestPacket(ip("192.168.20.77"), "Cafe"),
            TestPacket(ip("192.168.20.250"), "Cafe"),
            TestPacket(ip("8.8.8.8"), "Internet"),
        ),
        hints = hints, concepts = concepts,
    )

    /** The campus router: a dorm, a lab, and a server room inside the lab's network. */
    private val campusRouter = RoutingPuzzle(
        id = "route_campus", title = "Fix the campus router",
        story = "The campus router lost its masks in a power cut. Dorm, lab and server room all need them back.",
        rows = listOf(
            PuzzleRow("Dorm", ip("172.16.4.0"), "Dorm", startPrefix = 24),
            PuzzleRow("Lab", ip("172.16.8.0"), "Lab", startPrefix = 24),
            PuzzleRow("Server room", ip("172.16.8.64"), "Server room", startPrefix = 24),
        ),
        packets = listOf(
            TestPacket(ip("172.16.5.20"), "Dorm"),
            TestPacket(ip("172.16.7.200"), "Dorm"),
            TestPacket(ip("172.16.9.1"), "Lab"),
            TestPacket(ip("172.16.12.250"), "Lab"),
            TestPacket(ip("172.16.8.70"), "Server room"),
            TestPacket(ip("172.16.8.100"), "Server room"),
            TestPacket(ip("172.16.20.1"), "Internet"),
        ),
        hints = hints, concepts = concepts,
    )

    val all = listOf(townRouter, campusRouter)

    /** Each city-hall job is a different router, so the same one does not come twice in a row. */
    fun forJob(jobsDoneBefore: Int): RoutingPuzzle = all[jobsDoneBefore % all.size]
}
