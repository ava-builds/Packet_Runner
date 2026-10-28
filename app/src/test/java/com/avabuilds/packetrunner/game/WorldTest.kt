package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {
    private val town = Town.default()
    private val w = town.width
    private val h = town.height

    @Test fun theTownGridIsNeverRiverBridgeOrScenery() {
        for (x in 0 until w) for (y in 0 until h) {
            assertFalse(World.isRiver(x, y))
            assertFalse(World.isBridge(x, y))
            assertFalse(World.treeAt(x, y, w, h))
        }
    }

    @Test fun theRiverLiesBeyondTheTownAndTheBridgeCrossesItAlongTheMainRoad() {
        assertTrue(World.RIVER_FROM >= h)
        for (y in World.RIVER_FROM..World.RIVER_TO) {
            assertTrue(World.isBridge(World.BRIDGE_X, y))
            assertFalse(World.isRiver(World.BRIDGE_X, y))
            assertTrue(World.isRiver(World.BRIDGE_X - 1, y))
            assertTrue(World.isRiver(World.BRIDGE_X + 1, y))
        }
    }

    @Test fun theMainRoadCarriesOnOverTheBridge() {
        assertTrue(town.roads.contains(TilePos(World.BRIDGE_X, h - 1)))
        assertTrue(World.isOutsideRoad(World.BRIDGE_X, h, h))
        assertTrue(World.isOutsideRoad(World.BRIDGE_X, World.RIVER_TO + 1, h))
        assertFalse(World.isOutsideRoad(World.BRIDGE_X + 1, h, h))
    }

    @Test fun landFadesAwayWithDistanceFromTheTown() {
        assertEquals(1f, World.fade(5, 5, w, h), 0.0001f)
        val near = World.fade(-1, 5, w, h)
        val far = World.fade(-World.PAD, 5, w, h)
        assertTrue(near > far)
        assertTrue(far in 0f..0.35f)
        assertEquals(0f, World.fade(-World.PAD - 5, 5, w, h), 0.0001f)
    }

    @Test fun distanceOutsideCountsTilesFromTheGrid() {
        assertEquals(0, World.distanceOutside(0, 0, w, h))
        assertEquals(1, World.distanceOutside(-1, 3, w, h))
        assertEquals(3, World.distanceOutside(w + 2, 4, w, h))
        assertEquals(4, World.distanceOutside(-2, h + 3, w, h))
    }

    @Test fun sceneryTreesAreStableAndNeverOnWaterRoadsOrHills() {
        var trees = 0
        for (x in -World.PAD until w + World.PAD) for (y in -World.PAD until h + World.PAD) {
            val first = World.treeAt(x, y, w, h)
            assertEquals(first, World.treeAt(x, y, w, h))
            if (first) {
                trees++
                assertFalse(World.isRiver(x, y) || World.isBridge(x, y) || World.isOutsideRoad(x, y, h) || World.underHill(x, y))
            }
        }
        assertTrue("some trees", trees > 10)
    }

    @Test fun hillsStayOutsideTheTown() {
        World.hills.forEach { hill ->
            val nearestX = hill.x.coerceIn(0f, (w - 1).toFloat())
            val nearestY = hill.y.coerceIn(0f, (h - 1).toFloat())
            val dx = hill.x - nearestX
            val dy = hill.y - nearestY
            assertTrue(Math.sqrt((dx * dx + dy * dy).toDouble()) > hill.radius * 0.8)
        }
    }
}

class PropsTest {
    private val town = Town.default()

    @Test fun everyBlockingPropBlocksWalkingAndALampDoesNot() {
        town.props.forEach { p ->
            assertEquals("${p.kind} at ${p.tile}", !p.blocks, town.grid.isWalkable(p.tile))
        }
    }

    @Test fun propsNeverStandOnRoadsBuildingsTreesOrDoors() {
        val doors = town.buildings.map { it.door }.toSet()
        town.props.forEach { p ->
            assertFalse(p.tile in town.roads)
            assertTrue(town.buildingAt(p.tile) == null)
            assertFalse(p.tile in town.trees)
            assertFalse(p.tile in doors)
            assertFalse(p.tile == town.start)
        }
    }

    @Test fun noTwoPropsShareATile() {
        assertEquals(town.props.size, town.props.map { it.tile }.toSet().size)
    }

    @Test fun theTownHasEveryKindOfProp() {
        assertEquals(PropKind.entries.toSet(), town.props.map { it.kind }.toSet())
    }

    @Test fun aPropOnARoadIsRefused() {
        var refused = false
        try {
            TownMap(10, 10, roads = setOf(TilePos(1, 1)), buildings = emptyList(), trees = emptySet(), start = TilePos(0, 0), props = listOf(Prop(PropKind.BENCH, TilePos(1, 1))))
        } catch (e: IllegalArgumentException) { refused = true }
        assertTrue(refused)
    }

    @Test fun aPropOnADoorIsRefused() {
        val house = Building("h", BuildingType.HOUSE, "House", TilePos(2, 2), 2, door = TilePos(4, 2))
        var refused = false
        try {
            TownMap(10, 10, emptySet(), listOf(house), emptySet(), start = TilePos(0, 0), props = listOf(Prop(PropKind.CAR, TilePos(4, 2))))
        } catch (e: IllegalArgumentException) { refused = true }
        assertTrue(refused)
    }
}
