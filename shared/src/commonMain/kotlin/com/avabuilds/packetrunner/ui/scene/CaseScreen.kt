package com.avabuilds.packetrunner.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.ActionResult
import com.avabuilds.packetrunner.network.CaseFile
import com.avabuilds.packetrunner.network.CaseSession
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.Scan
import com.avabuilds.packetrunner.ui.NanaSays
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

/**
 * A "look, find, fix" job: read the story and symptoms, open the scans to see what really happens,
 * then pick the action you think fixes it. Wrong actions cost money and explain themselves.
 */
@Composable
fun CaseScreen(
    case: CaseFile,
    callerName: String,
    reward: Int,
    onFinish: (success: Boolean, payout: Int, stars: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToNotebook: ((title: String, concepts: List<Concept>) -> Unit)? = null,
) {
    val session = remember(case) { CaseSession(case) }
    var version by remember(case) { mutableIntStateOf(0) } // the session is a plain class, so this tells Compose it changed
    var scanIndex by remember(case) { mutableIntStateOf(0) }
    var last by remember(case) { mutableStateOf<ActionResult?>(null) }
    var hintsShown by remember(case) { mutableIntStateOf(0) }
    var showConcepts by remember(case) { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION") version

    val solved = session.solved
    val payout = session.payout(reward)

    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(case.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("$callerName · pays ₩$reward", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = { onFinish(false, 0, 0) }) { Text("Give up", color = PR.Red800, fontSize = 13.sp) }
            }
            Text(case.story, fontSize = 13.sp, color = PR.Ink)

            Panel(color = PR.White, borderColor = PR.Line) {
                Text("Symptoms", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                case.symptoms.forEach { Text("• $it", fontSize = 12.sp, color = PR.Ink) }
            }

            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                Text("Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    case.scans.forEachIndexed { i, s ->
                        Box(Modifier.clickable { scanIndex = i; session.openScan(i); version++ }) {
                            Pill(s.name, if (i == scanIndex) PR.Blue50 else PR.White, if (i == scanIndex) PR.Blue800 else PR.Muted)
                        }
                    }
                }
                ScanTable(case.scans[scanIndex.coerceIn(0, case.scans.lastIndex)])
            }

            if (!solved) {
                Panel(color = PR.White, borderColor = PR.Line) {
                    Text("What do you do?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    case.actions.forEachIndexed { i, action ->
                        SecondaryButton(if (action.cost > 0) "${action.label}  (−₩${action.cost} if wrong)" else action.label) {
                            last = session.choose(i)
                            version++
                        }
                    }
                }
            }

            last?.let { result ->
                Panel(color = if (result.solved) PR.Green50 else PR.Red50, borderColor = if (result.solved) PR.Green600 else PR.Red800) {
                    Text(
                        if (result.solved) "✓ ${result.action.label}" else "✗ ${result.action.label}${if (result.action.cost > 0) " · −₩${result.action.cost}" else ""}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (result.solved) PR.Green800 else PR.Red800,
                    )
                    Text(result.action.consequence, fontSize = 12.sp, color = PR.Ink)
                }
            }

            if (solved) {
                Panel(color = PR.White, borderColor = PR.Green600) {
                    Text("After the fix", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    ScanTable(case.afterFix)
                }
                PrimaryButton("Finish job · ${"★".repeat(session.stars())} · you get ₩$payout") { showConcepts = true }
            } else {
                if (hintsShown > 0) NanaSays(case.hints[(hintsShown - 1).coerceAtMost(case.hints.lastIndex)])
                SecondaryButton("📻 Ask Nana") { hintsShown++ }
            }
        }

        if (showConcepts) {
            ConceptCardView(
                concepts = case.concepts,
                reward = reward,
                spent = session.spent,
                stars = session.stars(),
                tip = session.tip(),
                payout = payout,
                onSaveToNotebook = onSaveToNotebook?.let { save -> { save(case.title, case.concepts) } },
                onContinue = { onFinish(true, payout, session.stars()) },
            )
        }
    }
}

/** A small table: a header row and the scan's rows, in a monospace font so addresses line up. */
@Composable
private fun ScanTable(scan: Scan) {
    Text(scan.note, fontSize = 11.sp, color = PR.Muted)
    // each column is as wide as its longest text, so long MAC addresses get room and short ones do not waste it
    val weights = scan.columns.indices.map { c -> (scan.rows.map { row -> row[c].lines().maxOf { it.length } } + scan.columns[c].length).max().coerceAtLeast(4).toFloat() }
    Column(
        Modifier.fillMaxWidth().border(1.dp, PR.Line, RoundedCornerShape(10.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scan.columns.forEachIndexed { c, name -> Text(name, Modifier.weight(weights[c]), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Palette5.PersianBlue) }
        }
        scan.rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { c, cell -> Text(cell, Modifier.weight(weights[c]), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = PR.Ink) }
            }
        }
    }
}
