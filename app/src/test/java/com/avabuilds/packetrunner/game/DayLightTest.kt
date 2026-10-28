package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLightTest {
    private fun mi(h: Int, min: Int = 0) = h * 60 + min
    private fun m(h: Int, min: Int = 0) = mi(h, min).toFloat()

    @Test fun theAfternoonIsBrightWithNoTintAndNoLights() {
        val l = DayLight.at(m(13))
        assertEquals(0f, l.tintAlpha, 0.001f)
        assertEquals(0f, l.windowGlow, 0.001f)
        assertEquals(0f, l.lampGlow, 0.001f)
    }

    @Test fun theMorningIsWarmerThanNoon() {
        val morning = DayLight.at(m(8))
        assertTrue(morning.tintAlpha > 0.1f)
        assertTrue(morning.tintRed > morning.tintBlue) // warm: more red than blue
    }

    @Test fun theEveningIsOrangeAndTheFirstLightsComeOn() {
        val e = DayLight.at(m(18))
        assertTrue(e.tintRed > e.tintGreen && e.tintGreen > e.tintBlue)
        assertTrue(e.windowGlow > 0f && e.lampGlow > 0f)
    }

    @Test fun atTheEndOfTheDayItIsDarkBlueAndEverythingIsLit() {
        val night = DayLight.at(m(20))
        assertTrue(night.tintBlue > night.tintRed)
        assertTrue(night.tintAlpha >= 0.5f)
        assertEquals(1f, night.windowGlow, 0.001f)
        assertEquals(1f, night.lampGlow, 0.001f)
    }

    @Test fun itStaysNightAfterTheDayEndsAndMorningBeforeItStarts() {
        assertEquals(DayLight.at(m(20)), DayLight.at(m(23)))
        assertEquals(DayLight.at(m(8)), DayLight.at(m(5)))
    }

    @Test fun theLightsNeverFlickerOffAgainDuringTheEvening() {
        var window = 0f
        var lamp = 0f
        for (minute in mi(15)..mi(20) step 5) {
            val l = DayLight.at(minute.toFloat())
            assertTrue(l.windowGlow >= window - 0.0001f)
            assertTrue(l.lampGlow >= lamp - 0.0001f)
            window = l.windowGlow
            lamp = l.lampGlow
        }
    }

    @Test fun theTintGetsStrongerThroughTheEveningUntilNight() {
        var alpha = -1f
        for (minute in mi(15)..mi(20) step 5) {
            val a = DayLight.at(minute.toFloat()).tintAlpha
            assertTrue("at $minute", a >= alpha - 0.0001f)
            alpha = a
        }
    }

    @Test fun theLightChangesSmoothlyWithNoJumps() {
        var previous = DayLight.at(m(8))
        for (minute in mi(8)..mi(20)) {
            val l = DayLight.at(minute.toFloat())
            assertTrue(Math.abs(l.tintAlpha - previous.tintAlpha) < 0.02f)
            assertTrue(Math.abs(l.tintGreen - previous.tintGreen) < 0.03f)
            assertTrue(Math.abs(l.windowGlow - previous.windowGlow) < 0.02f)
            previous = l
        }
    }

    @Test fun allValuesStayBetweenZeroAndOne() {
        for (minute in 0..24 * 60 step 7) {
            val l = DayLight.at(minute.toFloat())
            listOf(l.tintRed, l.tintGreen, l.tintBlue, l.tintAlpha, l.windowGlow, l.lampGlow).forEach { assertTrue(it in 0f..1f) }
        }
    }

    @Test fun thePeriodsFollowTheDay() {
        assertEquals(DayPeriod.MORNING, DayLight.period(m(8)))
        assertEquals(DayPeriod.AFTERNOON, DayLight.period(m(13)))
        assertEquals(DayPeriod.EVENING, DayLight.period(m(17, 45)))
        assertEquals(DayPeriod.NIGHT, DayLight.period(m(19, 30)))
        assertEquals(DayPeriod.NIGHT, DayLight.period(Clock.DAY_END))
    }
}
