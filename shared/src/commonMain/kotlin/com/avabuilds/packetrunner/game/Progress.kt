package com.avabuilds.packetrunner.game

import kotlin.math.max

/** Where the engineer stands: the level, how far into it, and how much more is needed for the next one. */
data class LevelProgress(val level: Int, val xpIntoLevel: Int, val xpForLevel: Int) {
    /** 0..1, how full the XP bar is. */
    val fraction: Float get() = if (xpForLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForLevel).coerceIn(0f, 1f)
}

/**
 * Experience points (XP) and levels. Jobs, Lab games and early catches on the monitor give XP; every level needs
 * more than the one before: level 2 at 100 XP, level 3 at 300, level 4 at 600, level 5 at 1000.
 */
object Progress {
    const val LAB_XP = 15
    const val EARLY_CATCH_XP = 10

    /** Extra XP for a job done so well that the neighbor was extra happy. */
    const val GREAT_WORK_XP = 10

    fun xpForJob(type: RequestType): Int = when (type) {
        RequestType.INSTALL -> 60
        RequestType.FIX -> 50
        RequestType.HELP -> 40
        RequestType.ROUTE -> 70
        RequestType.NAT -> 70
        RequestType.CASE -> 60
    }

    /** The XP a player needs in total to reach [level]. */
    fun xpToReach(level: Int): Int = 50 * level * (level - 1)

    fun levelOf(xp: Int): Int {
        var level = 1
        while (xp >= xpToReach(level + 1)) level++
        return level
    }

    fun progress(xp: Int): LevelProgress {
        val safe = max(xp, 0)
        val level = levelOf(safe)
        val start = xpToReach(level)
        return LevelProgress(level, safe - start, xpToReach(level + 1) - start)
    }
}
