package com.avabuilds.packetrunner.game

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** How much a sprite is stretched sideways and up while it bounces: 1 and 1 means unchanged. */
data class Squash(val x: Float, val y: Float)

/** One coin flying up from where money was earned. [progress] is 0 when it appears and 1 when it is gone. */
data class CoinState(val dx: Float, val rise: Float, val alpha: Float)

/** Small rules for the "juice": how a tapped building bounces and how coins pop up. Pure maths, so it can be tested. */
object Juice {
    /** How long a bounce lasts, in seconds. */
    const val BOUNCE_SECONDS = 0.55f

    /** How long a coin pop lasts, in seconds. */
    const val COIN_SECONDS = 1.1f

    /** The most coins shown for one payment. */
    const val MAX_COINS = 6

    /**
     * A damped spring: right after the tap the building squashes down and widens, then springs up and tall,
     * and settles back within [BOUNCE_SECONDS]. Before the tap and after the bounce it is exactly 1 and 1.
     */
    fun bounce(secondsSinceTap: Float): Squash {
        if (secondsSinceTap < 0f || secondsSinceTap >= BOUNCE_SECONDS) return Squash(1f, 1f)
        val wave = -sin(secondsSinceTap * 2f * PI.toFloat() * 3f) * exp(-secondsSinceTap * 6f)
        val tall = 1f + 0.14f * wave
        return Squash(x = 1f - 0.06f * wave, y = tall)
    }

    /** How many coins to show for earning [amount] won: one for a small amount, more for a big one, never more than [MAX_COINS]. */
    fun coinCount(amount: Int): Int = if (amount <= 0) 0 else (1 + amount / 60).coerceAtMost(MAX_COINS)

    /**
     * Where coin number [index] of [count] is [secondsSince] after the payment: it rises with an ease-out, drifts sideways
     * to its own spot and fades out over the second half. [rise] is in tile-widths; [dx] in tile-widths from the middle.
     */
    fun coin(index: Int, count: Int, secondsSince: Float): CoinState? {
        val delay = index * 0.07f
        val t = (secondsSince - delay) / COIN_SECONDS
        if (t < 0f || t > 1f) return null
        val ease = 1f - (1f - t) * (1f - t)
        val spread = if (count <= 1) 0f else (index.toFloat() / (count - 1) - 0.5f) * 1.2f
        return CoinState(dx = spread * ease, rise = ease * 0.9f, alpha = if (t < 0.5f) 1f else 1f - (t - 0.5f) * 2f)
    }
}
