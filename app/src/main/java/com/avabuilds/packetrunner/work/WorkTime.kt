package com.avabuilds.packetrunner.work

import java.util.Calendar
import java.util.TimeZone

enum class Weekday(val short: String) { MON("Mon"), TUE("Tue"), WED("Wed"), THU("Thu"), FRI("Fri"), SAT("Sat"), SUN("Sun") }

/**
 * A moment on the wall clock: the day (counted from 1970-01-01, local time) and the second of that day.
 * It avoids java.time so it works on every supported Android version, and it is easy to build in tests.
 */
data class WallTime(val epochDay: Long, val secondOfDay: Int) {
    val minuteOfDay: Int get() = secondOfDay / 60

    /** 1970-01-01 was a Thursday. */
    val weekday: Weekday get() = Weekday.entries[Math.floorMod(epochDay + 3, 7L).toInt()]

    /** This moment shifted by [seconds] (may be negative). */
    fun plusSeconds(seconds: Long): WallTime {
        val total = epochDay * 86_400L + secondOfDay + seconds
        return WallTime(Math.floorDiv(total, 86_400L), Math.floorMod(total, 86_400L).toInt())
    }

    /** Seconds from [other] to this moment. */
    fun secondsSince(other: WallTime): Long = (epochDay - other.epochDay) * 86_400L + (secondOfDay - other.secondOfDay)

    companion object {
        fun of(epochDay: Long, hour: Int, minute: Int = 0) = WallTime(epochDay, hour * 3600 + minute * 60)

        /** A Monday 1970-01-05 plus [dayIndex] days (Mon = 0), handy for tests. */
        fun week(dayIndex: Int, hour: Int, minute: Int = 0) = of(4L + dayIndex, hour, minute)
    }
}

/** Where "now" comes from. The game asks this, never the system, so tests and demo mode can steer time. */
interface TimeSource {
    fun now(): WallTime
}

/** The real clock of the phone, in its own time zone. */
class SystemTimeSource : TimeSource {
    override fun now(): WallTime {
        val c = Calendar.getInstance()
        val localMillis = c.timeInMillis + TimeZone.getDefault().getOffset(c.timeInMillis)
        return WallTime(Math.floorDiv(localMillis, 86_400_000L), (Math.floorMod(localMillis, 86_400_000L) / 1000).toInt())
    }
}

/**
 * Demo mode: time runs [factor] times faster than real time, starting from the moment [anchorGame] (which was
 * [anchorReal] on the real clock). [real] is the clock it speeds up.
 */
class ScaledTimeSource(
    private val real: TimeSource,
    private val anchorReal: WallTime,
    private val anchorGame: WallTime,
    val factor: Int = DEMO_FACTOR,
) : TimeSource {
    override fun now(): WallTime = anchorGame.plusSeconds(real.now().secondsSince(anchorReal) * factor)

    companion object {
        const val DEMO_FACTOR = 60
    }
}

/** A clock that only moves when told to; for tests. */
class FakeTimeSource(var time: WallTime) : TimeSource {
    override fun now(): WallTime = time
}

fun formatMinuteOfDay(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
