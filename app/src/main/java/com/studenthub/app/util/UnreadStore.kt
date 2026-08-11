package com.studenthub.app.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.unreadDataStore by preferencesDataStore(name = "studenthub_unread")

/**
 * Native port of the web app's `studenthub-seen-{uid}-{roomId}` localStorage keys.
 * Stores, per (uid, roomId), the timestamp (epoch millis) of the last message the user
 * has seen in that room. A room is "unread" when the latest message's createdAt is
 * newer than the stored seen-timestamp.
 */
class UnreadStore(private val context: Context) {

    private fun key(uid: String, roomId: String) =
        longPreferencesKey("seen_${uid}_$roomId")

    fun seenAt(uid: String, roomId: String): Flow<Long> =
        context.unreadDataStore.data.map { prefs -> prefs[key(uid, roomId)] ?: 0L }

    suspend fun markSeen(uid: String, roomId: String, atMillis: Long = System.currentTimeMillis()) {
        context.unreadDataStore.edit { prefs -> prefs[key(uid, roomId)] = atMillis }
    }
}
