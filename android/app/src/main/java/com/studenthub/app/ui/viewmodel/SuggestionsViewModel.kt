package com.studenthub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studenthub.app.data.repository.FeedbackRepository
import com.studenthub.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val feedbackRepository: FeedbackRepository = FeedbackRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    fun submit(text: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepository.observeUser(uid).first()
            feedbackRepository.submitSuggestion(uid, user?.username ?: "unknown", text)
            _submitted.value = true
        }
    }
}
