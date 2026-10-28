package com.avabuilds.packetrunner.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.DayLight
import com.avabuilds.packetrunner.game.DayPeriod
import com.avabuilds.packetrunner.game.GameState
import com.avabuilds.packetrunner.game.Progress
import com.avabuilds.packetrunner.game.formatTime
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.TownColors
import kotlin.math.cos
import kotlin.math.sin

/** The height of the top bar, so panels and buttons can sit just below it. */
val TopBarHeight = 76.dp

/**
 * The bar across the top: the engineer's face, the level with its XP bar, the money, and the day with the time and a
 * sun or moon. Tapping the face opens the profile. [onTimeLongPress] is the debug shortcut (null in a release build).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    state: GameState,
    saveMark: String,
    onAvatar: () -> Unit,
    onTimeLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val progress = Progress.progress(state.xp)
    Row(
        modifier
            .fillMaxWidth()
            .height(TopBarHeight - 12.dp)
            .background(Color.White.copy(alpha = 0.93f), shape)
            .border(1.5.dp, PR.Line, shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EngineerAvatar(Modifier.clickable(onClick = onAvatar), size = 44.dp)

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lv ${progress.level}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                if (saveMark.isNotEmpty()) Text(saveMark, fontSize = 10.sp, color = PR.Muted)
            }
            XpBar(progress.fraction)
            Text("${progress.xpIntoLevel} / ${progress.xpForLevel} XP", fontSize = 10.sp, color = PR.Muted)
        }

        Text(
            "₩ ${state.money}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber800,
            modifier = Modifier.background(PR.Amber50, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 6.dp),
        )

        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PR.Blue50)
                .then(if (onTimeLongPress != null) Modifier.combinedClickable(onClick = {}, onLongClick = onTimeLongPress) else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SunMoonIcon(DayLight.period(state.minute), Modifier.size(24.dp))
            Column {
                Text("Day ${state.day}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PR.Blue800)
                Text(formatTime(state.minute), fontSize = 11.sp, color = PR.Muted)
            }
        }
    }
}

/** The XP bar: a rounded track with a blue fill. */
@Composable
private fun XpBar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(PR.GrayFill)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette5.PersianBlue))
    }
}

/** The engineer's face with the hard hat, on a round light-blue badge. */
@Composable
fun EngineerAvatar(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size).clip(CircleShape).background(Palette5.IcyBlue).border(2.dp, Palette5.PersianBlue, CircleShape)) {
        val s = this.size.width / 100f
        drawCircle(TownColors.Skin, 30 * s, Offset(50 * s, 58 * s))
        drawRoundRect(PR.Amber, Offset(20 * s, 14 * s), Size(60 * s, 26 * s), CornerRadius(13 * s))
        drawRect(PR.Amber, Offset(14 * s, 34 * s), Size(72 * s, 8 * s))
        drawCircle(Palette5.DeepNavy, 4.5f * s, Offset(38 * s, 58 * s))
        drawCircle(Palette5.DeepNavy, 4.5f * s, Offset(62 * s, 58 * s))
        drawArc(Palette5.DeepNavy, 20f, 140f, false, Offset(38 * s, 60 * s), Size(24 * s, 18 * s), style = androidx.compose.ui.graphics.drawscope.Stroke(3 * s, cap = StrokeCap.Round))
    }
}

/** The sun or moon for a real clock time: sun from 06:00 to 19:00, a setting sun near the end of the day, a moon otherwise. */
@Composable
fun SunMoonIconAt(minuteOfDay: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val period = when {
        minuteOfDay < 6 * 60 || minuteOfDay >= 19 * 60 -> DayPeriod.NIGHT
        minuteOfDay >= 17 * 60 -> DayPeriod.EVENING
        else -> DayPeriod.AFTERNOON
    }
    SunMoonIcon(period, modifier.size(size))
}

private val SunColor = Color(0xFFFFC233)
private val SunsetColor = Color(0xFFFF8A3D)
private val MoonColor = Color(0xFFFFF1B8)

/** A sun in the morning and afternoon, a setting sun in the evening, a crescent moon with a star at night. */
@Composable
fun SunMoonIcon(period: DayPeriod, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        when (period) {
            DayPeriod.MORNING, DayPeriod.AFTERNOON -> drawSun(c, size.width * 0.22f, SunColor, rays = true)
            DayPeriod.EVENING -> {
                drawSun(Offset(c.x, c.y + size.height * 0.12f), size.width * 0.22f, SunsetColor, rays = true)
                drawRect(PR.Blue50, Offset(0f, c.y + size.height * 0.12f), Size(size.width, size.height)) // the horizon hides the lower half
                drawLine(Palette5.DuskBlue, Offset(size.width * 0.05f, c.y + size.height * 0.12f), Offset(size.width * 0.95f, c.y + size.height * 0.12f), strokeWidth = size.width * 0.06f, cap = StrokeCap.Round)
            }
            DayPeriod.NIGHT -> {
                val r = size.width * 0.3f
                drawCircle(MoonColor, r, c - Offset(size.width * 0.04f, 0f))
                drawCircle(PR.Blue50, r * 0.85f, c + Offset(size.width * 0.12f, -size.height * 0.07f)) // the bite that makes the crescent
                drawCircle(MoonColor, size.width * 0.045f, Offset(size.width * 0.8f, size.height * 0.2f))
            }
        }
    }
}

private fun DrawScope.drawSun(c: Offset, r: Float, color: Color, rays: Boolean) {
    drawCircle(color, r, c)
    if (!rays) return
    for (k in 0 until 8) {
        val a = k * Math.PI.toFloat() / 4f
        drawLine(color, c + Offset(cos(a) * r * 1.35f, sin(a) * r * 1.35f), c + Offset(cos(a) * r * 1.85f, sin(a) * r * 1.85f), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
    }
}
