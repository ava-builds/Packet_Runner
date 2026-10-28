package com.avabuilds.packetrunner.ui.work

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.GameState
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.Upgrades
import com.avabuilds.packetrunner.ui.heartsText
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import com.avabuilds.packetrunner.ui.SecondaryButtonSized
import com.avabuilds.packetrunner.ui.TownColors
import com.avabuilds.packetrunner.work.Payday
import com.avabuilds.packetrunner.work.WorkState

/** The card for after work and days off: no calls, only things to enjoy. */
@Composable
fun FreeTimeCard(
    state: WorkState,
    marketOpen: Boolean,
    onRoom: () -> Unit,
    onMarket: () -> Unit,
    onShop: () -> Unit,
    onTopic: () -> Unit,
    onLab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, color = PR.White, borderColor = Palette5.Cornflower) {
        Text(if (state == WorkState.DAY_OFF) "🌿 Day off" else "🌆 Work is over", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("No customer calls now. Walk around town, or enjoy something.", fontSize = 13.sp, color = PR.Muted)
        if (marketOpen) PrimaryButton("🎪 Saturday market", onClick = onMarket)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButtonSized("🛋 Room", 14.sp, Modifier.weight(1f), onRoom)
            SecondaryButtonSized("🛒 Shop", 14.sp, Modifier.weight(1f), onShop)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButtonSized("📚 Topic", 14.sp, Modifier.weight(1f), onTopic)
            SecondaryButtonSized("🔬 Lab", 14.sp, Modifier.weight(1f), onLab)
        }
    }
}

/** Your office room, with the decorations you have bought. Buying more happens in the Shop. */
@Composable
fun OfficeRoomScreen(owned: Set<String>, onShop: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🛋 Your office room", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("${owned.size} of ${Upgrades.decor.size} decorations", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            Canvas(Modifier.fillMaxWidth().height(300.dp).background(Color(0xFFF3E3C8), RoundedCornerShape16())) { drawRoom(owned) }
            Panel(color = PR.White, borderColor = PR.Line) {
                Upgrades.decor.forEach { id ->
                    val u = Upgrades.byId(id)!!
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(u.title, Modifier.weight(1f), fontSize = 13.sp, color = PR.Ink)
                        if (id in owned) Pill("✓ in your room", PR.Green50, PR.Green800) else Pill("₩${u.prices[0]}", PR.Amber50, PR.Amber800)
                    }
                }
            }
            SecondaryButton("🛒 Go to the Shop", onClick = onShop)
        }
    }
}

