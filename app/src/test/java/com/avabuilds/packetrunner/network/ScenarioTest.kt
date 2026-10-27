package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.RequestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioTest {

    private fun solved(s: Scenario, scene: NetworkScene) = s.isSolved(NetworkSimulator.evaluate(scene))

    /** Plays the edits a player would make. Fails the test if the game would refuse one of them. */
    private fun build(s: Scenario, steps: (NetworkScene) -> EditResult): NetworkScene {
        val result = steps(s.scene)
        assertTrue(result.error ?: "", result.ok)
        return result.scene
    }

    private fun Scenario.place(scene: NetworkScene, type: DeviceType, at: CellPos): NetworkScene {
        val r = SceneEditor.place(scene, type, at, allowed.getValue(type).price, allowed.getValue(type).maxCount)
        assertTrue(r.error ?: "", r.ok)
        return r.scene
    }

    private fun Scenario.cable(scene: NetworkScene, a: String, b: String): NetworkScene {
        val r = SceneEditor.toggleCable(scene, a, b, cablePricePerCell)
        assertTrue(r.error ?: "", r.ok)
        return r.scene
    }

    @Test fun everyRoomStartsUnsolved() {
        listOf("house1", "house2", "house3").forEach { assertFalse(solved(Scenarios.install(it), Scenarios.install(it).scene)) }
    }

    @Test fun houseOneIsSolvedWithOneRouterNearTheMiddle() {
        val s = Scenarios.install("house1")
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(2, 2))
        scene = s.cable(scene, "socket", "router_1")
        assertTrue(solved(s, scene))
    }

    @Test fun houseOneRouterTooCloseToTheWallLeavesTheLaptopOutOfRange() {
        val s = Scenarios.install("house1")
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(1, 2))
        scene = s.cable(scene, "socket", "router_1")
        val report = NetworkSimulator.evaluate(scene)
        assertEquals(ClientStatus.NO_SIGNAL, report.forClient("laptop")!!.status)
        assertEquals(ClientStatus.CONNECTED, report.forClient("phone")!!.status)
    }

    @Test fun houseTwoCanBeSolvedWithACentralRouter() {
        val s = Scenarios.install("house2")
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(4, 2))
        scene = s.cable(scene, "socket", "router_1")
        assertTrue(solved(s, scene))
    }

    @Test fun houseTwoCanAlsoBeSolvedWithARouterAndARepeater() {
        val s = Scenarios.install("house2")
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(1, 2))
        scene = s.cable(scene, "socket", "router_1")
        assertFalse(solved(s, scene)) // the phone is still out of range
        scene = s.place(scene, DeviceType.REPEATER, CellPos(4, 3))
        assertTrue(solved(s, scene))
        assertTrue(Concepts.REPEATER in s.conceptsUsed(scene))
    }

    @Test fun houseTwoRouterNextToTheSocketCannotReachThePhone() {
        val s = Scenarios.install("house2")
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(1, 2))
        scene = s.cable(scene, "socket", "router_1")
        assertEquals(ClientStatus.NO_SIGNAL, NetworkSimulator.evaluate(scene).forClient("phone")!!.status)
    }

    @Test fun theSolutionLeavesMoneyFromTheRewardAfterMaterials() {
        val s = Scenarios.install("house1")
        val router = SceneEditor.place(s.scene, DeviceType.ROUTER, CellPos(2, 2), s.allowed.getValue(DeviceType.ROUTER).price, 1)
        val cable = SceneEditor.toggleCable(router.scene, "socket", "router_1", s.cablePricePerCell)
        val materials = router.cost + cable.cost
        assertTrue("materials ($materials) must be below the reward of 200", materials < 200)
    }

    @Test fun everyConceptIsInEnglishAndKoreanWithAnExplanationAndABookTopic() {
        val all = listOf(Concepts.ROUTER, Concepts.CABLE, Concepts.WIFI_RANGE, Concepts.BANDWIDTH_LATENCY, Concepts.APP_NEEDS, Concepts.REPEATER, Concepts.ACCESS_POINT, Concepts.HIDDEN_NODE, Concepts.RTS_CTS, Concepts.COLLISION_AVOIDANCE)
        all.forEach { c ->
            assertTrue(c.en.isNotBlank())
            assertTrue("${c.en} needs Korean", c.ko.any { it in '가'..'힣' })
            assertTrue(c.explanation.length > 20)
            assertTrue(c.book.isNotBlank())
        }
    }

    @Test fun aJobHasHintsPricesAndAGoal() {
        val s = Scenarios.install("house1")
        assertTrue(s.hints.size >= 3)
        assertTrue(s.allowed.values.all { it.price > 0 && it.maxCount > 0 })
        assertTrue(s.cablePricePerCell > 0)
        assertTrue(s.goal.isNotBlank())
        assertTrue(s.conceptsUsed(s.scene).isNotEmpty())
    }

    @Test fun roomJobsHaveARoomAndTheRoutingJobHasItsOwnScreen() {
        fun request(type: RequestType) = Request(1, type, "mina", "house1", "hi", 100, 540f)
        listOf(RequestType.INSTALL, RequestType.FIX, RequestType.HELP).forEach {
            assertEquals(it.name.lowercase(), Scenarios.forRequest(request(it))!!.id.substringBefore('_'))
        }
        assertNull(Scenarios.forRequest(request(RequestType.ROUTE)))
    }

    // ---- Step 5: what devices need, the bigger house, stars ----

    private fun house3Router(at: CellPos, extra: (Scenario, NetworkScene) -> NetworkScene = { _, scene -> scene }): Pair<Scenario, NetworkScene> {
        val s = Scenarios.install("house3")
        var scene = s.place(s.scene, DeviceType.ROUTER, at)
        scene = s.cable(scene, "socket", "router_1")
        return s to extra(s, scene)
    }

    @Test fun theBigHouseIsSolvedByOneRouterAtTheFarEndOfTheCableWhenRtsCtsIsOn() {
        val (s, scene) = house3Router(CellPos(6, 2)) { sc, r ->
            val ok = SceneEditor.setSettings(r, "router_1", DeviceSettings(rtsCts = true))
            assertTrue(ok.ok)
            ok.scene
        }
        val report = NetworkSimulator.evaluate(scene)
        assertTrue(s.isSolved(report))
        assertEquals(Verdict.OK, s.verdict(report.forClient("tv")!!))
    }

    @Test fun theSamePlacementWithoutRtsCtsCollidesAndTheMessageSendsYouToTheSettings() {
        val (s, scene) = house3Router(CellPos(6, 2))
        val report = NetworkSimulator.evaluate(scene)
        assertEquals(ClientStatus.COLLISIONS, report.forClient("tv")!!.status)
        assertFalse(s.isSolved(report))
        assertTrue(s.firstProblem(report, scene)!!.contains("Settings"))
    }

    @Test fun theBigHouseIsAlsoSolvedByACentralRouterPlusARepeaterForThePhone() {
        val (s, scene) = house3Router(CellPos(4, 2)) { sc, r -> sc.place(r, DeviceType.REPEATER, CellPos(7, 2)) }
        assertTrue(s.isSolved(NetworkSimulator.evaluate(scene)))
    }

    @Test fun beingOnlineIsNotEnoughWhenTheTvNeedsBandwidth() {
        // the repeater sits next to the TV, so the TV joins the closest signal: the slow repeater
        val (s, scene) = house3Router(CellPos(6, 2)) { sc, r -> sc.place(r, DeviceType.REPEATER, CellPos(3, 2)) }
        val tv = NetworkSimulator.evaluate(scene).forClient("tv")!!
        assertEquals(ClientStatus.CONNECTED, tv.status)
        assertEquals("repeater_1", tv.sourceId)
        assertEquals(Verdict.TOO_SLOW, s.verdict(tv))
        assertFalse(s.isSolved(NetworkSimulator.evaluate(scene)))
    }

    @Test fun aVideoCallIsTooLaggyThroughARepeater() {
        val (s, scene) = house3Router(CellPos(3, 2)) { sc, r -> sc.place(r, DeviceType.REPEATER, CellPos(6, 1)) }
        val laptop = NetworkSimulator.evaluate(scene).forClient("laptop")!!
        assertEquals("repeater_1", laptop.sourceId)
        assertEquals(Verdict.TOO_LAGGY, s.verdict(laptop))
    }

    @Test fun firstProblemExplainsWhatIsWrongInPlainWords() {
        val s = Scenarios.install("house3")
        val (_, scene) = house3Router(CellPos(6, 2)) { sc, r ->
            val polite = SceneEditor.setSettings(r, "router_1", DeviceSettings(rtsCts = true)).scene
            sc.place(polite, DeviceType.REPEATER, CellPos(3, 2)) // the TV joins this slow repeater
        }
        val message = s.firstProblem(NetworkSimulator.evaluate(scene), scene)!!
        assertTrue(message, message.startsWith("TV:") && message.contains("45"))
        val (_, good) = house3Router(CellPos(6, 2)) { _, r -> SceneEditor.setSettings(r, "router_1", DeviceSettings(rtsCts = true)).scene }
        assertNull(s.firstProblem(NetworkSimulator.evaluate(good), good))
    }

    @Test fun roomsWithoutGoalsBehaveExactlyAsBefore() {
        val s = Scenarios.install("house1")
        assertTrue(s.goals.isEmpty())
        var scene = s.place(s.scene, DeviceType.ROUTER, CellPos(2, 2))
        scene = s.cable(scene, "socket", "router_1")
        val report = NetworkSimulator.evaluate(scene)
        assertTrue(report.allConnected)
        assertTrue(s.isSolved(report))
    }

    @Test fun starsDependOnHowCloseYouGetToTheCheapestSolution() {
        val s = Scenarios.install("house3") // par 110
        assertEquals(3, s.stars(110))
        assertEquals(3, s.stars(126))
        assertEquals(2, s.stars(127))
        assertEquals(2, s.stars(176))
        assertEquals(1, s.stars(177))
        assertEquals(0, s.tip(1))
        assertEquals(10, s.tip(2))
        assertEquals(20, s.tip(3))
    }

    /** Tries every router spot (and every repeater spot) and returns the cheapest cost that solves the job. */
    private fun cheapestSolution(s: Scenario): Int {
        val room = s.scene
        val router = s.allowed.getValue(DeviceType.ROUTER).price
        val repeater = s.allowed.getValue(DeviceType.REPEATER).price
        var best = Int.MAX_VALUE
        val cells = (0 until room.width).flatMap { x -> (0 until room.height).map { y -> CellPos(x, y) } }
        for (routerCell in cells) {
            if (room.deviceAt(routerCell) != null) continue
            val withRouter = SceneEditor.place(room, DeviceType.ROUTER, routerCell, router, 1).scene
            val cabled = SceneEditor.toggleCable(withRouter, "socket", "router_1", s.cablePricePerCell)
            if (!cabled.ok) continue
            val baseCost = router + cabled.cost
            for (rts in listOf(false, true)) {
                val configured = SceneEditor.setSettings(cabled.scene, "router_1", DeviceSettings(rtsCts = rts)).scene
                if (s.isSolved(NetworkSimulator.evaluate(configured))) best = minOf(best, baseCost)
                for (repCell in cells) {
                    if (configured.deviceAt(repCell) != null) continue
                    val withRepeater = SceneEditor.place(configured, DeviceType.REPEATER, repCell, repeater, 1).scene
                    if (s.isSolved(NetworkSimulator.evaluate(withRepeater))) best = minOf(best, baseCost + repeater)
                }
            }
        }
        return best
    }

    @Test fun theParCostIsReallyTheCheapestWayToSolveEachRoom() {
        listOf("house1", "house2", "house3").forEach { house ->
            val s = Scenarios.install(house)
            assertEquals("par of $house", s.parCost, cheapestSolution(s))
        }
    }

    @Test fun theBigHouseHasACheapSettingsSolutionAndAPricierRepeaterOne() {
        val s = Scenarios.install("house3")
        assertTrue(s.goals.isNotEmpty())
        assertTrue(Concepts.APP_NEEDS in s.conceptsUsed(s.scene))
        // the repeater solution costs more than the par, so it earns fewer stars
        val (_, withRepeater) = house3Router(CellPos(4, 2)) { sc, r -> sc.place(r, DeviceType.REPEATER, CellPos(7, 2)) }
        val repeaterCost = 80 + 4 * 5 + 40
        assertTrue(s.isSolved(NetworkSimulator.evaluate(withRepeater)))
        assertTrue(repeaterCost > s.parCost)
        assertEquals(2, s.stars(repeaterCost))
    }

    // ---- Step 6: the FIX jobs ----

    private fun withRtsCts(scene: NetworkScene) = SceneEditor.setSettings(scene, "router_1", DeviceSettings(rtsCts = true)).scene

    @Test fun theCafeStartsBrokenWithCollisionsEvenThoughEveryoneIsInRange() {
        val s = Scenarios.fix("cafe")
        val report = NetworkSimulator.evaluate(s.scene)
        assertFalse(s.isSolved(report))
        assertTrue(report.clients.all { it.status == ClientStatus.COLLISIONS })
        assertTrue("the router stays online", "router_1" in report.onlineSources)
    }

    @Test fun switchingOnRtsCtsFixesTheCafeAndCostsNothing() {
        val s = Scenarios.fix("cafe")
        val report = NetworkSimulator.evaluate(withRtsCts(s.scene))
        assertTrue(s.isSolved(report))
        assertEquals(Verdict.OK, s.verdict(report.forClient("card_reader")!!)) // card payments are fast enough
        assertEquals(3, s.stars(0))
    }

    @Test fun theHouseWifiIsFixedTheSameWay() {
        val s = Scenarios.fix("house1")
        assertFalse(s.isSolved(NetworkSimulator.evaluate(s.scene)))
        assertTrue(s.isSolved(NetworkSimulator.evaluate(withRtsCts(s.scene))))
    }

    @Test fun anAccessPointByTheWindowIsAWrongFixThatCostsMoneyAndStillLeavesCollisions() {
        val s = Scenarios.fix("cafe")
        var scene = s.place(s.scene, DeviceType.ACCESS_POINT, CellPos(2, 0))
        scene = s.cable(scene, "router_1", "access_point_1")
        val report = NetworkSimulator.evaluate(scene)
        assertFalse(s.isSolved(report))
        assertTrue(report.clients.any { it.status == ClientStatus.COLLISIONS })
        val spent = 50 + SceneEditor.cableCost(scene, Cable.between("router_1", "access_point_1"), s.cablePricePerCell)
        assertEquals(1, s.stars(spent))
    }

    @Test fun aRepeaterByThePatioHappensToWorkButItIsAnExpensiveWayToFixASetting() {
        val s = Scenarios.fix("cafe")
        val scene = s.place(s.scene, DeviceType.REPEATER, CellPos(7, 3))
        assertTrue(s.isSolved(NetworkSimulator.evaluate(scene)))
        assertEquals(1, s.stars(40)) // the same job done with a setting earns three stars
    }

    @Test fun aRepeaterNextToTheCardReaderMakesPaymentsTooSlow() {
        val s = Scenarios.fix("cafe")
        val scene = withRtsCts(s.place(s.scene, DeviceType.REPEATER, CellPos(3, 2)))
        val reader = NetworkSimulator.evaluate(scene).forClient("card_reader")!!
        assertEquals("repeater_1", reader.sourceId)
        assertEquals(Verdict.TOO_LAGGY, s.verdict(reader))
    }

    @Test fun nothingInTheBuildingCanBeRemovedAndTheBuildingsCableStays() {
        val s = Scenarios.fix("cafe")
        s.scene.devices.forEach { assertFalse(it.placedByPlayer) }
        assertNotNull(SceneEditor.remove(s.scene, "router_1", 0, 0).error)
        assertNotNull(SceneEditor.toggleCable(s.scene, "socket", "router_1", 5).error)
    }

    @Test fun theCheapestFixNeedsNoPurchaseAtAll() {
        listOf("cafe", "house1").forEach { place ->
            val s = Scenarios.fix(place)
            assertEquals(0, s.parCost)
            // nothing to buy and no setting changed: still broken
            assertFalse(s.isSolved(NetworkSimulator.evaluate(s.scene)))
            // the DHCP pool is a red herring: changing it does not help
            val bigPool = SceneEditor.setSettings(s.scene, "router_1", DeviceSettings(dhcpPoolSize = 16)).scene
            assertFalse(s.isSolved(NetworkSimulator.evaluate(bigPool)))
        }
    }

    @Test fun theScanNamesTheCauseButDoesNotGiveTheCureAwayInAFixJob() {
        val s = Scenarios.fix("cafe")
        val message = s.firstProblem(NetworkSimulator.evaluate(s.scene), s.scene)!!
        assertTrue(message, message.startsWith("Card reader:") || message.startsWith("Patio phone:") || message.startsWith("Window laptop:"))
        assertTrue(message.contains("Collisions"))
        assertFalse(message.contains("Settings"))
    }

    @Test fun fixJobsTeachTheHiddenNodeConceptsAndNanaKnowsTheCure() {
        val s = Scenarios.fix("cafe")
        val concepts = s.conceptsUsed(withRtsCts(s.scene))
        assertTrue(Concepts.HIDDEN_NODE in concepts && Concepts.RTS_CTS in concepts)
        assertTrue(s.hints.any { it.contains("RTS/CTS") })
        assertTrue(s.hasGlitch)
    }

    @Test fun jobsThatNeedNoPurchaseGiveFullStarsOnlyForSpendingNothingMuch() {
        val s = Scenarios.fix("cafe")
        assertEquals(3, s.stars(0))
        assertEquals(3, s.stars(23))
        assertEquals(2, s.stars(24))
        assertEquals(1, s.stars(33))
    }

    // ---- Step 7: the HELP jobs (DHCP pool) ----

    private fun withDhcp(scene: NetworkScene, pool: Int, lease: Int) =
        SceneEditor.setSettings(scene, "router_1", scene.device("router_1")!!.settings.copy(dhcpPoolSize = pool, leaseMinutes = lease)).scene

    private fun noIpCount(scene: NetworkScene) = NetworkSimulator.evaluate(scene).clients.count { it.status == ClientStatus.NO_IP }

    @Test fun theHelpRoomStartsWithTwoDevicesWithoutAnIpAddressEvenThoughTheWifiWorks() {
        listOf("cafe", "house1").forEach { place ->
            val s = Scenarios.help(place)
            val report = NetworkSimulator.evaluate(s.scene)
            assertFalse(s.isSolved(report))
            assertEquals(2, noIpCount(s.scene))
            assertEquals(3, report.clients.count { it.status == ClientStatus.CONNECTED })
            assertTrue(report.clients.none { it.status == ClientStatus.COLLISIONS })
        }
    }

    @Test fun aBiggerPoolSolvesIt() {
        val s = Scenarios.help("cafe")
        assertTrue(solved(s, withDhcp(s.scene, pool = 10, lease = 120)))
        assertFalse(solved(s, withDhcp(s.scene, pool = 9, lease = 120)))
    }

    @Test fun aShorterLeaseAlsoSolvesIt() {
        val s = Scenarios.help("cafe")
        assertTrue(solved(s, withDhcp(s.scene, pool = 8, lease = 60)))
        assertFalse(solved(s, withDhcp(s.scene, pool = 8, lease = 120)))
    }

    @Test fun theCureCostsNothingSoItGetsThreeStars() {
        val s = Scenarios.help("house1")
        assertEquals(0, s.allowed.size)
        assertEquals(3, s.stars(spent = 0))
    }

    @Test fun theScanNamesTheCauseAndPointsToTheSettings() {
        val s = Scenarios.help("cafe")
        val problem = s.firstProblem(NetworkSimulator.evaluate(s.scene), s.scene)!!
        assertTrue(problem, "ran out of IP addresses" in problem)
        assertTrue(problem, "Settings" in problem)
    }

    @Test fun helpJobsTeachDhcpAndTheBuildingsCableCannotBeRemoved() {
        val s = Scenarios.help("cafe")
        val concepts = s.conceptsUsed(s.scene)
        assertTrue(Concepts.DHCP_POOL in concepts && Concepts.DHCP_LEASE in concepts)
        assertFalse(SceneEditor.toggleCable(s.scene, "socket", "router_1", s.cablePricePerCell).ok)
        assertTrue(s.hints.size >= 3)
    }
}
