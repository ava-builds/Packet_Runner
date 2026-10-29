package com.avabuilds.packetrunner.ui

import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.Vec2
import com.avabuilds.packetrunner.game.pointInPolygon
import com.avabuilds.packetrunner.ui.sprites.buildingSilhouette
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingSilhouetteTest {
    private val center = Vec2(200f, 300f)
    private val tw = 100f

    @Test fun officeSilhouetteCoversItsTallWalls() {
        val office = buildingSilhouette(BuildingType.OFFICE, center, tw, size = 2)
        assertTrue(pointInPolygon(office, Vec2(200f, 200f))) // halfway up the wall
        assertTrue(pointInPolygon(office, Vec2(115f, 180f))) // near the left edge, up by the roof
    }

    @Test fun houseRoofComesToAPointSoItsUpperCornersAreNotPartOfTheHouse() {
        val house = buildingSilhouette(BuildingType.HOUSE, center, tw, size = 2)
        assertTrue(pointInPolygon(house, Vec2(200f, 220f))) // inside the roof, under the apex
        // far up and to the side: above the slope of the roof, so a tap here is not on the house
        assertFalse(pointInPolygon(house, Vec2(115f, 200f)))
    }

    @Test fun tappingTheSkyAboveABuildingMissesIt() {
        BuildingType.entries.forEach { type ->
            assertFalse(type.name, pointInPolygon(buildingSilhouette(type, center, tw, 2), Vec2(200f, -1000f))) // far above even the tallest building
        }
    }

    @Test fun theTowerIsTallerThanTheOfficeSoATapHighUpHitsOnlyTheTower() {
        val tower = buildingSilhouette(BuildingType.TOWER, center, tw, size = 2)
        val office = buildingSilhouette(BuildingType.OFFICE, center, tw, size = 2)
        val high = Vec2(center.x, center.y - tw * 2.2f)
        assertTrue(pointInPolygon(tower, high))
        assertFalse(pointInPolygon(office, high))
    }
}
