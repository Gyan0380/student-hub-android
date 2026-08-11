package com.studenthub.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.Message
import com.studenthub.app.data.model.ReplyPreview
import com.studenthub.app.data.repo.AuthRepository
import com.studenthub.app.data.repo.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val roomId: String,
    private val currentUser: AppUser,
    private val chatRepo: ChatRepository = ChatRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _replyTo = MutableStateFlow<ReplyPreview?>(null)
    val replyTo: StateFlow<ReplyPreview?> = _replyTo

    // Up to 5 base64-encoded photos staged for the next send (matches web's photos[] cap).
    private val _pendingPhotos = MutableStateFlow<List<String>>(emptyList())
    val pendingPhotos: StateFlow<List<String>> = _pendingPhotos
    val maxPhotos = 5

    init {
        viewModelScope.launch {
            chatRepo.observeMessages(roomId).collect { _messages.value = it }
        }
    }

    /** Mirrors the web fix: reply is open to everyone; edit is own-message-only;
     *  delete is own-message-or-admin. Non-admins get no menu at all on others' messages. */
    fun canOpenMenu(message: Message) = isMine(message) || currentUser.isAdminOrOwner
    fun canEdit(message: Message) = isMine(message)
    fun canDelete(message: Message) = isMine(message) || currentUser.isAdminOrOwner
    private fun isMine(message: Message) = message.senderId == currentUser.uid

    /** Whether the current user is @mentioned in this message (word-boundary match on
     *  their username, case-insensitive — same rule as the web build's mention highlight). */
    fun isMentioned(message: Message): Boolean {
        val username = currentUser.username
        if (username.isBlank() || isMine(message)) return false
        val pattern = Regex("(?<![\\w.])@${Regex.escape(username)}(?![\\w.])", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(message.text)
    }

    fun setReply(message: Message) {
        _replyTo.value = ReplyPreview(message.id, message.text, message.senderName)
    }
    fun clearReply() { _replyTo.value = null }

    fun addPhoto(base64: String) {
        if (_pendingPhotos.value.size >= maxPhotos) return
        _pendingPhotos.value = _pendingPhotos.value + base64
    }

    fun removePhoto(index: Int) {
        _pendingPhotos.value = _pendingPhotos.value.filterIndexed { i, _ -> i != index }
    }

    fun send(text: String) {
        if (text.isBlank() && _pendingPhotos.value.isEmpty()) return
        viewModelScope.launch {
            chatRepo.send(
                roomId,
                Message(
                    text = text,
                    photos = _pendingPhotos.value.ifEmpty { null },
                    senderId = currentUser.uid,
                    senderName = currentUser.username,
                    senderPhoto = currentUser.profilePhoto ?: "",
                    replyTo = _replyTo.value
                )
            )
            _replyTo.value = null
            _pendingPhotos.value = emptyList()
        }
    }

    fun edit(messageId: String, newText: String) {
        viewModelScope.launch { chatRepo.edit(roomId, messageId, newText) }
    }

    fun delete(messageId: String) {
        viewModelScope.launch { chatRepo.delete(roomId, messageId) }
    }
}
