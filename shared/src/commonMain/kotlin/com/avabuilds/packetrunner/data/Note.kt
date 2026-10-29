package com.avabuilds.packetrunner.data

/** One page of the Engineer's notebook. Times are milliseconds since 1970. */
data class Note(
    val id: String,
    val title: String,
    val body: String,
    /** Optional label, for example the concept the note is about. */
    val topic: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title, "body" to body, "topic" to topic, "createdAt" to createdAt, "updatedAt" to updatedAt,
    )

    companion object {
        /** Reads a stored note. Missing fields become empty text or zero, so a bad document never crashes the notebook. */
        fun fromMap(id: String, map: Map<String, Any?>): Note {
            fun long(v: Any?) = (v as? Number)?.toLong() ?: 0L
            return Note(
                id = id,
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                topic = map["topic"] as? String ?: "",
                createdAt = long(map["createdAt"]),
                updatedAt = long(map["updatedAt"]),
            )
        }
    }
}

/** What the player typed in the editor, before it is a [Note]. */
data class NoteDraft(val title: String, val body: String, val topic: String = "")

object NoteValidation {
    const val TITLE_MAX = 60
    const val BODY_MAX = 1000

    fun title(text: String): String? = when {
        text.isBlank() -> "Give your note a title."
        text.trim().length > TITLE_MAX -> "The title can have at most $TITLE_MAX letters."
        else -> null
    }

    fun body(text: String): String? =
        if (text.length > BODY_MAX) "The note is too long (at most $BODY_MAX letters)." else null
}

/** Sorting and searching a list of notes. */
object NoteList {
    /** The most recently changed note first. */
    fun sorted(notes: List<Note>): List<Note> = notes.sortedByDescending { it.updatedAt }

    /** Notes whose title, text or topic contains [query] (ignoring upper and lower case). A blank query keeps everything. */
    fun filtered(notes: List<Note>, query: String): List<Note> {
        val q = query.trim()
        if (q.isEmpty()) return notes
        return notes.filter { n -> listOf(n.title, n.body, n.topic).any { it.contains(q, ignoreCase = true) } }
    }
}
