package com.studenthub.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Mirrors Notifications/{id}. toUid is either "all" or a specific uid. */
data class AppNotification(
    val id: String = "",
    val toUid: String = "all",
    val title: String = "",
    val body: String = "",
    val photos: List<String>? = null,
    @ServerTimestamp val createdAt: Date? = null
)

/** Mirrors Suggestions/{id}. */
data class Suggestion(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val photo: String? = null,
    @ServerTimestamp val createdAt: Date? = null
)

/** Mirrors BugReports/{id}. */
data class BugReport(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val photos: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null
)
