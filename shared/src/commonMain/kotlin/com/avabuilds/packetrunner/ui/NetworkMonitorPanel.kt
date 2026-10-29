package com.avabuilds.packetrunner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.GameState
import com.avabuilds.packetrunner.game.LinkStatus
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.NetworkMonitor
import com.avabuilds.packetrunner.game.PacketFlow

private val Healthy = Color(0xFF2F8F57)

private fun LinkStatus.color(): Color = when (this) {
    LinkStatus.NO_LINK -> PR.Gray
    LinkStatus.HEALTHY -> Healthy
    LinkStatus.DEGRADED -> PR.Amber
    LinkStatus.DOWN -> PR.Red
}

/**
 * The office Network Monitor: the office on the left, every neighbor's place on the right, and
 * packets flowing along the links. A link that starts losing packets is a problem that is about to
 * become a phone call. Tap its building to report it early for a bonus; tapping a healthy link is a false alarm.
 */
@Composable
fun NetworkMonitorPanel(
    state: GameState,
    neighbors: List<Neighbor>,
    message: String?,
    onReport: (Neighbor) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val flow = rememberInfiniteTransition(label = "packets")
    val phase = flow.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase")

    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📡 Network Monitor", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clickable(onClick = onClose)) { Pill("Close", PR.Blue50, PR.Blue800) }
        }
        Text("Packets flow from the office to each place. When a link starts losing them, tap that place before the phone rings.", fontSize = 12.sp, color = PR.Muted)

        val rows = neighbors.size
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(216.dp)
                .background(PR.Teal50, RoundedCornerShape(12.dp))
                .pointerInput(neighbors) {
                    detectTapGestures { tap ->
                        if (tap.x < size.width * 0.55f) return@detectTapGestures
                        val row = (tap.y / (size.height / rows)).toInt().coerceIn(0, rows - 1)
                        onReport(neighbors[row])
                    }
                },
        ) {
            val officeAt = Offset(size.width * 0.12f, size.height / 2f)
            // the circles shrink a little when there are many rows, so the names under them still fit
            val nodeRadius = minOf(15.dp.toPx(), size.height / rows * 0.26f)
            val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PR.Ink)

            // the office
            val box = 22.dp.toPx()
            drawRoundRect(Palette5.PersianBlue, officeAt - Offset(box, box), Size(2 * box, 2 * box), CornerRadius(8.dp.toPx()))
            val officeLabel = textMeasurer.measure("Office", labelStyle.copy(color = Color.White))
            drawText(officeLabel, topLeft = officeAt - Offset(officeLabel.size.width / 2f, officeLabel.size.height / 2f))

            neighbors.forEachIndexed { i, neighbor ->
                val status = NetworkMonitor.status(state, neighbor)
                val color = status.color()
                val to = Offset(size.width * 0.86f, (i + 0.42f) * size.height / rows)
                val from = Offset(officeAt.x + box, officeAt.y)

                // the link
                val dashed = if (status == LinkStatus.NO_LINK) PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
                drawLine(color.copy(alpha = 0.45f), from, to, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashed)

                // the packets
                PacketFlow.dots(status, phase.value).forEach { dot ->
                    val at = Offset(from.x + (to.x - from.x) * dot.progress, from.y + (to.y - from.y) * dot.progress)
                    if (dot.lost) {
                        val r = 4.dp.toPx()
                        drawLine(PR.Red, at - Offset(r, r), at + Offset(r, r), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(PR.Red, at + Offset(-r, r), at + Offset(r, -r), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    } else {
                        drawCircle(Palette5.PersianBlue, 4.dp.toPx(), at)
                    }
                }

                // the building at the end of the link: a ring pulses while a problem is showing
                if (status == LinkStatus.DEGRADED || status == LinkStatus.DOWN) {
                    drawCircle(color.copy(alpha = 0.35f * (1f - phase.value)), nodeRadius * (1f + 0.8f * phase.value), to)
                }
                drawCircle(color, nodeRadius, to)
                val reported = NetworkMonitor.isReported(state, neighbor)
                val name = textMeasurer.measure(if (reported) "${neighbor.name} ✓" else neighbor.name, labelStyle)
                drawText(name, topLeft = Offset(to.x - name.size.width / 2f, to.y + nodeRadius + 1.dp.toPx()))
            }
        }

        Box(Modifier.fillMaxWidth().height(36.dp)) {
            if (message != null) Text(message, fontSize = 13.sp, lineHeight = 18.sp, color = PR.Ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("Caught early ${state.todayCaught}", PR.Green50, PR.Green800)
            Pill("Bonus ₩${state.todayBonus}", PR.Amber50, PR.Amber800)
            Pill("False alarms ${state.todayFalseAlarms}", PR.Red50, PR.Red800)
        }
    }
}
