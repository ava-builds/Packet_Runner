package com.avabuilds.packetrunner.work

/** Is the engineer free to relax? After work and on days off there are no work calls, only free activities. */
object FreeTime {
    /** Free time is a day off, or off duty after work has ended (not before work starts, when you can still clock in). */
    fun isFreeTime(state: WorkState, canClockIn: Boolean): Boolean =
        state == WorkState.DAY_OFF || (state == WorkState.OFF_DUTY && !canClockIn)

    /** The small town event: a market on Saturdays. */
    fun marketOpen(day: WallTime): Boolean = day.weekday == Weekday.SAT
}
