package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CableRoutingTest {
    private val town = Town.default()

    @Test fun everyBuildingHasACableFromTheOfficeDoor() {
        val links = TownNetwork.links(town)
        val officeDoor = town.building("office").door
        assertEquals(town.buildings.size - 1, links.size)
        links.forEach { link ->
            assertEquals(officeDoor, link.path.first())
            assertEquals(town.building(link.buildingId).door, link.path.last())
        }
    }

    @Test fun theOfficeItselfHasNoCable() {
        assertTrue(TownNetwork.links(town).none { it.buildingId == "office" })
    }

    @Test fun aCableOnlyCrossesWalkableTilesAndMovesOneTileAtATime() {
        TownNetwork.links(town).forEach { link ->
            link.path.forEach { assertTrue("$it", town.grid.isWalkable(it)) }
            link.path.zipWithNext().forEach { (a, b) -> assertEquals(1, kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y)) }
        }
    }

    @Test fun cablesFollowTheRoadsWheneverTheyCan() {
        val link = TownNetwork.links(town).first { it.buildingId == "cafe" }
        // office door -> road -> road -> road -> cafe door: three of the five tiles are road
        assertEquals(5, link.path.size)
        assertEquals(3, link.path.count { it in town.roads })
    }

    @Test fun aCableNeverLoopsBackOnItself() {
        TownNetwork.links(town).forEach { assertEquals(it.path.size, it.path.toSet().size) }
    }

    @Test fun anUnreachableDoorGivesNoRoute() {
        val grid = Grid(3, 1, blocked = setOf(TilePos(1, 0)))
        assertTrue(CableRouting.route(grid, emptySet(), TilePos(0, 0), TilePos(2, 0)).isEmpty())
    }

    @Test fun routeToTheSameTileIsThatTile() {
        assertEquals(listOf(TilePos(1, 1)), CableRouting.route(Grid(3, 3), emptySet(), TilePos(1, 1), TilePos(1, 1)))
    }

    @Test fun aRoadIsPreferredEvenWhenItIsLonger() {
        // a straight line across grass (length 4) against a road around it (length 6)
        val roads = setOf(TilePos(0, 1), TilePos(0, 2), TilePos(1, 2), TilePos(2, 2), TilePos(3, 2), TilePos(4, 2), TilePos(4, 1))
        val route = CableRouting.route(Grid(5, 3), roads, TilePos(0, 0), TilePos(4, 0))
        assertTrue(route.size > 5)
        assertTrue(roads.all { it in route })
        assertFalse(TilePos(2, 0) in route)
    }
}
