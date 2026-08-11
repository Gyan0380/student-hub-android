package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.studenthub.app.data.model.AppTag
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TagRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = db.collection("Tags")

    fun observeAll(): Flow<List<AppTag>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(snap.documents.map { d ->
                d.toObject(AppTag::class.java)?.copy(id = d.id) ?: AppTag(id = d.id)
            })
        }
        awaitClose { reg.remove() }
    }

    suspend fun create(label: String, color: String, type: String) {
        col.add(mapOf("label" to label, "color" to color, "type" to type)).await()
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}
