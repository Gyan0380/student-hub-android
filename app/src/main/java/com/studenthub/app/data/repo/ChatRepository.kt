package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studenthub.app.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads/writes the exact same Firestore paths the website uses:
 *   Chats/{roomId}/Messages/{msgId}
 * so a message sent from the native app or the website shows up identically in both,
 * in the same order, for every user in that room.
 */
class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun messagesRef(roomId: String) =
        db.collection("Chats").document(roomId).collection("Messages")

    fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val reg = messagesRef(roomId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.map { d ->
                    d.toObject(Message::class.java)?.copy(id = d.id) ?: Message(id = d.id)
                })
            }
        awaitClose { reg.remove() }
    }

    /** Latest message only — used for the Home room-list preview + unread check. */
    fun observeLastMessage(roomId: String): Flow<Message?> = callbackFlow {
        val reg = messagesRef(roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val doc = snap.documents.firstOrNull()
                trySend(doc?.toObject(Message::class.java)?.copy(id = doc.id))
            }
        awaitClose { reg.remove() }
    }

    suspend fun send(roomId: String, message: Message) {
        messagesRef(roomId).add(
            hashMapOf(
                "text" to message.text,
                "photos" to message.photos,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderPhoto" to message.senderPhoto,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "replyTo" to message.replyTo?.let {
                    mapOf("id" to it.id, "text" to it.text, "senderName" to it.senderName)
                }
            )
        ).await()
    }

    suspend fun edit(roomId: String, messageId: String, newText: String) {
        messagesRef(roomId).document(messageId).update(
            mapOf(
                "text" to newText,
                "edited" to true,
                "editedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun delete(roomId: String, messageId: String) {
        messagesRef(roomId).document(messageId).delete().await()
    }
}
