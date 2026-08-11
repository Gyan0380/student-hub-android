package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studenthub.app.data.model.Suggestion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SuggestionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = db.collection("Suggestions")

    fun observeAll(): Flow<List<Suggestion>> = callbackFlow {
        val reg = col.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { d ->
                    d.toObject(Suggestion::class.java)?.copy(id = d.id) ?: Suggestion(id = d.id)
                })
            }
        awaitClose { reg.remove() }
    }

    suspend fun submit(uid: String, username: String, text: String, photo: String?) {
        col.add(
            hashMapOf(
                "uid" to uid,
                "username" to username,
                "text" to text,
                "photo" to photo,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}
