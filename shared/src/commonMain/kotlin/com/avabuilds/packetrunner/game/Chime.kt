package com.avabuilds.packetrunner.game

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** One note of a little tune: its pitch in hertz and how long it lasts in milliseconds. */
data class ChimeNote(val hz: Double, val ms: Int)

/**
 * A short happy sound made from a few notes, generated as 16-bit samples so the game needs no sound files.
 * Each note starts quickly and fades away like a bell; the last one rings a little longer.
 */
object Chime {
    const val SAMPLE_RATE = 22_050

    /** C, E, G and a high C: the level-up arpeggio. */
    val levelUp = listOf(ChimeNote(523.25, 110), ChimeNote(659.25, 110), ChimeNote(783.99, 110), ChimeNote(1046.5, 450))

    private const val ATTACK_MS = 6
    private const val RELEASE_MS = 12

    fun samples(notes: List<ChimeNote>, sampleRate: Int = SAMPLE_RATE, volume: Double = 0.4): ShortArray {
        require(volume in 0.0..1.0) { "volume must be between 0 and 1" }
        val out = ArrayList<Short>()
        for (note in notes) {
            val count = note.ms * sampleRate / 1000
            val attack = (ATTACK_MS * sampleRate / 1000).coerceAtLeast(1)
            val release = (RELEASE_MS * sampleRate / 1000).coerceAtLeast(1)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val fadeIn = if (i < attack) i.toDouble() / attack else 1.0
                val fadeOut = if (count - i < release) (count - i).toDouble() / release else 1.0 // no click at the end of a note
                val bell = exp(-t * 6.0)
                val value = sin(2 * PI * note.hz * t) * bell * fadeIn * fadeOut * volume
                out.add((value * Short.MAX_VALUE).toInt().toShort())
            }
        }
        return out.toShortArray()
    }
}
