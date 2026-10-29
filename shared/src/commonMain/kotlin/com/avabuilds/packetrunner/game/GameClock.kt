package com.avabuilds.packetrunner.game


/**
 * The time rules of the town. A day runs from 08:00 to 20:00 game time.
 * One real second is [MINUTES_PER_SECOND] game minutes, so a day lasts about two real minutes.
 * (Short on purpose while we build and test; raise the day length later by lowering the speed.)
 */
object Clock {
    const val DAY_START = 8 * 60f
    const val DAY_END = 20 * 60f
    const val MINUTES_PER_SECOND = 6f

    /** Neighbors only phone between these times. */
    const val CALLS_FROM = 9 * 60
    const val CALLS_UNTIL = 16 * 60
}

/** 755.9 -> "12:35" */
fun formatTime(minute: Float): String {
    val m = minute.toInt()
    return "${(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}"
}
