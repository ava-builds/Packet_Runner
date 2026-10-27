package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PathfindingTest {

    private fun assertValidPath(grid: Grid, path: List<TilePos>) {
        path.forEach { assertTrue("$it must be walkable", grid.isWalkable(it)) }
        path.zipWithNext().forEach { (a, b) ->
            assertEquals("$a to $b must be one step", 1, abs(a.x - b.x) + abs(a.y - b.y))
        }
    }

    @Test fun straightPathOnOpenGroundIsTheShortest() {
        val grid = Grid(5, 5)
        val path = Pathfinding.findPath(grid, TilePos(0, 0), TilePos(4, 4))
        assertEquals(9, path.size) // 8 steps + the start tile
        assertEquals(TilePos(0, 0), path.first())
        assertEquals(TilePos(4, 4), path.last())
        assertValidPath(grid, path)
    }

    @Test fun pathGoesAroundAWall() {
        // a wall at x=2 from y=0 to y=3 leaves a gap only at y=4
        val grid = Grid(5, 5, blocked = (0..3).map { TilePos(2, it) }.toSet())
        val path = Pathfinding.findPath(grid, TilePos(0, 0), TilePos(4, 0))
        assertTrue(TilePos(2, 4) in path)
        assertValidPath(grid, path)
        assertEquals(TilePos(4, 0), path.last())
    }

    @Test fun noPathWhenTheGoalIsWalledOff() {
        val grid = Grid(5, 5, blocked = (0..4).map { TilePos(2, it) }.toSet())
        assertTrue(Pathfinding.findPath(grid, TilePos(0, 0), TilePos(4, 4)).isEmpty())
    }

    @Test fun startEqualsGoalIsASingleTile() {
        assertEquals(listOf(TilePos(1, 1)), Pathfinding.findPath(Grid(3, 3), TilePos(1, 1), TilePos(1, 1)))
    }

    @Test fun blockedOrOutsideGoalGivesNoPath() {
        val grid = Grid(3, 3, blocked = setOf(TilePos(2, 2)))
        assertTrue(Pathfinding.findPath(grid, TilePos(0, 0), TilePos(2, 2)).isEmpty())
        assertTrue(Pathfinding.findPath(grid, TilePos(0, 0), TilePos(5, 5)).isEmpty())
    }
}
