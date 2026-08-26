package com.expenseassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val monthSpendMinor: Long = 0,
    val monthIncomeMinor: Long = 0,
    val spendByCategory: List<Pair<Category, Long>> = emptyList(),
    val needsReviewCount: Int = 0,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    val uiState: StateFlow<HomeUiState> = repository.observeSince(startOfMonth())
        .map { transactions -> transactions.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun recategorize(id: Long, category: Category) = viewModelScope.launch {
        repository.recategorize(id, category)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    private fun List<TransactionEntity>.toUiState(): HomeUiState {
        val debits = filter { it.direction == Direction.DEBIT }
        return HomeUiState(
            transactions = this,
            monthSpendMinor = debits.sumOf { it.amountMinor },
            monthIncomeMinor = filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            spendByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            needsReviewCount = count { !it.userCorrected && it.categoryConfidence < 0.6f },
        )
    }

    private fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]))
            }
        }
    }
}
