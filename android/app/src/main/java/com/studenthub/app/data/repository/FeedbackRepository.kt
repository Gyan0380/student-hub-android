package com.studenthub.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FeedbackRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun submitSuggestion(uid: String, username: String, text: String): Result<Unit> {
        return try {
            firestore.collection("Suggestions").add(
                hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "text" to text,
                    "createdAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitBugReport(uid: String, username: String, text: String): Result<Unit> {
        return try {
            firestore.collection("BugReports").add(
                hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "text" to text,
                    "createdAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
