package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Colors of the town art. Kept in one place so a SpriteProvider can restyle it later. */
object TownColors {
    val GrassLight = Color(0xFFA9D8A4)
    val GrassDark = Color(0xFF9CCF98)
    val Road = Color(0xFF98A2BB)
    val TileEdge = Color(0x22000000)
    val Trunk = Color(0xFF7A4B2A)
    val LeafDark = Color(0xFF3F8F49)
    val Leaf = Color(0xFF55AA57)
    val LeafLight = Color(0xFF79C871)
    val Window = Color(0xFFD6E7FD)
    val Marker = Color(0xFFEF9F27)
    val Shadow = Color(0x40091540)
    val Skin = Color(0xFFF2C9A0)
    val Water = Color(0xFF86BDEB)
    val WaterLight = Color(0xFFCFE6FA)
    val Wood = Color(0xFFB98A55)
    val WoodDark = Color(0xFF8A6238)
    val HillDark = Color(0xFF86BE82)
    val HillLight = Color(0xFFB4DDA8)
}

/** The path of a diamond (a tile seen from above in isometric view) around [center]. */
internal fun diamondPath(center: Offset, w: Float, h: Float) = Path().apply {
    moveTo(center.x, center.y - h / 2)
    lineTo(center.x + w / 2, center.y)
    lineTo(center.x, center.y + h / 2)
    lineTo(center.x - w / 2, center.y)
    close()
}

fun DrawScope.drawDiamond(center: Offset, w: Float, h: Float, color: Color) {
    drawPath(diamondPath(center, w, h), color)
    drawPath(diamondPath(center, w, h), TownColors.TileEdge, style = Stroke(width = 1f))
}

/** A diamond without an edge line, for the scenery outside the town (cheaper, and it can fade out). */
fun DrawScope.drawDiamondFlat(center: Offset, w: Float, h: Float, color: Color) {
    drawPath(diamondPath(center, w, h), color)
}

fun DrawScope.drawDiamondOutline(center: Offset, w: Float, h: Float, color: Color, strokeWidth: Float) {
    drawPath(diamondPath(center, w, h), color, style = Stroke(width = strokeWidth))
}

/**
 * A box standing on the tile at [center] (the center of its ground diamond).
 * [boxHeight] is how tall it is in pixels. Left and right faces are shaded differently for depth.
 */
fun DrawScope.drawIsoBox(
    center: Offset,
    w: Float,
    h: Float,
    boxHeight: Float,
    top: Color,
    left: Color,
    right: Color,
) {
    val lift = Offset(0f, -boxHeight)
    val leftFace = Path().apply {
        moveTo(center.x - w / 2, center.y)
        lineTo(center.x, center.y + h / 2)
        lineTo(center.x, center.y + h / 2 - boxHeight)
        lineTo(center.x - w / 2, center.y - boxHeight)
        close()
    }
    val rightFace = Path().apply {
        moveTo(center.x + w / 2, center.y)
        lineTo(center.x, center.y + h / 2)
        lineTo(center.x, center.y + h / 2 - boxHeight)
        lineTo(center.x + w / 2, center.y - boxHeight)
        close()
    }
    drawPath(leftFace, left)
    drawPath(rightFace, right)
    drawPath(diamondPath(center + lift, w, h), top)
}

/**
 * The player: a small network engineer with a hard hat. [ground] is where the feet stand;
 * [tileWidth] is the size of a tile in pixels, which everything scales with.
 * [time] drives the animation: while [moving] the body bobs with each step; standing still it breathes and,
 * every few seconds, waves.
 */
fun DrawScope.drawPlayer(ground: Offset, tileWidth: Float, time: Float = 0f, moving: Boolean = false) {
    val s = tileWidth / 100f
    val bob = if (moving) -abs(sin(time * 9f)) * 2.2f * s else sin(time * 2f) * 0.9f * s
    // shadow
    drawOval(TownColors.Shadow, Offset(ground.x - 14 * s, ground.y - 5 * s), Size(28 * s, 10 * s))
    val up = Offset(0f, bob)
    // arms: one hangs, the other swings when walking and waves now and then when standing
    val waving = !moving && (time % 7f) < 1.4f
    val swing = if (moving) sin(time * 9f) * 5f * s else 0f
    drawLine(Palette5.DuskBlue, Offset(ground.x - 9 * s, ground.y - 31 * s) + up, Offset(ground.x - 11 * s, ground.y - 19 * s + swing) + up, strokeWidth = 4 * s, cap = StrokeCap.Round)
    val hand = if (waving) Offset(ground.x + 15 * s, ground.y - 41 * s + sin(time * 14f) * 3 * s) else Offset(ground.x + 11 * s, ground.y - 19 * s - swing)
    drawLine(Palette5.DuskBlue, Offset(ground.x + 9 * s, ground.y - 31 * s) + up, hand + up, strokeWidth = 4 * s, cap = StrokeCap.Round)
    // body
    drawRoundRect(Palette5.PersianBlue, Offset(ground.x - 9 * s, ground.y - 36 * s) + up, Size(18 * s, 28 * s), CornerRadius(7 * s))
    // head
    drawCircle(TownColors.Skin, 9 * s, Offset(ground.x, ground.y - 44 * s) + up)
    // hard hat
    drawRoundRect(PR.Amber, Offset(ground.x - 10.5f * s, ground.y - 57 * s) + up, Size(21 * s, 10 * s), CornerRadius(5 * s))
    // eyes
    drawCircle(Palette5.DeepNavy, 1.4f * s, Offset(ground.x - 3.5f * s, ground.y - 43 * s) + up)
    drawCircle(Palette5.DeepNavy, 1.4f * s, Offset(ground.x + 3.5f * s, ground.y - 43 * s) + up)
}

private val ShirtColors = listOf(Color(0xFFE24B4A), Color(0xFF2F8F57), Color(0xFFEF9F27), Color(0xFF8E6CEF))
private val HairColors = listOf(Color(0xFF3B2A20), Color(0xFF8A5A2B), Color(0xFF1E1E2C), Color(0xFFC98A3D))

/** A neighbor out for a walk: a little person in a colored shirt whose legs swing. [shirt] picks the colors. */
fun DrawScope.drawNeighbor(ground: Offset, tileWidth: Float, shirt: Int, time: Float) {
    val s = tileWidth / 100f
    val k = shirt.mod(ShirtColors.size)
    val bob = -abs(sin(time * 7f + shirt)) * 1.6f * s
    val step = sin(time * 7f + shirt) * 4f * s
    drawOval(TownColors.Shadow, Offset(ground.x - 10 * s, ground.y - 4 * s), Size(20 * s, 8 * s))
    drawLine(Palette5.DeepNavy, Offset(ground.x - 3 * s, ground.y - 12 * s), Offset(ground.x - 3 * s + step, ground.y - 1 * s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
    drawLine(Palette5.DeepNavy, Offset(ground.x + 3 * s, ground.y - 12 * s), Offset(ground.x + 3 * s - step, ground.y - 1 * s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
    val up = Offset(0f, bob)
    drawRoundRect(ShirtColors[k], Offset(ground.x - 7 * s, ground.y - 29 * s) + up, Size(14 * s, 19 * s), CornerRadius(5 * s))
    drawCircle(TownColors.Skin, 6.5f * s, Offset(ground.x, ground.y - 35 * s) + up)
    drawArc(HairColors[k], 180f, 180f, true, Offset(ground.x - 6.5f * s, ground.y - 41.5f * s) + up, Size(13 * s, 13 * s))
}
