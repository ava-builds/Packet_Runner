package com.avabuilds.packetrunner.ui.lab

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Curriculum
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.LabGame
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.scene.ConceptCardView

/** The Lab menu: small games that pay a little once a day. A game opens with the level of its topic; locked ones show which level. */
@Composable
fun LabMenu(doneToday: Set<String>, level: Int, onPick: (LabGame) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🔬 The Lab", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("Small games between calls. Each pays once a day.", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            LabGame.entries.forEach { game ->
                val done = game.id in doneToday
                val open = Curriculum.labOpen(game.id, level)
                Panel(
                    if (open) Modifier.clickable { onPick(game) } else Modifier,
                    color = if (open) PR.White else PR.GrayFill,
                    borderColor = if (!open) PR.Line else if (done) PR.Green600 else Palette5.PersianBlue,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(game.title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (open) PR.Ink else PR.Gray)
                        when {
                            !open -> Pill("🔒 Level ${Curriculum.labLevel(game.id)}", PR.GrayFill, PR.Gray)
                            done -> Pill("✓ paid today", PR.Green50, PR.Green800)
                            else -> Pill("₩${game.reward}", PR.Amber50, PR.Amber800)
                        }
                    }
                    Text(if (open) game.blurb else "Opens at level ${Curriculum.labLevel(game.id)}: ${Curriculum.stage(Curriculum.labLevel(game.id)).title}.", fontSize = 13.sp, color = PR.Muted)
                }
            }
        }
    }
}

/**
 * The frame around a Lab game: a title and a Back button, then the game. When the game reports success
 * the "What you just did" card appears with what was learned; Continue hands the result back.
 */
@Composable
fun LabGameShell(
    game: LabGame,
    paysToday: Boolean,
    onQuit: () -> Unit,
    onFinish: () -> Unit,
    onSaveToNotebook: (title: String, concepts: List<Concept>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (onSuccess: () -> Unit) -> Unit,
) {
    var won by remember(game) { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(game.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text(if (paysToday) "Pays ₩${game.reward} once today" else "Already paid today · practice", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onQuit) { Text("Back", color = PR.Red800, fontSize = 14.sp) }
            }
            content { won = true }
        }
        if (won) {
            ConceptCardView(
                concepts = game.concepts,
                reward = game.reward, spent = 0, stars = 0, tip = 0, payout = game.reward,
                summary = if (paysToday) "Lab reward: +₩${game.reward}" else "Practice round: already paid today",
                onSaveToNotebook = { onSaveToNotebook(game.title, game.concepts) },
                onContinue = onFinish,
            )
        }
    }
}
