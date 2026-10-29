package com.avabuilds.packetrunner.ui.office

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Upgrade
import com.avabuilds.packetrunner.game.Upgrades
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill

/** The Upgrade shop: spend money on things that last. Each upgrade has levels, and each level costs more. */
@Composable
fun UpgradeShop(money: Int, levels: Map<String, Int>, onBuy: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🛒 Upgrade shop", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("Your money: ₩$money", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            Upgrades.all.forEach { u -> UpgradeCard(u, levels[u.id] ?: 0, money, onBuy) }
        }
    }
}

@Composable
private fun UpgradeCard(u: Upgrade, level: Int, money: Int, onBuy: (String) -> Unit) {
    val maxed = level >= u.maxLevel
    val price = if (maxed) 0 else u.prices[level]
    Panel(color = PR.White, borderColor = if (maxed) PR.Green600 else Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(u.title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            Text("●".repeat(level) + "○".repeat(u.maxLevel - level), fontSize = 14.sp, color = Palette5.PersianBlue)
        }
        Text(u.description, fontSize = 13.sp, color = PR.Muted)
        Text(effectText(u, level, maxed), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PR.Ink)
        when {
            maxed -> Pill("✓ Top level", PR.Green50, PR.Green800)
            money >= price -> Box(Modifier.clickable { onBuy(u.id) }) { Pill("Buy level ${level + 1} · ₩$price", Palette5.PersianBlue, PR.White) }
            else -> Pill("Level ${level + 1} · ₩$price (not enough money)", PR.GrayFill, PR.Gray)
        }
    }
}

/** What the upgrade does now, and what the next level will do. */
private fun effectText(u: Upgrade, level: Int, maxed: Boolean): String = when (u.id) {
    Upgrades.SCOOTER -> "Speed ${fmt(Upgrades.walkSpeed(level))} tiles per second" + if (maxed) "" else " → ${fmt(Upgrades.walkSpeed(level + 1))}"
    Upgrades.CABLE_SPOOL -> "Cable ₩${Upgrades.cablePrice(5, level)} per cell" + if (maxed) "" else " → ₩${Upgrades.cablePrice(5, level + 1)}"
    else -> ""
}

private fun fmt(speed: Float) = if (speed % 1f == 0f) speed.toInt().toString() else ((speed * 10).roundToInt() / 10.0).toString()
