package com.avabuilds.packetrunner.ui.hud

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5

/** The four places of the bottom bar. */
enum class NavTab(val label: String, val icon: String) {
    TOWN("Town", "🏘"),
    NOTEBOOK("Notebook", "📓"),
    SHOP("Shop", "🛒"),
    PROFILE("Profile", "👤"),
}

/** The height of the bottom bar, so cards and panels can keep above it. */
val BottomNavHeight = 64.dp

/** The bottom bar: Town, Notebook, Shop and Profile. The chosen place has a blue pill behind it. */
@Composable
fun BottomNav(selected: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(BottomNavHeight)
            .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab.entries.forEach { tab ->
            val on = tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) PR.Blue50 else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(tab.icon, fontSize = 20.sp)
                Text(tab.label, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) Palette5.PersianBlue else PR.Muted)
            }
        }
    }
}

/**
 * The round phone button floating over the town. When a call arrives it shakes, pulses a ring and shows a red badge;
 * tapping it then picks up the phone. With no call it just says so.
 */
@Composable
fun PhoneFab(ringing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, voicemails: Int = 0) {
    val transition = rememberInfiniteTransition(label = "phone")
    val shake = if (ringing) {
        transition.animateFloat(-14f, 14f, infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse), label = "shake").value
    } else 0f
    val pulse = if (ringing) {
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "pulse").value
    } else 0f

    Box(modifier.size(64.dp), contentAlignment = Alignment.Center) {
        if (ringing) {
            Canvas(Modifier.size(64.dp)) {
                drawCircle(PR.Amber.copy(alpha = (1f - pulse) * 0.55f), size.width * (0.42f + 0.28f * pulse))
            }
        }
        Box(
            Modifier
                .size(52.dp)
                .graphicsLayer { rotationZ = shake }
                .clip(CircleShape)
                .background(if (ringing) PR.Amber else Palette5.PersianBlue)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PhoneGlyph(Modifier.size(26.dp), if (ringing) Palette5.DeepNavy else Color.White)
        }
        if (!ringing && voicemails > 0) {
            Box(Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Palette5.Cornflower), contentAlignment = Alignment.Center) {
                Text("$voicemails", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        if (ringing) {
            Box(Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(PR.Red), contentAlignment = Alignment.Center) {
                Text("1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/** A simple handset-style phone icon: a rounded phone body with a screen and a home dot. */
@Composable
private fun PhoneGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width * 0.58f
        val h = size.height * 0.95f
        val tl = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        drawRoundRect(color, tl, Size(w, h), CornerRadius(w * 0.22f), style = Stroke(size.width * 0.1f, cap = StrokeCap.Round))
        drawLine(color, Offset(size.width / 2f - w * 0.18f, tl.y + h * 0.82f), Offset(size.width / 2f + w * 0.18f, tl.y + h * 0.82f), strokeWidth = size.width * 0.09f, cap = StrokeCap.Round)
    }
}
