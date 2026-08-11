package com.studenthub.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.repo.AuthRepository
import com.studenthub.app.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    object ResetEmailSent : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    val currentUid: String? get() = authRepo.currentUid

    fun resetState() {
        _state.value = AuthUiState.Idle
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState.Error("Email aur password dono chahiye")
            return
        }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepo.signIn(email.trim(), password)
                .onSuccess { _state.value = AuthUiState.Success }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun register(email: String, password: String, username: String) {
        if (username.isBlank()) {
            _state.value = AuthUiState.Error("Username daalo")
            return
        }
        if (password.length < 6) {
            _state.value = AuthUiState.Error("Password kam se kam 6 characters ka ho")
            return
        }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepo.register(email.trim(), password)
                .onSuccess { user ->
                    runCatching { userRepo.createUserDoc(user.uid, username.trim()) }
                    _state.value = AuthUiState.Success
                }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Registration failed") }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _state.value = AuthUiState.Error("Email daalo")
            return
        }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepo.sendPasswordReset(email.trim())
                .onSuccess { _state.value = AuthUiState.ResetEmailSent }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Reset email nahi bheja ja saka") }
        }
    }

    fun signOut() {
        authRepo.signOut()
        _state.value = AuthUiState.Idle
    }
}
