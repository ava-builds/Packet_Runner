package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** World 4 · Mom moved! Content and addresses for the tunnel-building puzzle. */
object MobileIpLevel {
    const val WORLD = 4

    val pip = Ipv4.parse("203.0.113.5")
    val home = Ipv4.parse("128.96.5.9")
    val homeAgent = Ipv4.parse("128.96.5.1")
    val careOf = Ipv4.parse("45.64.20.77")

    val original = IpPacket(src = pip, dst = home)

    val briefing = Briefing(
        chapter = "World 4 · Mom moved!",
        title = "Follow Mom",
        nanaSays = "Pip, Mom moved to Yangon but her phone keeps its home address. " +
            "Everyone still sends to the old address. You get to be the tunnel builder!",
        why = "Mobile IP (모바일 IP) lets a host keep one permanent home address while it moves. " +
            "A home agent (홈 에이전트) receives packets for it and tunnels them to the care-of address, " +
                "where a foreign agent (in the visited network) hands them to the host.",
        howToThink = "Senders never change the destination. The home agent wraps the whole packet " +
            "inside a new IP header (source = home agent, destination = care-of address). " +
            "At the end of the tunnel the outer header is removed.",
        keywords = listOf(
            "Home agent · 홈 에이전트",
            "Care-of address · 임시 주소",
            "Tunneling · 터널링",
            "Triangle routing · 삼각 라우팅",
        ),
        book = "Advanced Internetworking · Routing to mobile hosts (Mobile IP): home agent, foreign " +
            "agent, care-of address, tunneling, and the triangle routing problem.",
    )

    const val WHAT_REALLY_HAPPENED =
        "Pip's packet is addressed to Mom's permanent home address. The home agent captures it and " +
            "encapsulates it: outer source = home agent, outer destination = care-of address, original " +
            "packet inside. At Mom's location the outer header is removed, and the original packet is " +
            "delivered to her phone. Her reply goes straight to Pip, without the tunnel, so the two " +
            "directions take different paths (this detour is called triangle routing)."
}

private const val STEPS = 4

private enum class Place(val emoji: String, val title: String, val sub: String) {
    HOME_AGENT("🏢", "Home agent", "on Mom's home network"),
    MOM_HOME("🏠", "Mom's old room", "nobody is there"),
    FOREIGN("🛰", "Foreign agent", "in Yangon"),
}

private val Addresses = listOf(
    "Pip" to MobileIpLevel.pip,
    "Home address" to MobileIpLevel.home,
    "Home agent" to MobileIpLevel.homeAgent,
    "Care-of address" to MobileIpLevel.careOf,
)

/**
 * World 4 gameplay, in four steps:
 * 1. Tap the device on the home network that grabs Pip's packet.
 * 2. Build the tunnel: fill the outer header's source and destination.
 * 3. Peel the right layer off at the end of the tunnel.
 * 4. Address Mom's reply.
 */
