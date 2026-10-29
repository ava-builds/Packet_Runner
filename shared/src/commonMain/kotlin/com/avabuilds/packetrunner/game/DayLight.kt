package com.avabuilds.packetrunner.game

/** The four moods of the day, for the sun and moon icon and for anything that depends on whether it is light. */
enum class DayPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

/**
 * How the day looks at one moment: a color that the whole scene is multiplied by ([tintRed], [tintGreen], [tintBlue] as 0..1,
 * mixed with white by [tintAlpha], so 0 means no change at all), and how brightly the windows and the streetlights shine
 * (0 = off, 1 = full).
 */
data class Light(
    val tintRed: Float,
    val tintGreen: Float,
    val tintBlue: Float,
    val tintAlpha: Float,
    val windowGlow: Float,
    val lampGlow: Float,
)

/**
 * The light of the day as it follows the game clock (08:00 to 20:00): a warm morning, a bright afternoon, an orange
 * evening and, as the day ends, a dark blue night with lit windows and streetlights. After 20:00 it stays night.
 * The numbers are keyframes; times between two keyframes are blended smoothly.
 */
object DayLight {
    private class Key(val minute: Float, val light: Light)

    private fun at(hour: Int, minute: Int = 0) = hour * 60f + minute

    private val keys = listOf(
        Key(at(8), Light(1.00f, 0.85f, 0.62f, 0.25f, 0.00f, 0.00f)), // morning: a warm, gentle gold
        Key(at(10), Light(1.00f, 0.92f, 0.78f, 0.10f, 0.00f, 0.00f)),
        Key(at(12), Light(1.00f, 1.00f, 1.00f, 0.00f, 0.00f, 0.00f)), // bright afternoon
        Key(at(15), Light(1.00f, 1.00f, 0.95f, 0.00f, 0.00f, 0.00f)),
        Key(at(16, 30), Light(1.00f, 0.70f, 0.40f, 0.40f, 0.00f, 0.00f)), // the light turns golden
        Key(at(18), Light(1.00f, 0.50f, 0.25f, 0.55f, 0.35f, 0.35f)), // orange evening, the first lights come on
        Key(at(19), Light(0.55f, 0.35f, 0.60f, 0.70f, 0.85f, 0.85f)), // dusk
        Key(at(20), Light(0.12f, 0.20f, 0.50f, 0.85f, 1.00f, 1.00f)), // dark blue night
    )

    /** The light at [minute] of the day. Before the first keyframe it is the morning light, after the last it is night. */
    fun at(minute: Float): Light {
        if (minute <= keys.first().minute) return keys.first().light
        if (minute >= keys.last().minute) return keys.last().light
        val i = keys.indexOfLast { it.minute <= minute }
        val a = keys[i]
        val b = keys[i + 1]
        val t = (minute - a.minute) / (b.minute - a.minute)
        fun mix(x: Float, y: Float) = x + (y - x) * t
        return Light(
            mix(a.light.tintRed, b.light.tintRed), mix(a.light.tintGreen, b.light.tintGreen), mix(a.light.tintBlue, b.light.tintBlue),
            mix(a.light.tintAlpha, b.light.tintAlpha), mix(a.light.windowGlow, b.light.windowGlow), mix(a.light.lampGlow, b.light.lampGlow),
        )
    }

    fun period(minute: Float): DayPeriod = when {
        minute < at(10, 30) -> DayPeriod.MORNING
        minute < at(16, 30) -> DayPeriod.AFTERNOON
        minute < at(19) -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }
}
