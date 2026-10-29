package com.avabuilds.packetrunner.game

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** A hill in the world outside the town: its center tile (may be outside the grid) and its radius in tiles. */
data class Hill(val x: Float, val y: Float, val radius: Float)

/**
 * The land around the town: grass and hills that fade away at the edges, a river with a bridge on one side,
 * and the road carrying on over the bridge. It is only scenery: nobody can walk here, and nothing here
 * ever lies inside the town grid. Pure rules, so they are easy to test.
 */
object World {
    /** How many tiles of land are drawn beyond each edge of the town grid. */
    const val PAD = 8

    /** The river runs along the rows RIVER_FROM..RIVER_TO, just outside the bottom edge of a 10 x 10 town. */
    const val RIVER_FROM = 11
    const val RIVER_TO = 12

    /** The bridge carries the town's main road (x = 5) over the river. */
    const val BRIDGE_X = 5

    val hills = listOf(
        Hill(-5f, 3f, 3.4f),
        Hill(2f, -5.5f, 3.2f),
        Hill(11f, -4f, 3.8f),
        Hill(-5f, 13f, 3.2f),
        Hill(15f, 8f, 3.4f),
    )

    /** How many tiles outside the grid this tile is (0 = inside the grid). */
    fun distanceOutside(x: Int, y: Int, width: Int, height: Int): Int {
        val dx = max(max(-x, x - (width - 1)), 0)
        val dy = max(max(-y, y - (height - 1)), 0)
        return max(dx, dy)
    }

    fun inGrid(x: Int, y: Int, width: Int, height: Int) = distanceOutside(x, y, width, height) == 0

    fun isBridge(x: Int, y: Int) = x == BRIDGE_X && y in RIVER_FROM..RIVER_TO

    fun isRiver(x: Int, y: Int) = y in RIVER_FROM..RIVER_TO && !isBridge(x, y)

    /** The road that carries on outside the town: before the bridge (the tile after the grid) and after it. */
    fun isOutsideRoad(x: Int, y: Int, height: Int) = x == BRIDGE_X && (y == height || y > RIVER_TO)

    /** 1 near the town, fading to 0 at the edge of the world. */
    fun fade(x: Int, y: Int, width: Int, height: Int): Float {
        val d = distanceOutside(x, y, width, height)
        return (1f - d.toFloat() / (PAD + 1)).coerceIn(0f, 1f).pow(1.2f)
    }

    /** True when a hill covers this tile. */
    fun underHill(x: Int, y: Int): Boolean = hills.any { h ->
        val dx = x - h.x
        val dy = y - h.y
        sqrt(dx * dx + dy * dy) < h.radius + 0.5f
    }

    /** Whether a scenery tree stands on this outside tile. The same tile always gives the same answer. */
    fun treeAt(x: Int, y: Int, width: Int, height: Int): Boolean {
        if (inGrid(x, y, width, height) || isRiver(x, y) || isBridge(x, y) || isOutsideRoad(x, y, height) || underHill(x, y)) return false
        val h = (x * 73856093) xor (y * 19349663)
        return h.mod(9) == 0 && fade(x, y, width, height) > 0.15f
    }
}