@Composable
fun MobileIpScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = MobileIpLevel
    val run = rememberLevelRun(onFinish)
    var step by remember { mutableIntStateOf(0) }
    var done by remember(step) { mutableStateOf(false) }
    var outerSrc by remember(step) { mutableStateOf<Ipv4?>(null) }
    var outerDst by remember(step) { mutableStateOf<Ipv4?>(null) }
    var say by remember { mutableStateOf(Say("Pip's packet is flying to Mom's home address. Who catches it?")) }

    val goal = when (step) {
        0 -> "Pip's packet arrives on Mom's home network. Tap the device that grabs it."
        1 -> "Build the tunnel. Fill the OUTER header, then send it to Mom."
        2 -> "The tunnel packet reached Yangon. Tap the layer to peel off."
        else -> "Mom answers Pip. Address her reply packet."
    }

    fun wrong(question: String, yours: String, correct: String, why: String) {
        run.fail(Mistake(question, yours, correct, why))
        say = Say(why, isError = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LevelHeader("World 4 · Mom moved!", "Step ${step + 1} of $STEPS", run.lives, onQuit)

        Panel(color = PR.Purple50) {
            Text("Goal", fontSize = 12.sp, color = PR.Muted)
            Text(goal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Panel(color = PR.Board) {
            Text(
                "Mom's home address ${level.home}   ·   now in Yangon at ${level.careOf}",
                fontSize = 11.sp,
                color = PR.BoardText,
                fontFamily = FontFamily.Monospace,
            )
        }

        when (step) {
            0 -> {
                PacketView(inner = level.original, outer = null)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Place.entries.forEach { place ->
                        Column(
                            Modifier
                                .weight(1f)
                                .background(PR.White, RoundedCornerShape(14.dp))
                                .border(1.dp, PR.Line, RoundedCornerShape(14.dp))
                                .clickable(enabled = !done) {
                                    if (place == Place.HOME_AGENT) {
                                        done = true
                                        say = Say("The home agent knows Mom has moved, so it catches her packets " +
                                            "instead of delivering them.")
                                    } else {
                                        wrong(
                                            "Who grabs Mom's packet on the home network?",
                                            place.title,
                                            Place.HOME_AGENT.title,
                                            if (place == Place.MOM_HOME) "Mom isn't home. Nobody there can pick up her packets."
                                            else "The foreign agent is in Yangon, but the packet is still on the home network.",
                                        )
                                    }
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(place.emoji, fontSize = 28.sp)
                            Text(place.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(place.sub, fontSize = 10.sp, color = PR.Muted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            1 -> {
                PacketView(inner = level.original, outer = null)
                HeaderBuilder(
                    title = "Outer header",
                    src = outerSrc,
                    dst = outerDst,
                    enabled = !done,
                    onSrc = { outerSrc = it },
                    onDst = { outerDst = it },
                    onSend = {
                        val expected = MobileIp.encapsulate(level.original, level.homeAgent, level.careOf)
                        if (outerSrc == expected.src && outerDst == expected.dst) {
                            done = true
                            say = Say("Tunnel built: ${expected.src} → ${expected.dst}, with Pip's packet inside. " +
                                "Routers on the way only look at the outer header.")
                        } else {
                            wrong(
                                "Fill the tunnel's outer header",
                                "${outerSrc} → ${outerDst}",
                                "${expected.src} → ${expected.dst}",
                                "The home agent builds the tunnel, so it is the source. The tunnel must end " +
                                    "where Mom is now, so the destination is the care-of address.",
                            )
                        }
                    },
                )
            }

            2 -> {
                val tunnel = MobileIp.encapsulate(level.original, level.homeAgent, level.careOf)
                Text("Tap the layer to remove", fontSize = 13.sp, color = PR.Muted)
                PacketView(
                    inner = tunnel.inner!!,
                    outer = tunnel,
                    onOuter = {
                        if (!done) {
                            val delivered = MobileIp.decapsulate(tunnel)
                            done = true
                            say = Say("Peeled! The original packet ${delivered.src} → ${delivered.dst} is " +
                                "delivered. Mom's phone still thinks she is at home.")
                        }
                    },
                    onInner = {
                        if (!done) {
                            wrong(
                                "Which layer is removed at the end of the tunnel?",
                                "Inner header (Pip → Mom)",
                                "Outer header (home agent → care-of)",
                                "The inner packet is the real message. Only the outer tunnel header is removed.",
                            )
                        }
                    },
                )
            }

            else -> {
                Text("Mom's reply packet", fontSize = 13.sp, color = PR.Muted)
                HeaderBuilder(
                    title = "Reply header",
                    src = outerSrc,
                    dst = outerDst,
                    enabled = !done,
                    onSrc = { outerSrc = it },
                    onDst = { outerDst = it },
                    onSend = {
                        if (outerSrc == level.home && outerDst == level.pip) {
                            done = true
                            say = Say("Straight to Pip, no tunnel needed! The reply path differs from the " +
                                "forward path (triangle routing).")
                        } else {
                            wrong(
                                "Address Mom's reply to Pip",
                                "${outerSrc} → ${outerDst}",
                                "${level.home} → ${level.pip}",
                                "Mom sends from her home address to Pip's address. Pip's address is known, so " +
                                    "no home agent or tunnel is involved.",
                            )
                        }
                    },
                )
            }
        }

        if (done) {
            val last = step == STEPS - 1
            PrimaryButton(if (last) "Finish mission" else "Next step →") {
                if (last) {
                    run.clear()
                } else {
                    step += 1
                    say = Say(
                        when (step) {
                            1 -> "Now the home agent must wrap the packet. What goes on the outside?"
                            2 -> "The tunnel packet made it to Yangon. Time to unwrap!"
                            else -> "Mom got the message and wants to answer. Where does her reply go?"
                        },
                    )
                }
            }
        }

        NanaSays(say.text, say.isError)
    }
}

/** A packet drawn as layers: optional outer (tunnel) header, the inner header, and the payload. */
@Composable
private fun PacketView(
    inner: IpPacket,
    outer: IpPacket?,
    onOuter: (() -> Unit)? = null,
    onInner: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PR.Blue50, RoundedCornerShape(18.dp))
            .border(1.5.dp, PR.Blue, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (outer != null) {
            HeaderBar("Outer header", outer.src, outer.dst, PR.Purple50, PR.Purple800, onOuter)
        }
        HeaderBar("Inner header", inner.src, inner.dst, PR.Teal50, PR.Teal800, onInner)
        Text(
            "💌 Pip's message",
            fontSize = 12.sp,
            color = PR.Amber800,
            modifier = Modifier
                .fillMaxWidth()
                .background(PR.Amber50, RoundedCornerShape(10.dp))
                .padding(10.dp),
        )
    }
}

@Composable
private fun HeaderBar(label: String, src: Ipv4, dst: Ipv4, fill: Color, ink: Color, onClick: (() -> Unit)?) {
    var m = Modifier
        .fillMaxWidth()
        .background(fill, RoundedCornerShape(10.dp))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(10.dp)) {
        Text(label, fontSize = 11.sp, color = PR.Muted)
        Text("$src → $dst", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink, fontFamily = FontFamily.Monospace)
    }
}

/**
 * Two empty slots (source, destination). Tap a slot to select it, then tap an address to fill it.
 * The first empty slot is selected automatically.
 */
@Composable
private fun HeaderBuilder(
    title: String,
    src: Ipv4?,
    dst: Ipv4?,
    enabled: Boolean,
    onSrc: (Ipv4) -> Unit,
    onDst: (Ipv4) -> Unit,
    onSend: () -> Unit,
) {
    var picking by remember(title) { mutableStateOf("src") }
    Panel(color = PR.Purple50) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Slot("Source", src, picking == "src", Modifier.weight(1f)) { if (enabled) picking = "src" }
            Slot("Destination", dst, picking == "dst", Modifier.weight(1f)) { if (enabled) picking = "dst" }
        }
        Text("Tap an address to put it in the highlighted slot:", fontSize = 12.sp, color = PR.Muted)
        Addresses.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (name, ip) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(PR.White, RoundedCornerShape(12.dp))
                            .border(1.dp, PR.Line, RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) {
                                if (picking == "src") {
                                    onSrc(ip)
                                    picking = "dst"
                                } else {
                                    onDst(ip)
                                    picking = "src"
                                }
                            }
                            .padding(10.dp),
                    ) {
                        Text(name, fontSize = 11.sp, color = PR.Muted)
                        Text(ip.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
    if (enabled && src != null && dst != null) {
        PrimaryButton("Send packet", onClick = onSend)
    }
}

@Composable
private fun Slot(label: String, value: Ipv4?, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(if (active) PR.Amber50 else PR.White, RoundedCornerShape(12.dp))
            .border(if (active) 2.dp else 1.dp, if (active) PR.Amber else PR.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = PR.Muted)
        Text(
            value?.toString() ?: "tap to fill",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (value == null) PR.Gray else PR.Ink,
        )
    }
}
