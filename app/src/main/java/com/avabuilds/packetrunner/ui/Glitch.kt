package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlitchRed = Color(0xFFE24B4A)
private val GlitchDark = Color(0xFF501313)

/** Glitch, the villain. Shakes nervously like a noisy signal. */
@Composable
fun Glitch(modifier: Modifier = Modifier, size: Dp = 80.dp, animated: Boolean = true) {
    val shake = if (animated) rememberWave(1.5f, 90, smooth = false) else 0f

    Canvas(
        modifier
            .size(size, size * 0.875f)
            .graphicsLayer { translationX = shake * density }
    ) {
        val u = this.size.width / 80f

        // Jagged body
        drawPath(
            polygon(
                u,
                12f to 60f, 12f to 20f, 22f to 10f, 32f to 20f, 42f to 6f,
                52f to 20f, 64f to 10f, 68f to 60f,
            ),
            GlitchRed,
        )

        // Eyes
        drawCircle(Color.White, 6 * u, Offset(30 * u, 38 * u))
        drawCircle(Color.White, 6 * u, Offset(50 * u, 38 * u))
        drawCircle(GlitchDark, 3 * u, Offset(32 * u, 39 * u))
        drawCircle(GlitchDark, 3 * u, Offset(48 * u, 39 * u))

        // Angry brows
        drawLine(GlitchDark, Offset(22 * u, 27 * u), Offset(36 * u, 32 * u), 2.5f * u, StrokeCap.Round)
        drawLine(GlitchDark, Offset(58 * u, 27 * u), Offset(44 * u, 32 * u), 2.5f * u, StrokeCap.Round)

        // Zigzag mouth
        val mouth = Path().apply {
            moveTo(28 * u, 52 * u)
            lineTo(34 * u, 48 * u)
            lineTo(40 * u, 52 * u)
            lineTo(46 * u, 48 * u)
            lineTo(52 * u, 52 * u)
        }
        drawPath(mouth, Color.White, style = Stroke(2f * u, join = StrokeJoin.Round))
    }
}

@Preview(showBackground = true)
@Composable
fun GlitchPreview() {
    Glitch()
}
