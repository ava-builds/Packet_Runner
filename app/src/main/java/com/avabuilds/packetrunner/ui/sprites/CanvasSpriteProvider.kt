package com.avabuilds.packetrunner.ui.sprites

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.DoorSide
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.TownColors
import com.avabuilds.packetrunner.ui.drawDiamond
import com.avabuilds.packetrunner.ui.drawIsoBox
import com.avabuilds.packetrunner.ui.drawPlayer

/** Draws every sprite with the Canvas, so the game works without any image files. */
class CanvasSpriteProvider : SpriteProvider {

    override fun draw(scope: DrawScope, sprite: Sprite): Boolean {
        with(scope) {
            val tw = sprite.tileWidth
            when (sprite.key) {
                SpriteKey.GRASS_LIGHT -> drawDiamond(sprite.anchor, tw, tw / 2, TownColors.GrassLight)
                SpriteKey.GRASS_DARK -> drawDiamond(sprite.anchor, tw, tw / 2, TownColors.GrassDark)
                SpriteKey.ROAD -> drawDiamond(sprite.anchor, tw, tw / 2, TownColors.Road)
                SpriteKey.TREE -> drawTree(sprite.anchor, tw, sprite.variant)
                SpriteKey.OFFICE -> drawOffice(sprite)
                SpriteKey.HOUSE -> drawHouse(sprite)
                SpriteKey.CAFE -> drawCafe(sprite)
                SpriteKey.CITY_HALL -> drawCityHall(sprite)
                SpriteKey.PLAYER -> drawPlayer(sprite.anchor, tw)
            }
        }
        return true
    }
}

// ---------------------------------------------------------------------------
// Wall geometry. A box has two visible walls. Points on a wall are given as (u, v):
// u runs along the wall (0 to 1), v runs up the wall (0 = ground, 1 = the top).
// ---------------------------------------------------------------------------

private class Walls(val center: Offset, val w: Float, val h: Float, val height: Float) {
    /** The wall facing the lower left, running from the left corner (u = 0) to the front corner (u = 1). */
    fun left(u: Float, v: Float) = Offset(lerp(center.x - w / 2, center.x, u), lerp(center.y, center.y + h / 2, u) - v * height)

    /** The wall facing the lower right, running from the front corner (u = 0) to the right corner (u = 1). */
    fun right(u: Float, v: Float) = Offset(lerp(center.x, center.x + w / 2, u), lerp(center.y + h / 2, center.y, u) - v * height)
}

private fun DrawScope.fillQuad(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
    drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }, color)
}

/** Fills the rectangle u0..u1 x v0..v1 on a wall. [point] is [Walls.left] or [Walls.right]. */
private fun DrawScope.fillOnWall(
    point: (Float, Float) -> Offset,
    u0: Float, u1: Float, v0: Float, v1: Float,
    color: Color,
) = fillQuad(point(u0, v0), point(u1, v0), point(u1, v1), point(u0, v1), color)

private fun DrawScope.drawDoor(walls: Walls, sprite: Sprite, color: Color) {
    val n = sprite.size
    val half = 0.3f / n
    when (sprite.doorSide) {
        DoorSide.RIGHT -> {
            val u = 1f - (sprite.doorIndex + 0.5f) / n
            fillOnWall(walls::right, u - half, u + half, 0f, 0.42f, color)
        }
        DoorSide.LEFT -> {
            val u = (sprite.doorIndex + 0.5f) / n
            fillOnWall(walls::left, u - half, u + half, 0f, 0.42f, color)
        }
        null -> Unit
    }
}

private fun DrawScope.signalArcs(at: Offset, k: Float, color: Color) {
    listOf(8f, 14f).forEach { r ->
        val tl = at - Offset(r * k, r * k)
        val size = Size(r * 2 * k, r * 2 * k)
        drawArc(color, -50f, 100f, false, tl, size, style = Stroke(2f * k, cap = StrokeCap.Round))
        drawArc(color, 130f, 100f, false, tl, size, style = Stroke(2f * k, cap = StrokeCap.Round))
    }
}

// ---------------------------------------------------------------------------
// Trees
// ---------------------------------------------------------------------------

private fun DrawScope.drawTree(ground: Offset, tw: Float, variant: Int) {
    val s = tw / 100f * (0.9f + 0.1f * variant)
    drawOval(TownColors.Shadow, Offset(ground.x - 18 * s, ground.y - 6 * s), Size(36 * s, 12 * s))
    drawRoundRect(TownColors.Trunk, Offset(ground.x - 3.5f * s, ground.y - 30 * s), Size(7 * s, 32 * s), CornerRadius(2 * s))
    drawCircle(TownColors.LeafDark, 24 * s, Offset(ground.x, ground.y - 52 * s))
    drawCircle(TownColors.Leaf, 17 * s, Offset(ground.x - 9 * s, ground.y - 46 * s))
    drawCircle(TownColors.LeafLight, 12 * s, Offset(ground.x + 8 * s, ground.y - 60 * s))
}

// ---------------------------------------------------------------------------
// Buildings
// ---------------------------------------------------------------------------

