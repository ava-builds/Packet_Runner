package com.avabuilds.packetrunner.data

/** Turns the stored text into notes and back. Kept apart from DataStore so it can be tested. */
object NoteStorage {
    fun decode(text: String?): List<Note> =
        (JsonText.read(text) as? List<*>).orEmpty().mapNotNull { row ->
            @Suppress("UNCHECKED_CAST")
            val map = row as? Map<String, Any?> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            Note.fromMap(id, map)
        }

    fun encode(notes: List<Note>): String = JsonText.write(notes.map { it.toMap() + ("id" to it.id) })

    /** An id that no note uses yet. */
    fun nextId(notes: List<Note>, now: Long): String {
        var id = "n$now"
        var extra = 1
        while (notes.any { it.id == id }) id = "n$now-${extra++}"
        return id
    }
}
