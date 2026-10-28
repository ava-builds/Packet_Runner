package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.CableLink
import com.avabuilds.packetrunner.game.LinkStatus
import com.avabuilds.packetrunner.game.TownLife
import com.avabuilds.packetrunner.game.Vec2
import com.avabuilds.packetrunner.game.pointInPolygon
import com.avabuilds.packetrunner.ui.sprites.buildingSilhouette
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.game.keepRight
import com.avabuilds.packetrunner.ui.sprites.Sprite
import com.avabuilds.packetrunner.ui.sprites.SpriteKey
import com.avabuilds.packetrunner.ui.sprites.SpriteProvider
import kotlin.math.abs
import kotlin.math.sin

private val PipGreenColor = Color(0xFF5DCAA5)
private val PipDarkColor = Color(0xFF0F6E56)

/** How often Pip makes a trip along a cable, in seconds, and how fast he runs, in tiles per second. */
private const val PIP_PERIOD = 15f
private const val PIP_FIRST_TRIP_AT = 4f
private const val PIP_SPEED = 3.5f

/**
 * The town's everyday life, drawn over everything else: cloud shadows drifting across, smoke from the chimneys,
 * neighbors strolling on the pavement, cars driving on the roads, birds flying by, and now and then Pip
 * running along a cable. It all follows the one shared clock ([seconds]).
 */
internal fun DrawScope.drawLife(
    view: ViewTransform,
    sprites: SpriteProvider,
    town: TownMap,
    life: TownLife,
    cables: List<CableLink>,
    statuses: Map<String, LinkStatus>,
    seconds: Float,
) {
    val scope = this
    val tw = view.tw

    drawCloudShadows(view, seconds)

    // Chimney smoke: a few puffs that rise, grow and fade, drifting a little to the right
    town.buildings.filter { it.type == BuildingType.HOUSE }.forEach { b ->
        val base = view.screenOf(b.center.x, b.center.y) + Offset(0.215f * tw, -1.02f * tw)
        if (!view.onScreen(base)) return@forEach
        for (k in 0 until 4) {
            val p = ((seconds / 4f) + k / 4f + b.style * 0.21f) % 1f
            val at = base + Offset(p * 0.35f * tw + sin(seconds + k) * 0.03f * tw, -p * 0.9f * tw)
            drawCircle(Color.White.copy(alpha = (1f - p) * 0.55f), (0.04f + 0.09f * p) * tw, at)
        }
    }

    // A building in front of a person or car hides it. The layer is drawn over everything, so check it here:
    // is the point on the body inside the outline of a building that is nearer to the camera?
    // Three points on the body are tried; two inside the outline are enough to hide it.
    fun hiddenBehindBuilding(depth: Float, feet: Offset, height: Float): Boolean = town.buildings.any { b ->
        b.depth > depth && run {
            val outline = buildingSilhouette(b.type, view.screenOf(b.center.x, b.center.y).let { Vec2(it.x, it.y) }, tw, b.size)
            listOf(0.15f, 0.5f, 0.9f).count { f -> pointInPolygon(outline, Vec2(feet.x, feet.y - height * f)) } >= 2
        }
    }

    // People and cars, back to front by their position on the map so the nearer ones are drawn last
    class Mover(val depth: Float, val draw: () -> Unit)
    val movers = mutableListOf<Mover>()
    life.walkers.forEach { w ->
        val state = w.patrol.stateAt(seconds)
        val pos = keepRight(state.position, state.heading, w.sidewalk)
        val at = view.screenOf(pos.x, pos.y)
        if (view.onScreen(at) && !hiddenBehindBuilding(pos.x + pos.y, at, tw * 0.45f)) movers += Mover(pos.x + pos.y) {
            sprites.draw(scope, Sprite(SpriteKey.NEIGHBOR, at, tw, variant = w.shirt, time = seconds, moving = true))
        }
    }
    life.drivers.forEach { d ->
        val state = d.patrol.stateAt(seconds)
        val pos = keepRight(state.position, state.heading, d.lane)
        val at = view.screenOf(pos.x, pos.y)
        if (view.onScreen(at) && !hiddenBehindBuilding(pos.x + pos.y, at, tw * 0.3f)) movers += Mover(pos.x + pos.y) {
            sprites.draw(scope, Sprite(SpriteKey.CAR, at, tw, variant = d.color))
        }
    }
    movers.sortedBy { it.depth }.forEach { it.draw() }

    drawPipTrip(view, town, cables, statuses, seconds)
    drawBirds(view, seconds)
}