private fun RoundedCornerShape16() = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoom(owned: Set<String>) {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFFD9B98A), Offset(0f, h * 0.62f), Size(w, h * 0.38f)) // floor
    // desk with a computer
    drawRoundRect(Color(0xFFB5693E), Offset(w * 0.3f, h * 0.55f), Size(w * 0.4f, h * 0.07f), CornerRadius(10f))
    drawRect(Color(0xFF8A4E2A), Offset(w * 0.33f, h * 0.62f), Size(w * 0.03f, h * 0.22f))
    drawRect(Color(0xFF8A4E2A), Offset(w * 0.64f, h * 0.62f), Size(w * 0.03f, h * 0.22f))
    drawRoundRect(Palette5.DeepNavy, Offset(w * 0.42f, h * 0.36f), Size(w * 0.16f, h * 0.17f), CornerRadius(8f))
    drawRoundRect(Palette5.IcyBlue, Offset(w * 0.435f, h * 0.38f), Size(w * 0.13f, h * 0.12f), CornerRadius(4f))
    drawRect(Palette5.DeepNavy, Offset(w * 0.495f, h * 0.53f), Size(w * 0.01f, h * 0.02f))
    // window
    drawRoundRect(TownColors.Window, Offset(w * 0.06f, h * 0.1f), Size(w * 0.2f, h * 0.3f), CornerRadius(10f))
    drawRoundRect(Palette5.DuskBlue, Offset(w * 0.06f, h * 0.1f), Size(w * 0.2f, h * 0.3f), CornerRadius(10f), style = Stroke(5f))
    if (Upgrades.DECOR_POSTER in owned) {
        drawRoundRect(Color.White, Offset(w * 0.74f, h * 0.1f), Size(w * 0.2f, h * 0.3f), CornerRadius(6f))
        drawRoundRect(Palette5.PersianBlue, Offset(w * 0.74f, h * 0.1f), Size(w * 0.2f, h * 0.3f), CornerRadius(6f), style = Stroke(4f))
        for (i in 0 until 4) drawRoundRect(if (i % 2 == 0) Palette5.Cornflower else Palette5.IcyBlue, Offset(w * 0.76f, h * (0.13f + i * 0.065f)), Size(w * 0.16f, h * 0.045f), CornerRadius(4f))
    }
    if (Upgrades.DECOR_PLANT in owned) {
        drawRoundRect(Color(0xFFB5693E), Offset(w * 0.78f, h * 0.7f), Size(w * 0.1f, h * 0.14f), CornerRadius(8f))
        for (a in listOf(-40f, -15f, 10f, 35f)) drawLine(Color(0xFF3F8F49), Offset(w * 0.83f, h * 0.7f), Offset(w * 0.83f + a * 1.2f, h * 0.5f), strokeWidth = 10f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF55AA57), w * 0.045f, Offset(w * 0.83f, h * 0.5f))
    }
    if (Upgrades.DECOR_LAMP in owned) {
        drawLine(Palette5.DeepNavy, Offset(w * 0.14f, h * 0.84f), Offset(w * 0.14f, h * 0.55f), strokeWidth = 6f, cap = StrokeCap.Round)
        drawCircle(Color(0x44FFD36B), w * 0.12f, Offset(w * 0.14f, h * 0.5f))
        drawRoundRect(Color(0xFFFFD36B), Offset(w * 0.1f, h * 0.45f), Size(w * 0.08f, h * 0.09f), CornerRadius(10f))
    }
}

/** The Saturday market: give a neighbor a small gift. It costs a few won and they like you more; once per neighbor a day. */
@Composable
fun MarketScreen(state: GameState, neighbors: List<Neighbor>, onGift: (Neighbor) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🎪 Saturday market", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("Everyone is out in the square. Buy a little gift (₩${GameEngine.GIFT_COST}) for a neighbor.", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            Text("Your money: ₩${state.money}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PR.Amber800)
            neighbors.filter { it.movesInDay <= state.day }.forEach { n ->
                val given = GameEngine.giftId(n.id) in state.labDone
                Panel(color = PR.White, borderColor = if (given) PR.Green600 else Palette5.PersianBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(n.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                            Text(heartsText(state.hearts[n.id] ?: 0), fontSize = 13.sp, color = PR.Red)
                        }
                        when {
                            given -> Pill("✓ gift given today", PR.Green50, PR.Green800)
                            state.money < GameEngine.GIFT_COST -> Pill("not enough money", PR.GrayFill, PR.Gray)
                            else -> Box(Modifier.clickable { onGift(n) }) { Pill("🎁 Give a gift · ₩${GameEngine.GIFT_COST}", Palette5.PersianBlue, Color.White) }
                        }
                    }
                }
            }
        }
    }
}

/** Friday evening: what the week brought, and the payday bonus to collect. */
@Composable
fun PaydayScreen(payday: Payday, onCollect: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xCC091540)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Panel(Modifier.padding(20.dp), color = PR.White, borderColor = PR.Amber) {
            Text("💰 Payday!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            Text("Your week at Network Town", fontSize = 13.sp, color = PR.Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Calls solved ${payday.callsSolved}", PR.Green50, PR.Green800)
                Pill("Earned ₩${payday.moneyEarned}", PR.Amber50, PR.Amber800)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Friendship ${if (payday.heartsGained >= 0) "+" else ""}${payday.heartsGained} ♥", PR.Red50, PR.Red800)
                Pill("XP +${payday.xpGained}", PR.Blue50, PR.Blue800)
            }
            Text(
                if (payday.bonus > 0) "Payday bonus: ₩${payday.bonus}" else "No solved calls this week, so no bonus. Next week!",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = PR.Ink,
            )
            PrimaryButton(if (payday.bonus > 0) "Collect ₩${payday.bonus}" else "Continue", onClick = onCollect)
        }
    }
}
