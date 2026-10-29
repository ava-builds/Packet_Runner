package com.avabuilds.packetrunner.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.SlidingWindow
import com.avabuilds.packetrunner.network.WindowRound
import com.avabuilds.packetrunner.network.WindowRun
import com.avabuilds.packetrunner.network.WindowRounds
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton

/**
 * Three rounds. The player picks the window size, sends the frames, and sees a chart of when every frame
 * was sent and when its acknowledgement came back. A round is won by finishing as fast as possible.
 */
@Composable
fun WindowGame(onSuccess: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val spec: WindowRound = WindowRounds.all[round]
    var window by remember(round) { mutableIntStateOf(1) }
    var run by remember(round) { mutableStateOf<WindowRun?>(null) }
    val ideal = SlidingWindow.idealFinish(spec.frames, spec.rtt)

    Text("Round ${round + 1} of ${WindowRounds.all.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
    Text(
        "Send ${spec.frames} frames, one per tick. An acknowledgement comes back ${spec.rtt} ticks after its frame left. " +
            "The sender may have at most a window of frames waiting for acknowledgements. Find the smallest window that finishes in $ideal ticks.",
        fontSize = 13.sp, color = PR.Ink,
    )

    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Window size", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Box(Modifier.clickable(enabled = window > 1) { window--; run = null }) { Pill("−") }
            Text("$window", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clickable(enabled = window < spec.frames) { window++; run = null }) { Pill("+") }
        }
    }
    PrimaryButton("Send the frames") { run = SlidingWindow.run(spec.frames, window, spec.rtt) }

    run?.let { r ->
        Panel(color = PR.White, borderColor = if (r.finishTick == ideal) PR.Green600 else PR.Line) {
            Chart(r, ideal)
            Text("Finished at tick ${r.finishTick}. The best possible is $ideal.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PR.Ink)
            if (r.finishTick == ideal) {
                Text("The pipe is full: a new frame leaves every tick. A window of $window is exactly what ${spec.rtt} ticks of waiting needs.", fontSize = 12.sp, color = PR.Green800)
            } else {
                Text("The sender sat idle, waiting for acknowledgements. Dark blocks are frames being sent, light bars are frames waiting for their acknowledgement.", fontSize = 12.sp, color = PR.Muted)
            }
        }
        if (r.finishTick == ideal) {
            PrimaryButton(if (round == WindowRounds.all.lastIndex) "Finish" else "Next round") {
                if (round == WindowRounds.all.lastIndex) onSuccess() else round++
            }
        }
    }
}

/** One row per frame: a light bar from its send tick to its acknowledgement, a dark block at the send tick. */
@Composable
private fun Chart(run: WindowRun, ideal: Int) {
    val rows = run.traces.size
    Canvas(Modifier.fillMaxWidth().height((rows * 14 + 6).dp).background(PR.Teal50, RoundedCornerShape(8.dp))) {
        val ticks = maxOf(run.finishTick, ideal).toFloat()
        val unit = size.width / (ticks + 1f)
        val rowH = 14.dp.toPx()
        run.traces.forEachIndexed { i, t ->
            val y = 3.dp.toPx() + i * rowH
            drawRect(Palette5.IcyBlue, Offset(t.sentAt * unit, y), Size((t.ackedAt - t.sentAt) * unit, rowH - 3.dp.toPx()))
            drawRect(Palette5.PersianBlue, Offset(t.sentAt * unit, y), Size(unit.coerceAtLeast(2.dp.toPx()), rowH - 3.dp.toPx()))
        }
        drawLine(PR.Amber, Offset((ideal + 1) * unit, 0f), Offset((ideal + 1) * unit, size.height), strokeWidth = 2.dp.toPx())
    }
}
