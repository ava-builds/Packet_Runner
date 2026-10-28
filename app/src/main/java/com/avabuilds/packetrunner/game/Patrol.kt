package com.avabuilds.packetrunner.game

import kotlin.math.hypot

/** Where a patrolling character is and which way it is facing (a unit vector), in tile coordinates. */
data class PatrolState(val position: Vec2, val heading: Vec2)

/**
 * A character that walks back and forth along a line made of [points] (tile coordinates) at [speed] tiles
 * per second. At the end it turns around. [startAt] shifts where on the line it begins, in tiles.
 * It depends only on the time, so the same time always gives the same place.
 */
class Patrol(val points: List<Vec2>, val speed: Float, private val startAt: Float = 0f) {
    private val cumulative = FloatArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    }

    /** The length of the line, in tiles. */
    val length: Float get() = cumulative.last()

    init {
        require(points.size >= 2 && length > 0f) { "A patrol needs a line to walk along" }
        require(speed > 0f) { "A patrol must move" }
    }

    fun stateAt(seconds: Float): PatrolState {
        val round = 2f * length
        var d = (seconds * speed + startAt) % round
        if (d < 0f) d += round
        val forward = d <= length
        if (!forward) d = round - d

        var i = 1
        while (i < points.size - 1 && cumulative[i] < d) i++
        val a = points[i - 1]
        val b = points[i]
        val segment = cumulative[i] - cumulative[i - 1]
        val t = if (segment == 0f) 0f else (d - cumulative[i - 1]) / segment
        val dx = (b.x - a.x) / segment
        val dy = (b.y - a.y) / segment
        val sign = if (forward) 1f else -1f
        return PatrolState(Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t), Vec2(dx * sign, dy * sign))
    }
}

/** A neighbor out for a walk. [shirt] picks a color; [sidewalk] is how far from the middle of the road the walk keeps to the side. */
data class Walker(val patrol: Patrol, val shirt: Int, val sidewalk: Float = SIDEWALK)

/** A car driving along the roads; [lane] is how far it keeps to the right of the middle of the road. */
data class Driver(val patrol: Patrol, val color: Int, val lane: Float = LANE)

const val SIDEWALK = 0.3f
const val LANE = 0.17f

/** The tile position with the sideways shift of a lane or a sidewalk: a step of [amount] tiles to the right of [heading]. */
fun keepRight(position: Vec2, heading: Vec2, amount: Float): Vec2 =
    Vec2(position.x - heading.y * amount, position.y + heading.x * amount)

/** The people and cars of the town. They only use roads, so they never walk through anything. */
class TownLife(val walkers: List<Walker>, val drivers: List<Driver>) {
    companion object {
        private fun line(vararg tiles: Pair<Int, Int>) = tiles.map { Vec2(it.first.toFloat(), it.second.toFloat()) }

        fun default() = TownLife(
            walkers = listOf(
                Walker(Patrol(line(0 to 5, 4 to 5), speed = 0.45f, startAt = 1f), shirt = 0),
                Walker(Patrol(line(5 to 6, 5 to 9), speed = 0.4f, startAt = 0.5f), shirt = 1),
                Walker(Patrol(line(6 to 5, 9 to 5), speed = 0.5f, startAt = 2f), shirt = 2),
                Walker(Patrol(line(5 to 0, 5 to 4), speed = 0.35f, startAt = 0.2f), shirt = 3),
            ),
            drivers = listOf(
                Driver(Patrol(line(0 to 5, 5 to 5, 5 to 9), speed = 1.5f), color = 0),
                Driver(Patrol(line(5 to 0, 5 to 5, 9 to 5), speed = 1.1f, startAt = 6f), color = 2),
            ),
        )
    }
}
