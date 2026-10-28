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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.BridgeAction
import com.avabuilds.packetrunner.network.LanMission
import com.avabuilds.packetrunner.network.LanSession
import com.avabuilds.packetrunner.network.outPorts
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

private val Wasted = Color(0xFFE9A19F)
private val Delivered = Color(0xFF7CC79A)
private val Flooded = Color(0xFFF2C26B)

/** The office LAN: five computers on a hub, then on a switch that learns. The missions guide the player through the lesson. */
@Composable
fun LanGame(onSuccess: () -> Unit) {
    val session = remember { LanSession() }
    var tick by remember { mutableIntStateOf(0) } // changes after every action so the screen redraws
    var from by remember { mutableIntStateOf(1) }
    var to by remember { mutableIntStateOf(4) }
    var message by remember { mutableStateOf("Five computers share one hub. Pick who sends and who should receive, then send a frame.") }
    var colors by remember { mutableStateOf<Map<Int, Color>>(emptyMap()) }
    val measurer = rememberTextMeasurer()
    @Suppress("UNUSED_EXPRESSION") tick

    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val w = size.width
            val deviceAt = Offset(w / 2, 34.dp.toPx())
            val pcY = 128.dp.toPx()
            val pcSize = 40.dp.toPx()
            fun pcAt(i: Int) = Offset(w * (i - 0.5f) / session.computers, pcY)
            for (i in 1..session.computers) drawLine(Palette5.DuskBlue.copy(alpha = 0.5f), deviceAt, pcAt(i) - Offset(0f, pcSize / 2), strokeWidth = 2.dp.toPx())
            val boxW = 90.dp.toPx(); val boxH = 36.dp.toPx()
            drawRoundRect(if (session.usesSwitch) Palette5.PersianBlue else PR.Amber, deviceAt - Offset(boxW / 2, boxH / 2), Size(boxW, boxH), CornerRadius(8.dp.toPx()))
            val label = measurer.measure(if (session.usesSwitch) "SWITCH" else "HUB", TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White))
            drawText(label, topLeft = deviceAt - Offset(label.size.width / 2f, label.size.height / 2f))
            for (i in 1..session.computers) {
                val c = pcAt(i)
                drawRoundRect(colors[i] ?: PR.Teal50, c - Offset(pcSize / 2, pcSize / 2), Size(pcSize, pcSize), CornerRadius(8.dp.toPx()))
                drawRoundRect(Palette5.PersianBlue, c - Offset(pcSize / 2, pcSize / 2), Size(pcSize, pcSize), CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                val t = measurer.measure(session.name(i), TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink))
                drawText(t, topLeft = c - Offset(t.size.width / 2f, t.size.height / 2f))
            }
        }
        Text(message, fontSize = 13.sp, lineHeight = 18.sp, color = PR.Ink, modifier = Modifier.fillMaxWidth().height(76.dp))
    }

    if (session.allDone) PrimaryButton("Finish", onClick = onSuccess)

    Panel(color = PR.White, borderColor = PR.Line) {
        Text("Lesson", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        LanMission.entries.forEach { m ->
            Text((if (session.isDone(m)) "✓ " else "○ ") + m.text, fontSize = 12.sp, color = if (session.isDone(m)) PR.Green800 else PR.Ink)
        }
    }

    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("From", fontSize = 12.sp, color = PR.Muted, modifier = Modifier.height(24.dp))
            (1..session.computers).forEach { i ->
                Box(Modifier.clickable { from = i; if (to == i) to = if (i == session.computers) 1 else i + 1 }) {
                    Pill(session.name(i), if (from == i) Palette5.PersianBlue else PR.Blue50, if (from == i) PR.White else PR.Blue800)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("To     ", fontSize = 12.sp, color = PR.Muted, modifier = Modifier.height(24.dp))
            (1..session.computers).forEach { i ->
                Box(Modifier.clickable(enabled = i != from) { to = i }) {
                    Pill(session.name(i), if (to == i) Palette5.PersianBlue else if (i == from) PR.GrayFill else PR.Blue50, if (to == i) PR.White else if (i == from) PR.Gray else PR.Blue800)
                }
            }
        }
        PrimaryButton("Send ${session.name(from)} → ${session.name(to)}") {
            if (!session.usesSwitch) {
                val d = session.hubSend(from, to)
                colors = d.receivers.associateWith { if (it == to) Delivered else Wasted } + (from to Palette5.IcyBlue)
                message = "The hub repeats the frame to every other computer: ${d.receivers.joinToString(", ") { session.name(it) }} all hear it. " +
                    "Only ${session.name(to)} needed it, so ${d.wasted.joinToString(", ") { session.name(it) }} got a useless copy."
            } else {
                val action = session.switchSend(from, to)
                val out = action.outPorts()
                colors = out.associateWith { if (action is BridgeAction.Flood) (if (it == to) Delivered else Flooded) else Delivered } + (from to Palette5.IcyBlue)
                message = when (action) {
                    is BridgeAction.Flood -> "The switch does not know ${session.name(to)} yet, so it floods the frame to every other port. It does learn that ${session.name(from)} is on port $from."
                    is BridgeAction.Forward -> "The switch knows ${session.name(to)} is on port ${action.port}, so only ${session.name(to)} gets the frame. The others hear nothing."
                    BridgeAction.Filter -> "Both are on the same port: the switch filters the frame."
                }
            }
            tick++
        }
        if (!session.usesSwitch) {
            SecondaryButton("${session.name(1)} and ${session.name(3)} send at the same time") {
                val t = session.hubCollide(1, 3)
                colors = mapOf(1 to Wasted, 3 to Wasted)
                message = "Collision! On a shared cable both frames get mixed up. Both senders notice (they listen while they send), stop, and each waits a random time. " +
                    t.rounds.mapIndexed { i, (a, b) -> "Try ${i + 1}: A waits $a, C waits $b" + if (a == b) " (tie, collide again)" else " (different, it works)" }.joinToString(". ") + "."
                tick++
            }
            if (session.isDone(LanMission.HUB_SEND) && session.isDone(LanMission.HUB_COLLIDE)) {
                PrimaryButton("Replace the hub with a switch") {
                    session.installSwitch(); colors = emptyMap(); from = 1; to = 4
                    message = "The switch is in. Its table is empty: it learns by watching which port each sender uses."
                    tick++
                }
            }
        }
    }

    if (session.usesSwitch) {
        Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
            Text("Switch table (what it has learned)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (session.switchTable.isEmpty()) Text("Empty so far.", fontSize = 12.sp, color = PR.Muted)
            session.switchTable.forEach { (name, port) -> Text("$name → port $port", fontSize = 13.sp, color = PR.Ink) }
        }
    }

}
