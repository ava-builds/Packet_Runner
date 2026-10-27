package com.avabuilds.packetrunner

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Path

/** 1 = eyes open, ~0.1 = eyes closed. Blinks once every [periodMs]. */
@Composable
fun rememberBlink(periodMs: Int = 3200): Float {
    val transition = rememberInfiniteTransition(label = "blink")
    val value by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = periodMs
                1f at 0
                1f at periodMs - 260
                0.1f at periodMs - 160
                1f at periodMs - 60
            }
        ),
        label = "blinkValue",
    )
    return value
}

/** Smoothly moves between -amount and +amount (used for bounce, tilt, shake). */
@Composable
fun rememberWave(amount: Float, durationMs: Int, smooth: Boolean = true): Float {
    val transition = rememberInfiniteTransition(label = "wave")
    val value by transition.animateFloat(
        initialValue = -amount,
        targetValue = amount,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = if (smooth) FastOutSlowInEasing else LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "waveValue",
    )
    return value
}

/** Builds a closed shape from points given in the 80-unit design grid. */
fun polygon(u: Float, vararg points: Pair<Float, Float>): Path = Path().apply {
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u)
    }
    close()
}
