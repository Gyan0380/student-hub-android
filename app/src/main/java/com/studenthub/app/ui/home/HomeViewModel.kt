package com.studenthub.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.Message
import com.studenthub.app.data.model.RoomIds
import com.studenthub.app.data.repo.ChatRepository
import com.studenthub.app.util.UnreadStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class RoomListItem(
    val roomId: String,
    val displayName: String,
    val lastMessage: Message? = null,
    val unread: Boolean = false
)

class HomeViewModel(
    private val currentUser: AppUser,
    private val unreadStore: UnreadStore,
    private val chatRepo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<RoomListItem>>(emptyList())
    val rooms: StateFlow<List<RoomListItem>> = _rooms

    init {
        val roomIds = buildList {
            add(RoomIds.GLOBAL)
            add(RoomIds.ANONYMOUS)
            currentUser.classAccess?.forEach { add(RoomIds.slugifyClassName(it)) }
            if (currentUser.isAdminOrOwner) add(RoomIds.ADMIN_ROOM)
        }.distinct()

        // Seed rooms immediately with display names so the list isn't empty while
        // the first Firestore snapshots come in.
        _rooms.value = roomIds.map { RoomListItem(it, displayNameFor(it)) }

        roomIds.forEach { roomId ->
            combine(
                chatRepo.observeLastMessage(roomId),
                unreadStore.seenAt(currentUser.uid, roomId)
            ) { lastMessage, seenAt ->
                val unread = (lastMessage?.createdAt?.time ?: 0L) > seenAt &&
                    lastMessage?.senderId != currentUser.uid
                RoomListItem(roomId, displayNameFor(roomId), lastMessage, unread)
            }.onEach { updated ->
                _rooms.value = _rooms.value.map { if (it.roomId == updated.roomId) updated else it }
            }.launchIn(viewModelScope)
        }
    }

    fun markSeen(roomId: String) {
        viewModelScope.launch { unreadStore.markSeen(currentUser.uid, roomId) }
    }

    private fun displayNameFor(roomId: String): String = when (roomId) {
        RoomIds.GLOBAL -> "Global Chat"
        RoomIds.ANONYMOUS -> "Anonymous"
        RoomIds.ADMIN_ROOM -> "Admin Room"
        else -> roomId.replace('-', ' ').replaceFirstChar { it.uppercase() }
    }
}
