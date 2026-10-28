package com.avabuilds.packetrunner.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.avabuilds.packetrunner.network.NatPuzzle
import com.avabuilds.packetrunner.network.NatTable
import com.avabuilds.packetrunner.ui.NanaSays
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

/**
 * The Apartments job. Part 1: let each flat's packet out and watch the NAT table fill. Part 2: deliver the
 * replies that come back to the right flat (and drop the one nobody asked for). Part 3: add a port-forwarding rule
 * for a flat's website.
 */
@Composable
fun NatScreen(
    puzzle: NatPuzzle,
    tenantName: String,
    reward: Int,
    onFinish: (success: Boolean, payout: Int, stars: Int) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToNotebook: ((title: String, concepts: List<Concept>) -> Unit)? = null,
) {
    var part by remember(puzzle) { mutableIntStateOf(0) }
    val sent = remember(puzzle) { mutableStateListOf<Int>() } // connections that have left, in order
    val incoming = remember(puzzle) { puzzle.incoming() }
    var handled by remember(puzzle) { mutableIntStateOf(0) } // replies delivered so far
    var mistakes by remember(puzzle) { mutableIntStateOf(0) }
    var note by remember(puzzle) { mutableStateOf<String?>(null) }
    var ruleFlat by remember(puzzle) { mutableStateOf<Int?>(null) }
    var served by remember(puzzle) { mutableStateOf(false) }
    var hintsShown by remember(puzzle) { mutableIntStateOf(0) }
    var showConcepts by remember(puzzle) { mutableStateOf(false) }

    val table = NatTable().also { t -> sent.forEach { t.outbound(puzzle.connections[it]) } }
    val stars = puzzle.stars(mistakes)
    val payout = reward + puzzle.tip(stars)

    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(puzzle.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("$tenantName · pays ₩$reward", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = { onFinish(false, 0, 0) }) { Text("Give up", color = PR.Red800, fontSize = 13.sp) }
            }
            Text(puzzle.story, fontSize = 13.sp, color = PR.Ink)

            // The NAT table: always on screen, because every part needs it
            Panel(color = PR.White, borderColor = PR.Line) {
                Text("NAT table · public address ${puzzle.publicIp}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (table.entries.isEmpty()) Text("Empty. Nothing has left the building yet.", fontSize = 12.sp, color = PR.Muted)
                table.entries.forEach { e ->
                    val flat = puzzle.flats[e.flat]
                    Text("${flat.ip}:${e.privatePort}  ⇄  ${puzzle.publicIp}:${e.publicPort}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PR.Ink)
                }
                if (part == 2) Text("Port-forwarding row: ${puzzle.publicIp}:${puzzle.serverPort}  ⇄  ${ruleFlat?.let { puzzle.flats[it].ip.toString() + ":" + puzzle.serverPort } ?: "(none yet)"}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Palette5.PersianBlue)
            }

            if (served) PrimaryButton("Finish job · ${"★".repeat(stars)} · you get ₩$payout") { showConcepts = true }

            when (part) {
                0 -> OutgoingPart(puzzle, sent, onNext = { part = 1; note = null })
                1 -> RepliesPart(puzzle, incoming, handled, note,
                    onChoose = { packet, choice ->
                        if (puzzle.isRight(packet, choice)) {
                            handled++
                            note = if (choice == null) "Dropped. Nobody inside asked for that packet, so it has no row." else "Delivered to ${puzzle.flats[choice].name}."
                            if (handled == incoming.size) { part = 2; note = null }
                        } else {
                            mistakes++
                            note = if (choice == null) "That reply is in the table: its port belongs to ${puzzle.flats[puzzle.rightAnswer(packet)!!].name}. Do not drop it."
                            else if (puzzle.rightAnswer(packet) == null) "No row has that port: nobody asked for this packet, so the router drops it."
                            else "Not that flat. Look for port ${packet.publicPort} in the NAT table."
                        }
                    })
                else -> ServerPart(puzzle, ruleFlat, served, note,
                    onTry = { flat ->
                        ruleFlat = flat
                        if (puzzle.visitorsServed(flat)) { served = true; note = null } else { mistakes++; served = false; note = "Visitors reached ${puzzle.flats[flat].name}, but the website runs somewhere else. Try another flat." }
                    })
            }

            if (hintsShown > 0) NanaSays(puzzle.hints[(hintsShown - 1).coerceAtMost(puzzle.hints.lastIndex)])
            SecondaryButton("📻 Ask Nana") { hintsShown++ }
        }

        if (showConcepts) {
            ConceptCardView(
                concepts = puzzle.concepts,
                reward = reward, spent = 0, stars = stars, tip = puzzle.tip(stars), payout = payout,
                onSaveToNotebook = onSaveToNotebook?.let { save -> { save(puzzle.title, puzzle.concepts) } },
                onContinue = { onFinish(true, payout, stars) },
            )
        }
    }
}

@Composable
private fun OutgoingPart(puzzle: NatPuzzle, sent: MutableList<Int>, onNext: () -> Unit) {
    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Text("1 · Let the packets out", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Each flat opens a connection from its private address. Send them one by one and watch the router rewrite them.", fontSize = 12.sp, color = PR.Muted)
        puzzle.connections.forEachIndexed { i, c ->
            val flat = puzzle.flats[c.flat]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(flat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("${flat.ip}:${c.privatePort} → ${c.remote}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = PR.Muted)
                }
                val isNext = sent.size == i
                when {
                    i < sent.size -> Pill("✓ sent", PR.Green50, PR.Green800)
                    isNext -> Box(Modifier.clickable { sent.add(i) }) { Pill("Send", Palette5.PersianBlue, PR.White) }
                    else -> Pill("waiting", PR.Blue50, PR.Blue800)
                }
            }
        }
    }
    if (sent.size == puzzle.connections.size) PrimaryButton("The replies are coming back", onClick = onNext)
}

@Composable
private fun RepliesPart(puzzle: NatPuzzle, incoming: List<com.avabuilds.packetrunner.network.IncomingPacket>, handled: Int, note: String?, onChoose: (com.avabuilds.packetrunner.network.IncomingPacket, Int?) -> Unit) {
    val packet = incoming[handled]
    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Text("2 · Deliver the replies (${handled + 1} of ${incoming.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("From ${packet.fromRemote}", fontSize = 12.sp, color = PR.Muted)
        Text("→ ${puzzle.publicIp}:${packet.publicPort}", fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
        Text("Which flat is it for? Look the port up in the NAT table.", fontSize = 12.sp, color = PR.Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            puzzle.flats.forEachIndexed { i, f -> Box(Modifier.clickable { onChoose(packet, i) }) { Pill(f.name, PR.Blue50, PR.Blue800) } }
            Box(Modifier.clickable { onChoose(packet, null) }) { Pill("Drop it", PR.Red50, PR.Red800) }
        }
    }
    note?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
}

@Composable
private fun ServerPart(puzzle: NatPuzzle, ruleFlat: Int?, served: Boolean, note: String?, onTry: (Int) -> Unit) {
    Panel(color = PR.White, borderColor = if (served) PR.Green600 else Palette5.PersianBlue) {
        Text("3 · Let visitors in", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "${puzzle.flats[puzzle.serverFlat].name} runs a website. Visitors send packets to ${puzzle.publicIp}:${puzzle.serverPort}, " +
                "but nobody inside asked for them, so the router would drop them. Add a port-forwarding rule: which flat should port ${puzzle.serverPort} go to?",
            fontSize = 12.sp, color = PR.Muted,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            puzzle.flats.forEachIndexed { i, f ->
                Box(Modifier.clickable(enabled = !served) { onTry(i) }) {
                    Pill("Forward to ${f.name.substringBefore(" ·")}", if (ruleFlat == i) Palette5.PersianBlue else PR.Blue50, if (ruleFlat == i) PR.White else PR.Blue800)
                }
            }
        }
        if (ruleFlat != null) {
            puzzle.visitors.forEach { v ->
                val to = puzzle.visitorGoesTo(v, ruleFlat)
                Text(
                    "${v.note} (port ${v.port}) → " + (to?.let { "delivered to ${puzzle.flats[it].name}" } ?: "dropped: no rule for this port"),
                    fontSize = 12.sp, color = if (puzzle.visitorsServed(ruleFlat)) PR.Green800 else PR.Ink,
                )
            }
        }
        if (served) Text("Visitors reach the website, and the router still drops everything else.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PR.Green800)
    }
    note?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
}
