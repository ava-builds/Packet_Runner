package com.avabuilds.packetrunner.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.Crc
import com.avabuilds.packetrunner.network.CrcInbox
import com.avabuilds.packetrunner.network.CrcTrainer
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

private val Mono = FontFamily.Monospace

/** Part 1: the player does the long division. Part 2: the inbox, where the receiver checks frames. */
@Composable
fun CrcGame(onSuccess: () -> Unit) {
    var inbox by remember { mutableStateOf(false) }
    if (!inbox) DivisionPart(onDone = { inbox = true }) else InboxPart(onSuccess)
}

@Composable
private fun DivisionPart(onDone: () -> Unit) {
    val trainer = remember { CrcTrainer("10110010") }
    var tick by remember { mutableIntStateOf(0) } // changes after every step, so the screen redraws
    var note by remember { mutableStateOf("The sender adds a CRC to the message. Divide it by ${Crc.GENERATOR}, one step at a time.") }
    @Suppress("UNUSED_EXPRESSION") tick

    Text(
        "Message ${trainer.message}, generator ${Crc.GENERATOR}. We add ${Crc.GENERATOR.length - 1} zeros and divide. " +
            "If the bit under the arrow is 1, XOR the generator in. If it is 0, skip.",
        fontSize = 13.sp, color = PR.Ink,
    )
    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        val reg = trainer.register
        Text(
            buildAnnotatedString {
                reg.forEachIndexed { i, c ->
                    val inWindow = !trainer.done && i in trainer.step until trainer.step + Crc.GENERATOR.length
                    withStyle(SpanStyle(background = if (inWindow) PR.Amber50 else Color.Transparent, fontWeight = if (inWindow) FontWeight.Bold else FontWeight.Normal)) { append(c) }
                }
            },
            fontFamily = Mono, fontSize = 26.sp, color = PR.Ink,
        )
        Text(if (trainer.done) " ".repeat(trainer.step) else " ".repeat(trainer.step) + "↑", fontFamily = Mono, fontSize = 26.sp, color = PR.Red)
        Text(
            if (trainer.done) "Done. The remainder is ${trainer.remainder}." else "Step ${trainer.step + 1} of ${trainer.message.length}: the bit under the arrow is ${reg[trainer.step]}.",
            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PR.Ink,
        )
    }
    Text(note, fontSize = 13.sp, color = PR.Muted)
    if (!trainer.done) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("XOR with ${Crc.GENERATOR}", Modifier.weight(1f)) {
                note = if (trainer.apply(true)) "Right: a 1 means XOR." else "Not quite. The bit under the arrow is 0, so there is nothing to subtract: skip."
                tick++
            }
            SecondaryButton("Skip", Modifier.weight(1f)) {
                note = if (trainer.apply(false)) "Right: a 0 means skip." else "Not quite. The bit under the arrow is 1, so XOR the generator in."
                tick++
            }
        }
    } else {
        Panel(color = PR.White, borderColor = PR.Green600) {
            Text("The frame to send is ${trainer.message} + ${trainer.remainder}:", fontSize = 13.sp)
            Text(trainer.message + trainer.remainder, fontFamily = Mono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Green800)
        }
        PrimaryButton("Open the inbox", onClick = onDone)
    }
}

@Composable
private fun InboxPart(onSuccess: () -> Unit) {
    val frames = remember { CrcInbox.frames() }
    val checked = remember { mutableStateOf(setOf<Int>()) }
    val resent = remember { mutableStateOf(setOf<Int>()) }

    Text(
        "Five frames arrived. Check each one: divide the whole frame by ${Crc.GENERATOR}. A remainder of 000 means no error was found. " +
            "Ask for a resend of every damaged frame.",
        fontSize = 13.sp, color = PR.Ink,
    )
    frames.forEachIndexed { i, f ->
        val isChecked = i in checked.value
        val remainder = Crc.remainder(f.frame)
        val bad = isChecked && !Crc.isIntact(f.frame)
        Panel(color = PR.White, borderColor = when { bad -> PR.Red; isChecked -> PR.Green600; else -> PR.Line }) {
            Text(
                buildAnnotatedString {
                    f.frame.forEachIndexed { k, c -> withStyle(SpanStyle(color = if (k >= f.frame.length - 3) Palette5.PersianBlue else PR.Ink)) { append(c) } }
                },
                fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            if (!isChecked) {
                SecondaryButton("Check frame ${i + 1}") { checked.value = checked.value + i }
            } else if (bad) {
                Text("✗ Remainder $remainder, not 000: the frame was damaged.", fontSize = 13.sp, color = PR.Red800, fontWeight = FontWeight.Medium)
                if (i in resent.value) Text("Resend requested ✓", fontSize = 12.sp, color = PR.Green800)
                else SecondaryButton("Ask for a resend") { resent.value = resent.value + i }
            } else {
                Text("✓ Remainder 000: no error found.", fontSize = 13.sp, color = PR.Green800, fontWeight = FontWeight.Medium)
            }
        }
    }
    val finished = frames.indices.all { it in checked.value } && frames.indices.filter { frames[it].damaged }.all { it in resent.value }
    if (finished) PrimaryButton("Finish", onClick = onSuccess)
    else Text("${checked.value.size} of ${frames.size} checked.", fontSize = 12.sp, color = PR.Muted, modifier = Modifier.padding(top = 4.dp))
}
