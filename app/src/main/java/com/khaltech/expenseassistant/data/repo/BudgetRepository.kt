package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.local.BudgetDao
import com.khaltech.expenseassistant.data.model.BudgetEntity
import com.khaltech.expenseassistant.data.model.BudgetPeriod
import com.khaltech.expenseassistant.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(private val budgetDao: BudgetDao) {

    /** Every stored limit, for callers that need more than one period at a time. */
    fun observeAll(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    /** Limits for one period, keyed by category name or [BudgetEntity.OVERALL]. */
    fun observeBudgets(period: BudgetPeriod = BudgetPeriod.MONTHLY): Flow<Map<String, Long>> =
        budgetDao.observeAll().map { budgets ->
            budgets.filter { it.period == period }.associate { it.categoryKey to it.limitMinor }
        }

    suspend fun setBudget(
        category: Category?,
        limitMinor: Long,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
    ) {
        val key = BudgetEntity.keyFor(category)
        if (limitMinor <= 0) budgetDao.delete(key, period)
        else budgetDao.upsert(BudgetEntity(categoryKey = key, period = period, limitMinor = limitMinor))
    }

    /** A cap for a single day, independent of the monthly and yearly limits. */
    suspend fun setDailyBudget(limitMinor: Long) =
        setBudget(category = null, limitMinor = limitMinor, period = BudgetPeriod.DAILY)

    suspend fun clearAll() = budgetDao.deleteAll()
}
