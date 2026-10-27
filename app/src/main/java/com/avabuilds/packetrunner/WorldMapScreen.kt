package com.avabuilds.packetrunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

// Layout of the infographic, in dp
private val HeaderHeight = 200.dp
private val SectionHeight = 250.dp
private val FooterHeight = 170.dp
private val RoadWidth = 96.dp
private val BandHeight = 124.dp

// Colors, mapped from the poster's structure onto the app palette
/** Background of the whole screen while the map is showing, status bar areas included. */
val MapBackground = Palette5.Cornflower
private val RoadColor = Palette5.DuskBlue
private val BandColor = Color(0xFFF4F8FF)
private val CloudColor = Color.White
private val CrownA = Palette5.IcyBlue
private val CrownB = Palette5.PersianBlue
private val Trunk = Palette5.DuskBlue

/**
 * Pip's journey as a tall scrolling infographic: one winding road from Seoul (top) to Mom (bottom),
 * with each world in a band laid over the road. Tap a band to play that world.
 */
@Composable
fun WorldMapScreen(worldStars: Map<Int, Int>, onPlayWorld: (Int) -> Unit) {
    val pipWorld = Worlds.firstOrNull { (worldStars[it.number] ?: 0) == 0 }?.number
    val totalHeight = HeaderHeight + SectionHeight * Worlds.size + FooterHeight

    Column(
        Modifier
            .fillMaxSize()
            .background(MapBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(totalHeight)) {
            val width = maxWidth

            Canvas(Modifier.matchParentSize()) { drawWorld(Worlds.size) }

            Header(worldStars.values.sum())

            Worlds.forEachIndexed { i, world ->
                val centerY = HeaderHeight + SectionHeight * i + SectionHeight / 2
                val bandWidth = width * 0.9f
                val stars = worldStars[world.number] ?: 0
                Band(
                    world = world,
                    stars = stars,
                    isHere = world.number == pipWorld,
                    iconOnLeft = i % 2 == 0,
                    modifier = Modifier
                        .offset(x = (width - bandWidth) / 2, y = centerY - BandHeight / 2)
                        .width(bandWidth)
                        .height(BandHeight),
                    onClick = { if (world.playable) onPlayWorld(world.number) },
                )
                if (world.number == pipWorld) {
                    Box(
                        Modifier.offset(
                            x = if (i % 2 == 0) width * 0.05f + 4.dp else width * 0.95f - 60.dp,
                            y = centerY - BandHeight / 2 - 36.dp,
                        ),
                    ) { Pip(size = 56.dp) }
                }
            }

            Text(
                "Ready to bring Pip home to Mom?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = HeaderHeight + SectionHeight * Worlds.size + 40.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun Header(totalStars: Int) {
    Box(Modifier.fillMaxWidth().height(HeaderHeight).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("PIP'S", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)
            Text("JOURNEY", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)
            Text(
                "SEOUL → YANGON",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Palette5.DeepNavy,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Pip(size = 64.dp)
            Spacer(Modifier.height(6.dp))
            Pill("★ $totalStars / ${Worlds.size * 3}", PR.Amber50, PR.Amber800)
        }
    }
}

/** One world, as a cream band lying on the road. */
@Composable
private fun Band(
    world: WorldInfo,
    stars: Int,
    isHere: Boolean,
    iconOnLeft: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = if (world.number % 2 == 0) Palette5.DuskBlue else Palette5.PersianBlue
    Row(
        modifier
            .background(BandColor, RoundedCornerShape(BandHeight / 2))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val icon = @Composable { WorldIcon(world.number, Modifier.size(76.dp)) }
        val text = @Composable {
            Column(Modifier.weight(1f), horizontalAlignment = if (iconOnLeft) Alignment.Start else Alignment.End) {
                Text("${world.number} · ${world.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
                Text(world.topic, fontSize = 12.sp, color = PR.Muted, textAlign = if (iconOnLeft) TextAlign.Start else TextAlign.End)
                Spacer(Modifier.height(4.dp))
                when {
                    stars > 0 -> Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 16.sp, color = PR.Amber)
                    isHere -> Text("Play now ▶", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette5.PersianBlue)
                    else -> Text("Tap to play", fontSize = 12.sp, color = PR.Muted)
                }
            }
        }
        if (iconOnLeft) {
            icon(); text()
        } else {
            text(); icon()
        }
    }
}

// ---------------------------------------------------------------------------
// The road and scenery
// ---------------------------------------------------------------------------

/** Draws the winding road and the scenery around it for [sections] stacked bands. */
private fun DrawScope.drawWorld(sections: Int) {
    val d = density
    val w = size.width
    val header = HeaderHeight.value * d
    val sec = SectionHeight.value * d
    val roadW = RoadWidth.value * d
    val r = sec / 2
    val xR = w * 0.80f
    val xL = w * 0.20f
    fun cy(i: Int) = header + sec * i + sec / 2

    // Clouds
    cloud(Offset(w * 0.56f, header * 0.14f), 40f * d)
    cloud(Offset(w * 0.72f, header * 0.95f), 40f * d)
    cloud(Offset(w * 0.04f, header + sec * 2.0f), 38f * d)
    cloud(Offset(w * 0.95f, header + sec * 3.0f), 38f * d)

    // Road: left-to-right on even sections, right-to-left on odd, joined by U-turns
    val road = Path().apply {
        moveTo(-20f * d, cy(0))
        for (i in 0 until sections) {
            val goingRight = i % 2 == 0
            val x = if (goingRight) xR else xL
            lineTo(x, cy(i))
            if (i < sections - 1) {
                arcTo(
                    Rect(Offset(x - r, cy(i)), Size(2 * r, 2 * r)),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = if (goingRight) 180f else -180f,
                    forceMoveTo = false,
                )
            }
        }
    }
    drawPath(road, RoadColor, style = Stroke(width = roadW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
        road,
        Palette5.IcyBlue.copy(alpha = 0.55f),
        style = Stroke(width = 3f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * d, 16f * d))),
    )

    // Scenery in the free corners between bands (the gap is 126dp tall, centered on y)
    for (i in 0 until sections - 1) {
        val y = header + sec * (i + 1)
        val freeLeft = i % 2 == 0
        val side = if (freeLeft) 1f else -1f
        val base = if (freeLeft) 0f else w
        tree(Offset(base + side * w * 0.11f, y + 52f * d), 0.95f * d)
        lamp(Offset(base + side * w * 0.28f, y + 56f * d), d)
        building(Offset(base + side * w * 0.42f - (if (freeLeft) 0f else 34f * d), y - 28f * d), 34f * d, 62f * d)
        tree(Offset(base + side * w * 0.64f, y + 50f * d), 0.8f * d)
    }
    // Trees at the top and bottom of the page
    tree(Offset(w * 0.93f, header + 44f * d), 0.9f * d)
    tree(Offset(w * 0.07f, header + sec * sections + 20f * d), 1.0f * d)
    tree(Offset(w * 0.93f, header + sec * sections + 60f * d), 0.9f * d)
    // Finish flag at the end of the road
    val endX = xR
    val endY = cy(sections - 1)
    drawLine(Palette5.DeepNavy, Offset(endX + roadW * 0.9f, endY + roadW * 0.5f), Offset(endX + roadW * 0.9f, endY - roadW * 0.6f), strokeWidth = 4f * d)
    drawPath(
        Path().apply {
            moveTo(endX + roadW * 0.9f, endY - roadW * 0.6f)
            lineTo(endX + roadW * 0.9f + 30f * d, endY - roadW * 0.45f)
            lineTo(endX + roadW * 0.9f, endY - roadW * 0.3f)
            close()
        },
        PR.Amber,
    )
}

private fun DrawScope.cloud(at: Offset, s: Float) {
    drawCircle(CloudColor, s * 0.5f, at)
    drawCircle(CloudColor, s * 0.42f, at + Offset(s * 0.5f, s * 0.12f))
    drawCircle(CloudColor, s * 0.36f, at + Offset(-s * 0.46f, s * 0.16f))
    drawRoundRect(CloudColor, at + Offset(-s * 0.7f, s * 0.2f), Size(s * 1.7f, s * 0.38f), CornerRadius(s * 0.2f))
}

private fun DrawScope.tree(base: Offset, k: Float) {
    drawRoundRect(Trunk, base + Offset(-3f * k, -34f * k), Size(6f * k, 36f * k), CornerRadius(2f * k))
    drawCircle(CrownB, 24f * k, base + Offset(0f, -52f * k))
    drawCircle(CrownA, 9f * k, base + Offset(-8f * k, -60f * k))
}

private fun DrawScope.lamp(base: Offset, k: Float) {
    drawLine(Palette5.DeepNavy, base, base + Offset(0f, -62f * k), strokeWidth = 4f * k)
    drawRoundRect(Palette5.DeepNavy, base + Offset(-9f * k, -86f * k), Size(18f * k, 26f * k), CornerRadius(4f * k))
    drawRoundRect(Palette5.IcyBlue, base + Offset(-5f * k, -81f * k), Size(10f * k, 16f * k), CornerRadius(3f * k))
}

private fun DrawScope.building(topLeft: Offset, bw: Float, bh: Float) {
    drawRoundRect(Palette5.DuskBlue, topLeft, Size(bw, bh), CornerRadius(3f))
    val cols = 3
    val rows = 4
    for (cx in 0 until cols) for (ry in 0 until rows) {
        drawRect(
            Palette5.IcyBlue,
            topLeft + Offset(bw * (0.12f + cx * 0.29f), bh * (0.08f + ry * 0.22f)),
            Size(bw * 0.18f, bh * 0.12f),
        )
    }
}

// ---------------------------------------------------------------------------
// One drawn icon per world
// ---------------------------------------------------------------------------

@Composable
private fun WorldIcon(world: Int, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.07f
        val persian = Palette5.PersianBlue
        val dusk = Palette5.DuskBlue
        val navy = Palette5.DeepNavy
        val icy = Palette5.IcyBlue
        val corn = Palette5.Cornflower
        when (world) {
            1 -> { // Wi-Fi signal inside fog
                drawCircle(icy, w * 0.2f, Offset(w * 0.2f, h * 0.82f))
                drawCircle(icy, w * 0.24f, Offset(w * 0.82f, h * 0.84f))
                drawRoundRect(icy, Offset(w * 0.1f, h * 0.78f), Size(w * 0.85f, h * 0.18f), CornerRadius(w * 0.09f))
                val c = Offset(w * 0.5f, h * 0.72f)
                listOf(0.18f, 0.34f, 0.5f).forEach { rr ->
                    drawArc(persian, -135f, 90f, false, c - Offset(w * rr, w * rr), Size(w * rr * 2, w * rr * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                drawCircle(persian, w * 0.07f, c)
            }

            2 -> { // network switch: stacked units with LEDs
                listOf(0.1f, 0.38f, 0.66f).forEachIndexed { i, y ->
                    drawRoundRect(if (i == 1) persian else dusk, Offset(w * 0.05f, h * y), Size(w * 0.9f, h * 0.24f), CornerRadius(w * 0.06f))
                    repeat(4) { k ->
                        drawCircle(if (k == i) PR.Amber else icy, w * 0.035f, Offset(w * (0.2f + k * 0.12f), h * (y + 0.12f)))
                    }
                    drawRoundRect(navy, Offset(w * 0.68f, h * (y + 0.07f)), Size(w * 0.2f, h * 0.1f), CornerRadius(w * 0.02f))
                }
            }

            3 -> { // router with antennas
                drawRoundRect(persian, Offset(w * 0.1f, h * 0.55f), Size(w * 0.8f, h * 0.3f), CornerRadius(w * 0.08f))
                listOf(0.28f, 0.72f).forEach { x ->
                    drawLine(navy, Offset(w * x, h * 0.55f), Offset(w * x, h * 0.15f), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawCircle(PR.Amber, w * 0.06f, Offset(w * x, h * 0.13f))
                }
                repeat(3) { k -> drawCircle(icy, w * 0.04f, Offset(w * (0.28f + k * 0.14f), h * 0.7f)) }
            }

            4 -> { // house
                drawRect(corn, Offset(w * 0.18f, h * 0.45f), Size(w * 0.64f, h * 0.42f))
                drawPath(
                    Path().apply {
                        moveTo(w * 0.1f, h * 0.47f); lineTo(w * 0.5f, h * 0.12f); lineTo(w * 0.9f, h * 0.47f); close()
                    },
                    dusk,
                )
                drawRoundRect(navy, Offset(w * 0.43f, h * 0.62f), Size(w * 0.14f, h * 0.25f), CornerRadius(w * 0.03f))
                drawRect(icy, Offset(w * 0.24f, h * 0.55f), Size(w * 0.12f, w * 0.12f))
                drawRect(icy, Offset(w * 0.64f, h * 0.55f), Size(w * 0.12f, w * 0.12f))
            }

            else -> { // phone with signal
                drawRoundRect(navy, Offset(w * 0.28f, h * 0.06f), Size(w * 0.44f, h * 0.88f), CornerRadius(w * 0.08f))
                drawRoundRect(icy, Offset(w * 0.33f, h * 0.14f), Size(w * 0.34f, h * 0.66f), CornerRadius(w * 0.03f))
                drawCircle(corn, w * 0.035f, Offset(w * 0.5f, h * 0.87f))
                listOf(0.14f, 0.26f).forEach { rr ->
                    drawArc(persian, -50f, 100f, false, Offset(w * 0.72f - w * rr, h * 0.2f - w * rr), Size(w * rr * 2, w * rr * 2), style = Stroke(stroke * 0.8f, cap = StrokeCap.Round))
                }
            }
        }
    }
}
