package com.avabuilds.packetrunner

/**
 * Plain-Kotlin model of a drawable network map and the World 3 routing rules.
 * No Android or 3D code here, so it can be unit-tested.
 */

enum class NodeKind { SOURCE, ROUTER, NETWORK }

/** A place on the map. (x, z) are positions on the ground plane. */
data class MapNode(val id: String, val kind: NodeKind, val label: String, val x: Float, val z: Float)

data class NetworkMap(val nodes: List<MapNode>, val links: List<Pair<String, String>>) {
    fun node(id: String): MapNode = nodes.first { it.id == id }

    fun linked(a: String, b: String): Boolean =
        links.any { (x, y) -> (x == a && y == b) || (x == b && y == a) }

    fun neighbors(id: String): List<String> =
        links.mapNotNull { (a, b) -> if (a == id) b else if (b == id) a else null }
}

/** What happens when Pip runs a drawn route. */
sealed interface RouteVerdict {
    data object Delivered : RouteVerdict

    /** Pip gets lost at route[[atIndex]]. */
    data class Lost(val atIndex: Int, val explanation: String) : RouteVerdict
}

/**
 * World 3 · Router station. Pip starts at the source, crosses router R2 and must leave through
 * the exit that R2's forwarding table picks for the packet's destination.
 */
object RouterMap {
    const val SOURCE = "SRC"
    const val R2 = "R2"
    const val NET_A = "NET_A"
    const val NET_B = "NET_B"
    const val R3 = "R3"
    const val NET_C = "NET_C"

    val map = NetworkMap(
        nodes = listOf(
            MapNode(SOURCE, NodeKind.SOURCE, "Seoul", 0f, 6.5f),
            MapNode(R2, NodeKind.ROUTER, "R2", 0f, 3f),
            MapNode(NET_A, NodeKind.NETWORK, ".34.0/25", -3.6f, -1.5f),
            MapNode(NET_B, NodeKind.NETWORK, ".34.128/25", 0f, -1.5f),
            MapNode(R3, NodeKind.ROUTER, "R3", 3.6f, -1.5f),
            MapNode(NET_C, NodeKind.NETWORK, ".33.0/24", 3.6f, -5.5f),
        ),
        links = listOf(
            SOURCE to R2,
            R2 to NET_A,
            R2 to NET_B,
            R2 to R3,
            R3 to NET_C,
        ),
    )

    /** R2's forwarding table row i sends the packet toward this node. */
    private val rowExit = listOf(NET_A, NET_B, R3)

    /** The network each destination lives in. */
    private val netOf = listOf(
        NET_A to Route(Ipv4.parse("128.96.34.0"), 25, "if0"),
        NET_B to Route(Ipv4.parse("128.96.34.128"), 25, "if1"),
        NET_C to Route(Ipv4.parse("128.96.33.0"), 24, "R3"),
    )

    /** MAC address Pip's frame carries on each hop of a three-hop trip. */
    val macs = listOf(
        "E6-E9-00-17-BB-4B",
        "1A-23-F9-CD-06-9B",
        "49-BD-D2-C7-56-2A",
        "88-B2-2F-54-1A-0F",
    )

    val destinations = RouterStationLevel.rounds.map { it.dest }

    private val table get() = RouterStationLevel.table

    /** The network node that contains [dest]. */
    fun destinationNode(dest: Ipv4): String = netOf.first { it.second.matches(dest) }.first

    /** The correct route for a packet to [dest]. */
    fun expectedRoute(dest: Ipv4): List<String> {
        val row = Forwarding.lookup(table, dest) ?: return listOf(SOURCE, R2)
        val exit = rowExit[table.indexOf(row)]
        return buildList {
            add(SOURCE)
            add(R2)
            add(exit)
            if (exit == R3) add(NET_C)
        }
    }

    /** Run [route] and say whether Pip arrives, or where and why it is lost. */
    fun check(route: List<String>, dest: Ipv4): RouteVerdict {
        val expected = expectedRoute(dest)
        for (i in 1 until route.size) {
            if (route[i] != expected.getOrNull(i)) return RouteVerdict.Lost(i, explainWrongExit(route[i], dest))
        }
        if (route.size < expected.size) {
            val where = map.node(route.last()).label
            return RouteVerdict.Lost(
                route.lastIndex,
                "Pip stopped at $where, but $dest is not there. Keep drawing until Pip reaches the network that contains the destination.",
            )
        }
        return RouteVerdict.Delivered
    }

    private fun explainWrongExit(chosen: String, dest: Ipv4): String {
        val chosenRow = rowExit.indexOf(chosen).takeIf { it >= 0 }?.let { table[it] }
            ?: return "That hop is not part of the way to $dest."
        val rightRow = Forwarding.lookup(table, dest)!!
        val masked = dest and chosenRow.mask
        return "R2 checks each row with (D AND mask). Row ${chosenRow.nextHop}: $dest AND ${chosenRow.mask} = " +
            "$masked, but the row's network is ${chosenRow.network}. No match. " +
            "The matching row is ${rightRow.nextHop} (${rightRow.network}/${rightRow.prefix})."
    }
}
