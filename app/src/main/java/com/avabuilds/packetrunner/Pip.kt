package com.avabuilds.packetrunner

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val PipGreen = Color(0xFF5DCAA5)
val PipDark = Color(0xFF0F6E56)
val PipEye = Color(0xFF04342C)

@Composable
fun Pip(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(160.dp, 140.dp)) {
        val u = size.width / 80f

        // Body
        drawRoundRect(
            color = PipGreen,
            topLeft = Offset(10 * u, 10 * u),
            size = Size(60 * u, 50 * u),
            cornerRadius = CornerRadius(16 * u)
        )

        // Envelope flap
        val flap = Path().apply {
            moveTo(14 * u, 18 * u)
            lineTo(40 * u, 34 * u)
            lineTo(66 * u, 18 * u)
        }
        drawPath(
            flap, PipDark,
            style = Stroke(width = 2.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Eyes
        for (x in listOf(30f, 50f)) {
            drawCircle(Color.White, radius = 6 * u, center = Offset(x * u, 45 * u))
            drawCircle(PipEye, radius = 3 * u, center = Offset(x * u, 45 * u))
        }

        // Smile
        val smile = Path().apply {
            moveTo(35 * u, 54 * u)
            quadraticBezierTo(40 * u, 58 * u, 45 * u, 54 * u)
        }
        drawPath(smile, PipEye, style = Stroke(width = 1.8f * u, cap = StrokeCap.Round))
    }
}

@Preview(showBackground = true)
@Composable
fun PipPreview() {
    Pip()
}