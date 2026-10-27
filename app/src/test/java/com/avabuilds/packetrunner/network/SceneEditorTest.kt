package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneEditorTest {
    private val base = NetworkScene(
        width = 8, height = 5,
        devices = listOf(
            Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)),
            Device("laptop", DeviceType.LAPTOP, CellPos(6, 1)),
        ),
    )

    private fun placeRouter(scene: NetworkScene = base, at: CellPos = CellPos(2, 2)) =
        SceneEditor.place(scene, DeviceType.ROUTER, at, price = 80, maxCount = 1)

    @Test fun placingADeviceAddsItAndChargesThePrice() {
        val result = placeRouter()
        assertTrue(result.ok)
        assertEquals(80, result.cost)
        val router = result.scene.device("router_1")!!
        assertEquals(CellPos(2, 2), router.at)
        assertTrue(router.placedByPlayer)
    }

    @Test fun placingIsRefusedOutsideTheRoomOnATakenCellOrForTheWrongKindOfThing() {
        assertNotNull(placeRouter(at = CellPos(9, 9)).error)
        assertNotNull(placeRouter(at = CellPos(0, 2)).error) // the wall socket is there
        assertNotNull(SceneEditor.place(base, DeviceType.LAPTOP, CellPos(2, 2), 10, 1).error)
        assertNotNull(SceneEditor.place(base, DeviceType.WALL_SOCKET, CellPos(2, 2), 10, 1).error)
    }

    @Test fun refusedEditsLeaveTheSceneAndTheMoneyAlone() {
        val result = placeRouter(at = CellPos(9, 9))
        assertEquals(base, result.scene)
        assertEquals(0, result.cost)
    }

    @Test fun youCannotPlaceMoreThanTheAllowedNumber() {
        val one = placeRouter().scene
        val second = SceneEditor.place(one, DeviceType.ROUTER, CellPos(3, 3), 80, maxCount = 1)
        assertNotNull(second.error)
    }

    @Test fun idsStayUniqueAfterARemoval() {
        val a = SceneEditor.place(base, DeviceType.REPEATER, CellPos(2, 2), 40, maxCount = 2).scene
        val b = SceneEditor.place(a, DeviceType.REPEATER, CellPos(3, 3), 40, maxCount = 2).scene
        assertEquals(setOf("repeater_1", "repeater_2"), b.devices.filter { it.type == DeviceType.REPEATER }.map { it.id }.toSet())
        val c = SceneEditor.remove(b, "repeater_1", 40, 5).scene
        val d = SceneEditor.place(c, DeviceType.REPEATER, CellPos(4, 4), 40, maxCount = 2).scene
        assertEquals(2, d.devices.count { it.type == DeviceType.REPEATER })
        assertEquals(d.devices.map { it.id }.size, d.devices.map { it.id }.toSet().size)
    }

    @Test fun layingACableCostsItsLengthRoundedUpTimesThePrice() {
        val scene = placeRouter().scene // socket (0,2) to router (2,2): 2 cells
        val result = SceneEditor.toggleCable(scene, "socket", "router_1", pricePerCell = 5)
        assertTrue(result.ok)
        assertEquals(10, result.cost)
        assertTrue(Cable.between("router_1", "socket") in result.scene.cables)

        val diagonal = placeRouter(at = CellPos(2, 4)).scene // distance sqrt(8) = 2.83 -> 3 cells
        assertEquals(15, SceneEditor.toggleCable(diagonal, "socket", "router_1", 5).cost)
    }

    @Test fun layingTheSameCableAgainRemovesItAndRefundsIt() {
        val laid = SceneEditor.toggleCable(placeRouter().scene, "socket", "router_1", 5)
        val removed = SceneEditor.toggleCable(laid.scene, "router_1", "socket", 5)
        assertTrue(removed.scene.cables.isEmpty())
        assertEquals(-10, removed.cost)
    }

    @Test fun aCableThatIsTooLongIsRefused() {
        val far = placeRouter(at = CellPos(7, 2)).scene // 7 cells from the socket
        val result = SceneEditor.toggleCable(far, "socket", "router_1", 5)
        assertNotNull(result.error)
        assertTrue(result.scene.cables.isEmpty())
    }

    @Test fun deviceWithoutACableSocketCannotBeCabled() {
        val scene = placeRouter().scene
        assertNotNull(SceneEditor.toggleCable(scene, "router_1", "laptop", 5).error)
        assertNotNull(SceneEditor.toggleCable(scene, "router_1", "router_1", 5).error)
        assertNotNull(SceneEditor.toggleCable(scene, "router_1", "nothing", 5).error)
    }

    @Test fun removingADeviceRefundsHalfAndTakesItsCablesWithIt() {
        val cabled = SceneEditor.toggleCable(placeRouter().scene, "socket", "router_1", 5).scene
        val result = SceneEditor.remove(cabled, "router_1", price = 80, cablePricePerCell = 5)
        assertNull(result.scene.device("router_1"))
        assertTrue(result.scene.cables.isEmpty())
        assertEquals(-(40 + 10), result.cost) // half of 80, plus the 10 spent on the cable
    }

    @Test fun thingsThatWereAlreadyThereCannotBeRemoved() {
        assertNotNull(SceneEditor.remove(base, "socket", 0, 0).error)
        assertNotNull(SceneEditor.remove(base, "laptop", 0, 0).error)
    }

    @Test fun settingsCanBeChangedOnADevice() {
        val scene = placeRouter().scene
        val result = SceneEditor.setSettings(scene, "router_1", DeviceSettings(rtsCts = true, dhcpPoolSize = 4))
        assertTrue(result.scene.device("router_1")!!.settings.rtsCts)
        assertEquals(4, result.scene.device("router_1")!!.settings.dhcpPoolSize)
        assertFalse(SceneEditor.setSettings(scene, "ghost", DeviceSettings()).ok)
    }

    @Test fun aCableThatIsPartOfTheBuildingCannotBeRemoved() {
        val wall = Cable.between("socket", "router_1")
        val scene = NetworkScene(
            8, 5,
            listOf(Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)), Device("router_1", DeviceType.ROUTER, CellPos(3, 2))),
            cables = setOf(wall), fixedCables = setOf(wall),
        )
        val result = SceneEditor.toggleCable(scene, "socket", "router_1", 5)
        assertNotNull(result.error)
        assertEquals(scene, result.scene)
        assertEquals(0, result.cost)
    }

    @Test fun aDeviceWithoutANameShowsItsTypeAndANamedOneShowsItsName() {
        assertEquals("Laptop", Device("a", DeviceType.LAPTOP, CellPos(0, 0)).displayName)
        assertEquals("Window laptop", Device("a", DeviceType.LAPTOP, CellPos(0, 0), name = "Window laptop").displayName)
    }
}
