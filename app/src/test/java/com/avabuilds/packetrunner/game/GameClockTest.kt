package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameClockTest {
    @Test fun formatsMinutesAsHoursAndMinutes() {
        assertEquals("08:00", formatTime(Clock.DAY_START))
        assertEquals("12:35", formatTime(755.9f))
        assertEquals("20:00", formatTime(Clock.DAY_END))
        assertEquals("09:05", formatTime(545f))
    }

    @Test fun aDayLastsAboutTwoRealMinutes() {
        val seconds = (Clock.DAY_END - Clock.DAY_START) / Clock.MINUTES_PER_SECOND
        assertEquals(120f, seconds, 0.001f)
    }
}
