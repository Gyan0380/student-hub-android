package com.studenthub.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FcmTokenRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun saveToken(uid: String, token: String): Result<Unit> {
        return try {
            firestore.collection("Users").document(uid)
                .collection("FcmTokens").document(token)
                .set(
                    mapOf(
                        "enabled" to true,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
