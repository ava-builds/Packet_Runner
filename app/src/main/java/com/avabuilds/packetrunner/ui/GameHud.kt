package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.avabuilds.packetrunner.game.Building
import com.avabuilds.packetrunner.game.GameState
import com.avabuilds.packetrunner.game.MAX_HEARTS
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.formatTime

/** "♥♥♡♡♡" */
fun heartsText(hearts: Int) = "♥".repeat(hearts) + "♡".repeat(MAX_HEARTS - hearts)

/** A ringing phone: it shakes and sends out signal arcs. */
@Composable
fun PhoneIcon(modifier: Modifier = Modifier) {
    val shake = rememberWave(9f, 130)
    val pulse = (rememberWave(1f, 420) + 1f) / 2f // 0..1
    Canvas(modifier.graphicsLayer { rotationZ = shake }) {
        val s = size.width
        drawRoundRect(Palette5.DeepNavy, Offset(0.3f * s, 0.1f * s), Size(0.4f * s, 0.8f * s), CornerRadius(0.08f * s))
        drawRoundRect(Palette5.IcyBlue, Offset(0.35f * s, 0.18f * s), Size(0.3f * s, 0.55f * s), CornerRadius(0.03f * s))
        drawCircle(Palette5.Cornflower, 0.03f * s, Offset(0.5f * s, 0.82f * s))
        val arc = PR.Amber.copy(alpha = 0.4f + 0.6f * pulse)
        listOf(0.36f, 0.48f).forEach { r ->
            val tl = Offset(0.5f * s - r * s, 0.5f * s - r * s)
            val sz = Size(2 * r * s, 2 * r * s)
            drawArc(arc, -40f, 80f, false, tl, sz, style = Stroke(0.05f * s, cap = StrokeCap.Round))
            drawArc(arc, 140f, 80f, false, tl, sz, style = Stroke(0.05f * s, cap = StrokeCap.Round))
        }
    }
}

/** A round badge with the neighbor's first letter. */
@Composable
private fun Avatar(neighbor: Neighbor) {
    Box(Modifier.size(36.dp).background(PR.Purple, CircleShape), contentAlignment = Alignment.Center) {
        Text(neighbor.name.take(1), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** The phone is ringing: who it is, what they say, and buttons to answer or ignore. */
@Composable
fun CallBubble(
    request: Request,
    neighbor: Neighbor,
    hearts: Int,
    onAnswer: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, color = PR.White, borderColor = PR.Purple) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneIcon(Modifier.size(46.dp))
            Column(Modifier.weight(1f)) {
                Text("${neighbor.name} is calling…", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(heartsText(hearts), fontSize = 12.sp, color = PR.Red)
            }
            Pill("${request.type.label} · ₩${request.reward}", PR.Purple50, PR.Purple800)
        }
        Text("“${request.message}”", fontSize = 14.sp, lineHeight = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Answer", Modifier.weight(1f), onAnswer)
            SecondaryButton("Ignore", Modifier.weight(1f), onIgnore)
        }
    }
}

/** Shown while the player walks to the neighbor's building. */
@Composable
fun JobBanner(request: Request, neighbor: Neighbor, building: Building, modifier: Modifier = Modifier) {
    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(neighbor)
            Column(Modifier.weight(1f)) {
                Text("On the way to ${building.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${neighbor.name}: ${request.type.label.lowercase()} job · ₩${request.reward}", fontSize = 12.sp, color = PR.Muted)
            }
        }
    }
}

/** The player is at the door. "Start the job" opens the room to work in. */
@Composable
fun ArrivalCard(
    request: Request,
    neighbor: Neighbor,
    building: Building,
    onStart: () -> Unit,
    onGiveUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, color = PR.White, borderColor = PR.Amber) {
        Text("You arrived at ${building.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("${neighbor.name}: “${request.message}”", fontSize = 14.sp, lineHeight = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Start the job", Modifier.weight(1f), onStart)
            SecondaryButton("Give up", Modifier.weight(1f), onGiveUp)
        }
    }
}

/** The player is at the office with nothing to do: the Network Monitor is the work for quiet moments. */
@Composable
fun OfficeCard(
    onOpenMonitor: () -> Unit,
    onOpenLab: () -> Unit,
    onOpenWorkbench: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier,
    /** The clock in / clock out button (shown when it makes sense). */
    clockButton: @Composable () -> Unit = {},
    voicemails: Int = 0,
    onOpenVoicemail: () -> Unit = {},
    onOpenTopic: () -> Unit = {},
) {
    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Text("You are at the Office", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Nothing is ringing. Watch the network and catch problems before the neighbors do.", fontSize = 13.sp, color = PR.Muted)
        clockButton()
        if (voicemails > 0) SecondaryButton("📨 Voicemail ($voicemails)", onClick = onOpenVoicemail)
        PrimaryButton("Open Network Monitor", onClick = onOpenMonitor)
        SecondaryButton("📚 Weekly topic", onClick = onOpenTopic)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButtonSized("🔬 Lab", 14.sp, Modifier.weight(1f), onOpenLab)
            SecondaryButtonSized("🛠 Bench", 14.sp, Modifier.weight(1f), onOpenWorkbench)
            SecondaryButtonSized("🛒 Shop", 14.sp, Modifier.weight(1f), onOpenShop)
        }
    }
}

/** Everyone who has moved in, with their friendship hearts. */
@Composable
fun FriendsPanel(neighbors: List<Neighbor>, state: GameState, modifier: Modifier = Modifier) {
    Panel(modifier, color = PR.White, borderColor = PR.Line) {
        Text("Friends", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        neighbors.filter { it.movesInDay <= state.day }.forEach { n ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(n)
                Text(n.name, Modifier.weight(1f), fontSize = 14.sp)
                Text(heartsText(state.hearts[n.id] ?: 0), fontSize = 14.sp, color = PR.Red)
            }
        }
        if (neighbors.any { it.movesInDay > state.day }) {
            Text("New neighbors are moving in soon.", fontSize = 12.sp, color = PR.Muted)
        }
    }
}

/** The end-of-day summary, with a button for the next morning. */
@Composable
fun DayEndCard(state: GameState, onNextDay: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0x99091540)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Panel(Modifier.padding(24.dp), color = PR.White, borderColor = Palette5.PersianBlue) {
            Text("Day ${state.day} is over", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("It is ${formatTime(state.minute)}. Time to go home.", fontSize = 13.sp, color = PR.Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Solved ${state.todaySolved}", PR.Green50, PR.Green800)
                Pill("Missed ${state.todayMissed}", PR.Amber50, PR.Amber800)
                Pill("Failed ${state.todayFailed}", PR.Red50, PR.Red800)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Caught early ${state.todayCaught}", PR.Green50, PR.Green800)
                Pill("Bonus ₩${state.todayBonus}", PR.Amber50, PR.Amber800)
                Pill("False alarms ${state.todayFalseAlarms}", PR.Red50, PR.Red800)
            }
            Text("Earned today: ₩${state.todayEarned}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Money: ₩${state.money}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            PrimaryButton("Start day ${state.day + 1}", onClick = onNextDay)
        }
    }
}
