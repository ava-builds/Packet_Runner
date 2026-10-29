package com.avabuilds.packetrunner.game


/** A cable from the office to one building, as the tiles it runs along (office door first, building door last). */
data class CableLink(val buildingId: String, val path: List<TilePos>)

/**
 * Where the network cables run. A cable follows the roads like a real one in a conduit: a step on a road costs 1,
 * a step across grass costs [OFF_ROAD_COST], so cables share the roads and only leave them to reach a door.
 */
object CableRouting {
    const val OFF_ROAD_COST = 4

    private class Node(val tile: TilePos, val cost: Int, val order: Int)

    /** The cheapest walkable route from [from] to [to], both included, or an empty list when there is none. */
    fun route(grid: Grid, roads: Set<TilePos>, from: TilePos, to: TilePos): List<TilePos> {
        if (!grid.isWalkable(from) || !grid.isWalkable(to)) return emptyList()
        if (from == to) return listOf(from)

        var counter = 0
        val open = PriorityQueue<Node>(compareBy<Node>({ it.cost }, { it.order }))
        val best = HashMap<TilePos, Int>()
        val cameFrom = HashMap<TilePos, TilePos>()
        best[from] = 0
        open.add(Node(from, 0, counter++))

        while (open.isNotEmpty()) {
            val current = open.poll() ?: break
            if (current.cost > (best[current.tile] ?: Int.MAX_VALUE)) continue // an older, worse entry
            if (current.tile == to) break
            for (next in grid.walkableNeighbors(current.tile)) {
                val cost = current.cost + if (next in roads || next == to) 1 else OFF_ROAD_COST
                if (cost < (best[next] ?: Int.MAX_VALUE)) {
                    best[next] = cost
                    cameFrom[next] = current.tile
                    open.add(Node(next, cost, counter++))
                }
            }
        }
        if (to !in cameFrom) return emptyList()
        val path = mutableListOf(to)
        var step = to
        while (true) { step = cameFrom[step] ?: break; path.add(step) }
        return path.reversed()
    }
}

/** The office is the hub of the town's network: one cable runs from its door to every other building. */
object TownNetwork {
    fun links(town: TownMap): List<CableLink> {
        val office = town.buildings.firstOrNull { it.type == BuildingType.OFFICE } ?: return emptyList()
        return town.buildings.filter { it !== office }.mapNotNull { b ->
            val path = CableRouting.route(town.grid, town.roads, office.door, b.door)
            if (path.isEmpty()) null else CableLink(b.id, path)
        }
    }
}
