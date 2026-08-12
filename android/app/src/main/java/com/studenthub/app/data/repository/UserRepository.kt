package com.studenthub.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.studenthub.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.net.Uri

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val registration = firestore.collection("Users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(User.fromMap(uid, data))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val registration = firestore.collection("Users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { User.fromMap(doc.id, it) }
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateProfile(uid: String, fields: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("Users").document(uid).set(fields, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(uid: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("profilePhotos/$uid.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            firestore.collection("Users").document(uid)
                .set(mapOf("profilePhoto" to url), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUserRole(uid: String, role: String): Result<Unit> {
        return try {
            firestore.collection("Users").document(uid)
                .set(mapOf("role" to role), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun timeoutUser(uid: String, minutes: Int): Result<Unit> {
        return try {
            val expiry = com.google.firebase.Timestamp(
                com.google.firebase.Timestamp.now().seconds + minutes * 60L, 0
            )
            firestore.collection("Users").document(uid)
                .set(mapOf("timeoutExpiry" to expiry), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setBanned(uid: String, isBanned: Boolean): Result<Unit> {
        return try {
            firestore.collection("Users").document(uid)
                .set(mapOf("isBanned" to isBanned), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
