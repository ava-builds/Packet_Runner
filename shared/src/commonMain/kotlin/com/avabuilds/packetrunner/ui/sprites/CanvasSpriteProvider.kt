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
import kotlin.math.sin
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.DoorSide
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.TownColors
import com.avabuilds.packetrunner.ui.drawDiamond
import com.avabuilds.packetrunner.ui.drawIsoBox
import com.avabuilds.packetrunner.ui.drawNeighbor
import com.avabuilds.packetrunner.ui.drawPlayer

/** Draws every sprite with the Canvas, so the game works without any image files. */
class CanvasSpriteProvider : SpriteProvider {

    override fun draw(scope: DrawScope, sprite: Sprite): Boolean {
        with(scope) {
            val tw = sprite.tileWidth
            when (sprite.key) {
                SpriteKey.GRASS_LIGHT -> drawGround(sprite, TownColors.GrassLight)
                SpriteKey.GRASS_DARK -> drawGround(sprite, TownColors.GrassDark)
                SpriteKey.ROAD -> drawGround(sprite, TownColors.Road)
                SpriteKey.WATER -> drawWater(sprite)
                SpriteKey.BRIDGE -> drawBridge(sprite)
                SpriteKey.HILL -> drawHill(sprite)
                SpriteKey.STREETLIGHT -> drawStreetlight(sprite)
                SpriteKey.BENCH -> drawBench(sprite)
                SpriteKey.FENCE -> drawFence(sprite)
                SpriteKey.FLOWERS -> drawFlowers(sprite)
                SpriteKey.MAILBOX -> drawMailbox(sprite)
                SpriteKey.CAR -> drawCar(sprite)
                SpriteKey.BUS_STOP -> drawBusStop(sprite)
                SpriteKey.TREE -> drawTree(sprite.anchor, tw, sprite.variant, sprite.time, sprite.phase)
                SpriteKey.OFFICE -> drawOffice(sprite)
                SpriteKey.HOUSE -> drawHouse(sprite)
                SpriteKey.CAFE -> drawCafe(sprite)
                SpriteKey.CITY_HALL -> drawCityHall(sprite)
                SpriteKey.APARTMENTS -> drawApartments(sprite)
                SpriteKey.TOWER -> drawTower(sprite)
                SpriteKey.CONSTRUCTION -> drawConstruction(sprite)
                SpriteKey.FOR_RENT -> drawForRent(sprite)
                SpriteKey.PLAYER -> drawPlayer(sprite.anchor, tw, sprite.time, sprite.moving)
                SpriteKey.NEIGHBOR -> drawNeighbor(sprite.anchor, tw, sprite.variant, sprite.time)
                SpriteKey.LIGHTS -> drawLights(sprite)
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

/** A tree whose crown sways gently in the wind: higher leaves move more than lower ones, the trunk stays put. */
private fun DrawScope.drawTree(ground: Offset, tw: Float, variant: Int, time: Float, phase: Float) {
    val s = tw / 100f * (0.9f + 0.1f * variant)
    val sway = sin(time * 1.3f + phase) * 2.2f * s
    drawOval(TownColors.Shadow, Offset(ground.x - 18 * s, ground.y - 6 * s), Size(36 * s, 12 * s))
    drawRoundRect(TownColors.Trunk, Offset(ground.x - 3.5f * s, ground.y - 30 * s), Size(7 * s, 32 * s), CornerRadius(2 * s))
    drawCircle(TownColors.LeafDark, 24 * s, Offset(ground.x + sway * 0.6f, ground.y - 52 * s))
    drawCircle(TownColors.Leaf, 17 * s, Offset(ground.x - 9 * s + sway * 0.8f, ground.y - 46 * s))
    drawCircle(TownColors.LeafLight, 12 * s, Offset(ground.x + 8 * s + sway, ground.y - 60 * s))
}


// ---------------------------------------------------------------------------
// Windows: one list per building type, used by the daytime drawing and by the night lights
// ---------------------------------------------------------------------------

/** A window as a rectangle on a wall (u along the wall, v up it, both 0..1), on the left wall, the right wall, or both. */
private class Win(val u0: Float, val u1: Float, val v0: Float, val v1: Float, val onLeft: Boolean = true, val onRight: Boolean = true)

private fun windowsOf(type: BuildingType): List<Win> = when (type) {
    // rows of windows on both walls, leaving the bottom row for the door
    BuildingType.OFFICE -> (1..4).flatMap { row ->
        (0 until 3).map { col -> Win(0.12f + col * 0.28f, 0.12f + col * 0.28f + 0.18f, 0.1f + row * 0.18f, 0.1f + row * 0.18f + 0.1f) }
    }
    BuildingType.HOUSE -> listOf(Win(0.55f, 0.82f, 0.3f, 0.7f))
    // big shop windows, one on each wall
    BuildingType.CAFE -> listOf(Win(0.1f, 0.5f, 0.12f, 0.5f, onRight = false), Win(0.5f, 0.9f, 0.12f, 0.5f, onLeft = false))
    BuildingType.APARTMENTS -> (1..3).flatMap { floor ->
        (0 until 2).map { col -> Win(0.14f + col * 0.42f, 0.14f + col * 0.42f + 0.26f, 0.08f + floor * 0.22f, 0.08f + floor * 0.22f + 0.12f) }
    }
    // a glass tower: many narrow rows of windows
    BuildingType.TOWER -> (1..8).flatMap { row ->
        (0 until 3).map { col -> Win(0.12f + col * 0.28f, 0.12f + col * 0.28f + 0.18f, 0.06f + row * 0.11f, 0.06f + row * 0.11f + 0.06f) }
    }
    BuildingType.CITY_HALL -> emptyList()
}

private fun DrawScope.fillWindows(walls: Walls, type: BuildingType, color: Color) {
    windowsOf(type).forEach { w ->
        if (w.onLeft) fillOnWall(walls::left, w.u0, w.u1, w.v0, w.v1, color)
        if (w.onRight) fillOnWall(walls::right, w.u0, w.u1, w.v0, w.v1, color)
    }
}

private val WarmLight = Color(0xFFFFE08A)

/**
 * The night lights of a building: its windows shine warm yellow with a soft halo. [Sprite.variant] is the building type,
 * [Sprite.phase] a number that makes some windows stay dark, and [Sprite.alpha] how bright the lights are (0 to 1).
 */
private fun DrawScope.drawLights(sprite: Sprite) {
    val type = BuildingType.entries[sprite.variant.coerceIn(0, BuildingType.entries.lastIndex)]
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val height = if (type == BuildingType.HOUSE) HOUSE_WALL_HEIGHT * tw else type.heightInTiles() * tw
    val walls = Walls(sprite.anchor, w, w / 2, height)
    val seed = sprite.phase.toInt()
    windowsOf(type).forEachIndexed { i, win ->
        if ((i * 7 + seed * 3).mod(5) == 0) return@forEachIndexed // a few windows stay dark
        val halo = 0.02f
        if (win.onLeft) {
            fillOnWall(walls::left, win.u0 - halo, win.u1 + halo, win.v0 - halo, win.v1 + halo, WarmLight.copy(alpha = 0.28f * sprite.alpha))
            fillOnWall(walls::left, win.u0, win.u1, win.v0, win.v1, WarmLight.copy(alpha = sprite.alpha))
        }
        if (win.onRight) {
            fillOnWall(walls::right, win.u0 - halo, win.u1 + halo, win.v0 - halo, win.v1 + halo, WarmLight.copy(alpha = 0.28f * sprite.alpha))
            fillOnWall(walls::right, win.u0, win.u1, win.v0, win.v1, WarmLight.copy(alpha = sprite.alpha))
        }
    }
    drawDoor(walls, sprite, WarmLight.copy(alpha = 0.55f * sprite.alpha))
}

// ---------------------------------------------------------------------------
// Buildings
// ---------------------------------------------------------------------------

private fun DrawScope.drawOffice(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.OFFICE.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Palette5.Cornflower, left = Palette5.DuskBlue, right = Color(0xFF4B64A6))

    fillWindows(walls, BuildingType.OFFICE, TownColors.Window)
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
    fillWindows(walls, BuildingType.HOUSE, TownColors.Window)
    drawDoor(walls, sprite, TownColors.Trunk)

    // A brick chimney behind the roof; the smoke comes from the life layer
    val chimney = sprite.anchor + Offset(0.17f * tw, -1.0f * tw)
    drawRect(Color(0xFF9B4B3A), chimney, Size(0.09f * tw, 0.22f * tw))
    drawRect(Color(0xFF6E3328), chimney - Offset(0.01f * tw, 0f), Size(0.11f * tw, 0.04f * tw))

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
    fillWindows(walls, BuildingType.CAFE, TownColors.Window)
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

private fun DrawScope.drawApartments(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.APARTMENTS.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Color(0xFF8E4B3E), left = Color(0xFFD08A6E), right = Color(0xFFE5A184))

    // Three floors of small windows with a balcony rail under each row, the ground floor left for the door
    fillWindows(walls, BuildingType.APARTMENTS, TownColors.Window)
    for (floor in 1..3) {
        val v0 = 0.08f + floor * 0.22f
        fillOnWall(walls::left, 0.06f, 0.94f, v0 - 0.035f, v0 - 0.01f, Palette5.DuskBlue)
        fillOnWall(walls::right, 0.06f, 0.94f, v0 - 0.035f, v0 - 0.01f, Palette5.DuskBlue)
    }
    drawDoor(walls, sprite, Palette5.DeepNavy)

    // A satellite dish on the roof
    val roof = sprite.anchor - Offset(0f, walls.height)
    drawCircle(Color.White, tw * 0.09f, roof - Offset(0f, tw * 0.1f))
    drawCircle(Palette5.DuskBlue, tw * 0.09f, roof - Offset(0f, tw * 0.1f), style = Stroke(tw * 0.015f))
    drawLine(Palette5.DeepNavy, roof, roof - Offset(0f, tw * 0.1f), strokeWidth = tw * 0.02f)
}

private fun DrawScope.drawTower(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val walls = Walls(sprite.anchor, w, w / 2, BuildingType.TOWER.heightInTiles() * tw)
    drawIsoBox(sprite.anchor, w, w / 2, walls.height, top = Color(0xFF7FA3D9), left = Color(0xFF3E5C94), right = Color(0xFF5576B3))
    fillWindows(walls, BuildingType.TOWER, TownColors.Window)
    drawDoor(walls, sprite, Palette5.DeepNavy)

    // A smaller crown on top, then an antenna with a red light
    val roof = sprite.anchor - Offset(0f, walls.height)
    val crownHeight = 0.3f * tw
    drawIsoBox(roof, w * 0.6f, w * 0.3f, crownHeight, top = Palette5.Cornflower, left = Palette5.DuskBlue, right = Color(0xFF4B64A6))
    val top = roof - Offset(0f, crownHeight)
    drawLine(Palette5.DeepNavy, top, top - Offset(0f, tw * 0.45f), strokeWidth = tw * 0.03f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFE5484D), tw * 0.045f, top - Offset(0f, tw * 0.47f))
}

private val LotDirt = Color(0xFFC9A66B)
private val SiteOrange = Color(0xFFF08A24)

private fun DrawScope.lotDiamond(center: Offset, w: Float, h: Float, color: Color) {
    drawPath(
        Path().apply {
            moveTo(center.x - w / 2, center.y); lineTo(center.x, center.y + h / 2)
            lineTo(center.x + w / 2, center.y); lineTo(center.x, center.y - h / 2); close()
        },
        color,
    )
}

/** A line of orange and white stripes, like the barrier around a building site. */
private fun DrawScope.stripedBarrier(from: Offset, to: Offset, stripes: Int, thickness: Float) {
    for (k in 0 until stripes) {
        val a = Offset(lerp(from.x, to.x, k / stripes.toFloat()), lerp(from.y, to.y, k / stripes.toFloat()))
        val b = Offset(lerp(from.x, to.x, (k + 1) / stripes.toFloat()), lerp(from.y, to.y, (k + 1) / stripes.toFloat()))
        drawLine(if (k % 2 == 0) SiteOrange else Color.White, a, b, strokeWidth = thickness, cap = StrokeCap.Butt)
    }
}

/** A building site: a dirt lot with a half-built block, scaffolding, a swaying crane and a barrier in front. */
private fun DrawScope.drawConstruction(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val h = w / 2
    val c = sprite.anchor
    lotDiamond(c, w * 1.04f, h * 1.04f, LotDirt)

    // The first floor of the new building
    drawIsoBox(c, w * 0.62f, h * 0.62f, 0.3f * tw, top = Color(0xFFBFC6D4), left = Color(0xFF8E99B3), right = Color(0xFFA3AEC6))

    // Scaffolding: poles at the four corners of the block and two cross bars
    val pole = 0.8f * tw
    val corners = listOf(
        Offset(c.x - w * 0.31f, c.y), Offset(c.x, c.y + h * 0.31f), Offset(c.x + w * 0.31f, c.y), Offset(c.x, c.y - h * 0.31f),
    )
    corners.forEach { drawLine(Palette5.DeepNavy, it, it - Offset(0f, pole), strokeWidth = tw * 0.02f) }
    for (level in listOf(0.4f, 0.8f)) {
        val up = Offset(0f, pole * level)
        drawLine(Palette5.DeepNavy, corners[0] - up, corners[1] - up, strokeWidth = tw * 0.015f)
        drawLine(Palette5.DeepNavy, corners[1] - up, corners[2] - up, strokeWidth = tw * 0.015f)
    }

    // The crane: a mast at the back, a jib to the right, and a hook that swings a little
    val base = Offset(c.x - w * 0.2f, c.y - h * 0.15f)
    val mastTop = base - Offset(0f, 1.7f * tw)
    drawLine(SiteOrange, base, mastTop, strokeWidth = tw * 0.045f)
    drawLine(SiteOrange, mastTop + Offset(-0.22f * tw, 0f), mastTop + Offset(0.95f * tw, 0f), strokeWidth = tw * 0.035f, cap = StrokeCap.Round)
    drawRect(Palette5.DeepNavy, mastTop + Offset(-0.26f * tw, -0.02f * tw), Size(0.1f * tw, 0.08f * tw))
    val swing = sin(sprite.time * 1.1f) * 0.05f * tw
    val hookTop = mastTop + Offset(0.7f * tw, 0f)
    val hook = hookTop + Offset(swing, 0.6f * tw)
    drawLine(Palette5.DeepNavy, hookTop, hook, strokeWidth = tw * 0.012f)
    drawRect(Color(0xFF8E99B3), hook - Offset(0.05f * tw, 0f), Size(0.1f * tw, 0.07f * tw))

    // A barrier along the two front edges
    val left = Offset(c.x - w / 2, c.y)
    val front = Offset(c.x, c.y + h / 2)
    val right = Offset(c.x + w / 2, c.y)
    stripedBarrier(left, front, 6, tw * 0.05f)
    stripedBarrier(front, right, 6, tw * 0.05f)
}

/** A lot with nothing on it but a "for rent" sign: a white board with a red band and three lines of text. */
private fun DrawScope.drawForRent(sprite: Sprite) {
    val tw = sprite.tileWidth
    val w = sprite.size * tw * BUILDING_INSET
    val h = w / 2
    val c = sprite.anchor
    lotDiamond(c, w * 1.02f, h * 1.02f, Color(0xFFD9DEE8))
    lotDiamond(c, w * 0.9f, h * 0.9f, Color(0xFFE6EAF2))

    // The sign: two posts and a board
    val post = c + Offset(0f, h * 0.18f)
    val boardW = 0.62f * tw
    val boardH = 0.34f * tw
    val boardTop = post - Offset(boardW / 2, 0.95f * tw)
    drawLine(Color(0xFF8A6A47), post, post - Offset(boardW * 0.3f, 0.7f * tw), strokeWidth = tw * 0.035f)
    drawLine(Color(0xFF8A6A47), post, post - Offset(-boardW * 0.3f, 0.7f * tw), strokeWidth = tw * 0.035f)
    drawRoundRect(Color.White, boardTop, Size(boardW, boardH), CornerRadius(tw * 0.03f))
    drawRoundRect(Palette5.DeepNavy, boardTop, Size(boardW, boardH), CornerRadius(tw * 0.03f), style = Stroke(tw * 0.015f))
    drawRect(Color(0xFFE5484D), boardTop + Offset(tw * 0.015f, tw * 0.015f), Size(boardW - tw * 0.03f, boardH * 0.34f))
    for (line in 0..1) {
        val y = boardTop.y + boardH * (0.55f + line * 0.2f)
        drawLine(Palette5.DuskBlue, Offset(boardTop.x + boardW * 0.14f, y), Offset(boardTop.x + boardW * (0.86f - line * 0.2f), y), strokeWidth = tw * 0.02f, cap = StrokeCap.Round)
    }
}
