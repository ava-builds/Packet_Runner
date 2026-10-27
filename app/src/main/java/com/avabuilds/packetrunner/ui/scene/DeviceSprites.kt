package com.avabuilds.packetrunner.ui.scene

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avabuilds.packetrunner.network.DeviceType
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5

/** Draws a device centered on [center]. [c] is the size of one room cell in pixels. */
fun DrawScope.drawDevice(type: DeviceType, center: Offset, c: Float) {
    when (type) {
        DeviceType.WALL_SOCKET -> {
            drawRoundRect(Color.White, Offset(center.x - 0.28f * c, center.y - 0.28f * c), Size(0.56f * c, 0.56f * c), CornerRadius(0.08f * c))
            drawRoundRect(Palette5.DuskBlue, Offset(center.x - 0.28f * c, center.y - 0.28f * c), Size(0.56f * c, 0.56f * c), CornerRadius(0.08f * c), style = Stroke(0.035f * c))
            drawCircle(Palette5.DeepNavy, 0.045f * c, Offset(center.x - 0.1f * c, center.y))
            drawCircle(Palette5.DeepNavy, 0.045f * c, Offset(center.x + 0.1f * c, center.y))
            drawCircle(PR.Amber, 0.04f * c, Offset(center.x, center.y - 0.16f * c))
        }

        DeviceType.ROUTER -> {
            listOf(-0.24f, 0.24f).forEach { dx ->
                drawLine(Palette5.DeepNavy, Offset(center.x + dx * c, center.y - 0.06f * c), Offset(center.x + dx * c, center.y - 0.4f * c), 0.045f * c, StrokeCap.Round)
                drawCircle(PR.Amber, 0.05f * c, Offset(center.x + dx * c, center.y - 0.42f * c))
            }
            drawRoundRect(Palette5.PersianBlue, Offset(center.x - 0.38f * c, center.y - 0.06f * c), Size(0.76f * c, 0.34f * c), CornerRadius(0.06f * c))
            listOf(-0.18f, 0f, 0.18f).forEach { dx -> drawCircle(Palette5.IcyBlue, 0.03f * c, Offset(center.x + dx * c, center.y + 0.11f * c)) }
        }

        DeviceType.ACCESS_POINT -> {
            drawOval(Palette5.Cornflower, Offset(center.x - 0.36f * c, center.y - 0.1f * c), Size(0.72f * c, 0.36f * c))
            drawOval(Palette5.IcyBlue, Offset(center.x - 0.24f * c, center.y - 0.06f * c), Size(0.48f * c, 0.2f * c))
            drawCircle(PR.Amber, 0.04f * c, Offset(center.x, center.y + 0.04f * c))
            listOf(0.1f, 0.18f, 0.26f).forEach { r ->
                drawArc(Palette5.PersianBlue, -135f, 90f, false, Offset(center.x - r * c, center.y - 0.12f * c - r * c), Size(2 * r * c, 2 * r * c), style = Stroke(0.03f * c, cap = StrokeCap.Round))
            }
        }

        DeviceType.REPEATER -> {
            drawRoundRect(Palette5.DuskBlue, Offset(center.x - 0.22f * c, center.y - 0.22f * c), Size(0.44f * c, 0.44f * c), CornerRadius(0.08f * c))
            drawCircle(Palette5.IcyBlue, 0.05f * c, center)
            listOf(0.3f, 0.4f).forEach { r ->
                val tl = Offset(center.x - r * c, center.y - r * c)
                val size = Size(2 * r * c, 2 * r * c)
                drawArc(PR.Amber, -40f, 80f, false, tl, size, style = Stroke(0.03f * c, cap = StrokeCap.Round))
                drawArc(PR.Amber, 140f, 80f, false, tl, size, style = Stroke(0.03f * c, cap = StrokeCap.Round))
            }
        }

        DeviceType.SWITCH, DeviceType.HUB -> {
            val body = if (type == DeviceType.SWITCH) Palette5.DuskBlue else Color(0xFF7C86A0)
            drawRoundRect(body, Offset(center.x - 0.38f * c, center.y - 0.12f * c), Size(0.76f * c, 0.28f * c), CornerRadius(0.05f * c))
            for (i in 0 until 4) {
                drawRoundRect(Palette5.IcyBlue, Offset(center.x - 0.3f * c + i * 0.17f * c, center.y - 0.02f * c), Size(0.1f * c, 0.09f * c), CornerRadius(0.02f * c))
            }
        }

        DeviceType.LAPTOP -> {
            drawRoundRect(Palette5.DeepNavy, Offset(center.x - 0.26f * c, center.y - 0.3f * c), Size(0.52f * c, 0.34f * c), CornerRadius(0.04f * c))
            drawRect(Palette5.IcyBlue, Offset(center.x - 0.22f * c, center.y - 0.26f * c), Size(0.44f * c, 0.26f * c))
            drawRoundRect(Palette5.DuskBlue, Offset(center.x - 0.34f * c, center.y + 0.06f * c), Size(0.68f * c, 0.08f * c), CornerRadius(0.03f * c))
        }

        DeviceType.TV -> {
            drawRoundRect(Palette5.DeepNavy, Offset(center.x - 0.34f * c, center.y - 0.28f * c), Size(0.68f * c, 0.44f * c), CornerRadius(0.04f * c))
            drawRect(Palette5.IcyBlue, Offset(center.x - 0.3f * c, center.y - 0.24f * c), Size(0.6f * c, 0.36f * c))
            // a play triangle on the screen
            drawPath(
                Path().apply {
                    moveTo(center.x - 0.07f * c, center.y - 0.16f * c)
                    lineTo(center.x + 0.1f * c, center.y - 0.06f * c)
                    lineTo(center.x - 0.07f * c, center.y + 0.04f * c)
                    close()
                },
                Palette5.PersianBlue,
            )
            drawRect(Palette5.DuskBlue, Offset(center.x - 0.06f * c, center.y + 0.16f * c), Size(0.12f * c, 0.05f * c))
            drawRoundRect(Palette5.DuskBlue, Offset(center.x - 0.2f * c, center.y + 0.21f * c), Size(0.4f * c, 0.05f * c), CornerRadius(0.02f * c))
        }

        DeviceType.POS -> {
            drawRoundRect(Palette5.DuskBlue, Offset(center.x - 0.2f * c, center.y - 0.26f * c), Size(0.4f * c, 0.5f * c), CornerRadius(0.05f * c))
            drawRect(Palette5.IcyBlue, Offset(center.x - 0.15f * c, center.y - 0.21f * c), Size(0.3f * c, 0.14f * c))
            for (row in 0 until 3) for (col in 0 until 3) {
                drawCircle(
                    if (row == 2 && col == 2) PR.Amber else Palette5.IcyBlue, 0.026f * c,
                    Offset(center.x - 0.1f * c + col * 0.1f * c, center.y - 0.0f * c + row * 0.075f * c),
                )
            }
        }

        DeviceType.PHONE -> {
            drawRoundRect(Palette5.DeepNavy, Offset(center.x - 0.14f * c, center.y - 0.26f * c), Size(0.28f * c, 0.52f * c), CornerRadius(0.05f * c))
            drawRect(Palette5.IcyBlue, Offset(center.x - 0.11f * c, center.y - 0.21f * c), Size(0.22f * c, 0.38f * c))
        }
    }
}
