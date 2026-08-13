package com.studenthub.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studenthub.app.data.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val registration = firestore.collection("Notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { AppNotification.fromMap(doc.id, it) }
                }.filter { it.toUid == "all" || it.toUid == uid }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendGlobalNotification(title: String, body: String, sentBy: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "toUid" to "all",
                "title" to title,
                "body" to body,
                "createdAt" to Timestamp.now(),
                "sentBy" to sentBy
            )
            firestore.collection("Notifications").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
