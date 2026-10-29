package com.avabuilds.packetrunner.ui.office

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.Ipv4
import com.avabuilds.packetrunner.network.SubnetInfo
import com.avabuilds.packetrunner.network.toBitString
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill

private val Presets = listOf("192.168.10.141", "172.16.8.70", "10.1.2.3", "203.0.113.77")
private val Mono = FontFamily.Monospace

/**
 * The Workbench: a subnet calculator to practise with before a router job. Pick an address, move the mask,
 * and see the network, the broadcast address and how many hosts fit, with the network bits and host bits in two colors.
 */
@Composable
fun Workbench(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var preset by remember { mutableIntStateOf(0) }
    var lastOctet by remember { mutableIntStateOf(Ipv4.parse(Presets[0]).octets[3]) }
    var prefix by remember { mutableIntStateOf(24) }

    val base = Ipv4.parse(Presets[preset])
    val address = Ipv4(base.octets.take(3) + lastOctet)
    val info = SubnetInfo(address, prefix)

    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🛠 Workbench", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("A subnet calculator for practice. It pays nothing, but it makes the router jobs easy.", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }

            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                Text("Address", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Presets.forEachIndexed { i, text ->
                        Box(Modifier.clickable { preset = i; lastOctet = Ipv4.parse(text).octets[3] }) {
                            Pill(text, if (preset == i) Palette5.PersianBlue else PR.Blue50, if (preset == i) PR.White else PR.Blue800)
                        }
                    }
                }
                Stepper("Last number", "$lastOctet", onMinus = { lastOctet = (lastOctet - 1).coerceAtLeast(0) }, onPlus = { lastOctet = (lastOctet + 1).coerceAtMost(255) })
                Stepper("Prefix length", "/$prefix", onMinus = { prefix = (prefix - 1).coerceAtLeast(8) }, onPlus = { prefix = (prefix + 1).coerceAtMost(30) })
            }

            Panel(color = PR.White, borderColor = PR.Line) {
                Text("In binary: network bits and host bits", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Bits("address  ", address, prefix)
                Bits("mask     ", info.mask, prefix)
                Bits("network  ", info.network, prefix)
                Text("The mask keeps the first $prefix bits (blue) and clears the other ${info.hostBits} (orange). Address AND mask gives the network number.", fontSize = 11.sp, color = PR.Muted)
            }

            Panel(color = PR.White, borderColor = PR.Line) {
                Row("Mask", info.mask.toString())
                Row("Network number", info.network.toString())
                Row("Broadcast address", info.broadcast.toString())
                Row("First host", info.firstHost.toString())
                Row("Last host", info.lastHost.toString())
                Row("Hosts that fit", "${info.usableHosts}")
                Text("A router row with this network and prefix catches every address from ${info.network} to ${info.broadcast}.", fontSize = 11.sp, color = PR.Muted)
            }
        }
    }
}

@Composable
private fun Row(label: String, value: String) {
    Row(Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = PR.Muted)
        Text(value, fontSize = 13.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, color = PR.Ink)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.clickable(onClick = onMinus)) { Pill("−") }
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Mono)
        Box(Modifier.clickable(onClick = onPlus)) { Pill("+") }
    }
}

/** One address in binary, with the first [prefix] bits in blue and the rest in orange. */
@Composable
private fun Bits(label: String, ip: Ipv4, prefix: Int) {
    val bits = ip.toBitString()
    Text(
        buildAnnotatedString {
            append(label)
            var counted = 0
            bits.forEach { c ->
                if (c == '.') { append('.'); return@forEach }
                val isNetwork = counted < prefix
                withStyle(SpanStyle(color = if (isNetwork) Palette5.PersianBlue else PR.Amber800)) { append(c) }
                counted++
            }
        },
        fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Medium,
    )
}
