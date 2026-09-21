package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.categorize.Categorizer
import com.khaltech.expenseassistant.data.local.RecurringDismissalDao
import com.khaltech.expenseassistant.data.local.RecurringPlanDao
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.RecurringDismissal
import com.khaltech.expenseassistant.data.model.RecurringPlanEntity
import com.khaltech.expenseassistant.recurring.Cadence
import com.khaltech.expenseassistant.recurring.RecurringExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringPlanRepository(
    private val dao: RecurringPlanDao,
    private val dismissals: RecurringDismissalDao,
) {

    /** Merchant key to the moment it was deleted, for the detector to count afresh from. */
    fun observeDismissals(): Flow<Map<String, Long>> = dismissals.observeAll()
        .map { rows -> rows.associate { it.merchantKey to it.dismissedAt } }

    /**
     * The user's own recurring payments, shaped like the detector's findings so the insights card
     * can show one list.
     */
    fun observeAll(): Flow<List<RecurringExpense>> = dao.observeAll().map { plans ->
        val now = System.currentTimeMillis()
        plans.map { it.toRecurringExpense(now) }
    }

    /**
     * Stores a plan, replacing the one with [id] when editing rather than adding a second.
     *
     * Saving also lifts any dismissal on the merchant: the user is telling the app this payment
     * does repeat, which is the opposite of what deleting it said.
     */
    suspend fun save(
        id: Long = 0,
        merchant: String,
        category: Category,
        amountMinor: Long,
        cadence: Cadence,
        nextDueAt: Long,
    ) {
        dao.upsert(
            RecurringPlanEntity(
                id = id,
                merchant = merchant,
                category = category,
                amountMinor = amountMinor,
                cadence = cadence.name,
                nextDueAt = nextDueAt,
            )
        )
        dismissals.delete(merchantKeyOf(merchant))
    }

    /**
     * Removes a recurring payment from the card for good as far as the user is concerned.
     *
     * Both halves matter. Dropping the stored plan is not enough on its own, because the payments
     * that would have let the detector find the same merchant are still in the history; and
     * dismissing without dropping the plan would leave the user's own entry on screen. A detected
     * item has no [id] and only needs the dismissal.
     */
    suspend fun delete(id: Long?, merchantKey: String) {
        id?.let { dao.delete(it) }
        dismissals.upsert(RecurringDismissal(merchantKey = merchantKey))
    }

    suspend fun clearAll() {
        dao.deleteAll()
        dismissals.deleteAll()
    }
}

/** The same normalisation the detector groups payments by, so the two agree on what a merchant is. */
fun merchantKeyOf(merchant: String): String =
    Categorizer.merchantKey(merchant) ?: merchant.lowercase()

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Guards against a stored date so old that rolling it forward would spin; a year of weekly is ~52. */
private const val MaxRollForward = 400

/**
 * A stored plan as the recurring card wants it, with its due date brought up to date. Someone who
 * entered rent on the 1st six months ago should still be told about the 1st of next month.
 */
fun RecurringPlanEntity.toRecurringExpense(now: Long = System.currentTimeMillis()): RecurringExpense {
    val period = Cadence.fromName(cadence)
    val step = period.days * DAY_MILLIS
    var due = nextDueAt
    var elapsed = 0
    while (due < now && elapsed < MaxRollForward) {
        due += step
        elapsed++
    }
    return RecurringExpense(
        merchant = merchant,
        category = category,
        typicalAmountMinor = amountMinor,
        cadence = period,
        // Nothing was observed here, so this counts the payments due since it was entered.
        occurrences = elapsed + 1,
        lastSeenAt = due - step,
        nextExpectedAt = due,
        manualId = id,
        merchantKey = merchantKeyOf(merchant),
    )
}
