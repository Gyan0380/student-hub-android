package com.studenthub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studenthub.app.data.model.AppNotification
import com.studenthub.app.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val uid = auth.currentUser?.uid ?: ""

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.observeNotifications(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
