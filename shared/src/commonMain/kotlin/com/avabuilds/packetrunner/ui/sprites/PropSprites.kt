package com.avabuilds.packetrunner.ui.sprites

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.TownColors
import com.avabuilds.packetrunner.ui.diamondPath
import com.avabuilds.packetrunner.ui.drawDiamond
import com.avabuilds.packetrunner.ui.drawDiamondFlat
import com.avabuilds.packetrunner.ui.drawIsoBox

// The scenery and the props of the town, all drawn with the Canvas. Each function draws on the tile whose
// ground diamond is centered on sprite.anchor; one tile is sprite.tileWidth wide and half as tall.

/** A ground tile: the town's tiles have an edge line, the faded tiles of the world outside do not. */
internal fun DrawScope.drawGround(sprite: Sprite, color: Color) {
    val tw = sprite.tileWidth
    if (sprite.alpha >= 1f) drawDiamond(sprite.anchor, tw, tw / 2, color)
    else drawDiamondFlat(sprite.anchor, tw * 1.02f, tw * 0.51f, color.copy(alpha = sprite.alpha))
}

/** River water with a few light ripples; [Sprite.variant] moves the ripples so the river does not look tiled. */
internal fun DrawScope.drawWater(sprite: Sprite) {
    val tw = sprite.tileWidth
    val a = sprite.alpha
    drawDiamondFlat(sprite.anchor, tw * 1.02f, tw * 0.51f, TownColors.Water.copy(alpha = a))
    val shift = (sprite.variant % 3 - 1) * tw * 0.12f
    listOf(-0.16f, 0.1f).forEachIndexed { i, dy ->
        val c = sprite.anchor + Offset(shift * (if (i == 0) 1 else -1), dy * tw)
        drawLine(TownColors.WaterLight.copy(alpha = a), c - Offset(tw * 0.14f, 0f), c + Offset(tw * 0.14f, 0f), strokeWidth = tw * 0.025f, cap = StrokeCap.Round)
    }
}

/** A wooden bridge tile; the bridge runs along y (toward the lower left of the screen), with a rail on each side. */
internal fun DrawScope.drawBridge(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = tw
    val h = tw / 2
    val a = sprite.alpha
    val c = sprite.anchor
    drawPath(diamondPath(c, w, h), TownColors.Wood.copy(alpha = a))
    // planks across the bridge
    for (k in -1..1) {
        val m = c + Offset(-w * 0.12f * k, h * 0.12f * k)
        drawLine(TownColors.WoodDark.copy(alpha = a * 0.6f), m + Offset(w * 0.22f, h * 0.22f), m - Offset(w * 0.22f, h * 0.22f), strokeWidth = tw * 0.02f)
    }
    // rails: along the two edges that run in the y direction, raised a little
    val rail = Offset(0f, -tw * 0.14f)
    val top = c + Offset(0f, -h / 2)
    val left = c + Offset(-w / 2, 0f)
    val right = c + Offset(w / 2, 0f)
    val bottom = c + Offset(0f, h / 2)
    drawLine(TownColors.WoodDark.copy(alpha = a), top + rail, left + rail, strokeWidth = tw * 0.04f, cap = StrokeCap.Round)
    drawLine(TownColors.WoodDark.copy(alpha = a), right + rail, bottom + rail, strokeWidth = tw * 0.04f, cap = StrokeCap.Round)
    for (p in listOf(top, left, right, bottom)) drawLine(TownColors.WoodDark.copy(alpha = a), p, p + rail, strokeWidth = tw * 0.04f)
}

/** A soft hill. [Sprite.variant] is its radius in tenths of a tile; it is drawn as a few stacked green ovals. */
internal fun DrawScope.drawHill(sprite: Sprite) {
    val tw = sprite.tileWidth
    val r = sprite.variant / 10f * tw
    val a = sprite.alpha
    val c = sprite.anchor
    drawOval(TownColors.HillDark.copy(alpha = a), c - Offset(r * 0.9f, r * 0.42f), Size(r * 1.8f, r * 0.84f))
    drawOval(TownColors.HillDark.copy(alpha = a), c - Offset(r * 0.7f, r * 0.62f), Size(r * 1.4f, r * 0.8f))
    drawOval(TownColors.HillLight.copy(alpha = a), c - Offset(r * 0.45f, r * 0.72f), Size(r * 0.9f, r * 0.5f))
}

