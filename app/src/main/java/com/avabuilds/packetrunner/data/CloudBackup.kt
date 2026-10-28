package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData

/** What the cloud holds for a player: the saved game (null if never backed up) and the notes. */
data class CloudCopy(val save: SaveData?, val notes: List<Note>)

sealed interface RestoreResult {
    data class Found(val copy: CloudCopy) : RestoreResult
    data object NothingBackedUp : RestoreResult
    data class Failed(val message: String) : RestoreResult
}

/**
 * The optional cloud backup. The game always lives on the phone; this only copies it to the player's account
 * and back. It exists only when Firebase is set up, so the rest of the app never needs to know about Firebase.
 */
class CloudBackup(
    val auth: AuthRepository,
    private val saves: SaveRepository,
    private val notebook: NotebookRepository,
) {
    /** Copies the game on the phone to the account, together with the notes. */
    fun backUp(uid: String, save: SaveData, notes: List<Note>, onDone: (Boolean) -> Unit) {
        saves.save(uid, save) { saved ->
            if (saved) notebook.replaceAll(uid, notes, onDone) else onDone(false)
        }
    }

    /** Only the game, which the game screen sends after every change while the player is logged in. */
    fun backUpGame(uid: String, save: SaveData, onDone: (Boolean) -> Unit) = saves.save(uid, save, onDone)

    fun restore(uid: String, onDone: (RestoreResult) -> Unit) {
        saves.load(uid) { loaded ->
            when (loaded) {
                is LoadResult.Failed -> onDone(RestoreResult.Failed(loaded.message))
                is LoadResult.Found -> {
                    val save = loaded.save
                    notebook.list(uid) { notes ->
                        notes.onSuccess { list ->
                            onDone(if (save == null && list.isEmpty()) RestoreResult.NothingBackedUp else RestoreResult.Found(CloudCopy(save, list)))
                        }.onFailure { onDone(RestoreResult.Failed("Could not read your backed-up notes.")) }
                    }
                }
            }
        }
    }

    /** Removes the backup (used when the account is deleted); [proceed] says whether both parts are gone. */
    fun deleteBackup(uid: String, proceed: (Boolean) -> Unit) {
        notebook.deleteAll(uid) { ok -> if (ok) saves.delete(uid, proceed) else proceed(false) }
    }
}
