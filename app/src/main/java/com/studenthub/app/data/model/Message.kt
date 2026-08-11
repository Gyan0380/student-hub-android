package com.studenthub.app.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors the fields written by the live website (see app.js `addDoc(collection(db,
 * "Chats", chatRoomId, "Messages"), ...)`). Field names MUST match exactly — this is what
 * keeps native-app messages and website messages interchangeable in the same room.
 */
data class ReplyPreview(
    val id: String = "",
    val text: String = "",
    val senderName: String = ""
)

data class Message(
    val id: String = "",
    val text: String = "",
    val photos: List<String>? = null,      // base64 strings, same encoding the website uses
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    val replyTo: ReplyPreview? = null,
    val edited: Boolean = false,
    @ServerTimestamp val editedAt: Date? = null
)
