package com.avabuilds.packetrunner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BriefingScreen(briefing: Briefing, onStart: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("← Map", color = PR.Muted)
        }
        Column {
            Text("${briefing.chapter} · Mission briefing", fontSize = 13.sp, color = PR.Muted)
            Text(briefing.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        NanaSays(briefing.nanaSays)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Why this matters", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(briefing.why, fontSize = 14.sp, lineHeight = 21.sp, color = PR.Muted)
        }

        Panel(color = PR.Teal50) {
            Text("💡 How to think", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PR.Teal800)
            Text(briefing.howToThink, fontSize = 14.sp, lineHeight = 21.sp, color = PR.Teal800)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Key words", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            briefing.keywords.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { Pill(it) }
                }
            }
        }

        Panel(color = PR.Purple50) {
            Text("📖 In the book", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Purple800)
            Text(briefing.book, fontSize = 13.sp, lineHeight = 19.sp, color = PR.Muted)
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton("Start mission", onClick = onStart)
    }
}
