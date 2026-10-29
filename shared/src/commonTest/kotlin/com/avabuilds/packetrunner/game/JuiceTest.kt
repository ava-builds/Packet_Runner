package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertNotNull
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class JuiceTest {
    @Test fun aBuildingIsUnchangedBeforeAndAfterTheBounce() {
        assertEquals(Squash(1f, 1f), Juice.bounce(-0.1f))
        assertEquals(Squash(1f, 1f), Juice.bounce(Juice.BOUNCE_SECONDS))
        assertEquals(Squash(1f, 1f), Juice.bounce(5f))
    }

    @Test fun theBounceSquashesAndThenStretches() {
        val samples = (1 until 50).map { Juice.bounce(it * Juice.BOUNCE_SECONDS / 50f) }
        assertTrue(samples.any { it.y < 0.97f })
        assertTrue(samples.any { it.y > 1.03f })
    }

    @Test fun theBounceDiesDownSoTheEndIsGentlerThanTheStart() {
        val early = (0 until 10).maxOf { kotlin.math.abs(Juice.bounce(it * 0.02f).y - 1f) }
        val late = (0 until 10).maxOf { kotlin.math.abs(Juice.bounce(0.4f + it * 0.014f).y - 1f) }
        assertTrue(late < early)
    }

    @Test fun squashingMakesItWiderAndStretchingMakesItNarrower() {
        (1 until 50).map { Juice.bounce(it * 0.01f) }.forEach { s -> assertEquals(s.y > 1f, s.x < 1f) }
    }

    @Test fun theNumberOfCoinsGrowsWithTheAmountButIsCapped() {
        assertEquals(0, Juice.coinCount(0))
        assertEquals(0, Juice.coinCount(-20))
        assertEquals(1, Juice.coinCount(40))
        assertEquals(3, Juice.coinCount(130))
        assertEquals(Juice.MAX_COINS, Juice.coinCount(100000))
    }

    @Test fun aCoinOnlyExistsDuringItsFlight() {
        assertNull(Juice.coin(0, 3, -0.01f))
        assertNotNull(Juice.coin(0, 3, 0.3f))
        assertNull(Juice.coin(0, 3, Juice.COIN_SECONDS + 0.01f))
    }

    @Test fun laterCoinsStartLater() {
        assertNull(Juice.coin(5, 6, 0.1f))
        assertNotNull(Juice.coin(0, 6, 0.1f))
    }

    @Test fun coinsRiseAndFadeOut() {
        val early = Juice.coin(0, 1, 0.2f)!!
        val late = Juice.coin(0, 1, 1.0f)!!
        assertTrue(late.rise > early.rise)
        assertEquals(1f, early.alpha, 0.001f)
        assertTrue(late.alpha < 0.5f)
    }

    @Test fun coinsSpreadOutSideways() {
        val first = Juice.coin(0, 3, 0.9f)!!
        val last = Juice.coin(2, 3, 0.9f)!!
        assertTrue(first.dx < 0f && last.dx > 0f)
        assertEquals(0f, Juice.coin(0, 1, 0.5f)!!.dx, 0.001f)
    }
}
