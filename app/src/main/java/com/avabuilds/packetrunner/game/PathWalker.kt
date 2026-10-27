package com.avabuilds.packetrunner.game

import kotlin.math.hypot

/**
 * Moves a character along a path at a steady speed.
 *
 * Positions are in tile coordinates. [speed] is in tiles per second. Call [advance] every frame
 * with the time passed; [position] then follows the path smoothly from tile center to tile center.
 */
class PathWalker(start: Vec2, private val path: List<TilePos>, private val speed: Float) {

    var position: Vec2 = start
        private set

    private var next = 0 // index of the tile center we are walking toward

    val finished: Boolean get() = next >= path.size

    /** The tiles the character has not reached yet, in order. */
    val remaining: List<TilePos> get() = path.drop(next)

    fun advance(seconds: Float) {
        var budget = speed * seconds
        while (budget > 0f && !finished) {
            val target = Vec2(path[next].x.toFloat(), path[next].y.toFloat())
            val dx = target.x - position.x
            val dy = target.y - position.y
            val distance = hypot(dx, dy)
            if (distance <= budget) {
                position = target
                budget -= distance
                next++
            } else {
                position = Vec2(position.x + dx / distance * budget, position.y + dy / distance * budget)
                budget = 0f
            }
        }
    }
}
