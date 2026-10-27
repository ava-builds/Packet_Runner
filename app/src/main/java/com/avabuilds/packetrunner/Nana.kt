package com.avabuilds.packetrunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val NanaFur = Color(0xFFAFA9EC)
private val NanaLine = Color(0xFF3C3489)
private val NanaEye = Color(0xFF26215C)
private val NanaNose = Color(0xFFD4537E)

/** Nana, the mentor cat. Tilts her head and blinks. */
@Composable
fun Nana(modifier: Modifier = Modifier, size: Dp = 80.dp, animated: Boolean = true) {
    val blink = if (animated) rememberBlink(4000) else 1f
    val tilt = if (animated) rememberWave(4f, 1400) else 0f

    Canvas(modifier.size(size).graphicsLayer { rotationZ = tilt }) {
        val u = this.size.width / 80f

        // Ears and head
        drawPath(polygon(u, 16f to 34f, 20f to 10f, 36f to 24f), NanaFur)
        drawPath(polygon(u, 44f to 24f, 60f to 10f, 64f to 34f), NanaFur)
        drawCircle(NanaFur, radius = 30 * u, center = Offset(40 * u, 50 * u))

        // Glasses
        val glass = Stroke(1.8f * u)
        drawCircle(NanaLine, radius = 8 * u, center = Offset(29 * u, 48 * u), style = glass)
        drawCircle(NanaLine, radius = 8 * u, center = Offset(51 * u, 48 * u), style = glass)
        drawLine(NanaLine, Offset(37 * u, 48 * u), Offset(43 * u, 48 * u), strokeWidth = 1.8f * u)

        // Eyes
        for (x in listOf(29f, 51f)) {
            drawOval(
                NanaEye,
                topLeft = Offset((x - 3) * u, (48 - 3 * blink) * u),
                size = Size(6 * u, 6 * u * blink),
            )
        }

        // Nose and whiskers
        drawPath(polygon(u, 37f to 60f, 43f to 60f, 40f to 64f), NanaNose)
        drawLine(NanaLine, Offset(10 * u, 62 * u), Offset(26 * u, 64 * u), strokeWidth = 1.2f * u)
        drawLine(NanaLine, Offset(54 * u, 64 * u), Offset(70 * u, 62 * u), strokeWidth = 1.2f * u)
    }
}

@Preview(showBackground = true)
@Composable
fun NanaPreview() {
    Nana()
}
