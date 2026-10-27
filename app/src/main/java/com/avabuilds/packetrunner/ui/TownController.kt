package com.avabuilds.packetrunner.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avabuilds.packetrunner.game.Building
import com.avabuilds.packetrunner.game.TilePos
import com.avabuilds.packetrunner.game.TownMap
import com.avabuilds.packetrunner.game.Vec2

/** A command to walk to a tile. A new object every time, so the same tile can be ordered twice. */
class WalkRequest(val tile: TilePos)

/**
 * Where the player is and what they are doing. The town screen shows it and carries out walk
 * orders; the game (calls, days) gives the orders, for example "walk to the cafe door".
 */
@Stable
class TownController(val town: TownMap) {

    /** The player's position in tile coordinates (fractions while walking). */
    var playerPos by mutableStateOf(town.start.toVec2())
        internal set

    /** Tiles still ahead of a walking player. Empty when standing still. */
    var trail by mutableStateOf<List<TilePos>>(emptyList())
        internal set

    /** The building whose door the player stands at, or null. */
    var arrivedAt by mutableStateOf<Building?>(town.buildings.firstOrNull { it.door == town.start })
        internal set

    internal var walkRequest by mutableStateOf<WalkRequest?>(null)

    val isWalking: Boolean get() = trail.isNotEmpty()

    fun walkTo(tile: TilePos) {
        walkRequest = WalkRequest(tile)
    }

    fun walkToDoorOf(building: Building) = walkTo(building.door)

    /** Puts the player on [tile] at once (for example back at the office door each morning). */
    fun teleportTo(tile: TilePos) {
        walkRequest = null // cancels a walk in progress
        playerPos = tile.toVec2()
        trail = emptyList()
        arrivedAt = town.buildings.firstOrNull { it.door == tile }
    }
}

private fun TilePos.toVec2() = Vec2(x.toFloat(), y.toFloat())
