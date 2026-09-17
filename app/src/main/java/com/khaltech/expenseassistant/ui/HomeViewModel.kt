package com.khaltech.expenseassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khaltech.expenseassistant.data.model.BudgetEntity
import com.khaltech.expenseassistant.data.model.BudgetPeriod
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.TagUsage
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.recurring.RecurringDetector
import com.khaltech.expenseassistant.recurring.RecurringExpense
import com.khaltech.expenseassistant.ui.detail.TransactionEdits
import com.khaltech.expenseassistant.ui.insights.AnalyticsRange
import com.khaltech.expenseassistant.ui.insights.AnalyticsUiState
import com.khaltech.expenseassistant.ui.insights.BudgetProgress
import com.khaltech.expenseassistant.ui.insights.PeriodSelection
import com.khaltech.expenseassistant.ui.insights.Periods
import com.khaltech.expenseassistant.ui.insights.PieSlice
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
    val todaySpendMinor: Long = 0,
    val todaySpendCount: Int = 0,
)

/** Auto-categorised with low confidence and never confirmed by the user. */
val TransactionEntity.needsCategoryReview: Boolean
    get() = !userCorrected && categoryConfidence < 0.6f

private data class PeriodSnapshot(
    val selection: PeriodSelection,
    val transactions: List<TransactionEntity>,
    val budgets: Map<String, Long>,
)

/**
 * Scope of the home summary card, independent of the Insights period navigation. [label] names the
 * toggle segment, [headline] titles the card for the scope in view.
 */
