package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

/** World 5 · Mom's phone. Numbers and apps for the TCP / UDP puzzle. */
object TransportLevel {
    const val WORLD = 5
    const val CLIENT_ISN = 1000L
    const val SERVER_ISN = 5000L

    const val FIRST_SEQ = 1001L
    const val SEGMENT_LEN = 500
    const val SEGMENT_COUNT = 5

    /** The segment the glitch eats on the way (0-based). */
    const val LOST_INDEX = 1

    const val DEST_PORT = 5004

    val segments = (0 until SEGMENT_COUNT).map { TcpSegment(FIRST_SEQ + it * SEGMENT_LEN, SEGMENT_LEN) }

    data class PhoneApp(val emoji: String, val name: String, val port: Int)

    val apps = listOf(
        PhoneApp("🌐", "Browser", 80),
        PhoneApp("💬", "Chat", 5222),
        PhoneApp("📹", "Video call", DEST_PORT),
    )

    val briefing = Briefing(
        chapter = "World 5 · Mom's phone",
        title = "Ring ring!",
        nanaSays = "Pip, you finally reached Mom's phone! The last step is making sure nothing " +
            "gets lost, and picking the right app on the phone.",
        why = "The transport layer (트랜스포트 계층) delivers data to the right application using " +
            "port numbers. TCP adds a handshake, sequence numbers and ACKs for reliability; " +
            "UDP just sends.",
        howToThink = "TCP: SYN(seq=x) → SYN+ACK(seq=y, ack=x+1) → ACK(ack=y+1). " +
            "An ACK number is the NEXT byte expected: seq + length. ACKs are cumulative: " +
            "they stop at the first missing byte.",
        keywords = listOf(
            "3-way handshake · 3-웨이 핸드셰이크",
            "Sequence / ACK number",
            "Port · 포트",
            "TCP vs UDP",
        ),
    )

    const val WHAT_REALLY_HAPPENED =
        "TCP opens with a 3-way handshake, and each ACK number is the next byte expected (a SYN uses " +
            "up one sequence number). ACKs are cumulative, so a lost segment stalls the ACK number " +
            "until it is resent. UDP skips all of this for speed. Port numbers pick the application " +
            "on the phone."
}

private const val STEPS = 4

/**
 * World 5 gameplay, in four steps:
 * 1. Dial the ACK number of Mom's SYN+ACK.
 * 2. Dial the ACK number of Pip's final handshake ACK.
 * 3. Send data through the glitchy link, then resend the segment that got lost.
 * 4. Deliver a UDP packet to the app listening on its port.
 */
