package com.avabuilds.packetrunner.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

/** Keeps each note as a document in players/{uid}/notes. */
class FirestoreNotebookRepository : NotebookRepository {
    private fun notes(uid: String): CollectionReference =
        FirebaseFirestore.getInstance().collection("players").document(uid).collection("notes")

    override fun list(uid: String, onDone: (Result<List<Note>>) -> Unit) {
        notes(uid).get()
            .addOnSuccessListener { snap -> onDone(Result.success(snap.documents.map { Note.fromMap(it.id, it.data.orEmpty()) })) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    override fun add(uid: String, draft: NoteDraft, now: Long, onDone: (Note?) -> Unit) {
        val ref = notes(uid).document()
        val note = Note(ref.id, draft.title.trim(), draft.body, draft.topic, createdAt = now, updatedAt = now)
        ref.set(note.toMap())
            .addOnSuccessListener { onDone(note) }
            .addOnFailureListener { onDone(null) }
    }

    override fun update(uid: String, note: Note, onDone: (Boolean) -> Unit) {
        notes(uid).document(note.id).set(note.toMap())
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    override fun delete(uid: String, noteId: String, onDone: (Boolean) -> Unit) {
        notes(uid).document(noteId).delete()
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    override fun deleteAll(uid: String, onDone: (Boolean) -> Unit) {
        notes(uid).get()
            .addOnSuccessListener { snap ->
                val batch = FirebaseFirestore.getInstance().batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener { onDone(false) }
            }
            .addOnFailureListener { onDone(false) }
    }

    override fun replaceAll(uid: String, items: List<Note>, onDone: (Boolean) -> Unit) {
        notes(uid).get()
            .addOnSuccessListener { snap ->
                val batch = FirebaseFirestore.getInstance().batch()
                snap.documents.forEach { batch.delete(it.reference) }
                items.forEach { batch.set(notes(uid).document(it.id), it.toMap()) }
                batch.commit()
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener { onDone(false) }
            }
            .addOnFailureListener { onDone(false) }
    }
}
