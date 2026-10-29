package com.avabuilds.packetrunner.work

/** What the engineer is doing right now. */
enum class WorkState(val label: String) {
    OFF_DUTY("Off duty"),
    WORKING("Working"),
    LUNCH("Lunch break"),
    DAY_OFF("Day off"),
}

/**
 * The player's working week. Times are minutes since midnight. Work runs from [startMinute] to [endMinute] with a
 * lunch break of [lunchMinutes] starting at [lunchStartMinute]; the days in [daysOff] are free.
 */
data class WorkSchedule(
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 18 * 60,
    val lunchStartMinute: Int = 12 * 60,
    val lunchMinutes: Int = 60,
    val daysOff: Set<Weekday> = setOf(Weekday.SAT, Weekday.SUN),
) {
    val lunchEndMinute: Int get() = lunchStartMinute + lunchMinutes

    /** A message about what is wrong with this schedule, or null when it is fine. */
    fun problem(): String? = when {
        endMinute <= startMinute -> "Work must end after it starts."
        endMinute - startMinute < 3 * 60 -> "Work needs at least 3 hours."
        lunchStartMinute < startMinute + 60 -> "Lunch must start at least an hour after work starts."
        lunchEndMinute > endMinute - 60 -> "Lunch must end at least an hour before work ends."
        daysOff.size != 2 -> "Choose exactly 2 days off."
        else -> null
    }

    fun isWorkday(day: Weekday) = day !in daysOff

    /** Hours at work, not counting lunch, in minutes. */
    val workMinutesPerDay: Int get() = endMinute - startMinute - lunchMinutes
}

/** What happened today at the office door. A new day starts with a fresh log. */
data class WorkLog(val epochDay: Long, val clockInMinute: Int? = null, val clockOutMinute: Int? = null)

/** The rules that turn a schedule, a log and the time into a [WorkState]. Pure functions, easy to test. */
object WorkRules {

    /** The state at [now]. Before you clock in (or after you clock out) you are off duty, even in work hours. */
    fun stateAt(schedule: WorkSchedule, log: WorkLog, now: WallTime): WorkState {
        if (!schedule.isWorkday(now.weekday)) return WorkState.DAY_OFF
        val m = now.minuteOfDay
        val today = log.epochDay == now.epochDay
        val clockIn = if (today) log.clockInMinute else null
        val clockOut = if (today) log.clockOutMinute else null
        return when {
            clockIn == null || m < clockIn -> WorkState.OFF_DUTY
            clockOut != null && m >= clockOut -> WorkState.OFF_DUTY
            m < schedule.startMinute || m >= schedule.endMinute -> WorkState.OFF_DUTY
            m >= schedule.lunchStartMinute && m < schedule.lunchEndMinute -> WorkState.LUNCH
            else -> WorkState.WORKING
        }
    }

    /** Opening the app during work hours clocks you in automatically. Returns the log to keep (maybe a fresh one). */
    fun autoClockIn(schedule: WorkSchedule, log: WorkLog, now: WallTime): WorkLog {
        val fresh = if (log.epochDay == now.epochDay) log else WorkLog(now.epochDay)
        val m = now.minuteOfDay
        val inHours = schedule.isWorkday(now.weekday) && m >= schedule.startMinute && m < schedule.endMinute
        return if (inHours && fresh.clockInMinute == null && fresh.clockOutMinute == null) fresh.copy(clockInMinute = m) else fresh
    }

    /** Clock in by hand (on a workday only). Clocking in again after a clock-out starts a new shift. */
    fun clockIn(schedule: WorkSchedule, log: WorkLog, now: WallTime): WorkLog {
        if (!schedule.isWorkday(now.weekday)) return log
        val base = if (log.epochDay == now.epochDay) log else WorkLog(now.epochDay)
        return base.copy(clockInMinute = now.minuteOfDay, clockOutMinute = null)
    }

    fun clockOut(log: WorkLog, now: WallTime): WorkLog {
        val base = if (log.epochDay == now.epochDay) log else WorkLog(now.epochDay)
        return if (base.clockInMinute == null) base else base.copy(clockOutMinute = now.minuteOfDay)
    }

    /** True when the player can press "Clock in" right now. */
    fun canClockIn(schedule: WorkSchedule, log: WorkLog, now: WallTime): Boolean =
        schedule.isWorkday(now.weekday) && stateAt(schedule, log, now) == WorkState.OFF_DUTY && now.minuteOfDay < schedule.endMinute

    fun canClockOut(schedule: WorkSchedule, log: WorkLog, now: WallTime): Boolean =
        stateAt(schedule, log, now).let { it == WorkState.WORKING || it == WorkState.LUNCH }

    /** What is next, in words: for example "Lunch at 12:00" or "Clock out at 18:00" or "Back to work Monday". */
    fun nextUp(schedule: WorkSchedule, log: WorkLog, now: WallTime): String {
        val m = now.minuteOfDay
        return when (stateAt(schedule, log, now)) {
            WorkState.DAY_OFF -> "Back to work on ${nextWorkday(schedule, now).short}"
            WorkState.LUNCH -> "Back to work at ${formatMinuteOfDay(schedule.lunchEndMinute)}"
            WorkState.WORKING ->
                if (m < schedule.lunchStartMinute) "Lunch at ${formatMinuteOfDay(schedule.lunchStartMinute)}"
                else "Work ends at ${formatMinuteOfDay(schedule.endMinute)}"
            WorkState.OFF_DUTY -> when {
                m < schedule.startMinute && schedule.isWorkday(now.weekday) -> "Work starts at ${formatMinuteOfDay(schedule.startMinute)}"
                m < schedule.endMinute -> "Clock in at the office door"
                else -> "Back to work on ${nextWorkday(schedule, now.plusSeconds(86_400)).short}"
            }
        }
    }

    /** The first workday on or after the day of [from]. */
    fun nextWorkday(schedule: WorkSchedule, from: WallTime): Weekday {
        var t = from
        repeat(7) { if (schedule.isWorkday(t.weekday)) return t.weekday; t = t.plusSeconds(86_400) }
        return from.weekday
    }

    /** True from 06:00 to 19:00; the HUD shows a sun then and a moon otherwise. */
    fun isDaylight(minuteOfDay: Int): Boolean = minuteOfDay in 6 * 60 until 19 * 60
}
