package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatrolTest {
    private val straight = Patrol(listOf(Vec2(0f, 0f), Vec2(4f, 0f)), speed = 2f)

    @Test fun itStartsAtTheFirstPointFacingTheSecond() {
        val s = straight.stateAt(0f)
        assertEquals(0f, s.position.x, 0.001f)
        assertEquals(1f, s.heading.x, 0.001f)
    }

    @Test fun itMovesAtItsSpeed() {
        assertEquals(1f, straight.stateAt(0.5f).position.x, 0.001f)
        assertEquals(3f, straight.stateAt(1.5f).position.x, 0.001f)
    }

    @Test fun atTheEndItTurnsAroundAndComesBack() {
        assertEquals(4f, straight.stateAt(2f).position.x, 0.001f)
        val back = straight.stateAt(3f)
        assertEquals(2f, back.position.x, 0.001f)
        assertEquals(-1f, back.heading.x, 0.001f)
    }

    @Test fun aFullRoundTakesTwiceTheLengthAndThenRepeats() {
        assertEquals(straight.stateAt(0.7f).position.x, straight.stateAt(0.7f + 4f).position.x, 0.001f)
        assertEquals(0f, straight.stateAt(4f).position.x, 0.001f)
    }

    @Test fun itFollowsCornersAndTurnsItsHeading() {
        val l = Patrol(listOf(Vec2(0f, 0f), Vec2(2f, 0f), Vec2(2f, 2f)), speed = 1f)
        val before = l.stateAt(1f)
        val after = l.stateAt(3f)
        assertEquals(1f, before.heading.x, 0.001f)
        assertEquals(1f, after.heading.y, 0.001f)
        assertEquals(2f, after.position.x, 0.001f)
        assertEquals(1f, after.position.y, 0.001f)
    }

    @Test fun theHeadingIsAlwaysAUnitVector() {
        val p = Patrol(listOf(Vec2(0f, 0f), Vec2(3f, 0f), Vec2(3f, 4f)), speed = 1.3f)
        for (t in 0..40) {
            val h = p.stateAt(t * 0.37f).heading
            assertEquals(1f, Math.sqrt((h.x * h.x + h.y * h.y).toDouble()).toFloat(), 0.001f)
        }
    }

    @Test fun theStartOffsetShiftsTheFirstPosition() {
        val shifted = Patrol(listOf(Vec2(0f, 0f), Vec2(4f, 0f)), speed = 2f, startAt = 1f)
        assertEquals(1f, shifted.stateAt(0f).position.x, 0.001f)
    }

    @Test fun keepingRightPutsTheCharacterOnTheRightHandOfTheHeading() {
        // heading along +x: the right hand is +y in tile coordinates
        val p = keepRight(Vec2(2f, 2f), Vec2(1f, 0f), 0.3f)
        assertEquals(2f, p.x, 0.001f)
        assertEquals(2.3f, p.y, 0.001f)
        // coming back the other way, the other side of the road
        val q = keepRight(Vec2(2f, 2f), Vec2(-1f, 0f), 0.3f)
        assertEquals(1.7f, q.y, 0.001f)
    }

    @Test fun aPatrolNeedsALineToWalkAlong() {
        var refused = false
        try { Patrol(listOf(Vec2(1f, 1f)), speed = 1f) } catch (e: IllegalArgumentException) { refused = true }
        assertTrue(refused)
    }
}

class TownLifeTest {
    private val town = Town.default()
    private val life = TownLife.default()

    private fun onRoad(v: Vec2) = TilePos(Math.round(v.x), Math.round(v.y)) in town.roads

    @Test fun everyCornerOfEveryRouteIsOnARoad() {
        (life.walkers.map { it.patrol } + life.drivers.map { it.patrol }).forEach { p ->
            p.points.forEach { assertTrue("$it", onRoad(it)) }
        }
    }

    @Test fun walkersAndCarsStayOnTheRoadsAtAnyTime() {
        (life.walkers.map { it.patrol } + life.drivers.map { it.patrol }).forEach { p ->
            for (t in 0..200) assertTrue(onRoad(p.stateAt(t * 0.9f).position))
        }
    }

    @Test fun carsAreFasterThanPeopleAndThereAreSeveralOfBoth() {
        assertTrue(life.walkers.size >= 3 && life.drivers.size >= 2)
        assertTrue(life.drivers.minOf { it.patrol.speed } > life.walkers.maxOf { it.patrol.speed })
    }
}
