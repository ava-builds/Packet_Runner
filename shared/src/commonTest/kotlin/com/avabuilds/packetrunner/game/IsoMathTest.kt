package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.testing.assertEquals
import kotlin.test.Test

class IsoMathTest {
    private val iso = IsoMath(tileWidth = 64f, tileHeight = 32f)

    @Test fun tileZeroIsAtTheOrigin() {
        assertEquals(Vec2(0f, 0f), iso.tileCenter(TilePos(0, 0)))
    }

    @Test fun movingInXGoesRightAndDown_movingInYGoesLeftAndDown() {
        assertEquals(Vec2(32f, 16f), iso.tileCenter(TilePos(1, 0)))
        assertEquals(Vec2(-32f, 16f), iso.tileCenter(TilePos(0, 1)))
        assertEquals(Vec2(0f, 32f), iso.tileCenter(TilePos(1, 1)))
    }

    @Test fun everyTileCenterMapsBackToItsTile() {
        for (x in 0 until 10) for (y in 0 until 10) {
            val tile = TilePos(x, y)
            assertEquals(tile, iso.tileAt(iso.tileCenter(tile)))
        }
    }

    @Test fun pointsInsideTheDiamondBelongToThatTile() {
        val c = iso.tileCenter(TilePos(3, 4))
        // near each corner of the diamond but still inside it
        assertEquals(TilePos(3, 4), iso.tileAt(Vec2(c.x + 28f, c.y)))
        assertEquals(TilePos(3, 4), iso.tileAt(Vec2(c.x - 28f, c.y)))
        assertEquals(TilePos(3, 4), iso.tileAt(Vec2(c.x, c.y + 14f)))
        assertEquals(TilePos(3, 4), iso.tileAt(Vec2(c.x, c.y - 14f)))
    }

    @Test fun aPointJustOutsideTheDiamondBelongsToTheNeighbor() {
        val c = iso.tileCenter(TilePos(0, 0))
        // 0.9 of the half width to the right and 0.2 of the half height down is past the edge
        assertEquals(TilePos(1, 0), iso.tileAt(Vec2(c.x + 0.9f * 32f, c.y + 0.2f * 16f)))
    }

    @Test fun centeredOnPutsTheGridMiddleAtWorldZero() {
        val grid = Grid(10, 10)
        val centered = IsoMath.centeredOn(grid, 64f, 32f)
        val middle = centered.tileCenter(4.5f, 4.5f)
        assertEquals(0f, middle.x, 0.001f)
        assertEquals(0f, middle.y, 0.001f)
        assertEquals(TilePos(0, 0), centered.tileAt(centered.tileCenter(TilePos(0, 0))))
    }
}
