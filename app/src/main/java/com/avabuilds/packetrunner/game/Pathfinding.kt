package com.avabuilds.packetrunner.game

import java.util.PriorityQueue
import kotlin.math.abs

/** A* pathfinding on the town grid (4 directions). */
object Pathfinding {

    private class Entry(val tile: TilePos, val cost: Int, val estimate: Int, val order: Int)

    /**
     * The shortest walkable path from [start] to [goal], including both ends.
     * Returns an empty list when either end is blocked or the goal cannot be reached.
     */
    fun findPath(grid: Grid, start: TilePos, goal: TilePos): List<TilePos> {
        if (!grid.isWalkable(start) || !grid.isWalkable(goal)) return emptyList()
        if (start == goal) return listOf(start)

        // Manhattan distance never overestimates on a 4-direction grid, so A* stays optimal.
        fun heuristic(t: TilePos) = abs(t.x - goal.x) + abs(t.y - goal.y)

        var counter = 0
        val open = PriorityQueue<Entry>(
            compareBy<Entry>({ it.cost + it.estimate }, { it.estimate }, { it.order }),
        )
        val bestCost = HashMap<TilePos, Int>()
        val cameFrom = HashMap<TilePos, TilePos>()
        val closed = HashSet<TilePos>()

        bestCost[start] = 0
        open.add(Entry(start, 0, heuristic(start), counter++))

        while (open.isNotEmpty()) {
            val current = open.poll()
            if (current.tile in closed) continue // an older, worse entry for a finished tile
            if (current.tile == goal) return rebuild(cameFrom, goal)
            closed.add(current.tile)

            for (next in grid.walkableNeighbors(current.tile)) {
                if (next in closed) continue
                val cost = current.cost + 1
                if (cost < (bestCost[next] ?: Int.MAX_VALUE)) {
                    bestCost[next] = cost
                    cameFrom[next] = current.tile
                    open.add(Entry(next, cost, heuristic(next), counter++))
                }
            }
        }
        return emptyList()
    }

    private fun rebuild(cameFrom: Map<TilePos, TilePos>, goal: TilePos): List<TilePos> {
        val path = mutableListOf(goal)
        var step = goal
        while (true) {
            step = cameFrom[step] ?: break
            path.add(step)
        }
        return path.reversed()
    }
}
