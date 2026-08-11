package com.studenthub.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.studenthub.app.data.model.AppUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun toAppUser(uid: String, snap: com.google.firebase.firestore.DocumentSnapshot): AppUser =
        AppUser(
            uid = uid,
            username = snap.getString("username") ?: "Student",
            role = snap.getString("role") ?: "Student",
            profilePhoto = snap.getString("profilePhoto"),
            bio = snap.getString("bio"),
            tags = snap.get("tags") as? List<String>,
            isBanned = snap.getBoolean("isBanned") ?: false,
            timedOutUntil = snap.getLong("timedOutUntil"),
            classAccess = snap.get("classAccess") as? List<String>
        )

    suspend fun getUser(uid: String): AppUser? {
        val snap = db.collection("Users").document(uid).get().await()
        if (!snap.exists()) return null
        return toAppUser(uid, snap)
    }

    suspend fun createUserDoc(uid: String, username: String) {
        db.collection("Users").document(uid).set(
            mapOf(
                "username" to username,
                "role" to "Student",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Edit Profile screen: updates only the fields the website's edit-profile modal
     * exposes (bio, profilePhoto as base64). Username changes are intentionally not
     * offered here since usernames are used as chat identity/search keys elsewhere.
     * Pass null for a field to leave it unchanged.
     */
    suspend fun updateProfile(uid: String, bio: String?, profilePhotoBase64: String?) {
        val updates = mutableMapOf<String, Any>()
        if (bio != null) updates["bio"] = bio
        if (profilePhotoBase64 != null) updates["profilePhoto"] = profilePhotoBase64
        if (updates.isEmpty()) return
        db.collection("Users").document(uid).update(updates).await()
    }

    // ---- Admin panel operations ----

    /** Live list of all users, for the admin "manage users" screen. Client-side search/
     *  filter by username happens in the ViewModel — the collection is small enough
     *  (a class chat app) that a full live listener is simpler than paginated queries. */
    fun observeAllUsers(): Flow<List<AppUser>> = callbackFlow {
        val reg = db.collection("Users").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(snap.documents.map { d -> toAppUser(d.id, d) })
        }
        awaitClose { reg.remove() }
    }

    /**
     * Change a user's role. Mirrors the web build's `roleUnchanged()` Firestore-rules
     * guard (see NATIVE_BUILD_PROMPT.md "Security" note): only an Owner may grant or
     * revoke the Owner role. This client-side check is a UX guard only — the real
     * enforcement is (and must stay) in firestore.rules; don't rely on this alone.
     */
    suspend fun setRole(actingUser: AppUser, targetUid: String, targetCurrentRole: String, newRole: String) {
        val touchesOwner = targetCurrentRole == "Owner" || newRole == "Owner"
        if (touchesOwner && !actingUser.isOwner) {
            throw IllegalStateException("Only the Owner can grant or revoke the Owner role.")
        }
        db.collection("Users").document(targetUid).update("role", newRole).await()
    }

    suspend fun setBanned(uid: String, banned: Boolean) {
        db.collection("Users").document(uid).update("isBanned", banned).await()
    }

    /** Pass null to clear an active timeout. */
    suspend fun setTimeout(uid: String, untilMillis: Long?) {
        db.collection("Users").document(uid).update("timedOutUntil", untilMillis).await()
    }

    suspend fun setClassAccess(uid: String, classes: List<String>) {
        db.collection("Users").document(uid).update("classAccess", classes).await()
    }

    suspend fun setTags(uid: String, tags: List<String>) {
        db.collection("Users").document(uid).update("tags", tags).await()
    }

    /**
     * Step 8 (NATIVE_BUILD_PROMPT.md): store the device's current FCM token on the
     * user doc so admin "Send Notification" can eventually target devices directly.
     * Safe to call every time a token is obtained (login, app start, onNewToken) —
     * just overwrites the field. Swallows failures silently since this is best-effort
     * (e.g. offline at login shouldn't block sign-in).
     */
    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            db.collection("Users").document(uid).update("fcmToken", token).await()
        } catch (e: Exception) {
            // best-effort; e.g. user doc not created yet, or offline
        }
    }
}
