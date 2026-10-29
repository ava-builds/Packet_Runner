package com.avabuilds.packetrunner.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.notebookDataStore by preferencesDataStore(name = "notebook")

/**
 * The notebook kept on the phone with DataStore, so notes stay after the app closes and need no account.
 * There is one notebook per phone, so [uid] is ignored.
 */
class LocalNotebookRepository(context: Context) : NotebookRepository {
    private val store = context.applicationContext.notebookDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex() // one change at a time, so quick taps cannot overwrite each other

    private suspend fun read(): List<Note> = NoteStorage.decode(store.data.first()[KEY])

    private suspend fun write(notes: List<Note>) {
        store.edit { p -> p[KEY] = NoteStorage.encode(notes) }
    }

    private fun run(onFail: () -> Unit, block: suspend () -> Unit) {
        scope.launch {
            try {
                lock.withLock { block() }
            } catch (e: Exception) {
                onFail()
            }
        }
    }

    override fun list(uid: String, onDone: (Result<List<Note>>) -> Unit) =
        run({ onDone(Result.failure(IllegalStateException("Could not read the notebook."))) }) { onDone(Result.success(read())) }

    override fun add(uid: String, draft: NoteDraft, now: Long, onDone: (Note?) -> Unit) =
        run({ onDone(null) }) {
            val notes = read()
            val note = Note(NoteStorage.nextId(notes, now), draft.title.trim(), draft.body, draft.topic, createdAt = now, updatedAt = now)
            write(notes + note)
            onDone(note)
        }

    override fun update(uid: String, note: Note, onDone: (Boolean) -> Unit) =
        run({ onDone(false) }) {
            val notes = read()
            val exists = notes.any { it.id == note.id }
            if (exists) write(notes.map { if (it.id == note.id) note else it })
            onDone(exists)
        }

    override fun delete(uid: String, noteId: String, onDone: (Boolean) -> Unit) =
        run({ onDone(false) }) {
            val notes = read()
            val exists = notes.any { it.id == noteId }
            if (exists) write(notes.filter { it.id != noteId })
            onDone(exists)
        }

    override fun deleteAll(uid: String, onDone: (Boolean) -> Unit) =
        run({ onDone(false) }) {
            store.edit { it.remove(KEY) }
            onDone(true)
        }

    override fun replaceAll(uid: String, notes: List<Note>, onDone: (Boolean) -> Unit) =
        run({ onDone(false) }) {
            write(notes)
            onDone(true)
        }

    private companion object {
        val KEY = stringPreferencesKey("notes")
    }
}
