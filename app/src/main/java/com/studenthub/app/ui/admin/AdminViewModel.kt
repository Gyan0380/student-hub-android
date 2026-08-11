package com.studenthub.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppNotification
import com.studenthub.app.data.model.AppTag
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.repo.AntiAbuseRepository
import com.studenthub.app.data.repo.NotificationRepository
import com.studenthub.app.data.repo.TagRepository
import com.studenthub.app.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val currentUser: AppUser,
    private val userRepo: UserRepository = UserRepository(),
    private val tagRepo: TagRepository = TagRepository(),
    private val antiAbuseRepo: AntiAbuseRepository = AntiAbuseRepository(),
    private val notificationRepo: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _users = MutableStateFlow<List<AppUser>>(emptyList())
    val users: StateFlow<List<AppUser>> = _users

    private val _userQuery = MutableStateFlow("")
    val userQuery: StateFlow<String> = _userQuery

    private val _tags = MutableStateFlow<List<AppTag>>(emptyList())
    val tags: StateFlow<List<AppTag>> = _tags

    private val _antiAbuseWords = MutableStateFlow<List<String>>(emptyList())
    val antiAbuseWords: StateFlow<List<String>> = _antiAbuseWords

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    init {
        viewModelScope.launch { userRepo.observeAllUsers().collect { _users.value = it } }
        viewModelScope.launch { tagRepo.observeAll().collect { _tags.value = it } }
        viewModelScope.launch { antiAbuseRepo.observeWords().collect { _antiAbuseWords.value = it } }
    }

    fun onUserQueryChange(q: String) { _userQuery.value = q }

    fun filteredUsers(): List<AppUser> {
        val q = _userQuery.value.trim().lowercase()
        if (q.isEmpty()) return _users.value
        return _users.value.filter { it.username.lowercase().contains(q) }
    }

    fun setRole(target: AppUser, newRole: String) {
        viewModelScope.launch {
            runCatching { userRepo.setRole(currentUser, target.uid, target.role, newRole) }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun setBanned(target: AppUser, banned: Boolean) {
        viewModelScope.launch { userRepo.setBanned(target.uid, banned) }
    }

    /** Times out a user for [hours] hours from now; pass null hours to clear the timeout. */
    fun setTimeout(target: AppUser, hours: Int?) {
        viewModelScope.launch {
            val until = hours?.let { System.currentTimeMillis() + it * 3_600_000L }
            userRepo.setTimeout(target.uid, until)
        }
    }

    fun setClassAccess(target: AppUser, classes: List<String>) {
        viewModelScope.launch { userRepo.setClassAccess(target.uid, classes) }
    }

    fun toggleTagOnUser(target: AppUser, tagId: String) {
        viewModelScope.launch {
            val current = target.tags ?: emptyList()
            val updated = if (current.contains(tagId)) current - tagId else current + tagId
            userRepo.setTags(target.uid, updated)
        }
    }

    fun createTag(label: String, color: String) {
        if (label.isBlank()) return
        viewModelScope.launch { tagRepo.create(label, color, "general") }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch { tagRepo.delete(id) }
    }

    fun addAntiAbuseWord(word: String) {
        val w = word.trim().lowercase()
        if (w.isBlank() || _antiAbuseWords.value.contains(w)) return
        viewModelScope.launch { antiAbuseRepo.setWords(_antiAbuseWords.value + w) }
    }

    fun removeAntiAbuseWord(word: String) {
        viewModelScope.launch { antiAbuseRepo.setWords(_antiAbuseWords.value - word) }
    }

    fun sendNotification(title: String, body: String, toUid: String) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            notificationRepo.send(AppNotification(toUid = toUid, title = title, body = body))
            _sending.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }
}
