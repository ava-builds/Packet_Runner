package com.avabuilds.packetrunner.ui.lab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.HomeAgentAction
import com.avabuilds.packetrunner.network.IpPacket
import com.avabuilds.packetrunner.network.Ipv4
import com.avabuilds.packetrunner.network.MobileIpSession
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton

private val Friend = Ipv4.parse("203.0.113.9")

/**
 * A delivery driver goes from the cafe to the depot and back home, and a friend keeps sending to the driver's
 * home address. At each stop the player registers the driver with the home agent and then decides what the home agent does.
 */
@Composable
fun MobileIpGame(onSuccess: () -> Unit) {
    val session = remember { MobileIpSession.driver() }
    var place by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(0) } // 0 register, 1 choose, 2 solved
    var tick by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    val current = session.places[place]
    remember(place) { session.moveTo(place); 0 }
    @Suppress("UNUSED_EXPRESSION") tick

    Text("Stop ${place + 1} of ${session.places.size}: ${current.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
    Text(
        "The driver's phone keeps its home address ${session.homeAddress}. A friend (${Friend}) always writes to that address, " +
            "and the packets arrive at the home agent (${session.homeAgent}) first.",
        fontSize = 13.sp, color = PR.Ink,
    )

    Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
        Text("Home agent's binding", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            session.binding?.let { "${session.homeAddress} → care-of address $it" } ?: "No binding: the driver is thought to be at home.",
            fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PR.Ink,
        )
    }

    if (step == 0) {
        Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
            Text("1 · The driver arrives at the ${current.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                current.careOf?.let { "The foreign agent here gives the phone a temporary care-of address: $it. The phone must tell its home agent." }
                    ?: "This is the home network. The phone needs no care-of address, and tells the home agent it is back.",
                fontSize = 12.sp, color = PR.Muted,
            )
            PrimaryButton(if (current.careOf != null) "Register ${current.careOf} with the home agent" else "Tell the home agent: I am home") {
                session.register(); step = 1; note = null; tick++
            }
        }
    }
    if (step >= 1) {
        Panel(color = PR.White, borderColor = if (step == 2) PR.Green600 else Palette5.PersianBlue) {
            Text("2 · A packet for ${session.homeAddress} reaches the home agent", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("What does the home agent do with it?", fontSize = 12.sp, color = PR.Muted)
            val options = listOf<HomeAgentAction>(HomeAgentAction.DeliverLocally) + session.places.mapNotNull { p -> p.careOf?.let { HomeAgentAction.Tunnel(it) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    Box(Modifier.clickable(enabled = step == 1) {
                        if (session.isRight(option)) {
                            step = 2
                            note = null
                        } else {
                            note = if (option is HomeAgentAction.Tunnel) "That is not where the driver is now. The binding says ${session.binding ?: "nothing: the driver is home"}."
                            else "The driver is not on the home network now. Look at the binding."
                        }
                    }) {
                        Pill(
                            when (option) { HomeAgentAction.DeliverLocally -> "Deliver on the home network"; is HomeAgentAction.Tunnel -> "Tunnel to ${option.careOf}" },
                            PR.Blue50, PR.Blue800,
                        )
                    }
                }
            }
        }
        note?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
    }
    if (step == 2) {
        val outer = session.tunnelled(Friend)
        Panel(color = PR.White, borderColor = PR.Green600) {
            if (outer != null) {
                Text("The packet goes through a tunnel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PR.Green800)
                PacketText("outer header", outer)
                PacketText("  inner packet", outer.inner!!)
                Text("The foreign agent removes the outer header and gives the driver the inner packet.", fontSize = 12.sp, color = PR.Muted)
            } else {
                Text("No tunnel needed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PR.Green800)
                Text("The driver is home again, so the packet is delivered like any other.", fontSize = 12.sp, color = PR.Muted)
            }
        }
        PrimaryButton(if (place == session.places.lastIndex) "Finish" else "Drive on to the next stop") {
            if (place == session.places.lastIndex) onSuccess() else { place++; step = 0; note = null }
        }
    }
}

@Composable
private fun PacketText(label: String, p: IpPacket) {
    Text("$label: from ${p.src} to ${p.dst}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PR.Ink)
}