internal fun DrawScope.drawStreetlight(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    val top = c - Offset(0f, tw * 0.78f)
    drawOval(TownColors.Shadow, c - Offset(tw * 0.1f, tw * 0.03f), Size(tw * 0.2f, tw * 0.06f))
    drawLine(Palette5.DeepNavy, c, top, strokeWidth = tw * 0.035f, cap = StrokeCap.Round)
    drawLine(Palette5.DeepNavy, top, top + Offset(tw * 0.1f, -tw * 0.03f), strokeWidth = tw * 0.03f, cap = StrokeCap.Round)
    val lamp = top + Offset(tw * 0.1f, 0f)
    drawCircle(PR.Amber.copy(alpha = 0.25f), tw * 0.13f, lamp)
    drawCircle(Color(0xFFFFE9A8), tw * 0.05f, lamp)
}

internal fun DrawScope.drawBench(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    drawOval(TownColors.Shadow, c - Offset(tw * 0.28f, tw * 0.07f), Size(tw * 0.56f, tw * 0.14f))
    // two legs, a seat, and a back on the far side
    drawLine(TownColors.WoodDark, c + Offset(-tw * 0.16f, 0f), c + Offset(-tw * 0.16f, -tw * 0.1f), strokeWidth = tw * 0.03f)
    drawLine(TownColors.WoodDark, c + Offset(tw * 0.16f, 0f), c + Offset(tw * 0.16f, -tw * 0.1f), strokeWidth = tw * 0.03f)
    drawIsoBox(c - Offset(0f, tw * 0.1f), tw * 0.5f, tw * 0.2f, tw * 0.04f, TownColors.Wood, TownColors.WoodDark, Color(0xFFA6763F))
    val backY = if (sprite.variant == 0) -tw * 0.04f else tw * 0.0f
    drawLine(TownColors.Wood, c + Offset(-tw * 0.2f, -tw * 0.2f + backY), c + Offset(tw * 0.2f, -tw * 0.2f + backY), strokeWidth = tw * 0.05f, cap = StrokeCap.Round)
}

/** Three pickets and two rails along x (variant 0) or along y (variant 1). */
internal fun DrawScope.drawFence(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    val along = if (sprite.variant == 0) Offset(tw * 0.5f, tw * 0.25f) else Offset(-tw * 0.5f, tw * 0.25f)
    val height = tw * 0.26f
    for (rail in listOf(0.09f, 0.19f)) {
        drawLine(Color.White, c - along * 0.5f - Offset(0f, tw * rail), c + along * 0.5f - Offset(0f, tw * rail), strokeWidth = tw * 0.025f)
    }
    for (k in -2..2) {
        val p = c + along * (k * 0.22f)
        drawLine(Color.White, p, p - Offset(0f, height), strokeWidth = tw * 0.045f, cap = StrokeCap.Round)
    }
}

internal fun DrawScope.drawFlowers(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    drawPath(diamondPath(c, tw * 0.82f, tw * 0.41f), Color(0xFF7A5A3A))
    drawPath(diamondPath(c - Offset(0f, tw * 0.015f), tw * 0.7f, tw * 0.35f), Color(0xFF5FA85C))
    val colors = listOf(Color(0xFFEF6F9D), Color(0xFFF2C94C), Color.White, Color(0xFF8E6CEF), Color(0xFFEF6F9D))
    val spots = listOf(Offset(-0.18f, -0.02f), Offset(0.08f, -0.07f), Offset(0.2f, 0.04f), Offset(-0.04f, 0.07f), Offset(-0.24f, 0.08f))
    spots.forEachIndexed { i, s ->
        val p = c + Offset(s.x * tw, s.y * tw)
        drawLine(Color(0xFF3F8F49), p, p - Offset(0f, tw * 0.1f), strokeWidth = tw * 0.02f)
        drawCircle(colors[(i + sprite.variant) % colors.size], tw * 0.04f, p - Offset(0f, tw * 0.11f))
    }
}

