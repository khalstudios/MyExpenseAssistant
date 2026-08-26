package com.expenseassistant.data.repo

import com.expenseassistant.data.local.BudgetDao
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun observeBudgets(): Flow<Map<String, Long>> = budgetDao.observeAll()
        .map { budgets -> budgets.associate { it.categoryKey to it.limitMinor } }

    suspend fun setBudget(category: Category?, limitMinor: Long) {
        val key = BudgetEntity.keyFor(category)
        if (limitMinor <= 0) budgetDao.delete(key)
        else budgetDao.upsert(BudgetEntity(categoryKey = key, limitMinor = limitMinor))
    }

    suspend fun clearAll() = budgetDao.deleteAll()
}