@Composable
fun TransportScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = TransportLevel
    val run = rememberLevelRun(onFinish)
    var step by remember { mutableIntStateOf(0) }
    var done by remember(step) { mutableStateOf(false) }
    var dial by remember(step) {
        mutableLongStateOf(if (step == 0) level.CLIENT_ISN else level.SERVER_ISN)
    }
    var sent by remember { mutableIntStateOf(0) }
    val arrived = remember { mutableStateListOf<TcpSegment>() }
    var say by remember { mutableStateOf(Say("Pip sent a SYN with seq = ${level.CLIENT_ISN}. Answer it.")) }

    val ack = Tcp.cumulativeAck(level.FIRST_SEQ, arrived.toList())

    val goal = when (step) {
        0 -> "Mom answers Pip's SYN (seq = ${level.CLIENT_ISN}). Dial the ACK number of her SYN+ACK."
        1 -> "Mom's SYN+ACK has seq = ${level.SERVER_ISN}. Dial the ACK number of Pip's final ACK."
        2 -> if (sent < level.SEGMENT_COUNT) "Send the data: five segments of ${level.SEGMENT_LEN} bytes."
        else "Mom keeps asking for byte $ack. Tap the segment Pip must resend."
        else -> "A UDP video packet arrives for port ${level.DEST_PORT}. Tap the app that gets it."
    }

    fun wrong(question: String, yours: String, correct: String, why: String) {
        run.fail(Mistake(question, yours, correct, why))
        say = Say(why, isError = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LevelHeader("World 5 · Mom's phone", "Step ${step + 1} of $STEPS", run.lives, onQuit)

        Panel(color = PR.Purple50) {
            Text("Goal", fontSize = 12.sp, color = PR.Muted)
            Text(goal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        when (step) {
            0, 1 -> {
                val expected = if (step == 0) Tcp.synAckAckNumber(level.CLIENT_ISN) else Tcp.finalAckNumber(level.SERVER_ISN)
                val label = if (step == 0) "SYN+ACK from Mom" else "ACK from Pip"
                val seq = if (step == 0) level.SERVER_ISN else level.CLIENT_ISN + 1
                Panel(color = PR.Board) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
                    Text(
                        "seq = $seq\nack = $dial",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PR.BoardText,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DialButton("− 1", Modifier.weight(1f)) { if (!done) dial -= 1 }
                    DialButton("+ 1", Modifier.weight(1f)) { if (!done) dial += 1 }
                }
                if (!done) PrimaryButton("Send ${if (step == 0) "SYN+ACK" else "ACK"}") {
                    if (dial == expected) {
                        done = true
                        say = Say(
                            if (step == 0) "A SYN uses up one sequence number, so Mom asks for the next one: " +
                                "${level.CLIENT_ISN} + 1 = $expected."
                            else "Mom's SYN used up one number too: ${level.SERVER_ISN} + 1 = $expected. " +
                                "The connection is open!",
                        )
                    } else {
                        wrong(
                            if (step == 0) "ACK number of the SYN+ACK (client seq ${level.CLIENT_ISN})"
                            else "ACK number of the final ACK (server seq ${level.SERVER_ISN})",
                            dial.toString(),
                            expected.toString(),
                            "The ACK number is the next byte expected. A SYN counts as one byte, " +
                                "so it is the other side's seq + 1.",
                        )
                    }
                }
            }

            2 -> {
                Panel(color = PR.Board) {
                    Text("Pip → Mom", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
                    level.segments.forEachIndexed { i, seg ->
                        val status = when {
                            i >= sent -> "waiting"
                            seg in arrived -> "arrived ✓"
                            else -> "LOST 👾"
                        }
                        Text(
                            "seq ${seg.seq}  ${seg.len} B   $status",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (status.startsWith("LOST")) PR.Red else PR.BoardText,
                        )
                    }
                }
                Text(
                    "Mom's ACK number: $ack",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (done) PR.Green50 else PR.Amber50, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )

                if (sent < level.SEGMENT_COUNT) {
                    PrimaryButton("Send segment ${sent + 1}") {
                        val seg = level.segments[sent]
                        if (sent == level.LOST_INDEX) {
                            say = Say("👾 The glitch ate seq ${seg.seq}! Pip doesn't know yet…", isError = true)
                        } else {
                            arrived.add(seg)
                            say = Say("Mom got seq ${seg.seq}. Her ACK is now ${Tcp.cumulativeAck(level.FIRST_SEQ, arrived.toList())}.")
                        }
                        sent += 1
                    }
                } else if (!done) {
                    Text("Resend which segment?", fontSize = 13.sp, color = PR.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        level.segments.forEach { seg ->
                            Text(
                                "seq ${seg.seq}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PR.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, PR.Line, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (seg.seq == ack) {
                                            arrived.add(seg)
                                            done = true
                                            say = Say("Resent seq ${seg.seq}! Mom now has every byte, so her ACK jumps " +
                                                "to ${Tcp.cumulativeAck(level.FIRST_SEQ, arrived.toList())}.")
                                        } else {
                                            wrong(
                                                "Which segment fills the gap Mom is asking about?",
                                                "seq ${seg.seq}",
                                                "seq $ack",
                                                "ACKs are cumulative: Mom keeps asking for byte $ack, the first byte " +
                                                    "she is missing. That's the segment to resend.",
                                            )
                                        }
                                    }
                                    .padding(vertical = 14.dp),
                            )
                        }
                    }
                }
            }

            else -> {
                Panel(color = PR.Board) {
                    Text("Incoming packet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
                    Text(
                        "UDP · to ${"45.64.20.77"}:${level.DEST_PORT}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PR.BoardText,
                    )
                }
                Text("Apps on Mom's phone", fontSize = 13.sp, color = PR.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    level.apps.forEach { app ->
                        Column(
                            Modifier
                                .weight(1f)
                                .background(if (done && app.port == level.DEST_PORT) PR.Green50 else PR.White, RoundedCornerShape(14.dp))
                                .border(1.dp, PR.Line, RoundedCornerShape(14.dp))
                                .clickable(enabled = !done) {
                                    if (app.port == level.DEST_PORT) {
                                        done = true
                                        say = Say("Delivered! IP got the packet to the phone; the port number " +
                                            "${level.DEST_PORT} got it to the ${app.name}.")
                                    } else {
                                        wrong(
                                            "Which app listens on port ${level.DEST_PORT}?",
                                            "${app.name} (port ${app.port})",
                                            "Video call (port ${level.DEST_PORT})",
                                            "The packet's destination port is ${level.DEST_PORT}, but the ${app.name} " +
                                                "listens on ${app.port}.",
                                        )
                                    }
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(app.emoji, fontSize = 28.sp)
                            Text(app.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("port ${app.port}", fontSize = 10.sp, color = PR.Muted)
                        }
                    }
                }
            }
        }

        if (done) {
            val last = step == STEPS - 1
            PrimaryButton(if (last) "Finish mission" else "Next step →") {
                if (last) {
                    run.clear()
                } else {
                    step += 1
                    say = Say(
                        when (step) {
                            1 -> "Almost connected! Now Pip answers Mom's SYN."
                            2 -> "The connection is open. Time to send data, but the link is glitchy!"
                            else -> "Last delivery. Mom's phone runs several apps, so who gets this packet?"
                        },
                    )
                }
            }
        }

        NanaSays(say.text, say.isError)
    }
}

@Composable
private fun DialButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(PR.White, RoundedCornerShape(14.dp))
            .border(1.dp, PR.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
