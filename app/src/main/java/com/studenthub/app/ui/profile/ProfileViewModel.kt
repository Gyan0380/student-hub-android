package com.studenthub.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.repo.AuthRepository
import com.studenthub.app.data.repo.UserRepository
import com.studenthub.app.util.AppThemeOption
import com.studenthub.app.util.ThemeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val currentUser: AppUser,
    private val themeStore: ThemeStore,
    private val userRepo: UserRepository = UserRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _bio = MutableStateFlow(currentUser.bio ?: "")
    val bio: StateFlow<String> = _bio

    private val _pendingPhotoBase64 = MutableStateFlow<String?>(null)
    val pendingPhotoBase64: StateFlow<String?> = _pendingPhotoBase64

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    val theme: StateFlow<AppThemeOption> = themeStore.theme
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, AppThemeOption.LIGHT)

    fun onBioChange(value: String) { _bio.value = value }

    fun onPhotoPicked(base64: String) { _pendingPhotoBase64.value = base64 }

    fun setTheme(option: AppThemeOption) {
        viewModelScope.launch { themeStore.setTheme(option) }
    }

    fun save() {
        viewModelScope.launch {
            _saving.value = true
            userRepo.updateProfile(
                uid = currentUser.uid,
                bio = _bio.value,
                profilePhotoBase64 = _pendingPhotoBase64.value
            )
            _saving.value = false
            _saved.value = true
        }
    }

    fun signOut() = authRepo.signOut()
}
