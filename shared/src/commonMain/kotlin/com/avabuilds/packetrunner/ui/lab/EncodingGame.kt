package com.avabuilds.packetrunner.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.EncodingRounds
import com.avabuilds.packetrunner.network.LineCodes
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PrimaryButton

/**
 * Three rounds: NRZ, NRZI and Manchester. The player taps each half of every bit to make the signal high or low,
 * then checks. Wrong half-bits are marked in red.
 */
@Composable
fun EncodingGame(onSuccess: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val spec = EncodingRounds.all[round]
    val drawn = remember(round) { mutableStateListOf(*Array(spec.bits.length * 2) { false }) }
    var wrong by remember(round) { mutableStateOf<List<Int>>(emptyList()) }
    var note by remember(round) { mutableStateOf<String?>(null) }
    val measurer = rememberTextMeasurer()

    Text("Round ${round + 1} of ${EncodingRounds.all.size}: ${spec.code.label}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
    Text(spec.code.rule, fontSize = 13.sp, color = PR.Ink)
    Text("Tap a half of a bit to switch it between high and low.", fontSize = 12.sp, color = PR.Muted)

    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Canvas(
            Modifier.fillMaxWidth().height(150.dp)
                .pointerInput(round) {
                    detectTapGestures { tap ->
                        val cell = (tap.x / (size.width / drawn.size)).toInt().coerceIn(0, drawn.size - 1)
                        drawn[cell] = !drawn[cell]
                        wrong = emptyList()
                        note = null
                    }
                },
        ) {
            val cells = drawn.size
            val cw = size.width / cells
            val top = 44.dp.toPx()
            val bottom = size.height - 14.dp.toPx()
            fun y(high: Boolean) = if (high) top else bottom

            for (i in 0 until cells) {
                if (i in wrong) drawRect(PR.Red50, Offset(i * cw, 26.dp.toPx()), androidx.compose.ui.geometry.Size(cw, size.height - 26.dp.toPx()))
            }
            // bit labels and the lines between bits
            spec.bits.forEachIndexed { b, c ->
                val label = measurer.measure(c.toString(), TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = PR.Ink))
                drawText(label, topLeft = Offset((2 * b + 1) * cw - label.size.width / 2f, 0f))
                drawLine(PR.Line, Offset(2 * b * cw, 26.dp.toPx()), Offset(2 * b * cw, size.height), strokeWidth = 1.dp.toPx())
            }
            // the signal
            for (i in 0 until cells) {
                val yy = y(drawn[i])
                drawLine(Palette5.PersianBlue, Offset(i * cw, yy), Offset((i + 1) * cw, yy), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                if (i > 0 && drawn[i] != drawn[i - 1]) drawLine(Palette5.PersianBlue, Offset(i * cw, y(drawn[i - 1])), Offset(i * cw, yy), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
    note?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
    PrimaryButton(if (round == EncodingRounds.all.lastIndex) "Check the last wave" else "Check my wave") {
        val mistakes = LineCodes.mistakes(spec.code, spec.bits, drawn.toList())
        wrong = mistakes
        if (mistakes.isEmpty()) {
            if (round == EncodingRounds.all.lastIndex) onSuccess() else round++
        } else {
            val bit = mistakes.first() / 2 + 1
            note = "${mistakes.size} half-bit${if (mistakes.size == 1) " is" else "s are"} wrong, the first one in bit $bit. Read the rule again."
        }
    }
}
