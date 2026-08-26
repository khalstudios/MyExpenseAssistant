package com.expenseassistant.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import com.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.budgetRepository(app)

    val budgets: StateFlow<Map<String, Long>> = repository.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setOverall(limitMinor: Long) = viewModelScope.launch {
        repository.setBudget(null, limitMinor)
    }

    fun setCategory(category: Category, limitMinor: Long) = viewModelScope.launch {
        repository.setBudget(category, limitMinor)
    }

    fun limitFor(category: Category?): Long =
        budgets.value[BudgetEntity.keyFor(category)] ?: 0L

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BudgetViewModel(checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]))
            }
        }
    }
}
