package com.studenthub.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    private fun emailFor(username: String): String = "${username.lowercase()}@studenthub.com"

    suspend fun login(username: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(emailFor(username), password).await()
            val user = result.user ?: return Result.failure(Exception("Login failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        username: String,
        password: String,
        fullName: String,
        dob: String,
        classLevel: String,
        schoolName: String
    ): Result<FirebaseUser> {
        return try {
            val usernameKey = username.lowercase()
            val usernameDocRef = firestore.collection("Usernames").document(usernameKey)
            val existing = usernameDocRef.get().await()
            if (existing.exists()) {
                return Result.failure(Exception("Username already taken"))
            }

            val authResult = auth.createUserWithEmailAndPassword(emailFor(username), password).await()
            val fbUser = authResult.user ?: return Result.failure(Exception("Registration failed"))

            val userData = hashMapOf(
                "uid" to fbUser.uid,
                "fullName" to fullName,
                "username" to usernameKey,
                "dob" to dob,
                "classLevel" to classLevel,
                "schoolName" to schoolName,
                "profilePhoto" to "",
                "bio" to "",
                "role" to "Student",
                "classAccess" to listOf(classLevel),
                "createdAt" to Timestamp.now(),
                "isBanned" to false,
                "timeoutExpiry" to null
            )

            firestore.collection("Users").document(fbUser.uid).set(userData).await()
            usernameDocRef.set(mapOf("uid" to fbUser.uid), SetOptions.merge()).await()

            Result.success(fbUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(username: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(emailFor(username)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
