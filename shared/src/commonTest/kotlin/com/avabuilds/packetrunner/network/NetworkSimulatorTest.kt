package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class NetworkSimulatorTest {
    private val socket = Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2))
    private val laptop = Device("laptop", DeviceType.LAPTOP, CellPos(3, 2))
    private val phone = Device("phone", DeviceType.PHONE, CellPos(3, 3))

    private fun router(at: CellPos, settings: DeviceSettings = DeviceSettings()) =
        Device("router_1", DeviceType.ROUTER, at, settings, placedByPlayer = true)

    private fun room(vararg extra: Device, cables: Set<Cable> = emptySet(), clients: List<Device> = listOf(laptop, phone)) =
        NetworkScene(10, 6, listOf(socket) + clients + extra, cables)

    private fun cable(a: String, b: String) = Cable.between(a, b)

    @Test fun withoutAnyWifiNobodyHasSignal() {
        val report = NetworkSimulator.evaluate(room())
        assertTrue(report.clients.all { it.status == ClientStatus.NO_SIGNAL })
        assertFalse(report.allConnected)
    }

    @Test fun aRouterThatIsNotCabledReachesButHasNoInternet() {
        val report = NetworkSimulator.evaluate(room(router(CellPos(2, 2))))
        assertTrue(report.clients.all { it.status == ClientStatus.NO_UPLINK })
        assertTrue(report.onlineSources.isEmpty())
    }

    @Test fun aRouterFarFromAClientIsJustOutOfRange() {
        val report = NetworkSimulator.evaluate(room(router(CellPos(9, 5)), cables = setOf(cable("socket", "router_1"))))
        // the cable is far too long to lay in the game, but the simulator only follows what is there
        assertEquals(ClientStatus.NO_SIGNAL, report.forClient("laptop")!!.status)
    }

    @Test fun aCabledRouterInRangeConnectsEveryone() {
        val report = NetworkSimulator.evaluate(room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1"))))
        assertTrue(report.allConnected)
        assertEquals(setOf("router_1"), report.onlineSources)
        val laptopReport = report.forClient("laptop")!!
        assertEquals(listOf("socket", "router_1", "laptop"), laptopReport.path)
        assertEquals("router_1", laptopReport.sourceId)
    }

    @Test fun theFartherTheClientTheWeakerTheSignalAndTheSlowerTheConnection() {
        val near = Device("near", DeviceType.PHONE, CellPos(3, 2))
        val far = Device("far", DeviceType.LAPTOP, CellPos(5, 2))
        val report = NetworkSimulator.evaluate(
            room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1")), clients = listOf(near, far)),
        )
        val n = report.forClient("near")!!
        val f = report.forClient("far")!!
        assertTrue(n.signal > f.signal)
        assertTrue(n.speedMbps > f.speedMbps)
        assertTrue(n.latencyMs < f.latencyMs)
    }

    @Test fun aClientBeyondTheRouterRangeStaysOffline() {
        val tooFar = Device("far", DeviceType.LAPTOP, CellPos(8, 2))
        val report = NetworkSimulator.evaluate(
            room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1")), clients = listOf(laptop, tooFar)),
        )
        assertEquals(ClientStatus.CONNECTED, report.forClient("laptop")!!.status)
        assertEquals(ClientStatus.NO_SIGNAL, report.forClient("far")!!.status)
        assertFalse(report.allConnected)
    }

    @Test fun aRepeaterExtendsTheRangeButHalvesTheSpeedAndAddsDelay() {
        val far = Device("far", DeviceType.LAPTOP, CellPos(8, 2))
        val withoutRepeater = NetworkSimulator.evaluate(
            room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1")), clients = listOf(far)),
        )
        assertEquals(ClientStatus.NO_SIGNAL, withoutRepeater.forClient("far")!!.status)

        val repeater = Device("repeater_1", DeviceType.REPEATER, CellPos(5, 2), placedByPlayer = true)
        val report = NetworkSimulator.evaluate(
            room(router(CellPos(2, 2)), repeater, cables = setOf(cable("socket", "router_1")), clients = listOf(far)),
        )
        val r = report.forClient("far")!!
        assertEquals(ClientStatus.CONNECTED, r.status)
        assertEquals("repeater_1", r.sourceId)
        assertEquals(listOf("socket", "router_1", "repeater_1", "far"), r.path)
        assertTrue(r.speedMbps <= 50)

        // the same client right next to the router is faster and has less delay
        val close = NetworkSimulator.evaluate(
            room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1")), clients = listOf(Device("far", DeviceType.LAPTOP, CellPos(3, 2)))),
        ).forClient("far")!!
        assertTrue(close.speedMbps > r.speedMbps)
        assertTrue(close.latencyMs < r.latencyMs)
    }

    @Test fun aRepeaterOutOfTheRoutersRangeIsOffline() {
        val repeater = Device("repeater_1", DeviceType.REPEATER, CellPos(8, 5), placedByPlayer = true)
        val report = NetworkSimulator.evaluate(room(router(CellPos(2, 2)), repeater, cables = setOf(cable("socket", "router_1"))))
        assertFalse("repeater_1" in report.onlineSources)
    }

    @Test fun anAccessPointOnACableToTheRouterIsOnline() {
        val ap = Device("access_point_1", DeviceType.ACCESS_POINT, CellPos(3, 3), placedByPlayer = true)
        val report = NetworkSimulator.evaluate(
            room(router(CellPos(1, 2)), ap, cables = setOf(cable("socket", "router_1"), cable("router_1", "access_point_1"))),
        )
        assertTrue("access_point_1" in report.onlineSources)
    }

    @Test fun theDhcpPoolDecidesHowManyDevicesGetAnAddress() {
        val small = router(CellPos(2, 2), DeviceSettings(dhcpPoolSize = 1))
        val report = NetworkSimulator.evaluate(room(small, cables = setOf(cable("socket", "router_1"))))
        val statuses = report.clients.map { it.status }
        assertEquals(1, statuses.count { it == ClientStatus.CONNECTED })
        assertEquals(1, statuses.count { it == ClientStatus.NO_IP })
        // addresses go out in a fixed order, so the result is repeatable
        assertEquals(report, NetworkSimulator.evaluate(room(small, cables = setOf(cable("socket", "router_1")))))
    }

    @Test fun anAccessPointAloneCannotHandOutAddresses() {
        val ap = Device("access_point_1", DeviceType.ACCESS_POINT, CellPos(2, 2), placedByPlayer = true)
        val report = NetworkSimulator.evaluate(room(ap, cables = setOf(cable("socket", "access_point_1"))))
        assertTrue(report.clients.all { it.status == ClientStatus.NO_IP })
    }

    @Test fun twoClientsThatCannotHearEachOtherCollideUnlessTheRouterAsksPermission() {
        val left = Device("left", DeviceType.LAPTOP, CellPos(1, 2))
        val right = Device("right", DeviceType.PHONE, CellPos(9, 2)) // 8 cells apart: hidden from each other
        val wide = DeviceSettings()
        val bigRouter = { s: DeviceSettings -> router(CellPos(5, 2), s) }
        val cables = setOf(cable("socket", "router_1"))

        // the router is 4 cells from both, so both are in range (and the socket is 5 cells from it)
        val collide = NetworkSimulator.evaluate(room(bigRouter(wide), cables = cables, clients = listOf(left, right)))
        assertTrue(collide.clients.all { it.status == ClientStatus.COLLISIONS })

        val polite = NetworkSimulator.evaluate(room(bigRouter(DeviceSettings(rtsCts = true)), cables = cables, clients = listOf(left, right)))
        assertTrue(polite.allConnected)
        assertTrue(polite.forClient("left")!!.speedMbps > collide.forClient("left")!!.speedMbps)
    }

    @Test fun clientsThatCanHearEachOtherNeverCollide() {
        val report = NetworkSimulator.evaluate(room(router(CellPos(2, 2)), cables = setOf(cable("socket", "router_1"))))
        assertTrue(report.clients.none { it.status == ClientStatus.COLLISIONS })
    }

    @Test fun withoutAWallSocketNothingIsOnline() {
        val scene = NetworkScene(10, 6, listOf(laptop, router(CellPos(2, 2))), emptySet())
        assertTrue(NetworkSimulator.evaluate(scene).onlineSources.isEmpty())
    }

    @Test fun aRoomWithoutClientsIsNeverSolved() {
        assertFalse(NetworkSimulator.evaluate(NetworkScene(5, 5, listOf(socket))).allConnected)
    }
}
