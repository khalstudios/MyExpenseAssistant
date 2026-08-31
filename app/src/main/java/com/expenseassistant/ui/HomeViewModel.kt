package com.expenseassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.data.repo.TagUsage
import com.expenseassistant.di.ServiceLocator
import com.expenseassistant.recurring.RecurringDetector
import com.expenseassistant.recurring.RecurringExpense
import com.expenseassistant.ui.insights.AnalyticsRange
import com.expenseassistant.ui.insights.AnalyticsUiState
import com.expenseassistant.ui.insights.BudgetProgress
import com.expenseassistant.ui.insights.PeriodSelection
import com.expenseassistant.ui.insights.Periods
import com.expenseassistant.ui.insights.PieSlice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val spendMinor: Long = 0,
    val incomeMinor: Long = 0,
    val spendByCategory: List<Pair<Category, Long>> = emptyList(),
    val needsReviewCount: Int = 0,
)

/** Auto-categorised with low confidence and never confirmed by the user. */
val TransactionEntity.needsCategoryReview: Boolean
    get() = !userCorrected && categoryConfidence < 0.6f

private data class PeriodSnapshot(
    val selection: PeriodSelection,
    val transactions: List<TransactionEntity>,
    val budgets: Map<String, Long>,
)

/** Scope of the home summary card, independent of the Insights period navigation. */
enum class SummaryScope(val label: String) { MONTH("Month"), YEAR("Year"), ALL("All time") }

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)
    private val budgetRepository = ServiceLocator.budgetRepository(app)

    private val _period = MutableStateFlow(PeriodSelection.now(AnalyticsRange.MONTH))
    val period: StateFlow<PeriodSelection> = _period

    @OptIn(ExperimentalCoroutinesApi::class)
    private val snapshot: StateFlow<PeriodSnapshot> = _period
        .flatMapLatest { selection ->
            combine(
                repository.observeBetween(Periods.previousStart(selection), Periods.endExclusive(selection)),
                budgetRepository.observeBudgets(),
            ) { transactions, budgets -> PeriodSnapshot(selection, transactions, budgets) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PeriodSnapshot(_period.value, emptyList(), emptyMap()),
        )

    val uiState: StateFlow<HomeUiState> = snapshot
        .map { it.toHomeState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _summaryScope = MutableStateFlow(SummaryScope.MONTH)
    val summaryScope: StateFlow<SummaryScope> = _summaryScope

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentState: StateFlow<HomeUiState> = _summaryScope
        .flatMapLatest { scope -> repository.observeBetween(scope.startMillis(), scope.endExclusiveMillis()) }
        .map { transactions -> transactions.toRecentHomeState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setSummaryScope(scope: SummaryScope) {
        _summaryScope.value = scope
    }

    private fun SummaryScope.startMillis(): Long = when (this) {
        SummaryScope.MONTH -> Periods.start(PeriodSelection.now(AnalyticsRange.MONTH))
        SummaryScope.YEAR -> Periods.start(PeriodSelection.now(AnalyticsRange.YEAR))
        SummaryScope.ALL -> 0L
    }

    /** Bounded so transactions dated in a future month don't leak into the current period. */
    private fun SummaryScope.endExclusiveMillis(): Long = when (this) {
        SummaryScope.MONTH -> Periods.endExclusive(PeriodSelection.now(AnalyticsRange.MONTH))
        SummaryScope.YEAR -> Periods.endExclusive(PeriodSelection.now(AnalyticsRange.YEAR))
        SummaryScope.ALL -> Long.MAX_VALUE
    }

    fun observeTransaction(id: Long) = repository.observeById(id)

    /** Every low-confidence transaction, regardless of period, for the "needs a category" screen. */
    val needsReviewTransactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .map { transactions -> transactions.filter { it.needsCategoryReview } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analytics: StateFlow<AnalyticsUiState> = snapshot
        .map { it.toAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    val recurring: StateFlow<List<RecurringExpense>> =
        repository.observeSince(sixMonthsAgo())
            .map { RecurringDetector.detect(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagSuggestions: StateFlow<List<String>> = repository.observeTagSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagUsage: StateFlow<List<TagUsage>> = repository.observeTagUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customCategories: StateFlow<List<com.expenseassistant.data.repo.CustomCategoryOption>> =
        repository.observeCustomCategorySuggestions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun transactionsForTag(tag: String): List<TransactionEntity> = repository.transactionsForTag(tag)

    fun setRange(range: AnalyticsRange) {
        _period.value = Periods.withRange(_period.value, range)
    }

    fun shiftPeriod(delta: Int) {
        val next = Periods.shift(_period.value, delta)
        if (delta > 0 && Periods.start(next) > System.currentTimeMillis()) return
        _period.value = next
    }

    fun jumpTo(year: Int, monthIndex: Int) {
        _period.value = Periods.jumpTo(_period.value.range, year, monthIndex)
    }

    fun resetToCurrent() {
        _period.value = PeriodSelection.now(_period.value.range)
    }

    private fun PeriodSnapshot.currentTransactions(): List<TransactionEntity> {
        val start = Periods.start(selection)
        return transactions.filter { it.occurredAt >= start }
    }

    private fun PeriodSnapshot.toHomeState(): HomeUiState {
        val current = currentTransactions()
        val debits = current.filter { it.direction == Direction.DEBIT }
        return HomeUiState(
            transactions = current,
            spendMinor = debits.sumOf { it.amountMinor },
            incomeMinor = current.filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            spendByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            needsReviewCount = current.count { it.needsCategoryReview },
        )
    }

    private fun List<TransactionEntity>.toRecentHomeState(): HomeUiState {
        val debits = filter { it.direction == Direction.DEBIT }
        return HomeUiState(
            transactions = this,
            spendMinor = debits.sumOf { it.amountMinor },
            incomeMinor = filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            spendByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            needsReviewCount = count { it.needsCategoryReview },
        )
    }

    private fun PeriodSnapshot.toAnalytics(): AnalyticsUiState {
        val start = Periods.start(selection)
        val current = currentTransactions()
        val previousSpend = transactions
            .filter { it.occurredAt < start && it.direction == Direction.DEBIT }
            .sumOf { it.amountMinor }

        val debits = current.filter { it.direction == Direction.DEBIT }
        val totalSpend = debits.sumOf { it.amountMinor }
        val elapsedDays = Periods.elapsedDays(selection)
        val totalDays = Periods.totalDays(selection)
        val dailyAverage = totalSpend / elapsedDays
        val spentByCategory = debits.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
        val paceFraction = elapsedDays.toFloat() / totalDays

        return AnalyticsUiState(
            selection = selection,
            transactions = current,
            periodLabel = Periods.label(selection),
            canGoForward = Periods.canGoForward(selection),
            isCurrentPeriod = Periods.isCurrent(selection),
            slices = spentByCategory
                .map { (category, amount) ->
                    PieSlice(
                        category = category,
                        amountMinor = amount,
                        fraction = if (totalSpend > 0) amount.toFloat() / totalSpend else 0f,
                        transactionCount = debits.count { it.category == category },
                    )
                }
                .sortedByDescending { it.amountMinor },
            totalSpendMinor = totalSpend,
            totalIncomeMinor = current.filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            previousTotalSpendMinor = previousSpend,
            dailyAverageMinor = dailyAverage,
            projectedTotalMinor = dailyAverage * totalDays,
            transactionCount = current.size,
            activeDays = debits.map { dayKey(it.occurredAt) }.distinct().size,
            largestTransaction = debits.maxByOrNull { it.amountMinor },
            topMerchant = debits.groupBy { it.merchant }
                .map { (merchant, items) -> merchant to items.sumOf { it.amountMinor } }
                .maxByOrNull { it.second },
            needsReviewCount = current.count { it.needsCategoryReview },
            overallBudget = budgets[BudgetEntity.OVERALL]?.let { limit ->
                BudgetProgress(null, limit, totalSpend, paceFraction)
            },
            categoryBudgets = budgets
                .filterKeys { it != BudgetEntity.OVERALL }
                .map { (key, limit) ->
                    val category = Category.fromName(key)
                    BudgetProgress(category, limit, spentByCategory[category] ?: 0L, paceFraction)
                }
                .sortedByDescending { it.fraction },
        )
    }

    fun recategorize(id: Long, category: Category, customName: String? = null, customColorHex: String? = null, customIconKey: String? = null) = viewModelScope.launch {
        repository.recategorize(id, category, customName, customColorHex, customIconKey)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    fun updateDescription(id: Long, description: String) = viewModelScope.launch {
        repository.updateDescription(id, description)
    }

    fun updateTags(id: Long, tags: List<String>) = viewModelScope.launch {
        repository.updateTags(id, tags)
    }

    fun updatePaymentMode(id: Long, mode: PaymentMode) = viewModelScope.launch {
        repository.updatePaymentMode(id, mode)
    }

    fun updateCore(id: Long, amountMinor: Long, direction: Direction, merchant: String, occurredAt: Long) =
        viewModelScope.launch {
            repository.updateCore(id, amountMinor, direction, merchant, occurredAt)
        }

    fun addManualTransaction(
        amountMinor: Long,
        direction: Direction,
        merchant: String,
        category: Category,
        customCategoryName: String? = null,
        customCategoryColor: String? = null,
        customCategoryIcon: String? = null,
        paymentMode: PaymentMode,
        occurredAt: Long,
        description: String,
        tags: List<String>,
    ) = viewModelScope.launch {
        repository.addManual(
            amountMinor = amountMinor,
            direction = direction,
            merchant = merchant,
            category = category,
            customCategoryName = customCategoryName,
            customCategoryColor = customCategoryColor,
            customCategoryIcon = customCategoryIcon,
            paymentMode = paymentMode,
            occurredAt = occurredAt,
            description = description,
            tags = tags,
        )
    }

    private fun dayKey(epochMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = epochMillis }.let {
            it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
        }

    private fun sixMonthsAgo(): Long =
        Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]))
            }
        }
    }
}