enum class SummaryScope(val label: String, val headline: String) {
    MONTH("Month", "Current Month"),
    YEAR("Year", "Current Year"),
    ALL("All time", "Overall"),
}

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

    /** Complete history for the "See more" screen, independent of the home summary scope. */
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every low-confidence transaction, regardless of period, for the "needs a category" screen. */
    val needsReviewTransactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .map { transactions -> transactions.filter { it.needsCategoryReview } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analytics: StateFlow<AnalyticsUiState> = snapshot
        .map { it.toAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    private val monthlyBudgetSelection = PeriodSelection.now(AnalyticsRange.MONTH)

    val monthlyBudgetAnalytics: StateFlow<AnalyticsUiState> = combine(
        repository.observeBetween(
            Periods.previousStart(monthlyBudgetSelection),
            Periods.endExclusive(monthlyBudgetSelection),
        ),
        budgetRepository.observeBudgets(),
    ) { transactions, budgets -> PeriodSnapshot(monthlyBudgetSelection, transactions, budgets).toAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState(selection = monthlyBudgetSelection))

    private val yearlyBudgetSelection = PeriodSelection.now(AnalyticsRange.YEAR)

    /** The same progress view as the monthly budgets, measured against the yearly limits. */
    val yearlyBudgetAnalytics: StateFlow<AnalyticsUiState> = combine(
        repository.observeBetween(
            Periods.previousStart(yearlyBudgetSelection),
            Periods.endExclusive(yearlyBudgetSelection),
        ),
        budgetRepository.observeBudgets(BudgetPeriod.YEARLY),
    ) { transactions, budgets -> PeriodSnapshot(yearlyBudgetSelection, transactions, budgets).toAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState(selection = yearlyBudgetSelection))

    /** A daily cap the user set by hand, or zero when they have not set one. */
    val explicitDailyBudgetMinor: StateFlow<Long> = budgetRepository.observeBudgets(BudgetPeriod.DAILY)
        .map { budgets -> budgets[BudgetEntity.OVERALL] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /**
     * What today is measured against: the user's own daily budget when set, otherwise the overall
     * monthly budget spread evenly over the month. Zero when neither exists.
     */
    val dailyBudgetMinor: StateFlow<Long> = combine(
        explicitDailyBudgetMinor,
        monthlyBudgetAnalytics,
    ) { explicit, analytics ->
        if (explicit > 0) explicit
        else analytics.overallBudget?.limitMinor?.div(Periods.totalDays(monthlyBudgetSelection)) ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setDailyBudget(limitMinor: Long) = viewModelScope.launch {
        budgetRepository.setDailyBudget(limitMinor)
    }

    /** Days remaining after today, for the status banner's "N days to go". */
    private fun daysLeftThisMonth(): Int =
        Periods.totalDays(monthlyBudgetSelection) - Periods.elapsedDays(monthlyBudgetSelection)

    /** One plain-language read on today and the month; null until an overall budget exists. */
    val spendingStatus: StateFlow<SpendingStatus?> = combine(
        recentState,
        monthlyBudgetAnalytics,
        dailyBudgetMinor,
    ) { recent, analytics, dailyShare ->
        spendingStatus(
            todaySpendMinor = recent.todaySpendMinor,
            dailyShareMinor = dailyShare,
            budget = analytics.overallBudget,
            daysLeft = daysLeftThisMonth(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Candidate one-liners for the tip bar; the screen picks one per opening. Built straight from
     * the stored flows rather than the seeded UI state: the bar takes a single snapshot, so it must
     * not fire on the empty value that precedes the first database emission.
     */
    val spendingTips: StateFlow<List<String>> = combine(
        repository.observeSince(startOfDay(System.currentTimeMillis())),
        budgetRepository.observeAll(),
    ) { todayTransactions, budgets ->
        val debits = todayTransactions.filter { it.direction == Direction.DEBIT }
        fun limitOf(period: BudgetPeriod) = budgets
            .firstOrNull { it.categoryKey == BudgetEntity.OVERALL && it.period == period }
            ?.limitMinor
        val dailyShare = limitOf(BudgetPeriod.DAILY)
            ?: limitOf(BudgetPeriod.MONTHLY)?.div(Periods.totalDays(monthlyBudgetSelection))
            ?: 0L
        spendingTips(
            todaySpendMinor = debits.sumOf { it.amountMinor },
            dailyBudgetMinor = dailyShare,
            todayByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            todayCount = debits.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurring: StateFlow<List<RecurringExpense>> =
        repository.observeSince(sixMonthsAgo())
            .map { RecurringDetector.detect(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagSuggestions: StateFlow<List<String>> = repository.observeTagSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagUsage: StateFlow<List<TagUsage>> = repository.observeTagUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customCategories: StateFlow<List<com.khaltech.expenseassistant.data.repo.CustomCategoryOption>> =
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
        val today = debits.spentToday()
        return HomeUiState(
            transactions = current,
            spendMinor = debits.sumOf { it.amountMinor },
            incomeMinor = current.filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            spendByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            needsReviewCount = current.count { it.needsCategoryReview },
            todaySpendMinor = today.sumOf { it.amountMinor },
            todaySpendCount = today.size,
        )
    }

    private fun List<TransactionEntity>.toRecentHomeState(): HomeUiState {
        val debits = filter { it.direction == Direction.DEBIT }
        val today = debits.spentToday()
        return HomeUiState(
            transactions = this,
            spendMinor = debits.sumOf { it.amountMinor },
            incomeMinor = filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor },
            spendByCategory = debits.groupBy { it.category }
                .map { (category, items) -> category to items.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
            needsReviewCount = count { it.needsCategoryReview },
            todaySpendMinor = today.sumOf { it.amountMinor },
            todaySpendCount = today.size,
        )
    }

    /** Debits dated today; the home banner reports the day's running total. */
    private fun List<TransactionEntity>.spentToday(): List<TransactionEntity> {
        val dayStart = startOfDay(System.currentTimeMillis())
        return filter { it.occurredAt >= dayStart }
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

    fun saveDetails(id: Long, edits: TransactionEdits) = viewModelScope.launch {
        repository.updateDetails(
            id = id,
            amountMinor = edits.amountMinor,
            direction = edits.direction,
            merchant = edits.merchant,
            occurredAt = edits.occurredAt,
            category = edits.category,
            customCategoryName = edits.customCategoryName,
            customCategoryColor = edits.customCategoryColor,
            customCategoryIcon = edits.customCategoryIcon,
            paymentMode = edits.paymentMode,
            description = edits.description,
            tags = edits.tags,
        )
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
