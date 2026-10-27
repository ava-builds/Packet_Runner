package com.avabuilds.packetrunner.data

/**
 * The notebook's storage: create, read, update and delete notes of one player.
 * [FirestoreNotebookRepository] is the real one; [InMemoryNotebookRepository] keeps notes only while the app runs.
 */
interface NotebookRepository {
    fun list(uid: String, onDone: (Result<List<Note>>) -> Unit)

    /** Stores a new note; [now] becomes its created and updated time. Gives back the stored note, or null on failure. */
    fun add(uid: String, draft: NoteDraft, now: Long, onDone: (Note?) -> Unit)
    fun update(uid: String, note: Note, onDone: (Boolean) -> Unit)
    fun delete(uid: String, noteId: String, onDone: (Boolean) -> Unit)

    /** Removes every note of the player (used when the account is deleted). */
    fun deleteAll(uid: String, onDone: (Boolean) -> Unit)
}

/** Notes kept in memory, for playing without an account and for tests. */
class InMemoryNotebookRepository : NotebookRepository {
    private val notes = linkedMapOf<String, MutableMap<String, Note>>()
    private var nextId = 1

    private fun of(uid: String) = notes.getOrPut(uid) { linkedMapOf() }

    override fun list(uid: String, onDone: (Result<List<Note>>) -> Unit) = onDone(Result.success(of(uid).values.toList()))

    override fun add(uid: String, draft: NoteDraft, now: Long, onDone: (Note?) -> Unit) {
        val note = Note("n${nextId++}", draft.title.trim(), draft.body, draft.topic, createdAt = now, updatedAt = now)
        of(uid)[note.id] = note
        onDone(note)
    }

    override fun update(uid: String, note: Note, onDone: (Boolean) -> Unit) {
        val exists = note.id in of(uid)
        if (exists) of(uid)[note.id] = note
        onDone(exists)
    }

    override fun delete(uid: String, noteId: String, onDone: (Boolean) -> Unit) = onDone(of(uid).remove(noteId) != null)

    override fun deleteAll(uid: String, onDone: (Boolean) -> Unit) {
        notes.remove(uid)
        onDone(true)
    }
}
