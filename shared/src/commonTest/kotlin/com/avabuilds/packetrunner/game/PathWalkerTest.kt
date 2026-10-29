package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class PathWalkerTest {
    private val path = listOf(TilePos(0, 0), TilePos(1, 0), TilePos(2, 0), TilePos(2, 1))

    @Test fun walksAtTheGivenSpeed() {
        val walker = PathWalker(Vec2(0f, 0f), path, speed = 2f)
        walker.advance(0.5f) // 1 tile
        assertEquals(1f, walker.position.x, 0.001f)
        assertEquals(0f, walker.position.y, 0.001f)
        assertFalse(walker.finished)
    }

    @Test fun turnsCornersWithoutLosingDistance() {
        val walker = PathWalker(Vec2(0f, 0f), path, speed = 1f)
        walker.advance(2.5f) // 2 tiles along x, then half a tile along y
        assertEquals(2f, walker.position.x, 0.001f)
        assertEquals(0.5f, walker.position.y, 0.001f)
    }

    @Test fun stopsAtTheEndEvenWithABigTimeStep() {
        val walker = PathWalker(Vec2(0f, 0f), path, speed = 3f)
        walker.advance(100f)
        assertTrue(walker.finished)
        assertEquals(Vec2(2f, 1f), walker.position)
    }

    @Test fun canStartBetweenTiles() {
        // the player was interrupted halfway between (0,0) and (1,0), then new path starts at (1,0)
        val walker = PathWalker(Vec2(0.5f, 0f), listOf(TilePos(1, 0), TilePos(1, 1)), speed = 1f)
        walker.advance(0.5f)
        assertEquals(1f, walker.position.x, 0.001f)
        walker.advance(1f)
        assertTrue(walker.finished)
        assertEquals(Vec2(1f, 1f), walker.position)
    }

    @Test fun remainingShrinksAsTilesAreReached() {
        val walker = PathWalker(Vec2(0f, 0f), path, speed = 1f)
        assertEquals(4, walker.remaining.size)
        walker.advance(2f) // reaches (2,0)
        assertEquals(listOf(TilePos(2, 1)), walker.remaining)
        walker.advance(5f)
        assertTrue(walker.remaining.isEmpty())
    }
}
