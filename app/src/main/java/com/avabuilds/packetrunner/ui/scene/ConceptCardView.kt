package com.avabuilds.packetrunner.ui.scene

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import com.avabuilds.packetrunner.ui.StarRow

/** "What you just did": the ideas the player used, in English and Korean, and how the job was paid. */
@Composable
fun ConceptCardView(
    concepts: List<Concept>,
    reward: Int,
    spent: Int,
    stars: Int,
    tip: Int,
    payout: Int,
    onSaveToNotebook: (() -> Unit)?,
    onContinue: () -> Unit,
    /** Replaces the payout line (for jobs that do not work like a customer paying). With stars = 0 the star row is hidden. */
    summary: String? = null,
) {
    var saved by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().background(Color(0x99091540)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Panel(Modifier.padding(20.dp), color = PR.White, borderColor = Palette5.PersianBlue) {
            Text("What you just did", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            if (stars > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StarRow(stars, size = 22)
                Text(
                    when (stars) {
                        3 -> "Great work! You spent close to the least possible."
                        2 -> "Good job. A cheaper way exists."
                        else -> "Done. There is a much cheaper way."
                    },
                    fontSize = 12.sp,
                    color = PR.Muted,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                concepts.forEach { c ->
                    Column {
                        Text(c.en, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                        Text(c.ko, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
                        Text(c.explanation, fontSize = 12.sp, lineHeight = 17.sp, color = PR.Muted)
                        Text("📖 ${c.book}", fontSize = 11.sp, color = PR.Muted)
                    }
                }
            }
            Text(
                summary ?: ("Customer pays ₩$reward − materials ₩$spent" + (if (tip > 0) " + tip ₩$tip" else "") + " = ₩$payout"),
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PR.Ink,
            )
            if (onSaveToNotebook != null) {
                SecondaryButton(if (saved) "📓 Saved to your notebook ✓" else "📓 Save to notebook") {
                    if (!saved) { saved = true; onSaveToNotebook() }
                }
            }
            PrimaryButton("Continue", onClick = onContinue)
        }
    }
}
