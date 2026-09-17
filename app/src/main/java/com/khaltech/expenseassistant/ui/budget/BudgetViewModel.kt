package com.khaltech.expenseassistant.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khaltech.expenseassistant.data.model.BudgetEntity
import com.khaltech.expenseassistant.data.model.BudgetPeriod
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.budgetRepository(app)

    private val _period = MutableStateFlow(BudgetPeriod.MONTHLY)

    /** Which set of limits is being edited; the screen shows one period at a time. */
    val period: StateFlow<BudgetPeriod> = _period

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<Map<String, Long>> = _period
        .flatMapLatest { repository.observeBudgets(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setPeriod(period: BudgetPeriod) {
        _period.value = period
    }

    fun setOverall(limitMinor: Long) = viewModelScope.launch {
        repository.setBudget(null, limitMinor, _period.value)
    }

    fun setCategory(category: Category, limitMinor: Long) = viewModelScope.launch {
        repository.setBudget(category, limitMinor, _period.value)
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
