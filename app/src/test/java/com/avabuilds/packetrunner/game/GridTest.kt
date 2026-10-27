package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTest {
    private val grid = Grid(4, 3, blocked = setOf(TilePos(1, 1)))

    @Test fun containsOnlyTilesInsideTheGrid() {
        assertTrue(grid.contains(TilePos(0, 0)))
        assertTrue(grid.contains(TilePos(3, 2)))
        assertFalse(grid.contains(TilePos(4, 0)))
        assertFalse(grid.contains(TilePos(0, -1)))
    }

    @Test fun blockedTilesAreNotWalkable() {
        assertFalse(grid.isWalkable(TilePos(1, 1)))
        assertTrue(grid.isWalkable(TilePos(2, 1)))
        assertFalse(grid.isWalkable(TilePos(9, 9)))
    }

    @Test fun cornerTileHasTwoNeighbors() {
        assertEquals(2, grid.walkableNeighbors(TilePos(0, 0)).size)
    }

    @Test fun blockedNeighborIsLeftOut() {
        // (1,1) is blocked, so (1,0) loses one of its three neighbors
        val neighbors = grid.walkableNeighbors(TilePos(1, 0))
        assertEquals(2, neighbors.size)
        assertFalse(TilePos(1, 1) in neighbors)
    }
}
