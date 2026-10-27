package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Test

class IsoCameraTest {
    private val viewCenter = Vec2(200f, 300f)

    @Test fun worldToScreenAndBackAreInverse() {
        val camera = IsoCamera(scale = 1.7f, pan = Vec2(40f, -25f))
        val world = Vec2(12f, -33f)
        val back = camera.screenToWorld(camera.worldToScreen(world, viewCenter), viewCenter)
        assertEquals(world.x, back.x, 0.001f)
        assertEquals(world.y, back.y, 0.001f)
    }

    @Test fun defaultCameraPutsWorldZeroInTheMiddleOfTheView() {
        assertEquals(viewCenter, IsoCamera().worldToScreen(Vec2(0f, 0f), viewCenter))
    }

    @Test fun zoomKeepsThePointUnderTheFingersFixed() {
        val camera = IsoCamera(scale = 1f, pan = Vec2(10f, 20f))
        val focus = Vec2(120f, 80f)
        val before = camera.screenToWorld(focus, viewCenter)
        val zoomed = camera.zoomAbout(focus, 2f, viewCenter)
        val after = zoomed.screenToWorld(focus, viewCenter)
        assertEquals(2f, zoomed.scale, 0.001f)
        assertEquals(before.x, after.x, 0.001f)
        assertEquals(before.y, after.y, 0.001f)
    }

    @Test fun zoomIsLimited() {
        val focus = Vec2(0f, 0f)
        assertEquals(IsoCamera.MAX_SCALE, IsoCamera().zoomAbout(focus, 100f, viewCenter).scale, 0.001f)
        assertEquals(IsoCamera.MIN_SCALE, IsoCamera().zoomAbout(focus, 0.001f, viewCenter).scale, 0.001f)
    }

    @Test fun tappingTheScreenFindsTheRightTileAfterPanAndZoom() {
        val iso = IsoMath(64f, 32f)
        val camera = IsoCamera().zoomAbout(Vec2(150f, 150f), 1.5f, viewCenter).panBy(Vec2(-30f, 45f))
        val tile = TilePos(5, 2)
        val onScreen = camera.worldToScreen(iso.tileCenter(tile), viewCenter)
        assertEquals(tile, iso.tileAt(camera.screenToWorld(onScreen, viewCenter)))
    }
}
