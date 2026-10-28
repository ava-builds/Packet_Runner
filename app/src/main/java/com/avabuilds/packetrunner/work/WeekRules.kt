package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.SolvedRecord

/** What the player had at the start of the week, to measure what the week brought. */
data class WeekSnapshot(val solvedCount: Int = 0, val heartsTotal: Int = 0, val xp: Int = 0)

/** The week in progress: which one, where it started, and which week was last paid. */
data class WeekTracker(val week: Long = -1, val start: WeekSnapshot = WeekSnapshot(), val paidWeek: Long = -1)

/** The payday summary of one week. */
data class Payday(
    val week: Long,
    val callsSolved: Int,
    val moneyEarned: Int,
    val heartsGained: Int,
    val xpGained: Int,
    val bonus: Int,
)

object WeekRules {
    const val BASE_BONUS = 100
    const val BONUS_PER_CALL = 10

    /** The payday bonus: nothing for a week without solved calls, otherwise a base amount plus a little for each call. */
    fun bonusFor(callsSolved: Int): Int = if (callsSolved <= 0) 0 else BASE_BONUS + BONUS_PER_CALL * callsSolved

    /** What week [tracker] brought, measured against how things stand now ([now] and the whole [solved] list). */
    fun summary(tracker: WeekTracker, solved: List<SolvedRecord>, now: WeekSnapshot): Payday {
        val calls = solved.drop(tracker.start.solvedCount)
        return Payday(
            week = tracker.week,
            callsSolved = calls.size,
            moneyEarned = calls.sumOf { it.payout },
            heartsGained = now.heartsTotal - tracker.start.heartsTotal,
            xpGained = now.xp - tracker.start.xp,
            bonus = bonusFor(calls.size),
        )
    }

    /** Whether [day] is the last day of its week (Monday to Sunday) on which the player works. */
    fun isLastWorkday(schedule: WorkSchedule, day: WallTime): Boolean {
        if (!schedule.isWorkday(day.weekday)) return false
        return (day.weekday.ordinal + 1 until Weekday.entries.size).none { schedule.isWorkday(Weekday.entries[it]) }
    }

    /** The tracker for a new week starting from [now]; the payment status of earlier weeks is kept. */
    fun startWeek(tracker: WeekTracker, week: Long, now: WeekSnapshot) = tracker.copy(week = week, start = now)

    /**
     * A week ended while nobody was looking (the app was closed on payday). Returns the payday of that week if it was
     * never paid and anything happened in it.
     */
    fun missedPayday(tracker: WeekTracker, currentWeek: Long, solved: List<SolvedRecord>, now: WeekSnapshot): Payday? {
        if (tracker.week < 0 || currentWeek <= tracker.week || tracker.paidWeek == tracker.week) return null
        return summary(tracker, solved, now).takeIf { it.callsSolved > 0 }
    }

    /** The payday for the end of a shift on the last workday of the week, if it has not been paid yet. */
    fun paydayAtClockOut(tracker: WeekTracker, schedule: WorkSchedule, today: WallTime, solved: List<SolvedRecord>, now: WeekSnapshot): Payday? {
        if (tracker.week != WeeklyTopics.weekOf(today) || tracker.paidWeek == tracker.week) return null
        if (!isLastWorkday(schedule, today)) return null
        return summary(tracker, solved, now)
    }
}
