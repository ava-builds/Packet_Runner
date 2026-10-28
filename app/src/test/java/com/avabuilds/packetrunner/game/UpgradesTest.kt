package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpgradesTest {
    private val neighbors = Neighbors.default()
    private val generator = RequestGenerator(2026L)
    private fun rich() = GameEngine.newGame(neighbors, generator).copy(money = 2000)

    @Test fun buyingTakesTheLevelPriceAndRaisesTheLevel() {
        val s = GameEngine.buy(rich(), Upgrades.SCOOTER)
        assertEquals(2000 - 250, s.money)
        assertEquals(1, s.upgrades[Upgrades.SCOOTER])
    }

    @Test fun theSecondLevelCostsMoreAndTheThirdDoesNotExist() {
        var s = GameEngine.buy(rich(), Upgrades.SCOOTER)
        s = GameEngine.buy(s, Upgrades.SCOOTER)
        assertEquals(2000 - 250 - 500, s.money)
        assertEquals(2, s.upgrades[Upgrades.SCOOTER])
        val again = GameEngine.buy(s, Upgrades.SCOOTER)
        assertEquals(s, again)
    }

    @Test fun youCannotBuyWithoutEnoughMoney() {
        val poor = GameEngine.newGame(neighbors, generator).copy(money = 249)
        assertEquals(poor, GameEngine.buy(poor, Upgrades.SCOOTER))
    }

    @Test fun anUnknownUpgradeChangesNothing() {
        val s = rich()
        assertEquals(s, GameEngine.buy(s, "jetpack"))
    }

    @Test fun everyUpgradeGetsMoreExpensiveWithEachLevel() {
        Upgrades.all.forEach { u -> assertEquals(u.prices.sorted(), u.prices) }
    }

    @Test fun theScooterWalksFasterAtEachLevelAndNeverBeyondTheTop() {
        assertEquals(4f, Upgrades.walkSpeed(0), 0.001f)
        assertTrue(Upgrades.walkSpeed(1) > Upgrades.walkSpeed(0))
        assertTrue(Upgrades.walkSpeed(2) > Upgrades.walkSpeed(1))
        assertEquals(Upgrades.walkSpeed(2), Upgrades.walkSpeed(9), 0.001f)
    }

    @Test fun theCableSpoolLowersThePriceButKeepsItPositive() {
        assertEquals(5, Upgrades.cablePrice(5, 0))
        assertEquals(4, Upgrades.cablePrice(5, 1))
        assertEquals(3, Upgrades.cablePrice(5, 2))
        assertEquals(1, Upgrades.cablePrice(2, 2))
    }

    @Test fun upgradesSurviveTheSaveFile() {
        val s = GameEngine.buy(GameEngine.buy(rich(), Upgrades.SCOOTER), Upgrades.CABLE_SPOOL)
        val back = SaveData.fromMap(SaveData.from(s).toMap())
        assertEquals(mapOf(Upgrades.SCOOTER to 1, Upgrades.CABLE_SPOOL to 1), back.upgrades)
        assertEquals(s.upgrades, GameEngine.restore(back, neighbors, generator).upgrades)
    }

    @Test fun aDamagedSaveCannotGiveUpgradesThatDoNotExist() {
        val clean = Upgrades.sanitized(mapOf(Upgrades.SCOOTER to 99, "jetpack" to 3, Upgrades.CABLE_SPOOL to -1))
        assertEquals(mapOf(Upgrades.SCOOTER to 2), clean)
    }

    @Test fun upgradesStayAfterTheNextDay() {
        val s = GameEngine.buy(rich(), Upgrades.SCOOTER)
        assertEquals(s.upgrades, GameEngine.nextDay(s, neighbors, generator).upgrades)
    }

    @Test fun aScenarioWithACheaperCableKeepsEverythingElse() {
        val base = com.avabuilds.packetrunner.network.Scenarios.install("house1")
        val cheaper = base.withCablePrice(3)
        assertEquals(3, cheaper.cablePricePerCell)
        assertEquals(base.id, cheaper.id)
        assertEquals(base.parCost, cheaper.parCost)
        assertEquals(base.allowed, cheaper.allowed)
    }
}
