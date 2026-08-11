package com.studenthub.app.data.model

/**
 * Canonical `Chats/{roomId}` values — must match the website exactly so a message sent
 * from either surface lands in the same Firestore room (see NATIVE_BUILD_PROMPT.md,
 * "roomId values: "global", "anonymous", "admin-room", or a slugified class name").
 *
 * This file was referenced by HomeViewModel.kt from Step 4 but never actually created —
 * that's the "Unresolved reference: RoomIds" compile failure.
 */
object RoomIds {
    const val GLOBAL = "global"
    const val ANONYMOUS = "anonymous"
    const val ADMIN_ROOM = "admin-room"

    /**
     * Turns a class-access entry (e.g. "Class 1", "12th Pass / College" — see
     * CLASS_ROOM_OPTIONS in User.kt) into its room-id slug: lowercase, non-alphanumeric
     * runs collapsed to a single "-", leading/trailing "-" trimmed.
     * "Class 1" -> "class-1", "12th Pass / College" -> "12th-pass-college".
     */
    fun slugifyClassName(className: String): String =
        className
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
