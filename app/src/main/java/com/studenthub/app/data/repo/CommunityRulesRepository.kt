package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Mirrors Settings/CommunityRules { rules: "" } — single shared document. */
class CommunityRulesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val docRef = db.collection("Settings").document("CommunityRules")

    fun observeRules(): Flow<String> = callbackFlow {
        val reg = docRef.addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            trySend(snap?.getString("rules") ?: "")
        }
        awaitClose { reg.remove() }
    }

    suspend fun setRules(text: String) {
        docRef.set(mapOf("rules" to text)).await()
    }
}
