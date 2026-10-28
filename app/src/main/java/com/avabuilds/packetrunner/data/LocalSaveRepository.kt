package com.avabuilds.packetrunner.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avabuilds.packetrunner.game.SaveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.gameDataStore by preferencesDataStore(name = "game_save")

/**
 * Keeps the game on the phone, with DataStore, so it works with no account and no internet.
 * The save is stored as JSON text under one key. [uid] is ignored: there is one local game per phone.
 */
class LocalSaveRepository(context: Context) : SaveRepository {
    private val store = context.applicationContext.gameDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun load(uid: String, onDone: (LoadResult) -> Unit) {
        scope.launch {
            val result = try {
                val map = JsonText.readMap(store.data.first()[KEY])
                LoadResult.Found(map?.let(SaveData::fromMap)) // no file yet, or a damaged one: start a new town
            } catch (e: Exception) {
                LoadResult.Failed("Could not read the saved town from this phone.")
            }
            onDone(result)
        }
    }

    override fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit) {
        scope.launch {
            val ok = try {
                store.edit { it[KEY] = JsonText.write(data.toMap()) }
                true
            } catch (e: Exception) {
                false
            }
            onDone(ok)
        }
    }

    override fun delete(uid: String, onDone: (Boolean) -> Unit) {
        scope.launch {
            val ok = try {
                store.edit { it.remove(KEY) }
                true
            } catch (e: Exception) {
                false
            }
            onDone(ok)
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("save")
    }
}
