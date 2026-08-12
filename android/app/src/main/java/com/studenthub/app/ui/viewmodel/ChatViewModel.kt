package com.studenthub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studenthub.app.data.model.ChatMessage
import com.studenthub.app.data.model.ReplyTo
import com.studenthub.app.data.repository.ChatRepository
import com.studenthub.app.data.repository.SettingsRepository
import com.studenthub.app.data.repository.UserRepository
import com.studenthub.app.util.AntiAbuse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val roomId: String,
    private val isAnonymous: Boolean,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages(roomId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val bannedWords: StateFlow<List<String>> = settingsRepository.observeAntiAbuseWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    var replyTarget: ReplyTo? = null

    fun sendMessage(text: String, senderName: String, senderPhoto: String) {
        val uid = auth.currentUser?.uid ?: return
        if (text.isBlank()) return
        if (AntiAbuse.containsBannedWord(text, bannedWords.value)) {
            _sendError.value = "Your message contains a blocked word. Please rephrase."
            return
        }
        _sendError.value = null
        viewModelScope.launch {
            chatRepository.sendMessage(
                roomId = roomId,
                text = text,
                senderId = uid,
                senderName = senderName,
                senderPhoto = senderPhoto,
                isAnonymous = isAnonymous,
                replyTo = replyTarget
            )
            replyTarget = null
        }
    }

    fun clearError() {
        _sendError.value = null
    }
}
