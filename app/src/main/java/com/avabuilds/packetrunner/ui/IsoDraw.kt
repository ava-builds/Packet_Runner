package com.avabuilds.packetrunner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
}

/** The path of a diamond (a tile seen from above in isometric view) around [center]. */
private fun diamondPath(center: Offset, w: Float, h: Float) = Path().apply {
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
 */
fun DrawScope.drawPlayer(ground: Offset, tileWidth: Float) {
    val s = tileWidth / 100f
    // shadow
    drawOval(TownColors.Shadow, Offset(ground.x - 14 * s, ground.y - 5 * s), Size(28 * s, 10 * s))
    // body
    drawRoundRect(Palette5.PersianBlue, Offset(ground.x - 9 * s, ground.y - 36 * s), Size(18 * s, 28 * s), CornerRadius(7 * s))
    // head
    drawCircle(TownColors.Skin, 9 * s, Offset(ground.x, ground.y - 44 * s))
    // hard hat
    drawRoundRect(PR.Amber, Offset(ground.x - 10.5f * s, ground.y - 57 * s), Size(21 * s, 10 * s), CornerRadius(5 * s))
    // eyes
    drawCircle(Palette5.DeepNavy, 1.4f * s, Offset(ground.x - 3.5f * s, ground.y - 43 * s))
    drawCircle(Palette5.DeepNavy, 1.4f * s, Offset(ground.x + 3.5f * s, ground.y - 43 * s))
}
