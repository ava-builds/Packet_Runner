package com.avabuilds.packetrunner.ui

import com.avabuilds.packetrunner.game.CableLink
import com.avabuilds.packetrunner.game.DoorSide
import com.avabuilds.packetrunner.game.Hill
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.game.TownNetwork
import com.avabuilds.packetrunner.game.World
import com.avabuilds.packetrunner.ui.sprites.SpriteKey
import com.avabuilds.packetrunner.ui.sprites.spriteKey

/** A tile of the land outside the town: what to draw there and how faded it is. */
class GroundTile(val x: Int, val y: Int, val key: SpriteKey, val alpha: Float, val variant: Int)

/** Something that stands on the ground: a building, tree or prop at tile position ([x], [y]). Drawn back to front by [depth]. */
class Standing(
    val depth: Float,
    val key: SpriteKey,
    val x: Float,
    val y: Float,
    val size: Int = 1,
    val variant: Int = 0,
    val doorSide: DoorSide? = null,
    val doorIndex: Int = 0,
    val phase: Float = 0f,
    /** Set for buildings, so a tap can make this one bounce. */
    val buildingId: String? = null,
)

/**
 * Everything that never changes while playing, worked out once: the land around the town, the hills, and
 * every building, tree and prop sorted back to front. The screen only has to draw them.
 */
class Scenery private constructor(
    val groundTiles: List<GroundTile>,
    val hills: List<Hill>,
    val standing: List<Standing>,
    /** The network cables from the office to every building. */
    val cables: List<CableLink>,
) {

    companion object {
        fun build(town: TownMap): Scenery {
            val w = town.width
            val h = town.height
            val pad = World.PAD

            val tiles = buildList {
                for (x in -pad until w + pad) for (y in -pad until h + pad) {
                    if (World.inGrid(x, y, w, h)) continue
                    val alpha = World.fade(x, y, w, h)
                    if (alpha <= 0.02f) continue
                    val variant = (x * 31 + y * 17).mod(3)
                    val key = when {
                        World.isBridge(x, y) -> SpriteKey.BRIDGE
                        World.isRiver(x, y) -> SpriteKey.WATER
                        World.isOutsideRoad(x, y, h) -> SpriteKey.ROAD
                        (x + y) % 2 == 0 -> SpriteKey.GRASS_LIGHT
                        else -> SpriteKey.GRASS_DARK
                    }
                    add(GroundTile(x, y, key, alpha, variant))
                }
            }

            val things = buildList {
                town.buildings.forEach { b ->
                    add(Standing(b.depth, b.type.spriteKey(), b.center.x, b.center.y, b.size, b.style, b.doorSide, b.doorIndex, buildingId = b.id))
                }
                town.trees.forEach { t -> add(Standing((t.x + t.y).toFloat(), SpriteKey.TREE, t.x.toFloat(), t.y.toFloat(), variant = (t.x * 5 + t.y * 3) % 3, phase = t.x * 0.7f + t.y * 1.3f)) }
                town.props.forEach { p -> add(Standing((p.tile.x + p.tile.y).toFloat(), p.kind.spriteKey(), p.tile.x.toFloat(), p.tile.y.toFloat(), variant = p.variant)) }
                for (x in -pad until w + pad) for (y in -pad until h + pad) {
                    if (World.treeAt(x, y, w, h)) add(Standing((x + y).toFloat(), SpriteKey.TREE, x.toFloat(), y.toFloat(), variant = (x * 5 + y * 3).mod(3), phase = x * 0.7f + y * 1.3f))
                }
            }
            return Scenery(tiles, World.hills, things.sortedBy { it.depth }, TownNetwork.links(town))
        }
    }
}
