package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TownMapTest {
    private val town = Town.default()

    private fun building(origin: TilePos, door: TilePos, id: String = "b") =
        Building(id, BuildingType.HOUSE, "House", origin, size = 2, door = door)

    private fun townWith(
        buildings: List<Building>,
        roads: Set<TilePos> = emptySet(),
        trees: Set<TilePos> = emptySet(),
        start: TilePos = TilePos(0, 0),
    ) = TownMap(10, 10, roads, buildings, trees, start)

    @Test fun defaultTownIsValidAndHasTheExpectedBuildings() {
        assertEquals(setOf("office", "cafe", "house1", "house2", "house3", "cityhall", "apartments", "tower"), town.buildings.map { it.id }.toSet())
        assertEquals(town.building("office").door, town.start)
    }

    @Test fun buildingsAndTreesBlockWalkingButRoadsAndDoorsDoNot() {
        town.buildings.forEach { b ->
            b.footprint.forEach { assertTrue("$it should be blocked", !town.grid.isWalkable(it)) }
            assertTrue(town.grid.isWalkable(b.door))
        }
        town.trees.forEach { assertTrue(!town.grid.isWalkable(it)) }
        town.roads.forEach { assertTrue(town.grid.isWalkable(it)) }
    }

    @Test fun everyDoorAndEveryWalkableTileCanBeReachedFromTheOffice() {
        for (x in 0 until town.width) for (y in 0 until town.height) {
            val tile = TilePos(x, y)
            if (town.grid.isWalkable(tile)) {
                assertTrue("$tile unreachable", Pathfinding.findPath(town.grid, town.start, tile).isNotEmpty())
            }
        }
    }

    @Test fun buildingAtFindsTheBuildingOnAnyOfItsTiles() {
        val cafe = town.building("cafe")
        cafe.footprint.forEach { assertEquals(cafe, town.buildingAt(it)) }
        assertNull(town.buildingAt(TilePos(0, 1)))
        assertNotNull(town.buildingAt(TilePos(2, 3)))
    }

    @Test fun doorSideAndIndexFollowTheWallTheDoorTouches() {
        val right = building(TilePos(2, 2), door = TilePos(4, 3))
        assertEquals(DoorSide.RIGHT, right.doorSide)
        assertEquals(1, right.doorIndex)
        val left = building(TilePos(2, 2), door = TilePos(3, 4))
        assertEquals(DoorSide.LEFT, left.doorSide)
        assertEquals(1, left.doorIndex)
    }

    @Test fun depthIsTheFrontCornerOfTheFootprint() {
        assertEquals(6f, town.building("office").depth, 0.001f) // origin (2,2), front tile (3,3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun doorOnTheBackWallIsRejected() {
        building(TilePos(2, 2), door = TilePos(1, 2)) // x = origin.x - 1 is the hidden back wall
    }

    @Test(expected = IllegalArgumentException::class)
    fun overlappingBuildingsAreRejected() {
        townWith(listOf(building(TilePos(2, 2), TilePos(4, 2), "a"), building(TilePos(3, 3), TilePos(5, 3), "b")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildingOutsideTheGridIsRejected() {
        townWith(listOf(building(TilePos(9, 9), TilePos(11, 9))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun treeOnARoadIsRejected() {
        townWith(emptyList(), roads = setOf(TilePos(3, 3)), trees = setOf(TilePos(3, 3)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blockedDoorIsRejected() {
        townWith(listOf(building(TilePos(2, 2), TilePos(4, 2))), trees = setOf(TilePos(4, 2)))
    }
}
