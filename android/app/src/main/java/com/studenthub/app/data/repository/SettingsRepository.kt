package com.studenthub.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeRules(): Flow<List<String>> = callbackFlow {
        val registration = firestore.collection("Settings").document("CommunityRules")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rules = (snapshot.get("rules") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                trySend(rules)
            }
        awaitClose { registration.remove() }
    }

    fun observeAntiAbuseWords(): Flow<List<String>> = callbackFlow {
        val registration = firestore.collection("Settings").document("AntiAbuse")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val words = (snapshot.get("words") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                trySend(words)
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateRules(rules: List<String>): Result<Unit> {
        return try {
            firestore.collection("Settings").document("CommunityRules")
                .set(mapOf("rules" to rules)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAntiAbuseWords(words: List<String>): Result<Unit> {
        return try {
            firestore.collection("Settings").document("AntiAbuse")
                .set(mapOf("words" to words)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
