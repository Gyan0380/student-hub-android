package com.studenthub.app.data.model

import com.google.firebase.Timestamp

data class ReplyTo(
    val senderName: String = "",
    val text: String = ""
)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val photos: List<String> = emptyList(),
    val photoUrl: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val createdAtMillis: Long = 0L,
    val replyTo: ReplyTo? = null
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ChatMessage {
            val replyMap = map["replyTo"] as? Map<*, *>
            val reply = replyMap?.let {
                ReplyTo(
                    senderName = it["senderName"] as? String ?: "",
                    text = it["text"] as? String ?: ""
                )
            }
            return ChatMessage(
                id = id,
                text = map["text"] as? String ?: "",
                photos = (map["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                photoUrl = map["photoUrl"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                senderPhoto = map["senderPhoto"] as? String ?: "",
                createdAtMillis = (map["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                replyTo = reply
            )
        }
    }
}
