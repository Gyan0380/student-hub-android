package com.studenthub.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studenthub.app.data.repo.CommunityRulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommunityRulesViewModel(
    private val repo: CommunityRulesRepository = CommunityRulesRepository()
) : ViewModel() {

    private val _rules = MutableStateFlow("")
    val rules: StateFlow<String> = _rules

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    init {
        viewModelScope.launch { repo.observeRules().collect { _rules.value = it } }
    }

    fun save(text: String) {
        viewModelScope.launch {
            _saving.value = true
            repo.setRules(text)
            _saving.value = false
        }
    }
}
