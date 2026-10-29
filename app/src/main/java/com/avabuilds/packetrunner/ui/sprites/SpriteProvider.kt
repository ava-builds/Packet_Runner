package com.avabuilds.packetrunner.ui.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.PropKind
import com.avabuilds.packetrunner.game.Vec2
import com.avabuilds.packetrunner.game.isoBoxSilhouette
import com.avabuilds.packetrunner.game.DoorSide

/** Everything the town can draw. Each key is one sprite; later a PNG can replace any of them. */
enum class SpriteKey {
    GRASS_LIGHT, GRASS_DARK, ROAD, WATER, BRIDGE, HILL,
    TREE, OFFICE, HOUSE, CAFE, CITY_HALL, APARTMENTS, TOWER, PLAYER, NEIGHBOR, LIGHTS,
    /** What stands where a building is not open yet: a building site or an empty lot with a "for rent" sign. */
    CONSTRUCTION, FOR_RENT,
    STREETLIGHT, BENCH, FENCE, FLOWERS, MAILBOX, CAR, BUS_STOP,
}

/**
 * One thing to draw. [anchor] is the on-screen center of its ground diamond (for a building, the
 * center of its whole footprint) and [tileWidth] is the on-screen width of one tile in pixels.
 * [size] is the footprint in tiles, [variant] picks a color or shape variation, and [doorSide] /
 * [doorIndex] say where a building's door is.
 */
data class Sprite(
    val key: SpriteKey,
    val anchor: Offset,
    val tileWidth: Float,
    val size: Int = 1,
    val variant: Int = 0,
    val doorSide: DoorSide? = null,
    val doorIndex: Int = 0,
    /** 1 = fully drawn; less fades the sprite (the land at the edge of the world). */
    val alpha: Float = 1f,
    /** Seconds on the shared animation clock, for things that sway, bob or wave. */
    val time: Float = 0f,
    /** Offsets one sprite's movement from another's, so a row of trees does not sway in step. */
    val phase: Float = 0f,
    /** For people: walking (true) or standing. */
    val moving: Boolean = false,
)

/**
 * Draws sprites. To use PNG art (for example Kenney's City Kit), write a provider that loads the
 * images and returns true for the keys it has, and return false for the rest: the
 * [FallbackSpriteProvider] then lets the Canvas drawing handle those.
 */
interface SpriteProvider {
    /** Draws [sprite] into [scope]. Returns false when this provider has no sprite for its key. */
    fun draw(scope: DrawScope, sprite: Sprite): Boolean
}

/** Tries [preferred] first and falls back to [fallback] for keys the preferred one does not have. */
class FallbackSpriteProvider(
    private val preferred: SpriteProvider,
    private val fallback: SpriteProvider,
) : SpriteProvider {
    override fun draw(scope: DrawScope, sprite: Sprite): Boolean =
        preferred.draw(scope, sprite) || fallback.draw(scope, sprite)
}

fun PropKind.spriteKey(): SpriteKey = when (this) {
    PropKind.STREETLIGHT -> SpriteKey.STREETLIGHT
    PropKind.BENCH -> SpriteKey.BENCH
    PropKind.FENCE -> SpriteKey.FENCE
    PropKind.FLOWERS -> SpriteKey.FLOWERS
    PropKind.MAILBOX -> SpriteKey.MAILBOX
    PropKind.CAR -> SpriteKey.CAR
    PropKind.BUS_STOP -> SpriteKey.BUS_STOP
}

fun BuildingType.spriteKey(): SpriteKey = when (this) {
    BuildingType.OFFICE -> SpriteKey.OFFICE
    BuildingType.HOUSE -> SpriteKey.HOUSE
    BuildingType.CAFE -> SpriteKey.CAFE
    BuildingType.CITY_HALL -> SpriteKey.CITY_HALL
    BuildingType.APARTMENTS -> SpriteKey.APARTMENTS
    BuildingType.TOWER -> SpriteKey.TOWER
}

/** The sprite that stands in place of a building that is not open yet: big public buildings are building sites, shops and homes are "for rent". */
fun BuildingType.lockedSpriteKey(): SpriteKey = when (this) {
    BuildingType.TOWER, BuildingType.CITY_HALL, BuildingType.OFFICE -> SpriteKey.CONSTRUCTION
    BuildingType.CAFE, BuildingType.APARTMENTS, BuildingType.HOUSE -> SpriteKey.FOR_RENT
}

/** How tall a building is drawn, in tile widths (a house includes its roof). */
fun BuildingType.heightInTiles(): Float = when (this) {
    BuildingType.OFFICE -> 1.5f
    BuildingType.HOUSE -> 1.15f
    BuildingType.CAFE -> 0.85f
    BuildingType.CITY_HALL -> 1.0f
    BuildingType.APARTMENTS -> 1.7f
    BuildingType.TOWER -> 2.6f
}

/** Where a building's name label floats, in tile widths above its ground (clear of roofs and antennas). */
fun BuildingType.labelHeightInTiles(): Float = when (this) {
    BuildingType.OFFICE -> 2.1f
    BuildingType.HOUSE -> 1.4f
    BuildingType.CAFE -> 1.6f
    BuildingType.CITY_HALL -> 2.5f
    BuildingType.APARTMENTS -> 2.3f
    BuildingType.TOWER -> 3.4f
}

/** The label height above a building that is not open yet: the crane of a building site is taller than a "for rent" sign. */
fun BuildingType.lockedLabelHeightInTiles(): Float = if (lockedSpriteKey() == SpriteKey.CONSTRUCTION) 2.5f else 1.3f

/** A building's walls are drawn a little smaller than its footprint, so neighbors do not touch. */
const val BUILDING_INSET = 0.92f

/** A house is a low box with a pyramid roof: the wall height and the roof height, in tile widths. */
const val HOUSE_WALL_HEIGHT = 0.62f
const val HOUSE_ROOF_HEIGHT = 0.5f

/**
 * The on-screen outline of a building, used to decide whether a tap hit it.
 * [center] is where the middle of its footprint is on screen.
 */
fun buildingSilhouette(type: BuildingType, center: Vec2, tileWidth: Float, size: Int): List<Vec2> {
    val w = size * tileWidth * BUILDING_INSET
    val h = w / 2
    if (type != BuildingType.HOUSE) return isoBoxSilhouette(center, w, h, type.heightInTiles() * tileWidth)

    // Walls, then a roof that comes to a point
    val wall = HOUSE_WALL_HEIGHT * tileWidth
    val eave = w * 1.1f / 2 // the roof overhangs the walls a little
    return listOf(
        Vec2(center.x - w / 2, center.y),
        Vec2(center.x, center.y + h / 2),
        Vec2(center.x + w / 2, center.y),
        Vec2(center.x + eave, center.y - wall),
        Vec2(center.x, center.y - wall - HOUSE_ROOF_HEIGHT * tileWidth),
        Vec2(center.x - eave, center.y - wall),
    )
}
