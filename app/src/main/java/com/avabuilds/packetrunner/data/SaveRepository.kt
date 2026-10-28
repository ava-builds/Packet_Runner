package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData

/** The result of loading a save: found one (or none yet, for a new player), or failed with a message. */
sealed interface LoadResult {
    data class Found(val save: SaveData?) : LoadResult
    data class Failed(val message: String) : LoadResult
}

/** The id the game and notebook are kept under on the phone: there is one local game per phone. */
const val LOCAL_UID = "local"

/** Where a player's game is kept. The game only knows this interface; [LocalSaveRepository] keeps it on the phone and [FirestoreSaveRepository] is the optional cloud copy. */
interface SaveRepository {
    fun load(uid: String, onDone: (LoadResult) -> Unit)
    fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit)
    fun delete(uid: String, onDone: (Boolean) -> Unit)
}
