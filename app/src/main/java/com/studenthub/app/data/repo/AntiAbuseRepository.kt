package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Mirrors Settings/AntiAbuse { words: [] } — banned-word list used by admins to filter chat. */
class AntiAbuseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val docRef = db.collection("Settings").document("AntiAbuse")

    fun observeWords(): Flow<List<String>> = callbackFlow {
        val reg = docRef.addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            trySend((snap?.get("words") as? List<String>) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    suspend fun setWords(words: List<String>) {
        docRef.set(mapOf("words" to words)).await()
    }
}
