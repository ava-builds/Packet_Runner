package com.avabuilds.packetrunner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebriefScreen(result: LevelResult, onRetry: () -> Unit, onMap: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(if (result.cleared) "Mission complete" else "Mission failed", fontSize = 13.sp, color = PR.Muted)
        Text(
            if (result.cleared) "All packets delivered!" else "Glitch caught Pip!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        StarRow(result.stars, size = 40)
        if (result.cleared) Pip(size = 110.dp) else Glitch(size = 100.dp)

        Panel {
            Text("What really happened", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(RouterStationLevel.WHAT_REALLY_HAPPENED, fontSize = 14.sp, lineHeight = 21.sp, color = PR.Muted)
        }

        if (result.mistakes.isNotEmpty()) {
            Text(
                "Mistakes to review (${result.mistakes.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            result.mistakes.forEach { MistakeCard(it) }
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(if (result.cleared) "Play again" else "Try again", onClick = onRetry)
        SecondaryButton("Back to map", onClick = onMap)
    }
}

@Composable
fun MistakeCard(mistake: Mistake) {
    Panel(borderColor = PR.Line) {
        Text(mistake.question, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("You: ${mistake.yourAnswer}", PR.Red50, PR.Red800)
            Pill("Answer: ${mistake.correctAnswer}", PR.Green50, PR.Green800)
        }
        Text(mistake.explanation, fontSize = 12.sp, lineHeight = 18.sp, color = PR.Muted, fontFamily = FontFamily.Monospace)
    }
}
