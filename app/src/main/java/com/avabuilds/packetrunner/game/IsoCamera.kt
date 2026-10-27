package com.avabuilds.packetrunner.game

/**
 * The camera for panning and zooming the town.
 *
 * A world point appears on screen at: viewCenter + pan + world * scale.
 * [viewCenter] is the middle of the view, in pixels.
 */
data class IsoCamera(val scale: Float = 1f, val pan: Vec2 = Vec2(0f, 0f)) {

    fun worldToScreen(world: Vec2, viewCenter: Vec2): Vec2 = viewCenter + pan + world * scale

    fun screenToWorld(screen: Vec2, viewCenter: Vec2): Vec2 = (screen - viewCenter - pan) * (1f / scale)

    fun panBy(delta: Vec2) = copy(pan = pan + delta)

    /** Zoom by [factor] while the world point under [focus] (for example, the pinch center) stays put. */
    fun zoomAbout(focus: Vec2, factor: Float, viewCenter: Vec2): IsoCamera {
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val worldUnderFocus = screenToWorld(focus, viewCenter)
        // Solve viewCenter + newPan + world * newScale = focus for newPan
        return IsoCamera(newScale, focus - viewCenter - worldUnderFocus * newScale)
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 3f
    }
}
