package com.avabuilds.packetrunner

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Step { CHECK_ROWS, SWAP_LAYER }

private data class Hint(val text: String, val isError: Boolean = false, val mono: Boolean = false)

private const val START_HINT =
    "Tap a row on the board to put its mask stencil on Pip's destination IP."

/**
 * World 3 gameplay. For each packet:
 * 1. Tap a forwarding-table row, compute (D AND mask) for the octet the mask cuts.
 * 2. If it equals the row's network, send Pip out through that row's next hop.
 * 3. Choose which layer changes at the router (only the Ethernet / MAC layer).
 */
@Composable
fun RouterStationScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = RouterStationLevel
    var roundIndex by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf(Step.CHECK_ROWS) }
    var lives by remember { mutableIntStateOf(3) }
    var ttl by remember { mutableIntStateOf(RouterStationLevel.START_TTL) }
    val mistakes = remember { mutableStateListOf<Mistake>() }
    var nana by remember { mutableStateOf(Hint(START_HINT)) }

    val round = level.rounds[roundIndex]
    var selectedRow by remember(roundIndex) { mutableStateOf<Int?>(null) }
    // row index -> did (D AND mask) equal the row's network?
    val rowResults = remember(roundIndex) { mutableStateMapOf<Int, Boolean>() }

    fun loseLife(mistake: Mistake, message: String, mono: Boolean = false) {
        mistakes.add(mistake)
        lives -= 1
        nana = Hint(message, isError = true, mono = mono)
        if (lives <= 0) onFinish(LevelResult(cleared = false, stars = 0, mistakes = mistakes.toList()))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- HUD ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onQuit) { Text("✕", fontSize = 18.sp, color = PR.Muted) }
            Column(Modifier.weight(1f)) {
                Text("World 3 · Router R2", fontSize = 12.sp, color = PR.Muted)
                Text(
                    "Packet ${roundIndex + 1} of ${level.rounds.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("❤".repeat(lives) + "♡".repeat(3 - lives), fontSize = 16.sp, color = PR.Red)
        }

        // ---------- Pip with its layers ----------
        PacketCard(round = round, ttl = ttl)

        // ---------- Forwarding table board ----------
        ForwardingBoard(
            table = level.table,
            selectedRow = selectedRow,
            rowResults = rowResults,
            enabled = step == Step.CHECK_ROWS,
            onRowClick = { index ->
                selectedRow = index
                if (rowResults[index] == null) {
                    nana = Hint("Put the stencil on: which octet does the mask cut? Compute D AND mask for it.")
                }
            },
        )

        // ---------- Step panels ----------
        when (step) {
            Step.CHECK_ROWS -> {
                val sel = selectedRow
                if (sel == null) {
                    Text(
                        "Destination: ${round.dest}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val row = level.table[sel]
                    val result = rowResults[sel]
                    if (result == null) {
                        MaskStencil(
                            dest = round.dest,
                            route = row,
                            key = "$roundIndex-$sel",
                            onAnswer = { chosen, correct, explanation ->
                                if (chosen == correct) {
                                    val match = row.matches(round.dest)
                                    rowResults[sel] = match
                                    nana = Hint(
                                        explanation + "\n" + if (match) {
                                            "${round.dest and row.mask} == ${row.network} ✓ Match! Exit: ${row.nextHop}"
                                        } else {
                                            "${round.dest and row.mask} ≠ ${row.network} ✗ Try another row."
                                        },
                                        mono = true,
                                    )
                                } else {
                                    loseLife(
                                        Mistake(
                                            question = "${round.dest} AND ${row.mask} → octet ${row.mask.firstPartialOctet() + 1}?",
                                            yourAnswer = chosen.toString(),
                                            correctAnswer = correct.toString(),
                                            explanation = explanation,
                                        ),
                                        "Not quite. Compare bit by bit, 1 only where BOTH are 1:\n$explanation",
                                        mono = true,
                                    )
                                }
                            },
                        )
                    } else if (result) {
                        PrimaryButton("Send Pip out through ${row.nextHop} →") {
                            step = Step.SWAP_LAYER
                            nana = Hint(
                                "Before Pip leaves through ${row.nextHop}, one layer must be replaced " +
                                    "for the next link. Which one?"
                            )
                        }
                    }
                }
            }

            Step.SWAP_LAYER -> {
                val exit = Forwarding.lookup(level.table, round.dest)?.nextHop ?: "?"
                LayerChoice(
                    exitHop = exit,
                    onChoose = { choice ->
                        if (choice == LAYER_MAC) {
                            ttl -= 1
                            val isLast = roundIndex == level.rounds.lastIndex
                            if (isLast) {
                                onFinish(
                                    LevelResult(
                                        cleared = true,
                                        stars = level.starsFor(mistakes.size),
                                        mistakes = mistakes.toList(),
                                    )
                                )
                            } else {
                                nana = Hint(
                                    "Swapped! New MAC ${round.newMac}. The IP coat stayed the same and " +
                                        "TTL is now $ttl. Here comes the next packet!"
                                )
                                roundIndex += 1
                                step = Step.CHECK_ROWS
                            }
                        } else {
                            loseLife(
                                Mistake(
                                    question = "At a router, which header is replaced for the next link?",
                                    yourAnswer = choice,
                                    correctAnswer = LAYER_MAC,
                                    explanation = "The router rebuilds the Ethernet frame with new MAC addresses " +
                                        "for each link. IP addresses stay the same end to end (only TTL drops by 1).",
                                ),
                                "If the IP coat changed, Mom would never be found! Only the link layer " +
                                    "(MAC) is local to each hop.",
                            )
                        }
                    },
                )
            }
        }

        NanaSays(nana.text, nana.isError, nana.mono)
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun PacketCard(round: RouterRound, ttl: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PR.Blue50, RoundedCornerShape(20.dp))
            .border(1.5.dp, PR.Blue, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background(PR.Purple50, RoundedCornerShape(16.dp))
                .border(1.dp, PR.Purple, RoundedCornerShape(16.dp))
                .padding(8.dp),
        ) {
            Pip(size = 64.dp)
        }
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LayerLine("🧥 Ethernet", "MAC ${round.oldMac}", PR.Blue800)
            LayerLine("🟣 IP coat", "to ${round.dest}", PR.Purple800)
            LayerLine("⏳ TTL", "$ttl", PR.Amber800)
        }
    }
}

