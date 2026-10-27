package com.avabuilds.packetrunner.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    private val square = listOf(Vec2(0f, 0f), Vec2(10f, 0f), Vec2(10f, 10f), Vec2(0f, 10f))

    @Test fun pointInsideAndOutsideASquare() {
        assertTrue(pointInPolygon(square, Vec2(5f, 5f)))
        assertFalse(pointInPolygon(square, Vec2(15f, 5f)))
        assertFalse(pointInPolygon(square, Vec2(5f, -1f)))
    }

    @Test fun boxSilhouetteCoversTheWallsAndTheRoofButNotTheSky() {
        val silhouette = isoBoxSilhouette(Vec2(100f, 100f), w = 80f, h = 40f, boxHeight = 60f)
        assertTrue(pointInPolygon(silhouette, Vec2(100f, 100f))) // the ground in the middle
        assertTrue(pointInPolygon(silhouette, Vec2(100f, 60f))) // the middle of the wall
        assertTrue(pointInPolygon(silhouette, Vec2(100f, 25f))) // near the roof's back corner
        assertFalse(pointInPolygon(silhouette, Vec2(100f, 10f))) // above the roof
        assertFalse(pointInPolygon(silhouette, Vec2(200f, 100f))) // far to the side
    }
}
