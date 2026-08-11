package com.studenthub.app.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.Suggestion
import com.studenthub.app.data.repo.SuggestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val currentUser: AppUser,
    private val repo: SuggestionRepository = SuggestionRepository()
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted

    init {
        viewModelScope.launch { repo.observeAll().collect { _suggestions.value = it } }
    }

    fun submit(text: String, photoBase64: String?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _submitting.value = true
            repo.submit(currentUser.uid, currentUser.username, text, photoBase64)
            _submitting.value = false
            _submitted.value = true
        }
    }

    fun resetSubmitted() { _submitted.value = false }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
