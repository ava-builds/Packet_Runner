package com.avabuilds.packetrunner.network

/** A flat in the apartment building. Its private address came from the router's DHCP server. */
data class NatFlat(val name: String, val ip: Ipv4)

/** A flat opens a connection to a server on the Internet from one of its own (private) ports. */
data class NatConnection(val flat: Int, val privatePort: Int, val remote: String)

/** One row of the NAT table: public port <-> flat and its private port. */
data class NatEntry(val publicPort: Int, val flat: Int, val privatePort: Int)

/**
 * The translation table of a NAT router. All flats share one public address, so the router tells their
 * connections apart by giving each one its own public port, and remembers which flat it belongs to.
 */
class NatTable {
    private val rows = mutableListOf<NatEntry>()
    private var nextPort = FIRST_PORT

    val entries: List<NatEntry> get() = rows.toList()

    /** A flat's packet leaves. A connection seen before keeps its public port; a new one gets the next free port. */
    fun outbound(connection: NatConnection): NatEntry {
        rows.firstOrNull { it.flat == connection.flat && it.privatePort == connection.privatePort }?.let { return it }
        return NatEntry(nextPort++, connection.flat, connection.privatePort).also { rows += it }
    }

    /** A reply arrives for [publicPort]: which row it belongs to, or null when nobody inside asked for it. */
    fun inbound(publicPort: Int): NatEntry? = rows.firstOrNull { it.publicPort == publicPort }

    companion object {
        const val FIRST_PORT = 50000
    }
}

/** A packet from the Internet that arrives at the public address. [fromRemote] is only for the story. */
data class IncomingPacket(val publicPort: Int, val fromRemote: String)

/** A visitor packet to the apartment's web server. */
data class VisitorPacket(val port: Int, val note: String)

/**
 * The apartment job in three parts: let the flats' packets out and fill the NAT table, deliver the
 * replies to the right flat (and drop the unasked one), then add a port-forwarding rule so visitors can reach a flat's web server.
 */
class NatPuzzle(
    val id: String,
    val title: String,
    val story: String,
    val publicIp: Ipv4,
    val flats: List<NatFlat>,
    val connections: List<NatConnection>,
    /** The order in which replies arrive (indexes into [connections]). */
    private val replyOrder: List<Int>,
    /** Public ports nobody asked for: they must be dropped. */
    private val unsolicitedPorts: List<Int>,
    /** The flat that runs a website, and the visitors who try to reach it. */
    val serverFlat: Int,
    val serverPort: Int,
    val visitors: List<VisitorPacket>,
    val hints: List<String>,
    val concepts: List<Concept>,
) {
    /** The table after every connection has left once, in the order of [connections]. */
    fun fullTable(): NatTable = NatTable().also { t -> connections.forEach { t.outbound(it) } }

    /** The replies and unasked packets, as they arrive: real replies in [replyOrder], the unasked ones mixed in at the end. */
    fun incoming(): List<IncomingPacket> {
        val table = fullTable()
        val replies = replyOrder.map { i -> IncomingPacket(table.inbound(table.outbound(connections[i]).publicPort)!!.publicPort, connections[i].remote) }
        val strangers = unsolicitedPorts.map { IncomingPacket(it, "198.51.100.77:4444") }
        return replies.take(2) + strangers + replies.drop(2)
    }

    /** Where a packet should go: the flat's index, or null for "drop it". */
    fun rightAnswer(packet: IncomingPacket): Int? = fullTable().inbound(packet.publicPort)?.flat

    /** Is the player's choice (a flat index, or null for drop) right for this packet? */
    fun isRight(packet: IncomingPacket, choice: Int?): Boolean = choice == rightAnswer(packet)

    /** With a forwarding rule for [serverPort] to [flat], where does a visitor's packet end up? Null means dropped. */
    fun visitorGoesTo(packet: VisitorPacket, ruleFlat: Int?): Int? = if (packet.port == serverPort) ruleFlat else null

    fun visitorsServed(ruleFlat: Int?): Boolean =
        visitors.all { v -> visitorGoesTo(v, ruleFlat) == (if (v.port == serverPort) serverFlat else null) }

    /** 3 stars for at most one mistake, 2 for at most three, 1 otherwise. */
    fun stars(mistakes: Int) = when {
        mistakes <= 1 -> 3
        mistakes <= 3 -> 2
        else -> 1
    }

    fun tip(stars: Int) = (stars - 1) * 10
}

object NatPuzzles {
    private fun ip(text: String) = Ipv4.parse(text)

    private val hints = listOf(
        "Nana on the radio. First let every flat's packet out. Watch what the router writes in its table.",
        "All flats share one public address, so the router tells them apart by the public port. Find the port of the reply in the table.",
        "A reply for a port that is not in the table was not asked for by anybody inside. A NAT router drops it.",
        "A visitor from outside has no row in the table. A port-forwarding rule says: port 80 always goes to the flat with the website.",
    )

    private val concepts = listOf(Concepts.PRIVATE_ADDRESSES, Concepts.NAT_TABLE, Concepts.PORT_FORWARDING, Concepts.DHCP_POOL)

    private val flats = listOf(
        NatFlat("Flat 1 · Mia's phone", ip("192.168.1.11")),
        NatFlat("Flat 2 · Noah's laptop", ip("192.168.1.12")),
        NatFlat("Flat 3 · Dev's laptop", ip("192.168.1.13")),
        NatFlat("Flat 4 · Ana's tablet", ip("192.168.1.14")),
    )

    val apartment = NatPuzzle(
        id = "nat_apartments", title = "Share one address",
        story = "Four flats share one Internet address. Their private addresses came from the router's DHCP server. " +
            "Let the router translate between the two worlds.",
        publicIp = ip("203.0.113.5"),
        flats = flats,
        connections = listOf(
            NatConnection(0, 5000, "93.184.216.34:80"),
            NatConnection(1, 5000, "151.101.1.69:443"),
            NatConnection(2, 6001, "8.8.8.8:53"),
            NatConnection(3, 5000, "93.184.216.34:80"),
        ),
        replyOrder = listOf(2, 0, 3, 1),
        unsolicitedPorts = listOf(50099),
        serverFlat = 2, serverPort = 80,
        visitors = listOf(VisitorPacket(80, "A visitor opens the website"), VisitorPacket(80, "Another visitor"), VisitorPacket(22, "Someone tries a remote login")),
        hints = hints, concepts = concepts,
    )

    fun forJob(jobsDoneBefore: Int): NatPuzzle = apartment
}