private fun DrawScope.drawOffice(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.OFFICE.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Palette5.Cornflower, left = Palette5.DuskBlue, right = Color(0xFF4B64A6))

    // Rows of windows on both walls, leaving the bottom row for the door
    for (row in 1..4) {
        val v0 = 0.1f + row * 0.18f
        for (col in 0 until 3) {
            val u0 = 0.12f + col * 0.28f
            fillOnWall(walls::left, u0, u0 + 0.18f, v0, v0 + 0.1f, TownColors.Window)
            fillOnWall(walls::right, u0, u0 + 0.18f, v0, v0 + 0.1f, TownColors.Window)
        }
    }
    drawDoor(walls, sprite, Palette5.DeepNavy)

    // A roof antenna: the engineer's office needs a signal
    val roof = sprite.anchor - Offset(0f, walls.height)
    drawLine(Palette5.DeepNavy, roof, roof - Offset(0f, tw * 0.4f), strokeWidth = tw * 0.03f, cap = StrokeCap.Round)
    drawCircle(PR.Amber, tw * 0.045f, roof - Offset(0f, tw * 0.42f))
    signalArcs(roof - Offset(0f, tw * 0.42f), tw / 100f, Palette5.PersianBlue)
}

private val HouseRoofs = listOf(
    Color(0xFFC94F4A) to Color(0xFFE0645F),
    Color(0xFF1B2CC1) to Color(0xFF3F51D8),
    Color(0xFFD9922F) to Color(0xFFEFB255),
)

private fun DrawScope.drawHouse(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val h = w / 2
    val wallHeight = HOUSE_WALL_HEIGHT * tw
    val walls = Walls(sprite.anchor, w, h, wallHeight)
    val (roofDark, roofLight) = HouseRoofs[sprite.variant.mod(HouseRoofs.size)]
    drawIsoBox(sprite.anchor, w, h, wallHeight, top = roofDark, left = Color(0xFFE8DCC4), right = Color(0xFFF6EBD6))

    // Windows and door on the walls
    fillOnWall(walls::left, 0.55f, 0.82f, 0.3f, 0.7f, TownColors.Window)
    fillOnWall(walls::right, 0.55f, 0.82f, 0.3f, 0.7f, TownColors.Window)
    drawDoor(walls, sprite, TownColors.Trunk)

    // Pyramid roof, slightly wider than the walls
    val w2 = w * 1.1f
    val h2 = w2 / 2
    val eaveY = sprite.anchor.y - wallHeight
    val apex = Offset(sprite.anchor.x, eaveY - HOUSE_ROOF_HEIGHT * tw)
    val leftEave = Offset(sprite.anchor.x - w2 / 2, eaveY)
    val frontEave = Offset(sprite.anchor.x, eaveY + h2 / 2)
    val rightEave = Offset(sprite.anchor.x + w2 / 2, eaveY)
    drawPath(Path().apply { moveTo(leftEave.x, leftEave.y); lineTo(frontEave.x, frontEave.y); lineTo(apex.x, apex.y); close() }, roofDark)
    drawPath(Path().apply { moveTo(rightEave.x, rightEave.y); lineTo(frontEave.x, frontEave.y); lineTo(apex.x, apex.y); close() }, roofLight)
}

private fun DrawScope.drawCafe(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.CAFE.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Color(0xFFB5693E), left = Color(0xFFE0B27C), right = Color(0xFFF0C995))

    // Big shop windows and a striped awning over them
    fillOnWall(walls::left, 0.1f, 0.5f, 0.12f, 0.5f, TownColors.Window)
    fillOnWall(walls::right, 0.5f, 0.9f, 0.12f, 0.5f, TownColors.Window)
    for (face in listOf<(Float, Float) -> Offset>(walls::left, walls::right)) {
        val stripes = 6
        for (k in 0 until stripes) {
            val color = if (k % 2 == 0) Palette5.PersianBlue else Color.White
            fillOnWall(face, k / stripes.toFloat(), (k + 1) / stripes.toFloat(), 0.58f, 0.8f, color)
        }
    }
    drawDoor(walls, sprite, Palette5.DeepNavy)

    // A round sign on a post above the roof
    val roof = sprite.anchor - Offset(0f, walls.height)
    drawLine(Palette5.DeepNavy, roof, roof - Offset(0f, tw * 0.3f), strokeWidth = tw * 0.025f)
    drawCircle(Color.White, tw * 0.13f, roof - Offset(0f, tw * 0.38f))
    drawCircle(Palette5.PersianBlue, tw * 0.13f, roof - Offset(0f, tw * 0.38f), style = Stroke(tw * 0.02f))
    drawCircle(PR.Amber, tw * 0.06f, roof - Offset(0f, tw * 0.38f))
}

private fun DrawScope.drawCityHall(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.CITY_HALL.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Color(0xFFF4F6FC), left = Color(0xFFCBD6EE), right = Color(0xFFDDE5F7))

    // Columns on both walls and a dark band under the roof
    for (face in listOf<(Float, Float) -> Offset>(walls::left, walls::right)) {
        for (k in 0 until 4) {
            val u = 0.12f + k * 0.24f
            fillOnWall(face, u, u + 0.08f, 0.05f, 0.78f, Color.White)
        }
        fillOnWall(face, 0f, 1f, 0.82f, 1f, Palette5.DuskBlue)
    }
    drawDoor(walls, sprite, Palette5.DeepNavy)

    // A flag on a pole: the town's seat of government
    val roof = sprite.anchor - Offset(0f, walls.height)
    val top = roof - Offset(0f, tw * 0.45f)
    drawLine(Palette5.DeepNavy, roof, top, strokeWidth = tw * 0.025f, cap = StrokeCap.Round)
    drawPath(
        Path().apply { moveTo(top.x, top.y); lineTo(top.x + tw * 0.22f, top.y + tw * 0.06f); lineTo(top.x, top.y + tw * 0.12f); close() },
        Palette5.PersianBlue,
    )
}
