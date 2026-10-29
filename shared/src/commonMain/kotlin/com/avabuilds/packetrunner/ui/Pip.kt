package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val PipGreen = Color(0xFF5DCAA5)
private val PipDark = Color(0xFF0F6E56)
private val PipEye = Color(0xFF04342C)

/** Pip, the hero packet. Bounces and blinks when [animated] is true. */
@Composable
fun Pip(modifier: Modifier = Modifier, size: Dp = 120.dp, animated: Boolean = true) {
    val blink = if (animated) rememberBlink(3200) else 1f
    val bounce = if (animated) rememberWave(3f, 650) else 0f

    Canvas(
        modifier
            .size(size, size * 0.875f)
            .graphicsLayer { translationY = bounce * density }
    ) {
        val u = this.size.width / 80f

        // Body
        drawRoundRect(
            color = PipGreen,
            topLeft = Offset(10 * u, 10 * u),
            size = Size(60 * u, 50 * u),
            cornerRadius = CornerRadius(16 * u),
        )

        // Envelope flap
        val flap = Path().apply {
            moveTo(14 * u, 18 * u)
            lineTo(40 * u, 34 * u)
            lineTo(66 * u, 18 * u)
        }
        drawPath(flap, PipDark, style = Stroke(2.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Eyes (squashed vertically when blinking)
        for (x in listOf(30f, 50f)) {
            drawOval(
                Color.White,
                topLeft = Offset((x - 6) * u, (45 - 6 * blink) * u),
                size = Size(12 * u, 12 * u * blink),
            )
            drawOval(
                PipEye,
                topLeft = Offset((x - 3) * u, (45 - 3 * blink) * u),
                size = Size(6 * u, 6 * u * blink),
            )
        }

        // Smile
        val smile = Path().apply {
            moveTo(35 * u, 54 * u)
            quadraticTo(40 * u, 58 * u, 45 * u, 54 * u)
        }
        drawPath(smile, PipEye, style = Stroke(1.8f * u, cap = StrokeCap.Round))
    }
}
