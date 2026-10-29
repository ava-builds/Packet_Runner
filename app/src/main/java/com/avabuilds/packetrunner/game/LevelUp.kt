package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.network.LabGame

/** What the player sees when they reach [level]: Nana's [message], the list of what opens, and the coin [bonus]. */
data class LevelUpEvent(val level: Int, val bonus: Int, val message: String, val whatsNew: List<String>)

/**
 * The level-up moment. Reaching a level gives a coin bonus and a phone call from Nana that says, in a friendly way,
 * what is new (see [Curriculum]). Past the last level there is nothing new to open, but the bonus and a call still come.
 */
object LevelUp {
    const val BONUS_PER_LEVEL = 25

    /** The names of the Lab games by id, for lists that say what opens. */
    val labTitles: Map<String, String> = LabGame.entries.associate { it.id to it.title }

    fun bonus(level: Int): Int = BONUS_PER_LEVEL * level

    /** The level-ups between two XP values, one for each level reached (a big jump gives several), lowest first. Empty when no level was reached. */
    fun events(fromXp: Int, toXp: Int): List<LevelUpEvent> {
        val from = Progress.levelOf(fromXp.coerceAtLeast(0))
        val to = Progress.levelOf(toXp.coerceAtLeast(0))
        return (from + 1..to).map(::event)
    }

    fun event(level: Int): LevelUpEvent =
        if (level <= Curriculum.maxLevel) {
            LevelUpEvent(level, bonus(level), Curriculum.stage(level).nana, Curriculum.whatsNew(level, labTitles))
        } else {
            LevelUpEvent(
                level, bonus(level),
                "Level $level! You have learned every topic of the course. Keep practising: the cases come in every order.",
                listOf("Every topic stays open", "Keep catching problems early"),
            )
        }

    fun totalBonus(events: List<LevelUpEvent>): Int = events.sumOf { it.bonus }
}
