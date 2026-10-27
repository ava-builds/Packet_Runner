package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/** Keeps each player's save in one Firestore document: players/{uid}. */
class FirestoreSaveRepository : SaveRepository {
    private fun doc(uid: String) = FirebaseFirestore.getInstance().collection(COLLECTION).document(uid)

    override fun load(uid: String, onDone: (LoadResult) -> Unit) {
        doc(uid).get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.data
                onDone(LoadResult.Found(if (snapshot.exists() && data != null) SaveData.fromMap(data) else null))
            }
            .addOnFailureListener { onDone(LoadResult.Failed(loadMessage(it))) }
    }

    override fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit) {
        doc(uid).set(data.toMap())
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    override fun delete(uid: String, onDone: (Boolean) -> Unit) {
        doc(uid).delete()
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    private fun loadMessage(e: Exception): String =
        if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "Firestore does not allow this yet. Check the security rules in the README."
        } else {
            "Could not load your town. Check your internet connection."
        }

    private companion object {
        const val COLLECTION = "players"
    }
}
