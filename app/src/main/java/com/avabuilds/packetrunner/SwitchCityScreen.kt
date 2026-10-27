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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Arrival(val action: BridgeAction, val table: Map<String, Int>)

private fun describe(ports: Set<Int>): String =
    if (ports.isEmpty()) "Drop the frame" else "Ports ${ports.sorted().joinToString(", ")}"

/**
 * World 2 gameplay: you run the switch. Each frame arrives on a port, the switch learns its
 * source, and you tap the ports the frame should leave through. Flooding really delivers
 * copies to every host behind the chosen ports, so you can see wasted traffic.
 */
@Composable
fun SwitchCityScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = SwitchCityLevel
    val bridge = remember { LearningBridge(level.PORT_COUNT) }
    var index by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    val mistakes = remember { mutableStateListOf<Mistake>() }
    var say by remember { mutableStateOf(Say("A frame is arriving. Look at the table, then tap the ports it should go out of.")) }

    val frame = level.frames[index]
    val inPort = level.portOf(frame.src)
    // The switch learns the source the moment the frame arrives, before anyone decides.
    val arrival = remember(index) {
        val action = bridge.receive(frame.src, frame.dst, inPort)
        Arrival(action, bridge.learned.toMap())
    }
    val selected = remember(index) { mutableStateListOf<Int>() }
    var solved by remember(index) { mutableStateOf(false) }

    fun send() {
        val expected = arrival.action.outPorts()
        val chosen = selected.toSet()
        if (chosen == expected) {
            solved = true
            say = Say(
                when (arrival.action) {
                    is BridgeAction.Flood -> "Flooded! ${frame.dst} isn't in the table yet, so every other port gets a copy. " +
                        "${frame.dst} will answer, and then the switch will know where it lives."
                    BridgeAction.Filter -> "Filtered! ${frame.dst} is on the same port as ${frame.src}, so " +
                        "they already heard each other. Nothing needs to leave."
                    is BridgeAction.Forward -> "Forwarded! The table says ${frame.dst} is on port " +
                        "${arrival.table.getValue(frame.dst)}, so only that port gets it."
                },
            )
        } else {
            val reason = when {
                inPort in chosen -> "Never send a frame back out of the port it came in on."
                expected.isEmpty() -> "${frame.dst} is on the same port as ${frame.src}. Nobody else needs this frame."
                expected.size == 1 -> "The table already says ${frame.dst} is on port ${expected.first()}. " +
                    "Flooding wastes bandwidth."
                else -> "${frame.dst} isn't in the table yet, so nobody knows where it is. " +
                    "Send it out of every other port."
            }
            mistakes.add(
                Mistake(
                    question = "Frame ${frame.src} → ${frame.dst} arrives on port $inPort. " +
                        "Table: ${arrival.table.entries.joinToString("  ") { "${it.key}→${it.value}" }}",
                    yourAnswer = describe(chosen),
                    correctAnswer = describe(expected),
                    explanation = reason,
                ),
            )
            lives -= 1
            say = Say(reason, isError = true)
            if (lives <= 0) onFinish(LevelResult(cleared = false, stars = 0, mistakes = mistakes.toList()))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onQuit) { Text("✕", fontSize = 18.sp, color = PR.Muted) }
            Column(Modifier.weight(1f)) {
                Text("World 2 · Switch city", fontSize = 12.sp, color = PR.Muted)
                Text(
                    "Frame ${index + 1} of ${level.frames.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("❤".repeat(lives) + "♡".repeat(3 - lives), fontSize = 16.sp, color = PR.Red)
        }

        FrameCard(frame, inPort)

        Column(
            Modifier
                .fillMaxWidth()
                .background(PR.Board, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Switch · MAC table", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
            arrival.table.forEach { (host, port) ->
                val fresh = host == frame.src
                Text(
                    "$host → port $port" + if (fresh) "   ← just learned" else "",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (fresh) PR.Amber else PR.BoardText,
                )
            }
            if (!arrival.table.containsKey(frame.dst)) {
                Text(
                    "${frame.dst} → ?   (unknown)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PR.Gray,
                )
            }
        }

        Text(
            if (solved) "Frame delivered" else "Tap the ports to send the frame out of",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..level.PORT_COUNT).forEach { port ->
                PortTile(
                    port = port,
                    hosts = level.hosts.getValue(port),
                    src = frame.src,
                    dst = frame.dst,
                    isIn = port == inPort,
                    selected = port in selected,
                    gotCopy = solved && port in selected,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!solved) {
                            if (port in selected) selected.remove(port) else selected.add(port)
                        }
                    },
                )
            }
        }

        if (solved) {
            val last = index == level.frames.lastIndex
            PrimaryButton(if (last) "Finish mission" else "Next frame →") {
                if (last) {
                    onFinish(
                        LevelResult(
                            cleared = true,
                            stars = RouterStationLevel.starsFor(mistakes.size),
                            mistakes = mistakes.toList(),
                        ),
                    )
                } else {
                    say = Say("Another frame is arriving. Check the table!")
                    index += 1
                }
            }
        } else {
            PrimaryButton(
                if (selected.isEmpty()) "Drop the frame" else "Send out of ${describe(selected.toSet()).lowercase()}",
            ) { send() }
        }

        NanaSays(say.text, say.isError)
    }
}

@Composable
private fun FrameCard(frame: SwitchFrame, inPort: Int) {
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
            Pip(size = 56.dp)
        }
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Arrived on port $inPort", fontSize = 11.sp, color = PR.Muted)
            Text(
                "from ${frame.src}  →  to ${frame.dst}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PR.Blue800,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun PortTile(
    port: Int,
    hosts: List<String>,
    src: String,
    dst: String,
    isIn: Boolean,
    selected: Boolean,
    gotCopy: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fill = when {
        gotCopy -> PR.Green50
        selected -> PR.Amber50
        else -> PR.White
    }
    val ring = when {
        gotCopy -> PR.Green600
        selected -> PR.Amber
        else -> PR.Line
    }
    Column(
        modifier
            .background(fill, RoundedCornerShape(14.dp))
            .border(if (selected) 2.5.dp else 1.dp, ring, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Port $port", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(if (isIn) "◀ IN" else "OUT ▶", fontSize = 10.sp, color = if (isIn) PR.Blue else PR.Muted)
        hosts.forEach { HostChip(it, src, dst) }
        Text(
            if (gotCopy) "copy sent" else if (selected) "selected" else " ",
            fontSize = 10.sp,
            color = if (gotCopy) PR.Green800 else PR.Amber800,
        )
    }
}

@Composable
private fun HostChip(host: String, src: String, dst: String) {
    val (bg, fg, tag) = when (host) {
        src -> Triple(PR.Blue50, PR.Blue800, "sender")
        dst -> Triple(PR.Teal50, PR.Teal800, "dest")
        else -> Triple(PR.GrayFill, PR.Ink, "")
    }
    Text(
        host + if (tag.isNotEmpty()) " · $tag" else "",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
