package com.studenthub.app.data.model

import com.google.firebase.Timestamp

data class FeedbackItem(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): FeedbackItem {
            return FeedbackItem(
                id = id,
                uid = map["uid"] as? String ?: "",
                username = map["username"] as? String ?: "",
                text = map["text"] as? String ?: "",
                createdAtMillis = (map["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
            )
        }
    }
}
