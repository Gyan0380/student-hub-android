package com.studenthub.app.data.model

data class AppUser(
    val uid: String = "",
    val username: String = "",
    val role: String = "Student",     // "Student" | "Admin" | "Owner"
    val profilePhoto: String? = null,
    val bio: String? = null,
    val tags: List<String>? = null,
    val isBanned: Boolean = false,
    val timedOutUntil: Long? = null,  // epoch millis; null/past = not timed out
    val classAccess: List<String>? = null
) {
    val isAdminOrOwner: Boolean get() = role == "Admin" || role == "Owner"
    val isOwner: Boolean get() = role == "Owner"
    val isTimedOut: Boolean get() = (timedOutUntil ?: 0L) > System.currentTimeMillis()
}

/** Class-room options shown in the admin's class-access picker — mirrors the web app's
 *  "Class 1–12 + 12th Pass / College" room list. */
val CLASS_ROOM_OPTIONS: List<String> =
    (1..12).map { "Class $it" } + "12th Pass / College"

