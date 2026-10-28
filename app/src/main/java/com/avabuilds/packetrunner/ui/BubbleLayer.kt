package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Alert
import com.avabuilds.packetrunner.game.BuildingBubble
import com.avabuilds.packetrunner.game.Mood
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.ui.sprites.labelHeightInTiles
import kotlin.math.abs
import kotlin.math.sin

/** The text style of a building's name label; it grows a little when the player zooms in. */
internal fun labelStyle(isTarget: Boolean, cameraScale: Float) = TextStyle(
    fontSize = (11f * cameraScale.coerceIn(0.8f, 1.5f)).sp,
    fontWeight = FontWeight.Bold,
    color = if (isTarget) Color.White else PR.Ink,
)

internal fun labelText(name: String, isTarget: Boolean) = if (isTarget) "! $name" else name

/** The size of the pill behind a building's name: the text plus a little padding. */
internal fun DrawScope.labelPillSize(measurer: TextMeasurer, text: String, style: TextStyle): Size {
    val m = measurer.measure(text, style)
    return Size(m.size.width + 16f * density, m.size.height + 6f * density)
}

private val FaceHappy = Color(0xFFFFD25E)
private val FaceDelighted = Color(0xFFFFC233)
private val FaceNeutral = Color(0xFFFFE2B5)
private val FaceWorried = Color(0xFF9EC5F2)
private val Ink = Palette5.DeepNavy

/**
 * The bubbles above the buildings, drawn on top of everything (and not darkened at night):
 *  - next to a building's name, a small badge with the neighbor's face and hearts;
 *  - above it, a phone that shakes while that neighbor is calling;
 *  - or a bouncing "!": red for a network problem, amber for a job you have taken on.
 */
internal fun DrawScope.drawBubbles(
    view: ViewTransform,
    town: TownMap,
    bubbles: List<BuildingBubble>,
    targetBuildingId: String?,
    measurer: TextMeasurer,
    seconds: Float,
) {
    val tw = view.tw
    bubbles.forEach { bubble ->
        val b = town.building(bubble.buildingId)
        val at = view.screenOf(b.center.x, b.center.y)
        if (!view.onScreen(at)) return@forEach

        val isTarget = b.id == targetBuildingId
        val style = labelStyle(isTarget, view.camera.scale)
        val pill = labelPillSize(measurer, labelText(b.name, isTarget), style)
        val pillTop = at.y - b.type.labelHeightInTiles() * tw - pill.height

        drawMoodBadge(bubble, Offset(at.x + pill.width / 2f + 4f * density, pillTop), pill.height, measurer, seconds)

        val above = Offset(at.x, pillTop - 0.05f * tw)
        when {
            bubble.ringing -> drawPhoneBubble(above, tw, seconds)
            bubble.alert != Alert.NONE -> drawBangBubble(above, tw, seconds, if (bubble.alert == Alert.PROBLEM) PR.Red else PR.Amber)
        }
    }
}

/** A small white badge: the neighbor's face on the left, a heart and the number of hearts on the right. */
private fun DrawScope.drawMoodBadge(bubble: BuildingBubble, topLeft: Offset, height: Float, measurer: TextMeasurer, seconds: Float) {
    val text = measurer.measure("♥${bubble.hearts}", TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PR.Red))
    val width = height + text.size.width + 8f * density
    drawRoundRect(Color.White.copy(alpha = 0.93f), topLeft, Size(width, height), CornerRadius(height / 2f))
    val r = height * 0.36f
    val faceCenter = topLeft + Offset(height / 2f, height / 2f)
    drawFace(faceCenter, r, bubble.mood, seconds)
    drawText(text, topLeft = topLeft + Offset(height - 1f * density, (height - text.size.height) / 2f))
}

