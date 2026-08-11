package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studenthub.app.data.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun observeFor(toUid: String): Flow<List<AppNotification>> = callbackFlow {
        val reg = db.collection("Notifications")
            .whereEqualTo("toUid", toUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { d ->
                    d.toObject(AppNotification::class.java)?.copy(id = d.id)
                        ?: AppNotification(id = d.id)
                })
            }
        awaitClose { reg.remove() }
    }

    /** Combines the "all" broadcast stream with this user's targeted notifications,
     *  newest first — same merge the web client does when rendering the bell icon. */
    fun observeForUser(uid: String): Flow<List<AppNotification>> =
        combine(observeFor("all"), observeFor(uid)) { all, mine ->
            (all + mine).sortedByDescending { it.createdAt?.time ?: 0L }
        }

    suspend fun send(notification: AppNotification) {
        db.collection("Notifications").add(
            hashMapOf(
                "toUid" to notification.toUid,
                "title" to notification.title,
                "body" to notification.body,
                "photos" to notification.photos,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun delete(id: String) {
        db.collection("Notifications").document(id).delete().await()
    }
}
