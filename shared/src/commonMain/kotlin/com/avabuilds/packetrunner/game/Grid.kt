package com.avabuilds.packetrunner.game

/** A 2D float vector, used for positions on the map and on the screen. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec2(x * k, y * k)
}

/** One tile of the town grid. x grows toward the lower right, y toward the lower left. */
data class TilePos(val x: Int, val y: Int)

/**
 * The town grid. [blocked] tiles (rocks, buildings, trees) cannot be walked on.
 * Walking is 4-directional: up, down, left and right on the grid.
 */
class Grid(val width: Int, val height: Int, blocked: Set<TilePos> = emptySet()) {
    val blocked: Set<TilePos> = blocked.toSet()

    init {
        require(width > 0 && height > 0) { "The grid must have at least one tile" }
    }

    fun contains(tile: TilePos) = tile.x in 0 until width && tile.y in 0 until height

    fun isWalkable(tile: TilePos) = contains(tile) && tile !in blocked

    /** Walkable tiles next to [tile], in a fixed order so results are repeatable. */
    fun walkableNeighbors(tile: TilePos): List<TilePos> =
        DIRECTIONS.map { (dx, dy) -> TilePos(tile.x + dx, tile.y + dy) }.filter { isWalkable(it) }

    private companion object {
        val DIRECTIONS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    }
}
