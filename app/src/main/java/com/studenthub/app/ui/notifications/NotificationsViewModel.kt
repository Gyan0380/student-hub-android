package com.studenthub.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppNotification
import com.studenthub.app.data.repo.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val uid: String,
    private val repo: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<AppNotification>>(emptyList())
    val items: StateFlow<List<AppNotification>> = _items

    init {
        viewModelScope.launch {
            repo.observeForUser(uid).collect { _items.value = it }
        }
    }
}
