package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avabuilds.packetrunner.game.IsoCamera
import com.avabuilds.packetrunner.game.IsoMath
import com.avabuilds.packetrunner.game.LinkStatus
import com.avabuilds.packetrunner.game.PacketFlow
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.game.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Colors of the network: a healthy link is green, a slow one amber, a broken one red, a missing one a faint dashed line. */
object NetworkColors {
    val Ok = Color(0xFF2FBF71)
    val Slow = Color(0xFFEF9F27)
    val Broken = Color(0xFFE24B4A)
    val Idle = Color(0xFF3D518C)
    val Spark = Color(0xFFFFE08A)

    fun of(status: LinkStatus): Color = when (status) {
        LinkStatus.HEALTHY -> Ok
        LinkStatus.DEGRADED -> Slow
        LinkStatus.DOWN -> Broken
        LinkStatus.NO_LINK -> Idle
    }
}

/** Where things are on the screen right now: the camera and the size of the view, plus the town's tile math. */
internal class ViewTransform(val camera: IsoCamera, private val iso: IsoMath, val baseTileWidth: Float, val size: Size) {
    private val center = Vec2(size.width / 2f, size.height * VIEW_CENTER_Y)

    /** The on-screen width of one tile. */
    val tw: Float = baseTileWidth * camera.scale
    val th: Float get() = tw / 2f

    fun screenOf(x: Float, y: Float): Offset = camera.worldToScreen(iso.tileCenter(x, y), center).let { Offset(it.x, it.y) }

    fun onScreen(at: Offset) = at.x in -tw * 2..size.width + tw * 2 && at.y in -tw * 4..size.height + tw * 2

    companion object {
        /** The middle of the town sits a little above the middle of the screen, so the cards at the bottom cover less of it. */
        const val VIEW_CENTER_Y = 0.42f
    }
}

private const val VIEW_CENTER_Y = ViewTransform.VIEW_CENTER_Y

/** A cable as a line on the screen, with the length up to each corner so a packet can be placed anywhere along it. */
internal class Polyline(val points: List<Offset>) {
    val cumulative = FloatArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    }
    val length: Float get() = cumulative.last()

    /** The point [progress] (0 to 1) of the way along the line. */
    fun at(progress: Float): Offset {
        if (points.size == 1 || length == 0f) return points.first()
        val target = progress.coerceIn(0f, 1f) * length
        var i = 1
        while (i < points.size - 1 && cumulative[i] < target) i++
        val segment = cumulative[i] - cumulative[i - 1]
        val t = if (segment == 0f) 0f else (target - cumulative[i - 1]) / segment
        return Offset(points[i - 1].x + (points[i].x - points[i - 1].x) * t, points[i - 1].y + (points[i].y - points[i - 1].y) * t)
    }
}

/**
 * The visible network. Every building with a network has a pulsing ring of Wi-Fi around it (the office has a big one);
 * a cable runs along the roads from the office to every building, with glowing packets flowing along it.
 * A healthy link is green, a slow one amber with some packets lost on the way, a broken one red with sparks where it stops.
 * A building without a network has a faint dashed line, waiting to be connected. [seconds] is the one shared animation clock.
 */
internal fun DrawScope.drawNetwork(
    view: ViewTransform,
    town: TownMap,
    cables: List<com.avabuilds.packetrunner.game.CableLink>,
    statuses: Map<String, LinkStatus>,
    seconds: Float,
) {
    val tw = view.tw
    val phase = (seconds / 2.8f) % 1f

    // Wi-Fi rings: expanding flattened circles on the ground, fading as they grow
    town.buildings.forEach { b ->
        val isOffice = b.id == OFFICE_ID
        val status = if (isOffice) LinkStatus.HEALTHY else statuses[b.id] ?: LinkStatus.NO_LINK
        if (status == LinkStatus.NO_LINK) return@forEach
        val c = view.screenOf(b.center.x, b.center.y)
        if (!view.onScreen(c)) return@forEach
        val color = if (isOffice) Palette5.PersianBlue else NetworkColors.of(status)
        val maxR = tw * (if (isOffice) 3.4f else 2.1f)
        for (k in 0 until 3) {
            val p = ((seconds / 3.2f) + k / 3f) % 1f
            val r = p * maxR
            drawOval(color.copy(alpha = (1f - p) * 0.14f), c - Offset(r, r / 2f), Size(2 * r, r))
            drawOval(color.copy(alpha = (1f - p) * 0.8f), c - Offset(r, r / 2f), Size(2 * r, r), style = Stroke(width = tw * 0.05f))
        }
    }

    // Cables and the packets on them
    val lanes = cables.size
    cables.forEachIndexed { index, link ->
        val status = statuses[link.buildingId] ?: LinkStatus.NO_LINK
        val lane = (index - (lanes - 1) / 2f) * tw * 0.05f // neighbouring cables run side by side on a shared road
        val line = Polyline(link.path.map { view.screenOf(it.x.toFloat(), it.y.toFloat()) + Offset(0f, lane) })
        if (line.points.none { view.onScreen(it) }) return@forEachIndexed
        val path = Path().apply {
            moveTo(line.points.first().x, line.points.first().y)
            line.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val color = NetworkColors.of(status)
        if (status == LinkStatus.NO_LINK) {
            drawPath(path, color.copy(alpha = 0.45f), style = Stroke(tw * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(tw * 0.12f, tw * 0.16f), 0f)))
            return@forEachIndexed
        }
        drawPath(path, color.copy(alpha = 0.3f), style = Stroke(tw * 0.26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, color, style = Stroke(tw * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, tw * 0.13f, line.points.last()) // the box at the building's door
        drawCircle(color, tw * 0.09f, line.points.last())

        PacketFlow.dots(status, phase).forEach { dot ->
            val at = line.at(dot.progress)
            if (dot.lost) {
                drawCircle(NetworkColors.Broken.copy(alpha = 0.7f), tw * 0.07f, at)
            } else {
                drawCircle(color.copy(alpha = 0.4f), tw * 0.17f, at)
                drawCircle(Color.White, tw * 0.07f, at)
            }
        }
        if (status == LinkStatus.DOWN) drawSparks(line.at(0.35f), tw, seconds, index)
    }
}

/** A few short bright lines around [at], different every ~1/8 s, like a cut cable. */
private fun DrawScope.drawSparks(at: Offset, tw: Float, seconds: Float, seed: Int) {
    val frame = (seconds * 8f).toInt()
    for (i in 0 until 5) {
        val h = (frame * 31 + i * 17 + seed * 13) % 360
        val angle = h * PI.toFloat() / 180f
        val inner = tw * 0.05f
        val outer = tw * (0.2f + 0.16f * ((h % 7) / 7f))
        drawLine(
            if (i % 2 == 0) NetworkColors.Spark else Color.White,
            at + Offset(cos(angle) * inner, sin(angle) * inner * 0.6f),
            at + Offset(cos(angle) * outer, sin(angle) * outer * 0.6f),
            strokeWidth = tw * 0.04f, cap = StrokeCap.Round,
        )
    }
    drawCircle(NetworkColors.Broken.copy(alpha = 0.5f + 0.3f * (frame % 2)), tw * 0.1f, at)
}

private const val OFFICE_ID = "office"
