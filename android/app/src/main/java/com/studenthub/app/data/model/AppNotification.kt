package com.studenthub.app.data.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val toUid: String = "",
    val title: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
    val sentBy: String = ""
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AppNotification {
            return AppNotification(
                id = id,
                toUid = map["toUid"] as? String ?: "",
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                createdAtMillis = (map["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                sentBy = map["sentBy"] as? String ?: ""
            )
        }
    }
}
