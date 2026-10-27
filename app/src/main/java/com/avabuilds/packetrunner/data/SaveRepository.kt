package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData

/** The result of loading a save: found one (or none yet, for a new player), or failed with a message. */
sealed interface LoadResult {
    data class Found(val save: SaveData?) : LoadResult
    data class Failed(val message: String) : LoadResult
}

/** Where a player's game is kept. The game only knows this interface; [FirestoreSaveRepository] is the real one. */
interface SaveRepository {
    fun load(uid: String, onDone: (LoadResult) -> Unit)
    fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit)
    fun delete(uid: String, onDone: (Boolean) -> Unit)
}
