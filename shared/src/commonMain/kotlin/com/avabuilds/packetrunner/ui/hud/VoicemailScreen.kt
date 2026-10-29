package com.avabuilds.packetrunner.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.formatTime
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton

/**
 * The voicemail inbox: calls that were missed. They can be returned any time before clock-out; a neighbor whose voicemail
 * is still unanswered at clock-out is a little sad.
 */
@Composable
fun VoicemailScreen(
    voicemails: List<Request>,
    neighbors: List<Neighbor>,
    canCallBack: Boolean,
    reason: String?,
    onCallBack: (Request) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📨 Voicemail", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("Missed calls. Call back before you clock out.", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            if (voicemails.isEmpty()) {
                Panel(color = PR.White, borderColor = PR.Line) {
                    Text("No voicemails. Everyone you missed has been called back.", fontSize = 13.sp, color = PR.Muted)
                }
            }
            reason?.let { Text(it, fontSize = 13.sp, color = PR.Red800) }
            voicemails.forEach { call ->
                val who = neighbors.firstOrNull { it.id == call.neighborId }?.name ?: "A neighbor"
                Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(who, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                        Pill(call.type.label, PR.Purple50, PR.Purple800)
                        Pill("₩${call.reward}", PR.Amber50, PR.Amber800)
                    }
                    Text("Left at ${formatTime(call.callMinute)}", fontSize = 11.sp, color = PR.Muted)
                    Text("“${call.message}”", fontSize = 13.sp, lineHeight = 18.sp, color = PR.Ink)
                    if (canCallBack) PrimaryButton("Call $who back") { onCallBack(call) }
                }
            }
        }
    }
}
