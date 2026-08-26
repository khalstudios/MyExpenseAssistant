package com.expenseassistant.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expenseassistant.data.prefs.UserProfile
import com.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(app: Application) : AndroidViewModel(app) {

    private val preferences = ServiceLocator.userPreferences(app)
    private val repository = ServiceLocator.repository(app)
    private val budgetRepository = ServiceLocator.budgetRepository(app)

    private val _profile = MutableStateFlow(preferences.load())
    val profile: StateFlow<UserProfile> = _profile

    val transactionCount: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _earliest = MutableStateFlow<Long?>(null)
    val earliest: StateFlow<Long?> = _earliest

    init {
        viewModelScope.launch { _earliest.value = repository.earliestTimestamp() }
    }

    fun save(profile: UserProfile) {
        preferences.save(profile)
        _profile.value = profile
    }

    fun clearAllTransactions(alsoResetSettings: Boolean) = viewModelScope.launch {
        repository.deleteAll()
        if (alsoResetSettings) {
            repository.clearLearnedRules()
            budgetRepository.clearAll()
        }
        _earliest.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AccountViewModel(checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]))
            }
        }
    }
}
