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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.Ipv4
import com.avabuilds.packetrunner.network.PacketResult
import com.avabuilds.packetrunner.network.Route
import com.avabuilds.packetrunner.network.RoutingPuzzle
import com.avabuilds.packetrunner.network.explainLookup
import com.avabuilds.packetrunner.ui.NanaSays
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

/**
 * The City Hall job: a router's forwarding table with the wrong subnet masks. The player changes each
 * row's mask, sends test packets, and taps a packet to see the AND of its address and the mask in binary.
 */
@Composable
fun RoutingScreen(
    puzzle: RoutingPuzzle,
    clerkName: String,
    reward: Int,
    onFinish: (success: Boolean, payout: Int, stars: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToNotebook: ((title: String, concepts: List<Concept>) -> Unit)? = null,
) {
    val prefixes = remember(puzzle) { mutableStateListOf(*puzzle.startPrefixes().toTypedArray()) }
    var results by remember(puzzle) { mutableStateOf<List<PacketResult>?>(null) }
    var tests by remember(puzzle) { mutableIntStateOf(0) }
    var selected by remember(puzzle) { mutableIntStateOf(-1) }
    var hintsShown by remember(puzzle) { mutableIntStateOf(0) }
    var showConcepts by remember(puzzle) { mutableStateOf(false) }

    val solved = results?.all { it.ok } == true
    val stars = puzzle.stars(tests)
    val payout = reward + puzzle.tip(stars)

    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(puzzle.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("$clerkName · pays ₩$reward", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = { onFinish(false, 0, 0) }) { Text("Give up", color = PR.Red800, fontSize = 13.sp) }
            }
            Text(puzzle.story, fontSize = 13.sp, color = PR.Ink)

            Panel(color = PR.White, borderColor = PR.Line) {
                Text("Forwarding table", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                puzzle.rows.forEachIndexed { i, row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(row.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                            Text("network ${row.network}", fontSize = 11.sp, color = PR.Muted)
                            Text("→ ${row.nextHop}", fontSize = 11.sp, color = Palette5.PersianBlue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.clickable(enabled = prefixes[i] > RoutingPuzzle.MIN_PREFIX) { prefixes[i] = prefixes[i] - 1; results = null; selected = -1 }) { Pill("−") }
                                Text("/${prefixes[i]}", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Box(Modifier.clickable(enabled = prefixes[i] < RoutingPuzzle.MAX_PREFIX) { prefixes[i] = prefixes[i] + 1; results = null; selected = -1 }) { Pill("+") }
                            }
                            Text(Ipv4.maskFromPrefix(prefixes[i]).toString(), fontSize = 11.sp, color = PR.Muted, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Text("Anything else → ${puzzle.defaultHop} (default route 0.0.0.0/0)", fontSize = 11.sp, color = PR.Muted)
            }

            PrimaryButton("Send test packets") {
                results = puzzle.send(prefixes.toList())
                tests++
                selected = -1
            }

            if (solved) PrimaryButton("Finish job · ${"★".repeat(stars)} · you get ₩$payout") { showConcepts = true }

            results?.let { list ->
                Panel(color = PR.White, borderColor = if (solved) PR.Green600 else PR.Line) {
                    Text("Test packets · sent $tests ${if (tests == 1) "time" else "times"}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    list.forEachIndexed { i, r ->
                        val tint = if (r.ok) PR.Green50 else PR.Red50
                        Column(
                            Modifier.fillMaxWidth().background(tint, RoundedCornerShape(10.dp))
                                .border(if (selected == i) 2.dp else 0.dp, Palette5.PersianBlue, RoundedCornerShape(10.dp))
                                .clickable { selected = if (selected == i) -1 else i }.padding(8.dp),
                        ) {
                            Text(
                                if (r.ok) "✓ ${r.packet.dest} → ${r.actual}" else "✗ ${r.packet.dest} → ${r.actual}, should be ${r.packet.expected}",
                                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (r.ok) PR.Green800 else PR.Red800,
                            )
                            if (selected == i) LookupDetail(puzzle, prefixes.toList(), r)
                        }
                    }
                    if (!solved) Text("Tap a packet to see how the router decided.", fontSize = 11.sp, color = PR.Muted)
                }
            }

            if (hintsShown > 0) NanaSays(puzzle.hints[(hintsShown - 1).coerceAtMost(puzzle.hints.lastIndex)])
            SecondaryButton("📻 Ask Nana") { hintsShown++ }
        }

        if (showConcepts) {
            ConceptCardView(
                concepts = puzzle.concepts,
                reward = reward,
                spent = 0,
                stars = stars,
                tip = puzzle.tip(stars),
                payout = payout,
                onSaveToNotebook = onSaveToNotebook?.let { save -> { save(puzzle.title, puzzle.concepts) } },
                onContinue = { onFinish(true, payout, stars) },
            )
        }
    }
}

/** The binary AND for one test packet: against the row that should catch it, or the row that did. */
@Composable
private fun LookupDetail(puzzle: RoutingPuzzle, prefixes: List<Int>, result: PacketResult) {
    val rowIndex = puzzle.rows.indexOfFirst { it.nextHop == result.packet.expected }
    val target = if (rowIndex >= 0) puzzle.rows[rowIndex].let { Route(it.network, prefixes[rowIndex], it.nextHop) } else Route(Ipv4.parse("0.0.0.0"), 0, puzzle.defaultHop)
    val e = explainLookup(result.packet.dest, target)
    val mono = FontFamily.Monospace
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Row for ${result.packet.expected}: ${target.network}/${target.prefix}", fontSize = 11.sp, color = PR.Ink)
        Text("address ${e.dest}", fontSize = 10.sp, fontFamily = mono)
        Text("mask    ${e.mask}", fontSize = 10.sp, fontFamily = mono)
        Text("AND     ${e.and}", fontSize = 10.sp, fontFamily = mono)
        Text("network ${e.network}", fontSize = 10.sp, fontFamily = mono)
        Text(
            if (e.matches) "AND equals the network: this row matches." else "AND is not the network: this row does not match.",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (e.matches) PR.Green800 else PR.Red800,
        )
    }
}