@Composable
private fun LayerLine(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = PR.Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ForwardingBoard(
    table: List<Route>,
    selectedRow: Int?,
    rowResults: Map<Int, Boolean>,
    enabled: Boolean,
    onRowClick: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PR.Board, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("R2 departures · forwarding table", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
        BoardRow("Network", "Mask", "Next hop", "", header = true)
        table.forEachIndexed { i, route ->
            val status = when (rowResults[i]) {
                true -> "✓"
                false -> "✗"
                null -> ""
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (i == selectedRow) Color(0xFF444441) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable(enabled = enabled) { onRowClick(i) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                BoardRow(route.network.toString(), "/${route.prefix}", route.nextHop, status)
            }
        }
    }
}

@Composable
private fun BoardRow(a: String, b: String, c: String, status: String, header: Boolean = false) {
    val color = if (header) PR.Gray else PR.BoardText
    val weight = if (header) FontWeight.Medium else FontWeight.Normal
    Row(Modifier.fillMaxWidth()) {
        Text(a, Modifier.weight(2.2f), fontSize = 12.sp, color = color, fontWeight = weight, fontFamily = FontFamily.Monospace)
        Text(b, Modifier.weight(0.9f), fontSize = 12.sp, color = color, fontWeight = weight, fontFamily = FontFamily.Monospace)
        Text(c, Modifier.weight(1f), fontSize = 12.sp, color = color, fontWeight = weight, fontFamily = FontFamily.Monospace)
        Text(
            status,
            Modifier.weight(0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (status == "✓") Color(0xFF5DCAA5) else PR.Red,
        )
    }
}

/**
 * The mask stencil: shows D, mask, and the result with one octet as "?".
 * The player picks the AND result for the octet the mask cuts.
 */
@Composable
private fun MaskStencil(
    dest: Ipv4,
    route: Route,
    key: String,
    onAnswer: (chosen: Int, correct: Int, explanation: String) -> Unit,
) {
    val mask = route.mask
    val qi = mask.firstPartialOctet()
    val d = dest.octets[qi]
    val m = mask.octets[qi]
    val correct = d and m
    val explanation = "$d = ${d.toBinary8()}\n$m = ${m.toBinary8()}\nAND → ${correct.toBinary8()} = $correct"
    val options = remember(key) {
        listOf(correct, d, d or m, correct xor 0x40, (correct + 1) % 256)
            .distinct()
            .take(3)
            .shuffled()
    }

    Panel {
        Text("Mask stencil · AND 연산", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OctetRow("Dest IP", dest.octets.map { it.toString() }, PR.GrayFill, PR.Ink)
        OctetRow("Mask", mask.octets.map { it.toString() }, PR.Purple50, PR.Purple800)
        val result = (dest and mask).octets.mapIndexed { i, v -> if (i == qi) "?" else v.toString() }
        OctetRow("Network", result, PR.Teal50, PR.Teal800)
        Text("Octet ${qi + 1}: $d AND $m = ?", fontSize = 13.sp, color = PR.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Text(
                    option.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, PR.Line, RoundedCornerShape(10.dp))
                        .clickable { onAnswer(option, correct, explanation) }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun OctetRow(label: String, values: List<String>, fill: Color, ink: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, Modifier.weight(1.3f), fontSize = 11.sp, color = PR.Muted)
        values.forEach { v ->
            val unknown = v == "?"
            Text(
                v,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (unknown) PR.Amber800 else ink,
                modifier = Modifier
                    .weight(1f)
                    .background(if (unknown) PR.Amber50 else fill, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp),
            )
        }
    }
}

private const val LAYER_MAC = "🧥 Ethernet raincoat (MAC)"
private const val LAYER_IP = "🟣 IP coat (IP address)"
private const val LAYER_PORT = "🎒 Port backpack (port)"

@Composable
private fun LayerChoice(exitHop: String, onChoose: (String) -> Unit) {
    val options = remember { listOf(LAYER_MAC, LAYER_IP, LAYER_PORT).shuffled() }
    Panel(color = PR.Purple50) {
        Text("Leaving through $exitHop", fontSize = 13.sp, color = PR.Muted)
        Text("Which layer does the router change?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        options.forEach { option ->
            Text(
                option,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PR.White, RoundedCornerShape(12.dp))
                    .border(1.dp, PR.Line, RoundedCornerShape(12.dp))
                    .clickable { onChoose(option) }
                    .padding(14.dp),
            )
        }
    }
}
