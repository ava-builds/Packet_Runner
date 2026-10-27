package com.avabuilds.packetrunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WorldInfo(
    val number: Int,
    val emoji: String,
    val title: String,
    val topic: String,
    val playable: Boolean,
)

val Worlds = listOf(
    WorldInfo(1, "📶", "Wi-Fi fog", "무선 LAN · RTS/CTS", playable = true),
    WorldInfo(2, "🔀", "Switch city", "패킷 스위칭 · bridge", playable = true),
    WorldInfo(3, "🚉", "Router station", "인터넷 · IP forwarding", playable = true),
    WorldInfo(4, "🏠", "Mom moved!", "Mobile IP · tunneling", playable = true),
    WorldInfo(5, "📱", "Mom's phone", "트랜스포트 · TCP/UDP", playable = true),
)

private const val LEFT = 0.22f
private const val RIGHT = 0.78f

@Composable
fun WorldMapScreen(worldStars: Map<Int, Int>, onPlayWorld: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pip's journey", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Seoul → Yangon · 5 worlds", fontSize = 13.sp, color = PR.Muted)
            }
            Pill("★ ${worldStars.values.sum()} / ${Worlds.size * 3}", PR.Amber50, PR.Amber800)
        }
        Spacer(Modifier.height(24.dp))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sidePad = maxWidth * LEFT - 36.dp
            val pipWorld = Worlds.firstOrNull { (worldStars[it.number] ?: 0) == 0 }?.number
            Column {
                Worlds.forEachIndexed { index, world ->
                    val onLeft = index % 2 == 0
                    WorldNode(
                        world = world,
                        stars = worldStars[world.number] ?: 0,
                        pipHere = world.number == pipWorld,
                        onLeft = onLeft,
                        modifier = if (onLeft) Modifier.padding(start = sidePad) else Modifier.padding(end = sidePad),
                        onClick = { if (world.playable) onPlayWorld(world.number) },
                    )
                    if (index < Worlds.lastIndex) {
                        Connector(fromLeft = onLeft, done = (worldStars[world.number] ?: 0) > 0)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Earn stars in every world to bring Pip home to Mom!",
            fontSize = 12.sp,
            color = PR.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WorldNode(
    world: WorldInfo,
    stars: Int,
    pipHere: Boolean,
    onLeft: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fill = when {
        stars > 0 -> PR.Teal50
        world.playable -> PR.Purple50
        else -> PR.GrayFill
    }
    val ring = when {
        stars > 0 -> PR.Teal
        world.playable -> PR.Purple
        else -> PR.Gray
    }
    val circle = @Composable {
        Box(
            Modifier
                .size(72.dp)
                .background(fill, CircleShape)
                .border(if (world.playable) 3.dp else 1.5.dp, ring, CircleShape)
                .clickable(enabled = world.playable, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (world.playable) world.emoji else "🔒", fontSize = 28.sp)
        }
    }
    val label = @Composable {
        Column(
            horizontalAlignment = if (onLeft) Alignment.Start else Alignment.End,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                "${world.number} · ${world.title}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (world.playable) PR.Ink else PR.Muted,
            )
            Text(world.topic, fontSize = 12.sp, color = PR.Muted)
            when {
                stars > 0 -> StarRow(stars, size = 14)
                world.playable -> Text("Play now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PR.Purple800)
                else -> Text("Coming soon", fontSize = 12.sp, color = PR.Muted)
            }
            if (pipHere) Pip(size = 40.dp)
        }
    }

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = if (onLeft) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLeft) { circle(); label() } else { label(); circle() }
    }
}

/** Dashed network link between two world nodes. */
@Composable
private fun Connector(fromLeft: Boolean, done: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val x1 = size.width * (if (fromLeft) LEFT else RIGHT)
        val x2 = size.width * (if (fromLeft) RIGHT else LEFT)
        drawLine(
            color = if (done) PR.Teal else PR.Gray,
            start = Offset(x1, 0f),
            end = Offset(x2, size.height),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (done) null else PathEffect.dashPathEffect(floatArrayOf(14f, 14f)),
        )
    }
}