/** A round face whose color and mouth show the [mood]. A worried face has a drop of sweat; a delighted one blushes. */
private fun DrawScope.drawFace(c: Offset, r: Float, mood: Mood, seconds: Float) {
    val skin = when (mood) {
        Mood.WORRIED -> FaceWorried
        Mood.NEUTRAL -> FaceNeutral
        Mood.HAPPY -> FaceHappy
        Mood.DELIGHTED -> FaceDelighted
    }
    drawCircle(skin, r, c)
    drawCircle(Ink.copy(alpha = 0.35f), r, c, style = Stroke(r * 0.1f))
    val eyeY = c.y - r * 0.2f
    drawCircle(Ink, r * 0.1f, Offset(c.x - r * 0.36f, eyeY))
    drawCircle(Ink, r * 0.1f, Offset(c.x + r * 0.36f, eyeY))
    val mouth = Stroke(r * 0.12f, cap = StrokeCap.Round)
    when (mood) {
        Mood.NEUTRAL -> drawLine(Ink, Offset(c.x - r * 0.3f, c.y + r * 0.42f), Offset(c.x + r * 0.3f, c.y + r * 0.42f), strokeWidth = r * 0.12f, cap = StrokeCap.Round)
        Mood.HAPPY -> drawArc(Ink, 20f, 140f, false, Offset(c.x - r * 0.42f, c.y - r * 0.05f), Size(r * 0.84f, r * 0.66f), style = mouth)
        Mood.DELIGHTED -> {
            drawArc(Ink, 15f, 150f, false, Offset(c.x - r * 0.5f, c.y - r * 0.12f), Size(r * 1.0f, r * 0.8f), style = mouth)
            drawCircle(Color(0xFFFF8A7A).copy(alpha = 0.7f), r * 0.14f, Offset(c.x - r * 0.62f, c.y + r * 0.2f))
            drawCircle(Color(0xFFFF8A7A).copy(alpha = 0.7f), r * 0.14f, Offset(c.x + r * 0.62f, c.y + r * 0.2f))
        }
        Mood.WORRIED -> {
            drawArc(Ink, 200f, 140f, false, Offset(c.x - r * 0.4f, c.y + r * 0.3f), Size(r * 0.8f, r * 0.55f), style = mouth)
            val drop = c + Offset(r * 0.8f, -r * 0.5f + abs(sin(seconds * 3f)) * r * 0.25f)
            drawCircle(Color(0xFF6FB6F2), r * 0.17f, drop)
        }
    }
}

/** The bubble's pointed tail, ending at [tip], used by both kinds of bubble. */
private fun DrawScope.drawTail(tip: Offset, half: Float, height: Float, color: Color) {
    drawPath(
        Path().apply {
            moveTo(tip.x - half, tip.y - height)
            lineTo(tip.x + half, tip.y - height)
            lineTo(tip.x, tip.y)
            close()
        },
        color,
    )
}

/** A white bubble with a phone that shakes and sends out ringing arcs. [tail] is where the bubble points (just above the name). */
private fun DrawScope.drawPhoneBubble(tail: Offset, tw: Float, seconds: Float) {
    val r = tw * 0.36f
    val center = tail - Offset(0f, r * 1.25f)
    drawTail(tail, r * 0.28f, r * 0.45f, Color.White)
    drawCircle(Color.White, r, center)
    drawCircle(PR.Amber, r, center, style = Stroke(r * 0.1f))
    val shake = sin(seconds * 24f) * 14f
    rotate(shake, center) {
        val w = r * 0.5f
        val h = r * 0.85f
        drawRoundRect(Ink, center - Offset(w / 2f, h / 2f), Size(w, h), CornerRadius(w * 0.25f))
        drawRoundRect(Palette5.IcyBlue, center - Offset(w * 0.36f, h * 0.38f), Size(w * 0.72f, h * 0.6f), CornerRadius(w * 0.1f))
        drawCircle(Palette5.Cornflower, w * 0.07f, center + Offset(0f, h * 0.38f))
    }
    // two ringing arcs on each side that pulse
    val pulse = (seconds * 2.5f) % 1f
    for (k in 0 until 2) {
        val p = (pulse + k * 0.5f) % 1f
        val rr = r * (0.7f + 0.35f * p)
        val alpha = (1f - p) * 0.9f
        val tl = center - Offset(rr, rr)
        val size = Size(2 * rr, 2 * rr)
        val stroke = Stroke(r * 0.09f, cap = StrokeCap.Round)
        drawArc(PR.Amber.copy(alpha = alpha), -45f, 30f, false, tl, size, style = stroke)
        drawArc(PR.Amber.copy(alpha = alpha), 195f, 30f, false, tl, size, style = stroke)
    }
}

/** A round bubble with a white "!" that bounces, squashing a little as it lands. */
private fun DrawScope.drawBangBubble(tail: Offset, tw: Float, seconds: Float, color: Color) {
    val bounce = abs(sin(seconds * 5f))
    val r = tw * 0.27f
    val lift = bounce * tw * 0.16f
    val squash = 1f - (1f - bounce) * 0.08f
    val center = tail - Offset(0f, r * 1.3f + lift)
    drawOval(Color(0x33091540), tail - Offset(r * 0.7f * (1f - bounce * 0.4f), -r * 0.05f), Size(r * 1.4f * (1f - bounce * 0.4f), r * 0.35f)) // shadow on the ground
    drawTail(tail - Offset(0f, lift * 0.5f), r * 0.32f, r * 0.55f, color)
    drawOval(color, center - Offset(r, r * squash), Size(2 * r, 2 * r * squash))
    drawOval(Color.White.copy(alpha = 0.9f), center - Offset(r, r * squash), Size(2 * r, 2 * r * squash), style = Stroke(r * 0.1f))
    drawLine(Color.White, center - Offset(0f, r * 0.5f), center + Offset(0f, r * 0.12f), strokeWidth = r * 0.3f, cap = StrokeCap.Round)
    drawCircle(Color.White, r * 0.17f, center + Offset(0f, r * 0.48f))
}
