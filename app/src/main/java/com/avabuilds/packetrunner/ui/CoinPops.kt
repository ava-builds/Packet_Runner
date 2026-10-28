package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Juice

private class Pop(val amount: Int, val startedAt: Float)

private val CoinGold = Color(0xFFFFC233)
private val CoinEdge = Color(0xFFD9922F)

/**
 * Coins that pop up and float away whenever [money] goes up (not when it goes down, as when buying something),
 * with a "+₩N" next to them. [anchor] is where they start, in pixels from the top-left of this layer.
 * The first value of [money] is just remembered, so loading a game does not shower you in coins.
 */
@Composable
fun CoinPops(money: Int, anchor: (Size) -> Offset, onEarned: (Int) -> Unit, modifier: Modifier = Modifier) {
    val pops = remember { mutableStateListOf<Pop>() }
    var clock by remember { mutableFloatStateOf(0f) }
    var last by remember { mutableIntStateOf(money) }
    val measurer = rememberTextMeasurer()

    // The clock only runs while something is flying, so an idle screen costs nothing
    LaunchedEffect(pops.size) {
        while (pops.isNotEmpty()) {
            withFrameNanos { clock = it / 1_000_000_000f }
            pops.removeAll { clock - it.startedAt > Juice.COIN_SECONDS + 0.6f }
        }
    }
    LaunchedEffect(money) {
        val gained = money - last
        last = money
        if (gained > 0) {
            if (pops.isEmpty()) withFrameNanos { clock = it / 1_000_000_000f }
            pops.add(Pop(gained, clock))
            onEarned(gained)
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val d = density
        val now = clock
        val from = anchor(size)
        pops.forEach { pop ->
            val count = Juice.coinCount(pop.amount)
            for (i in 0 until count) {
                val c = Juice.coin(i, count, now - pop.startedAt) ?: continue
                val at = from + Offset(c.dx * 90f * d, -c.rise * 126f * d)
                drawCircle(CoinEdge.copy(alpha = c.alpha), 11f * d, at)
                drawCircle(CoinGold.copy(alpha = c.alpha), 8.5f * d, at)
                drawCircle(Color.White.copy(alpha = c.alpha * 0.7f), 2.5f * d, at - Offset(2.5f * d, 2.5f * d))
            }
            val t = (now - pop.startedAt) / Juice.COIN_SECONDS
            if (t in 0f..1.3f) {
                val alpha = (1.3f - t).coerceIn(0f, 1f)
                val text = measurer.measure("+₩${pop.amount}", TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PR.Amber800.copy(alpha = alpha)))
                drawText(text, topLeft = from + Offset(-text.size.width / 2f, 16f * d - t * 40f * d))
            }
        }
    }
}
