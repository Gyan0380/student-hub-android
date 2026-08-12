package com.studenthub.app.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val dob: String = "",
    val classLevel: String = "",
    val schoolName: String = "",
    val profilePhoto: String = "",
    val bio: String = "",
    val role: String = "Student",
    val classAccess: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val isBanned: Boolean = false,
    val timeoutExpiry: Timestamp? = null
) {
    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): User {
            return User(
                uid = uid,
                fullName = map["fullName"] as? String ?: "",
                username = map["username"] as? String ?: "",
                dob = map["dob"] as? String ?: "",
                classLevel = map["classLevel"] as? String ?: "",
                schoolName = map["schoolName"] as? String ?: "",
                profilePhoto = map["profilePhoto"] as? String ?: "",
                bio = map["bio"] as? String ?: "",
                role = map["role"] as? String ?: "Student",
                classAccess = (map["classAccess"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = map["createdAt"] as? Timestamp,
                isBanned = map["isBanned"] as? Boolean ?: false,
                timeoutExpiry = map["timeoutExpiry"] as? Timestamp
            )
        }
    }
}
