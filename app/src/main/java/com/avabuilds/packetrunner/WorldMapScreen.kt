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
import androidx.compose.foundation.shape.CircleShape
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
private val SectionHeight = 176.dp
private val FooterHeight = 200.dp
private val RoadWidth = 80.dp
private val BandHeight = 104.dp

// Colors, mapped from the poster's structure onto the app palette
/** Background of the whole screen while the map is showing, status bar areas included. */
val MapBackground = Palette5.Cornflower
private val RoadColor = Palette5.DuskBlue
private val BandColor = Color(0xFFF4F8FF)
private val CloudColor = Color.White

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
                            y = centerY - BandHeight / 2 - 30.dp,
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
                    .offset(y = HeaderHeight + SectionHeight * Worlds.size + 128.dp)
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
        val icon = @Composable { WorldIcon(world.number, Modifier.size(62.dp)) }
        val text = @Composable {
            Column(Modifier.weight(1f), horizontalAlignment = if (iconOnLeft) Alignment.Start else Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(24.dp).background(accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${world.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    Text(world.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = accent)
                }
                Text(
                    world.topic,
                    fontSize = 11.sp,
                    color = PR.Muted,
                    textAlign = if (iconOnLeft) TextAlign.Start else TextAlign.End,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Spacer(Modifier.height(5.dp))
                when {
                    stars > 0 -> Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 14.sp, color = PR.Amber)
                    isHere -> Text(
                        "Play now ▶",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Palette5.PersianBlue, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                    else -> Text("Tap to play", fontSize = 11.sp, color = PR.Muted)
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

/** Draws the winding road and the networked town around it for [sections] stacked bands. */
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

    // The scenery was designed for a 126dp gap between bands; scale it to the real gap.
    val gap = (SectionHeight - BandHeight).value * d
    val sd = d * (gap / (126f * d))
    val feet = gap / 2 - 5f * d // where objects stand, below the center of a gap

    // Clouds
    cloud(Offset(w * 0.56f, header * 0.14f), 40f * d)
    cloud(Offset(w * 0.72f, header * 0.95f), 40f * d)
    cloud(Offset(w * 0.86f, header + sec * sections + 30f * d), 36f * d)

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
    drawPath(road, Palette5.IcyBlue, style = Stroke(width = roadW + 18f * d, cap = StrokeCap.Round, join = StrokeJoin.Round)) // kerb
    drawPath(road, RoadColor, style = Stroke(width = roadW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
        road,
        Color.White.copy(alpha = 0.7f),
        style = Stroke(width = 3f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * d, 16f * d))),
    )

    // Pylons carry two cables down the page, with data packets riding on them
    val pylons = buildList {
        add(Offset(w * 0.88f, header + gap / 2 - 2f * d))
        for (i in 0 until sections - 1) {
            add(Offset(if (i % 2 == 0) w * 0.10f else w * 0.90f, header + sec * (i + 1) + feet))
        }
        add(Offset(w * 0.10f, header + sec * sections + 90f * d))
    }
    val packetSpots = pylons.zipWithNext().map { (a, b) -> cables(a, b, sd, packetAt = 0.14f) }
    pylons.forEach { pylon(it, sd) }

    // The town in the free corners between bands (the gap is about 126dp tall, centered on y)
    for (i in 0 until sections - 1) {
        val y = header + sec * (i + 1) + feet
        val mirror = i % 2 != 0
        fun x(frac: Float) = if (mirror) w * (1f - frac) else w * frac
        if (!mirror) {
            cellTower(Offset(x(0.26f), y), 0.72f * sd)
            office(Offset(x(0.40f), y), 34f * sd, 70f * sd)
            shop(Offset(x(0.54f), y), 52f * sd, 42f * sd)
        } else {
            dataCenter(Offset(x(0.28f), y), 70f * sd, 60f * sd)
            house(Offset(x(0.45f), y), 46f * sd, 40f * sd)
            dish(Offset(x(0.56f), y), sd)
        }
        lamp(Offset(x(0.66f), y), sd)
    }

    // Packets go on last so no building covers them
    packetSpots.forEach { packet(it, sd) }

    // Mom's home at the end of the road, with an antenna
    momsHome(Offset(w * 0.52f, header + sec * sections + 92f * d), d)
}

private fun DrawScope.cloud(at: Offset, s: Float) {
    drawCircle(CloudColor, s * 0.5f, at)
    drawCircle(CloudColor, s * 0.42f, at + Offset(s * 0.5f, s * 0.12f))
    drawCircle(CloudColor, s * 0.36f, at + Offset(-s * 0.46f, s * 0.16f))
    drawRoundRect(CloudColor, at + Offset(-s * 0.7f, s * 0.2f), Size(s * 1.7f, s * 0.38f), CornerRadius(s * 0.2f))
}

private const val PYLON_HEIGHT = 118f
private val PylonArms = listOf(10f to 26f, 30f to 20f) // (distance below the top, half width) in dp

/** A lattice transmission pylon standing on [base] (bottom center). */
private fun DrawScope.pylon(base: Offset, d: Float) {
    val h = PYLON_HEIGHT * d
    val top = base.y - h
    val c = Palette5.DeepNavy
    val bw = 22f * d
    val tw = 6f * d
    fun leftX(t: Float) = base.x - (bw + (tw - bw) * t)
    fun rightX(t: Float) = base.x + (bw + (tw - bw) * t)
    fun yAt(t: Float) = base.y - h * t
    drawLine(c, Offset(leftX(0f), yAt(0f)), Offset(leftX(1f), yAt(1f)), 3f * d)
    drawLine(c, Offset(rightX(0f), yAt(0f)), Offset(rightX(1f), yAt(1f)), 3f * d)
    val levels = 5
    for (j in 0 until levels) {
        val t0 = j / levels.toFloat()
        val t1 = (j + 1) / levels.toFloat()
        drawLine(c, Offset(leftX(t0), yAt(t0)), Offset(rightX(t1), yAt(t1)), 2f * d)
        drawLine(c, Offset(rightX(t0), yAt(t0)), Offset(leftX(t1), yAt(t1)), 2f * d)
    }
    for ((dy, half) in PylonArms) {
        val y = top + dy * d
        drawLine(c, Offset(base.x - half * d, y), Offset(base.x + half * d, y), 3f * d)
        drawCircle(Palette5.IcyBlue, 3.2f * d, Offset(base.x - half * d, y + 4f * d))
        drawCircle(Palette5.IcyBlue, 3.2f * d, Offset(base.x + half * d, y + 4f * d))
    }
}

/** Draws two sagging cables between two pylons and returns where a packet can hang on the upper one. */
private fun DrawScope.cables(a: Offset, b: Offset, d: Float, packetAt: Float): Offset {
    var spot = Offset.Zero
    PylonArms.forEachIndexed { level, (dy, half) ->
        val x0 = if (b.x > a.x) a.x + half * d else a.x - half * d
        val x1 = if (b.x > a.x) b.x - half * d else b.x + half * d
        val p0 = Offset(x0, a.y - PYLON_HEIGHT * d + dy * d)
        val p1 = Offset(x1, b.y - PYLON_HEIGHT * d + dy * d)
        val ctrl = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f + 46f * d)
        drawPath(
            Path().apply { moveTo(p0.x, p0.y); quadraticTo(ctrl.x, ctrl.y, p1.x, p1.y) },
            Palette5.DeepNavy.copy(alpha = 0.8f),
            style = Stroke(width = 2f * d),
        )
        if (level == 0) {
            val t = packetAt
            val u = 1f - t
            val px = u * u * p0.x + 2 * u * t * ctrl.x + t * t * p1.x
            val py = u * u * p0.y + 2 * u * t * ctrl.y + t * t * p1.y
            spot = Offset(px, py)
        }
    }
    return spot
}

/** A little envelope (a data packet) hanging from a cable at [at]. */
private fun DrawScope.packet(at: Offset, d: Float) {
    drawLine(Palette5.DeepNavy, at, at + Offset(0f, 7f * d), 1.5f * d)
    val tl = at + Offset(-9f * d, 7f * d)
    drawRoundRect(Color.White, tl, Size(18f * d, 12f * d), CornerRadius(2.5f * d))
    drawPath(
        Path().apply {
            moveTo(tl.x + 1.5f * d, tl.y + 1.5f * d)
            lineTo(tl.x + 9f * d, tl.y + 7f * d)
            lineTo(tl.x + 16.5f * d, tl.y + 1.5f * d)
        },
        Palette5.PersianBlue,
        style = Stroke(width = 1.6f * d, join = StrokeJoin.Round),
    )
}

/** A cell tower with signal arcs. */
private fun DrawScope.cellTower(base: Offset, k: Float) {
    val top = base - Offset(0f, 112f * k)
    val c = Palette5.DuskBlue
    drawLine(c, base + Offset(-10f * k, 0f), top + Offset(-2f * k, 0f), 3f * k)
    drawLine(c, base + Offset(10f * k, 0f), top + Offset(2f * k, 0f), 3f * k)
    for (j in 0 until 4) {
        val t = (j + 0.5f) / 4f
        val y = base.y - 112f * k * t
        val half = (10f - 8f * t) * k
        drawLine(c, Offset(base.x - half, y), Offset(base.x + half, y), 2f * k)
    }
    drawRoundRect(Palette5.PersianBlue, top + Offset(-9f * k, -4f * k), Size(18f * k, 10f * k), CornerRadius(2f * k))
    drawCircle(PR.Amber, 3f * k, top + Offset(0f, -8f * k))
    listOf(14f, 24f, 34f).forEach { rr ->
        val sz = Size(rr * 2 * k, rr * 2 * k)
        val tl = top - Offset(rr * k, rr * k)
        drawArc(Palette5.IcyBlue, -50f, 100f, false, tl, sz, style = Stroke(2.5f * k, cap = StrokeCap.Round))
        drawArc(Palette5.IcyBlue, 130f, 100f, false, tl, sz, style = Stroke(2.5f * k, cap = StrokeCap.Round))
    }
}

/** A satellite dish on a stand. */
private fun DrawScope.dish(base: Offset, k: Float) {
    drawLine(Palette5.DeepNavy, base, base + Offset(0f, -24f * k), 4f * k)
    drawRoundRect(Palette5.DeepNavy, base + Offset(-10f * k, -4f * k), Size(20f * k, 5f * k), CornerRadius(2f * k))
    val c = base + Offset(0f, -38f * k)
    drawArc(Color.White, 10f, 160f, true, c - Offset(22f * k, 22f * k), Size(44f * k, 44f * k))
    drawArc(Palette5.DuskBlue, 10f, 160f, false, c - Offset(22f * k, 22f * k), Size(44f * k, 44f * k), style = Stroke(3f * k))
    drawLine(Palette5.DeepNavy, c, c + Offset(12f * k, -18f * k), 2f * k)
    drawCircle(PR.Amber, 3.5f * k, c + Offset(12f * k, -18f * k))
}

/** A data center: a block with rows of blinking server LEDs. */
private fun DrawScope.dataCenter(base: Offset, bw: Float, bh: Float) {
    val tl = base - Offset(bw / 2, bh)
    drawRoundRect(Palette5.PersianBlue, tl, Size(bw, bh), CornerRadius(5f))
    drawRect(Palette5.DeepNavy, tl, Size(bw, bh * 0.14f))
    for (row in 0 until 3) {
        for (col in 0 until 6) {
            drawCircle(
                if ((row + col) % 3 == 0) PR.Amber else Palette5.IcyBlue,
                bw * 0.028f,
                tl + Offset(bw * (0.16f + col * 0.136f), bh * (0.3f + row * 0.16f)),
            )
        }
    }
    drawRoundRect(Palette5.DeepNavy, tl + Offset(bw * 0.4f, bh * 0.72f), Size(bw * 0.2f, bh * 0.28f), CornerRadius(3f))
}

/** An office block with a roof antenna. */
private fun DrawScope.office(base: Offset, bw: Float, bh: Float) {
    val tl = base - Offset(bw / 2, bh)
    drawRoundRect(Palette5.DuskBlue, tl, Size(bw, bh), CornerRadius(3f))
    for (cx in 0 until 3) for (ry in 0 until 5) {
        drawRect(Palette5.IcyBlue, tl + Offset(bw * (0.12f + cx * 0.29f), bh * (0.07f + ry * 0.18f)), Size(bw * 0.18f, bh * 0.1f))
    }
    drawLine(Palette5.DeepNavy, tl + Offset(bw * 0.5f, 0f), tl + Offset(bw * 0.5f, -14f), 2.5f)
    drawCircle(PR.Amber, 3f, tl + Offset(bw * 0.5f, -15f))
}

/** A shop with a striped awning. */
private fun DrawScope.shop(base: Offset, bw: Float, bh: Float) {
    val tl = base - Offset(bw / 2, bh)
    drawRoundRect(BandColor, tl, Size(bw, bh), CornerRadius(3f))
    val stripes = 5
    for (s in 0 until stripes) {
        drawRect(
            if (s % 2 == 0) Palette5.PersianBlue else Color.White,
            tl + Offset(bw * s / stripes, 0f),
            Size(bw / stripes, bh * 0.26f),
        )
    }
    drawRect(Palette5.IcyBlue, tl + Offset(bw * 0.1f, bh * 0.42f), Size(bw * 0.4f, bh * 0.36f))
    drawRoundRect(Palette5.DuskBlue, tl + Offset(bw * 0.62f, bh * 0.42f), Size(bw * 0.26f, bh * 0.58f), CornerRadius(2f))
}

/** A small house with a roof. */
private fun DrawScope.house(base: Offset, bw: Float, bh: Float) {
    val tl = base - Offset(bw / 2, bh * 0.62f)
    drawRect(Palette5.IcyBlue, tl, Size(bw, bh * 0.62f))
    drawPath(
        Path().apply {
            moveTo(tl.x - bw * 0.08f, tl.y); lineTo(tl.x + bw / 2, tl.y - bh * 0.42f); lineTo(tl.x + bw * 1.08f, tl.y); close()
        },
        Palette5.DuskBlue,
    )
    drawRoundRect(Palette5.PersianBlue, tl + Offset(bw * 0.4f, bh * 0.22f), Size(bw * 0.2f, bh * 0.4f), CornerRadius(2f))
    drawRect(Palette5.Cornflower, tl + Offset(bw * 0.1f, bh * 0.14f), Size(bw * 0.18f, bw * 0.18f))
    drawRect(Palette5.Cornflower, tl + Offset(bw * 0.72f, bh * 0.14f), Size(bw * 0.18f, bw * 0.18f))
}

/** Mom's home at the end of the road, with an antenna and a flag. */
private fun DrawScope.momsHome(base: Offset, k: Float) {
    house(base, 78f * k, 66f * k)
    val roof = base + Offset(0f, -66f * k * 0.62f - 66f * k * 0.42f)
    drawLine(Palette5.DeepNavy, roof, roof + Offset(0f, -26f * k), 3f * k)
    drawCircle(PR.Amber, 4f * k, roof + Offset(0f, -28f * k))
    listOf(12f, 22f).forEach { rr ->
        val tl = roof + Offset(-rr * k, -28f * k - rr * k)
        val sz = Size(rr * 2 * k, rr * 2 * k)
        drawArc(Palette5.IcyBlue, -50f, 100f, false, tl, sz, style = Stroke(2.5f * k, cap = StrokeCap.Round))
        drawArc(Palette5.IcyBlue, 130f, 100f, false, tl, sz, style = Stroke(2.5f * k, cap = StrokeCap.Round))
    }
}

private fun DrawScope.lamp(base: Offset, k: Float) {
    drawLine(Palette5.DeepNavy, base, base + Offset(0f, -62f * k), strokeWidth = 4f * k)
    drawRoundRect(Palette5.DeepNavy, base + Offset(-9f * k, -86f * k), Size(18f * k, 26f * k), CornerRadius(4f * k))
    drawRoundRect(Palette5.IcyBlue, base + Offset(-5f * k, -81f * k), Size(10f * k, 16f * k), CornerRadius(3f * k))
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
