package com.studenthub.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studenthub.app.data.model.User
import com.studenthub.app.data.repository.AuthRepository
import com.studenthub.app.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val uid = auth.currentUser?.uid ?: ""

    val user: StateFlow<User?> = userRepository.observeUser(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateBio(bio: String) {
        viewModelScope.launch {
            userRepository.updateProfile(uid, mapOf("bio" to bio))
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            userRepository.uploadProfilePhoto(uid, uri)
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
