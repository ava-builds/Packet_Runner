package com.avabuilds.packetrunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** World 1 · Wi-Fi fog. Content and settings for the hands-on radio puzzle. */
object WifiFogLevel {
    const val WORLD = 1
    const val RANGE = 100
    const val TRACK = 260

    val a = WifiNode("A", 0)
    val b = WifiNode("B", 80)

    /** Where C stands in the RTS/CTS stage: hidden from A, but in range of B. */
    const val HIDDEN_C = 160

    const val CTS_HEARD_AT_US = 200
    const val CTS_DURATION_US = 500

    val briefing = Briefing(
        chapter = "World 1 · Wi-Fi fog",
        title = "Cross the fog",
        nanaSays = "Pip, the Wi-Fi fog is thick. Radios can't always hear each other, " +
            "so two senders may shout at the same time without knowing! Move the radios and see.",
        why = "Wireless LANs can't detect collisions like Ethernet: a radio can't listen while " +
            "it talks. So Wi-Fi tries to avoid them (CSMA/CA, 충돌 회피) and uses RTS/CTS to " +
            "fix the hidden terminal problem (숨은 단말 문제).",
        howToThink = "Each radio reaches 100 m. If two senders are both inside the receiver's reach " +
            "but outside each other's, they are hidden. RTS/CTS: ask (RTS), get permission (CTS), " +
            "send DATA, get ACK. Anyone who hears the CTS waits.",
        keywords = listOf(
            "CSMA/CA · 충돌 회피",
            "Hidden terminal · 숨은 단말",
            "RTS / CTS",
            "NAV · 대기 시간",
        ),
    )

    const val WHAT_REALLY_HAPPENED =
        "Wi-Fi radios can't transmit and listen at once, so collisions can't be detected. A hidden " +
            "terminal can't hear another sender that still reaches the same receiver. RTS/CTS lets " +
            "the receiver announce the transfer to everyone in its range. Anyone who overhears the " +
            "RTS or CTS sets its NAV and stays quiet for the stated duration."
}

private const val STAGES = 4

private fun hiddenAt(cx: Int): Boolean =
    Wifi.isHiddenPair(WifiFogLevel.a, WifiNode("C", cx), WifiFogLevel.b, WifiFogLevel.RANGE)

private fun hearsAt(cx: Int): Boolean =
    Wifi.canHear(WifiFogLevel.a, WifiNode("C", cx), WifiFogLevel.RANGE)

/**
 * World 1 gameplay, in four stages:
 * 1. Drag C so A and C are hidden from each other and collide at B.
 * 2. Drag C so it can hear A and politely waits.
 * 3. Put the RTS, CTS, DATA and ACK frames in order.
 * 4. Set C's NAV timer after overhearing the CTS.
 */
