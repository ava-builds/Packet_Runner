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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.Transport
import com.avabuilds.packetrunner.network.TransportApp
import com.avabuilds.packetrunner.network.TransportApps
import com.avabuilds.packetrunner.network.TransportSim
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton

private val OnTime = Color(0xFF7CC79A)
private val Late = Color(0xFFF2C26B)
private val Gone = Color(0xFFE9A19F)

/** Four apps, two transports. The player chooses, runs the same lossy network for all of them, and sees who suffers. */
@Composable
fun TransportGame(onSuccess: () -> Unit) {
    val choice = remember { mutableStateMapOf<String, Transport>() }
    var ran by remember { mutableStateOf(false) }

    Text("The network loses a few segments on the way. Choose UDP or TCP for each app, then run the test.", fontSize = 13.sp, color = PR.Ink)
    Text("UDP: just send, fast, nothing is resent.  TCP: resend what is lost, deliver in order.", fontSize = 12.sp, color = PR.Muted)

    TransportApps.all.forEach { app ->
        val picked = choice[app.name]
        val verdict = if (ran && picked != null) app.judge(picked) else null
        Panel(color = PR.White, borderColor = when (verdict) { null -> PR.Line; TransportApp.Verdict.GOOD -> PR.Green600; else -> PR.Red }) {
            Text(app.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            Text(app.story, fontSize = 12.sp, color = PR.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Transport.entries.forEach { t ->
                    Box(Modifier.clickable { choice[app.name] = t; ran = false }) {
                        Pill(t.label, if (picked == t) Palette5.PersianBlue else PR.Blue50, if (picked == t) PR.White else PR.Blue800)
                    }
                }
            }
            if (verdict != null && picked != null) {
                Timeline(app, picked)
                Text(
                    when (verdict) {
                        TransportApp.Verdict.GOOD -> "✓ Works. " + if (picked == Transport.UDP) "Nothing waits, and the few lost segments do not matter here." else "Everything arrives, in order."
                        TransportApp.Verdict.STUTTERS -> "✗ It stutters: after a loss TCP makes every later segment wait for the resend."
                        TransportApp.Verdict.DATA_MISSING -> "✗ Data is missing: UDP never resends, and this app needs every byte."
                    },
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (verdict == TransportApp.Verdict.GOOD) PR.Green800 else PR.Red800,
                )
            }
        }
    }

    val allChosen = TransportApps.all.all { it.name in choice }
    val allGood = allChosen && TransportApps.all.all { it.judge(choice.getValue(it.name)) == TransportApp.Verdict.GOOD }
    if (ran && allGood) PrimaryButton("Finish", onClick = onSuccess)
    else PrimaryButton(if (allChosen) "Run the network test" else "Choose a transport for every app first") { if (allChosen) ran = true }
}

/** One square per segment: green = on time, amber = late, red = lost. */
@Composable
private fun Timeline(app: TransportApp, transport: Transport) {
    val run = TransportSim.run(transport, app.segments, app.lost)
    Canvas(Modifier.fillMaxWidth().height(22.dp).background(PR.Teal50, RoundedCornerShape(6.dp))) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (run.fates.size + 1)) / run.fates.size
        run.fates.forEachIndexed { i, f ->
            val color = when {
                f.deliveredAt == null -> Gone
                app.maxLag != null && f.lag!! > app.maxLag -> Late
                f.retransmitted -> Late
                else -> OnTime
            }
            drawRect(color, Offset(gap + i * (w + gap), 3.dp.toPx()), Size(w, size.height - 6.dp.toPx()))
        }
    }
}
