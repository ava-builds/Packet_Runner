package com.avabuilds.packetrunner.ui.work

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.LunchChat
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import com.avabuilds.packetrunner.ui.TownColors
import kotlin.math.sin

private val Wall = Color(0xFFF3E3C8)
private val Floor = Color(0xFFD9B98A)
private val TableTop = Color(0xFFB5693E)
private val Bowl = Color(0xFFFFF7EA)

/**
 * A cozy lunch at the cafe: the engineer and the cafe owner sit at a table, steam rises from the bowls, and the owner
 * chats; tap to hear the next line. At the end the lunch is over and the friendship grows (the caller does that).
 */
@Composable
fun LunchScene(partner: Neighbor, alreadyHadLunch: Boolean, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val lines = remember(partner.id) { LunchChat.linesFor(partner) }
    var line by remember { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "lunch")
    val t by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart), label = "t")

    Box(modifier.fillMaxSize().background(Wall).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍜 Lunch at the Cafe", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            Canvas(Modifier.fillMaxWidth().height(300.dp)) { drawLunchTable(t) }
            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                Text(partner.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette5.PersianBlue)
                Text("“${lines[line.coerceIn(0, lines.lastIndex)]}”", fontSize = 15.sp, lineHeight = 21.sp, color = PR.Ink)
            }
            if (line < lines.lastIndex) {
                PrimaryButton("Keep chatting") { line++ }
            } else {
                PrimaryButton(if (alreadyHadLunch) "Done" else "Finish lunch · ${partner.name} likes you more") { onFinish() }
            }
            SecondaryButton("Back to work", onClick = onFinish)
        }
    }
}

private fun DrawScope.drawLunchTable(t: Float) {
    val w = size.width
    val h = size.height
    // back wall with a window and the floor
    drawRect(Floor, Offset(0f, h * 0.55f), Size(w, h * 0.45f))
    drawRoundRect(TownColors.Window, Offset(w * 0.38f, h * 0.06f), Size(w * 0.24f, h * 0.3f), CornerRadius(12f))
    drawRoundRect(Palette5.DuskBlue, Offset(w * 0.38f, h * 0.06f), Size(w * 0.24f, h * 0.3f), CornerRadius(12f), style = Stroke(6f))
    // the table
    drawRoundRect(TableTop, Offset(w * 0.12f, h * 0.58f), Size(w * 0.76f, h * 0.1f), CornerRadius(14f))
    drawRect(Color(0xFF8A4E2A), Offset(w * 0.2f, h * 0.68f), Size(w * 0.04f, h * 0.28f))
    drawRect(Color(0xFF8A4E2A), Offset(w * 0.76f, h * 0.68f), Size(w * 0.04f, h * 0.28f))

    // the engineer (left) and the cafe owner (right), each with a bowl and steam
    person(Offset(w * 0.28f, h * 0.55f), h, Palette5.PersianBlue, hat = true, t = t, eating = true)
    person(Offset(w * 0.72f, h * 0.55f), h, Color(0xFF8E6CEF), hat = false, t = t + 1.3f, eating = false)
    bowl(Offset(w * 0.34f, h * 0.585f), h, t)
    bowl(Offset(w * 0.66f, h * 0.585f), h, t + 0.7f)
}

private fun DrawScope.person(feet: Offset, h: Float, shirt: Color, hat: Boolean, t: Float, eating: Boolean) {
    val s = h / 300f
    val bob = sin(t * 2f) * 2f * s
    drawRoundRect(shirt, Offset(feet.x - 28 * s, feet.y - 95 * s + bob), Size(56 * s, 70 * s), CornerRadius(20 * s))
    drawCircle(TownColors.Skin, 26 * s, Offset(feet.x, feet.y - 122 * s + bob))
    if (hat) drawRoundRect(PR.Amber, Offset(feet.x - 30 * s, feet.y - 160 * s + bob), Size(60 * s, 26 * s), CornerRadius(13 * s))
    else drawArc(Color(0xFF3B2A20), 180f, 180f, true, Offset(feet.x - 26 * s, feet.y - 148 * s + bob), Size(52 * s, 52 * s))
    drawCircle(Palette5.DeepNavy, 3 * s, Offset(feet.x - 9 * s, feet.y - 122 * s + bob))
    drawCircle(Palette5.DeepNavy, 3 * s, Offset(feet.x + 9 * s, feet.y - 122 * s + bob))
    drawArc(Palette5.DeepNavy, 20f, 140f, false, Offset(feet.x - 10 * s, feet.y - 118 * s + bob), Size(20 * s, 12 * s), style = Stroke(2.5f * s, cap = StrokeCap.Round))
    // the spoon goes up and down while eating
    val spoon = if (eating) (sin(t * 3f) + 1f) / 2f else 0.2f
    val hand = Offset(feet.x + (if (feet.x < size.width / 2) 34 else -34) * s, feet.y - (60 + 50 * spoon) * s)
    drawLine(shirt, Offset(feet.x + (if (feet.x < size.width / 2) 22 else -22) * s, feet.y - 80 * s + bob), hand, strokeWidth = 12 * s, cap = StrokeCap.Round)
}

private fun DrawScope.bowl(at: Offset, h: Float, t: Float) {
    val s = h / 300f
    drawArc(Bowl, 0f, 180f, true, Offset(at.x - 28 * s, at.y - 24 * s), Size(56 * s, 40 * s))
    drawArc(Color(0xFFE0C9A6), 0f, 180f, false, Offset(at.x - 28 * s, at.y - 24 * s), Size(56 * s, 40 * s), style = Stroke(3 * s))
    for (k in 0 until 3) {
        val p = ((t * 0.5f) + k / 3f) % 1f
        val x = at.x + sin(p * 6f + k) * 8 * s + (k - 1) * 12 * s
        drawCircle(Color.White.copy(alpha = (1f - p) * 0.7f), (5f + 6f * p) * s, Offset(x, at.y - 30 * s - p * 55 * s))
    }
}

/** The card at the bottom of the town during the lunch break. */
@Composable
fun LunchCard(
    atCafe: Boolean,
    alreadyHadLunch: Boolean,
    partnerName: String,
    onWalk: () -> Unit,
    onEat: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, color = PR.White, borderColor = PR.Amber) {
        Text("🍜 It is your lunch break", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            if (atCafe) "You are at the Cafe. $partnerName has saved you a seat." else "No calls during lunch. Have lunch at the Cafe and chat with $partnerName.",
            fontSize = 13.sp, color = PR.Muted,
        )
        if (atCafe) PrimaryButton(if (alreadyHadLunch) "Sit down again" else "Have lunch", onClick = onEat)
        else PrimaryButton("Walk to the Cafe", onClick = onWalk)
        SecondaryButton("Not now", onClick = onSkip)
    }
}
