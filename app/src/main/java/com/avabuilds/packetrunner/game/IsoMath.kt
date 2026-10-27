package com.avabuilds.packetrunner.game

import kotlin.math.floor

/**
 * Isometric (2.5D) math: converts between tile coordinates and "world" positions.
 *
 * Every tile is drawn as a diamond [tileWidth] wide and [tileHeight] tall. Moving one tile in x
 * goes right and down; moving one tile in y goes left and down:
 *
 *     worldX = (x - y) * tileWidth / 2
 *     worldY = (x + y) * tileHeight / 2
 *
 * [origin] is where tile (0, 0) has its center.
 */
class IsoMath(val tileWidth: Float, val tileHeight: Float, val origin: Vec2 = Vec2(0f, 0f)) {

    /** Center of tile (x, y). Fractional values work too, which a walking player needs. */
    fun tileCenter(x: Float, y: Float) = Vec2(
        origin.x + (x - y) * tileWidth / 2f,
        origin.y + (x + y) * tileHeight / 2f,
    )

    fun tileCenter(tile: TilePos) = tileCenter(tile.x.toFloat(), tile.y.toFloat())

    /** The fractional tile coordinates under a world point (the inverse of [tileCenter]). */
    fun toTileCoords(world: Vec2): Vec2 {
        val u = (world.x - origin.x) / (tileWidth / 2f) // equals x - y
        val v = (world.y - origin.y) / (tileHeight / 2f) // equals x + y
        return Vec2((u + v) / 2f, (v - u) / 2f)
    }

    /** The tile whose diamond contains the world point. */
    fun tileAt(world: Vec2): TilePos {
        val c = toTileCoords(world)
        return TilePos(floor(c.x + 0.5f).toInt(), floor(c.y + 0.5f).toInt())
    }

    companion object {
        /** An [IsoMath] whose world origin (0, 0) is the middle of [grid], so the town sits centered. */
        fun centeredOn(grid: Grid, tileWidth: Float, tileHeight: Float): IsoMath {
            val middle = IsoMath(tileWidth, tileHeight).tileCenter((grid.width - 1) / 2f, (grid.height - 1) / 2f)
            return IsoMath(tileWidth, tileHeight, Vec2(-middle.x, -middle.y))
        }
    }
}