@Composable
fun WifiFogScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = WifiFogLevel
    val run = rememberLevelRun(onFinish)
    var stage by remember { mutableIntStateOf(0) }
    var done by remember(stage) { mutableStateOf(false) }
    var cx by remember { mutableIntStateOf(200) }
    var say by remember {
        mutableStateOf(Say("Drag the amber C along the track and watch whose reach it is inside."))
    }
    val order = remember { mutableStateListOf<String>() }
    var nav by remember { mutableFloatStateOf(0f) }

    val goal = when (stage) {
        0 -> "Make A and C collide at B. Drag C so it reaches B but A can't hear it."
        1 -> "Now be polite. Drag C so it can hear A and waits its turn."
        2 -> "A hidden C is in the fog. Tap the frames in the order that protects B's transfer."
        else -> "C heard B's CTS at ${level.CTS_HEARD_AT_US} µs. It says ${level.CTS_DURATION_US} µs. " +
            "Set the time C may try again."
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
        LevelHeader("World 1 · Wi-Fi fog", "Stage ${stage + 1} of $STAGES", run.lives, onQuit)

        Panel(color = PR.Purple50) {
            Text("Goal", fontSize = 12.sp, color = PR.Muted)
            Text(goal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        val cPos = if (stage >= 2) level.HIDDEN_C else cx
        RadioMap(cPos = cPos, draggable = stage < 2 && !done, onMoveC = { cx = it })

        val hearsText = if (hearsAt(cPos)) "A and C can hear each other" else "A and C cannot hear each other"
        Text(
            hearsText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        when (stage) {
            0 -> if (!done) PrimaryButton("A and C both send!") {
                if (hiddenAt(cx)) {
                    done = true
                    say = Say("💥 Collision at B! A thought the channel was idle because it can't hear C. " +
                        "That's the hidden terminal problem.")
                } else {
                    val spots = (0..level.TRACK).filter { hiddenAt(it) }
                    wrong(
                        "Place C so it collides with A at B",
                        "C at $cx m",
                        "C between ${spots.first()} and ${spots.last()} m",
                        if (hearsAt(cx)) "C is within A's reach, so they hear each other and one waits. No collision."
                        else "C is too far: B can't hear it either, so nothing reaches B.",
                    )
                }
            }

            1 -> if (!done) PrimaryButton("A and C both try") {
                if (hearsAt(cx)) {
                    done = true
                    say = Say("C senses A's signal, so it waits. Carrier sensing (CSMA/CA) works when " +
                        "senders can hear each other.")
                } else {
                    wrong(
                        "Place C so it can hear A",
                        "C at $cx m",
                        "C at ${level.RANGE} m or closer to A",
                        "C is ${cx} m from A and the reach is ${level.RANGE} m, so it can't sense A. " +
                            "They would collide again.",
                    )
                }
            }

            2 -> {
                val shuffled = remember { Wifi.rtsCtsSequence.shuffled() }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Wifi.rtsCtsSequence.indices.forEach { i ->
                        Text(
                            order.getOrNull(i) ?: "?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (i < order.size) PR.Green800 else PR.Muted,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (i < order.size) PR.Green50 else PR.GrayFill, RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shuffled.forEach { frame ->
                        val used = frame in order
                        Text(
                            frame,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (used) PR.Gray else PR.Ink,
                            modifier = Modifier
                                .weight(1f)
                                .background(PR.White, RoundedCornerShape(12.dp))
                                .border(1.dp, PR.Line, RoundedCornerShape(12.dp))
                                .clickable(enabled = !used && !done) {
                                    val next = Wifi.rtsCtsSequence[order.size]
                                    if (frame == next) {
                                        order.add(frame)
                                        say = Say(
                                            when (frame) {
                                                "RTS" -> "A asks B for the channel. Everyone near A hears it."
                                                "CTS" -> "B answers. Now C, hidden from A but near B, hears the CTS and stays quiet."
                                                "DATA" -> "The data frame is sent safely."
                                                else -> "B confirms receipt. The channel is free again."
                                            },
                                        )
                                        if (order.size == Wifi.rtsCtsSequence.size) done = true
                                    } else {
                                        wrong(
                                            "Which frame comes after ${order.lastOrNull() ?: "the start"}?",
                                            frame,
                                            next,
                                            "Order: ${Wifi.rtsCtsSequence.joinToString(" → ")}. " +
                                                "The sender asks first, the receiver grants, then data, then ACK.",
                                        )
                                    }
                                }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            }

            else -> {
                val target = Wifi.navEnd(level.CTS_HEARD_AT_US, level.CTS_DURATION_US)
                val value = (nav / 100f).roundToInt() * 100
                Panel {
                    Text("C may transmit again at t = $value µs", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = nav,
                        onValueChange = { if (!done) nav = it },
                        valueRange = 0f..1000f,
                        steps = 9,
                    )
                }
                if (!done) PrimaryButton("Set C's NAV timer") {
                    if (value == target) {
                        done = true
                        say = Say("NAV = ${level.CTS_HEARD_AT_US} + ${level.CTS_DURATION_US} = $target µs. " +
                            "C stays silent until then, so B's transfer is safe.")
                    } else {
                        wrong(
                            "When can C transmit after hearing the CTS?",
                            "t = $value µs",
                            "t = $target µs",
                            "C heard the CTS at ${level.CTS_HEARD_AT_US} µs and must wait the full " +
                                "${level.CTS_DURATION_US} µs: ${level.CTS_HEARD_AT_US} + ${level.CTS_DURATION_US} = $target µs.",
                        )
                    }
                }
            }
        }

        if (done) {
            val last = stage == STAGES - 1
            PrimaryButton(if (last) "Finish mission" else "Next stage →") {
                if (last) {
                    run.clear()
                } else {
                    stage += 1
                    say = Say(
                        when (stage) {
                            1 -> "Same radios, new question. Where must C stand to hear A?"
                            2 -> "Collisions at B still happen with hidden terminals. Time for RTS/CTS!"
                            else -> "Last one. Hearing the CTS isn't enough: C needs to know how long to wait."
                        },
                    )
                }
            }
        }

        NanaSays(say.text, say.isError)
    }
}

/** Three lanes (A, B, C). Each shows the node and the stretch of track its radio reaches. */
@Composable
private fun RadioMap(cPos: Int, draggable: Boolean, onMoveC: (Int) -> Unit) {
    val level = WifiFogLevel
    val measurer = rememberTextMeasurer()
    val lanes = listOf(
        Triple("A", level.a.x, Palette5.Cornflower),
        Triple("B", level.b.x, Palette5.IcyBlue),
        Triple("C", cPos, PR.Amber),
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .background(PR.Board, RoundedCornerShape(16.dp))
            .pointerInput(draggable) {
                if (draggable) {
                    detectDragGestures { change, _ ->
                        val pad = 24.dp.toPx()
                        val labelW = 48.dp.toPx()
                        val m = ((change.position.x - pad) / (size.width - 2 * pad - labelW) * level.TRACK).roundToInt()
                        onMoveC((m / 5 * 5).coerceIn(0, level.TRACK))
                    }
                }
            },
    ) {
        val pad = 24.dp.toPx()
        val labelW = 48.dp.toPx()
        fun mx(m: Int) = pad + m.coerceIn(0, level.TRACK) / level.TRACK.toFloat() * (size.width - 2 * pad - labelW)
        val laneH = size.height / 3
        lanes.forEachIndexed { i, (name, x, color) ->
            val cy = laneH * i + laneH / 2
            val left = mx(x - level.RANGE)
            val right = mx(x + level.RANGE)
            drawRoundRect(
                color = color.copy(alpha = 0.22f),
                topLeft = Offset(left, cy - 14.dp.toPx()),
                size = Size(right - left, 28.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            drawCircle(color, radius = 13.dp.toPx(), center = Offset(mx(x), cy))
            val label = measurer.measure(
                name,
                TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace),
            )
            drawText(label, topLeft = Offset(mx(x) - label.size.width / 2f, cy - label.size.height / 2f))
            val meters = measurer.measure(
                "$x m",
                TextStyle(fontSize = 10.sp, color = PR.BoardText),
            )
            drawText(meters, topLeft = Offset(size.width - pad / 2 - meters.size.width, cy - meters.size.height / 2f))
        }
    }
}
