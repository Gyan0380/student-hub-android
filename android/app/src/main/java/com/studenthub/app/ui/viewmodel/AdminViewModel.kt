package com.studenthub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studenthub.app.data.model.User
import com.studenthub.app.data.repository.NotificationRepository
import com.studenthub.app.data.repository.SettingsRepository
import com.studenthub.app.data.repository.UserRepository
import com.studenthub.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    val allUsers: StateFlow<List<User>> = userRepository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<String>> = settingsRepository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val antiAbuseWords: StateFlow<List<String>> = settingsRepository.observeAntiAbuseWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendGlobalNotification(title: String, body: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = userRepository.observeUser(uid).first()
            notificationRepository.sendGlobalNotification(title, body, user?.username ?: "admin")
        }
    }

    fun updateRules(rules: List<String>) {
        viewModelScope.launch { settingsRepository.updateRules(rules) }
    }

    fun updateAntiAbuseWords(words: List<String>) {
        viewModelScope.launch { settingsRepository.updateAntiAbuseWords(words) }
    }

    fun setUserRole(uid: String, role: String) {
        viewModelScope.launch { userRepository.setUserRole(uid, role) }
    }


    fun timeoutUser(uid: String, minutes: Int) {
        viewModelScope.launch { userRepository.timeoutUser(uid, minutes) }
    }

    fun setBanned(uid: String, isBanned: Boolean) {
        viewModelScope.launch { userRepository.setBanned(uid, isBanned) }
    }

    fun deleteMessage(roomId: String, messageId: String) {
        viewModelScope.launch { chatRepository.deleteMessage(roomId, messageId) }
    }
}
