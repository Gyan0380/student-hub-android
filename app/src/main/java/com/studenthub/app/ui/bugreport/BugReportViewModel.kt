package com.studenthub.app.ui.bugreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.BugReport
import com.studenthub.app.data.repo.BugReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BugReportViewModel(
    private val currentUser: AppUser,
    private val repo: BugReportRepository = BugReportRepository()
) : ViewModel() {

    private val _reports = MutableStateFlow<List<BugReport>>(emptyList())
    val reports: StateFlow<List<BugReport>> = _reports

    private val _pendingPhotos = MutableStateFlow<List<String>>(emptyList())
    val pendingPhotos: StateFlow<List<String>> = _pendingPhotos
    val maxPhotos = 5

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted

    init {
        viewModelScope.launch { repo.observeAll().collect { _reports.value = it } }
    }

    fun addPhoto(base64: String) {
        if (_pendingPhotos.value.size >= maxPhotos) return
        _pendingPhotos.value = _pendingPhotos.value + base64
    }

    fun removePhoto(index: Int) {
        _pendingPhotos.value = _pendingPhotos.value.filterIndexed { i, _ -> i != index }
    }

    fun submit(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _submitting.value = true
            repo.submit(currentUser.uid, currentUser.username, text, _pendingPhotos.value)
            _submitting.value = false
            _pendingPhotos.value = emptyList()
            _submitted.value = true
        }
    }

    fun resetSubmitted() { _submitted.value = false }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