/** Soft dark ovals that slide slowly across the view, like the shadows of clouds. */
private fun DrawScope.drawCloudShadows(view: ViewTransform, seconds: Float) {
    val w = size.width
    val tw = view.tw
    for (k in 0 until 3) {
        val rx = tw * (2.6f + 0.5f * k)
        val speed = tw * (0.06f + 0.015f * k)
        val x = ((seconds * speed + k * w / 3f) % (w + 2 * rx)) - rx
        val y = size.height * (0.28f + 0.13f * k) + sin(seconds * 0.07f + k) * tw * 0.4f
        drawOval(Palette5.DeepNavy.copy(alpha = 0.07f), Offset(x - rx, y - rx * 0.32f), Size(2 * rx, rx * 0.64f))
    }
}

/** Birds: a few small V shapes crossing the view, flapping. */
private fun DrawScope.drawBirds(view: ViewTransform, seconds: Float) {
    val w = size.width
    val tw = view.tw
    for (k in 0 until 3) {
        val speed = tw * (0.55f + 0.12f * k)
        val x = ((seconds * speed + k * w * 0.37f) % (w + 200f)) - 100f
        val y = size.height * (0.16f + 0.05f * k) + sin(seconds * 0.8f + k * 2f) * tw * 0.25f
        val flap = sin(seconds * 9f + k * 1.7f)
        val wing = tw * 0.16f
        val tip = -flap * wing * 0.7f
        val c = Offset(x, y)
        drawLine(Palette5.DeepNavy.copy(alpha = 0.75f), c, c + Offset(-wing, tip), strokeWidth = tw * 0.03f, cap = StrokeCap.Round)
        drawLine(Palette5.DeepNavy.copy(alpha = 0.75f), c, c + Offset(wing, tip), strokeWidth = tw * 0.03f, cap = StrokeCap.Round)
    }
}

/**
 * Every [PIP_PERIOD] seconds Pip runs from the office along the cable of one connected building and disappears at its door,
 * leaving a glowing trail. The building changes with each trip.
 */
private fun DrawScope.drawPipTrip(view: ViewTransform, town: TownMap, cables: List<CableLink>, statuses: Map<String, LinkStatus>, seconds: Float) {
    val live = cables.filter { (statuses[it.buildingId] ?: LinkStatus.NO_LINK) != LinkStatus.NO_LINK }
    if (live.isEmpty()) return
    val trip = (seconds / PIP_PERIOD).toInt()
    val link = live[trip % live.size]
    val duration = (link.path.size - 1) / PIP_SPEED
    val into = seconds % PIP_PERIOD - PIP_FIRST_TRIP_AT
    if (into < 0f || into > duration + 0.6f) return

    val tw = view.tw
    val line = Polyline(link.path.map { view.screenOf(it.x.toFloat(), it.y.toFloat()) })
    if (line.points.none { view.onScreen(it) }) return
    val progress = (into / duration).coerceIn(0f, 1f)
    val fade = if (into > duration) 1f - (into - duration) / 0.6f else 1f

    // trail
    for (k in 1..6) {
        val back = (progress - k * 0.035f).coerceAtLeast(0f)
        drawCircle(PipGreenColor.copy(alpha = fade * (0.45f - k * 0.06f).coerceAtLeast(0f)), tw * (0.12f - k * 0.012f), line.at(back))
    }
    val ground = line.at(progress)
    val tile = link.path[(progress * (link.path.size - 1)).toInt().coerceIn(0, link.path.size - 1)]
    val tileDepth = (tile.x + tile.y).toFloat()
    if (town.buildings.any { b -> b.depth > tileDepth && pointInPolygon(buildingSilhouette(b.type, view.screenOf(b.center.x, b.center.y).let { Vec2(it.x, it.y) }, tw, b.size), Vec2(ground.x, ground.y - tw * 0.2f)) }) return
    val at = ground - Offset(0f, tw * 0.13f + abs(sin(seconds * 12f)) * tw * 0.03f)
    val u = tw * 0.42f / 80f
    val a = fade
    drawRoundRect(PipGreenColor.copy(alpha = a), at - Offset(30 * u, 25 * u), Size(60 * u, 50 * u), CornerRadius(16 * u))
    drawLine(PipDarkColor.copy(alpha = a), at + Offset(-26 * u, -17 * u), at + Offset(0f, -2 * u), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
    drawLine(PipDarkColor.copy(alpha = a), at + Offset(26 * u, -17 * u), at + Offset(0f, -2 * u), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
    for (dx in listOf(-10f, 10f)) {
        drawCircle(Color.White.copy(alpha = a), 5 * u, at + Offset(dx * u, 8 * u))
        drawCircle(Palette5.DeepNavy.copy(alpha = a), 2.5f * u, at + Offset(dx * u, 8 * u))
    }
}
