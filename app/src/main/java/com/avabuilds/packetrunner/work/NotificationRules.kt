package com.avabuilds.packetrunner.work

import kotlin.math.max

/** The rules for sending a customer-call notification while the app is closed. Pure, so they can be tested. */
object NotificationRules {

    /**
     * Whether a phone call at this moment may be shown as a notification: it is a workday, inside work hours, not at
     * lunch, and you did not clock out early. You do not have to be clocked in (opening the app clocks you in).
     */
    fun isWorkingTime(schedule: WorkSchedule, log: WorkLog, now: WallTime): Boolean {
        if (!schedule.isWorkday(now.weekday)) return false
        val m = now.minuteOfDay
        if (m < schedule.startMinute || m >= schedule.endMinute) return false
        if (m >= schedule.lunchStartMinute && m < schedule.lunchEndMinute) return false
        val clockOut = if (log.epochDay == now.epochDay) log.clockOutMinute else null
        return clockOut == null || m < clockOut
    }

    /**
     * Whether to notify: the call has not rung yet, the app is not on screen (it rings inside the game then), and it
     * is working time.
     */
    fun shouldNotify(call: PlannedCall?, schedule: WorkSchedule, log: WorkLog, now: WallTime, appInForeground: Boolean): Boolean =
        call != null && call.status == CallStatus.PENDING && !appInForeground && isWorkingTime(schedule, log, now)

    /**
     * How many real seconds to wait before a call at [callDay] / [callMinute] rings, when the clock now says [now] and runs
     * [factor] times faster than real time (1 normally, 60 in demo mode). Never negative.
     */
    fun delaySeconds(callDay: Long, callMinute: Int, now: WallTime, factor: Int = 1): Long {
        val gameSeconds = (callDay * 86_400L + callMinute * 60L) - (now.epochDay * 86_400L + now.secondOfDay)
        return max(0L, gameSeconds / max(factor, 1))
    }

    /** The calls that still need an alarm: not rung and not missed, and not already past. */
    fun toSchedule(books: List<CallBook>, now: WallTime): List<Pair<CallBook, PlannedCall>> =
        books.flatMap { b -> b.calls.filter { it.status == CallStatus.PENDING && b.epochDay * 1440L + it.minute >= now.epochDay * 1440L + now.minuteOfDay }.map { b to it } }
}

/** The next workday after [today] (never today), as a [WallTime] at midnight, or null if every day is a day off. */
fun WorkSchedule.nextWorkdayAfter(today: WallTime): WallTime? {
    var t = WallTime(today.epochDay + 1, 0)
    repeat(7) { if (isWorkday(t.weekday)) return t; t = WallTime(t.epochDay + 1, 0) }
    return null
}
