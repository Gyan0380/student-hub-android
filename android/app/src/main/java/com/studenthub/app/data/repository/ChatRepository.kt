package com.studenthub.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studenthub.app.data.model.ChatMessage
import com.studenthub.app.data.model.ReplyTo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val ANONYMOUS_DISPLAY_NAME = "Anonymous Ninja"

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = firestore.collection("Chats").document(roomId)
            .collection("Messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { ChatMessage.fromMap(doc.id, it) }
                }
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        roomId: String,
        text: String,
        senderId: String,
        senderName: String,
        senderPhoto: String,
        isAnonymous: Boolean,
        replyTo: ReplyTo? = null,
        photos: List<String> = emptyList(),
        photoUrl: String = ""
    ): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any?>(
                "text" to text,
                "photos" to photos,
                "photoUrl" to photoUrl,
                "senderId" to senderId,
                "senderName" to if (isAnonymous) ANONYMOUS_DISPLAY_NAME else senderName,
                "senderPhoto" to if (isAnonymous) "" else senderPhoto,
                "createdAt" to Timestamp.now()
            )
            if (replyTo != null) {
                data["replyTo"] = mapOf(
                    "senderName" to replyTo.senderName,
                    "text" to replyTo.text
                )
            }
            firestore.collection("Chats").document(roomId)
                .collection("Messages")
                .add(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun deleteMessage(roomId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("Chats").document(roomId)
                .collection("Messages").document(messageId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