internal fun DrawScope.drawMailbox(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    drawOval(TownColors.Shadow, c - Offset(tw * 0.12f, tw * 0.03f), Size(tw * 0.24f, tw * 0.07f))
    drawLine(TownColors.WoodDark, c, c - Offset(0f, tw * 0.3f), strokeWidth = tw * 0.035f)
    val box = c - Offset(tw * 0.11f, tw * 0.47f)
    drawRoundRect(Palette5.PersianBlue, box, Size(tw * 0.22f, tw * 0.17f), CornerRadius(tw * 0.07f))
    drawLine(PR.Red, c + Offset(tw * 0.11f, -tw * 0.4f), c + Offset(tw * 0.11f, -tw * 0.52f), strokeWidth = tw * 0.025f, cap = StrokeCap.Round)
}

private val CarColors = listOf(Color(0xFFE24B4A), Color(0xFFF2C94C), Color(0xFF4A78E2))

/** A small parked car: a low body, a cabin with windows, and two wheels. [Sprite.variant] picks the color. */
internal fun DrawScope.drawCar(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    val body = CarColors[sprite.variant.mod(CarColors.size)]
    val dark = Color(body.red * 0.78f, body.green * 0.78f, body.blue * 0.78f)
    val light = Color(minOf(body.red * 1.08f, 1f), minOf(body.green * 1.08f, 1f), minOf(body.blue * 1.08f, 1f))
    drawOval(TownColors.Shadow, c - Offset(tw * 0.36f, tw * 0.1f), Size(tw * 0.72f, tw * 0.2f))
    drawIsoBox(c - Offset(0f, tw * 0.05f), tw * 0.66f, tw * 0.33f, tw * 0.15f, body, dark, light)
    drawIsoBox(c - Offset(0f, tw * 0.2f), tw * 0.4f, tw * 0.2f, tw * 0.11f, light, TownColors.Window, Color(0xFFBFD7F2))
    drawCircle(Palette5.DeepNavy, tw * 0.05f, c + Offset(-tw * 0.19f, tw * 0.07f))
    drawCircle(Palette5.DeepNavy, tw * 0.05f, c + Offset(tw * 0.19f, tw * 0.07f))
}

/** A bus shelter: a roof on two posts, a bench under it, and a round sign on a pole. */
internal fun DrawScope.drawBusStop(sprite: Sprite) {
    val tw = sprite.tileWidth
    val c = sprite.anchor
    drawOval(TownColors.Shadow, c - Offset(tw * 0.3f, tw * 0.08f), Size(tw * 0.6f, tw * 0.16f))
    for (dx in listOf(-0.26f, 0.26f)) drawLine(Palette5.DuskBlue, c + Offset(dx * tw, 0f), c + Offset(dx * tw, -tw * 0.5f), strokeWidth = tw * 0.03f)
    drawIsoBox(c - Offset(0f, tw * 0.5f), tw * 0.74f, tw * 0.37f, tw * 0.05f, Palette5.Cornflower, Palette5.DuskBlue, Color(0xFF4B64A6))
    drawLine(TownColors.Wood, c + Offset(-tw * 0.18f, -tw * 0.12f), c + Offset(tw * 0.18f, -tw * 0.12f), strokeWidth = tw * 0.04f, cap = StrokeCap.Round)
    val pole = c + Offset(tw * 0.38f, tw * 0.02f)
    drawLine(Palette5.DeepNavy, pole, pole - Offset(0f, tw * 0.62f), strokeWidth = tw * 0.025f)
    drawCircle(Palette5.PersianBlue, tw * 0.1f, pole - Offset(0f, tw * 0.66f))
    drawCircle(Color.White, tw * 0.055f, pole - Offset(0f, tw * 0.66f))
}
