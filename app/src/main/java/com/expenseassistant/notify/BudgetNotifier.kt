package com.expenseassistant.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationManagerCompat
import com.expenseassistant.R
import com.expenseassistant.data.local.BudgetDao
import com.expenseassistant.data.local.TransactionDao
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

/**
 * Warns once per month per budget when spending crosses 80% and again at 100%.
 */
class BudgetNotifier(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
) {

    private val prefs = context.applicationContext
        .getSharedPreferences("budget-alerts", Context.MODE_PRIVATE)

    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = 0
    }

    suspend fun onTransactionRecorded(transaction: TransactionEntity) {
        if (!isInCurrentMonth(transaction.occurredAt)) return
        val budgets = budgetDao.allOnce().takeIf { it.isNotEmpty() } ?: return

        val from = monthStart()
        val to = monthEndExclusive()

        budgets.firstOrNull { it.categoryKey == BudgetEntity.OVERALL }?.let { budget ->
            evaluate(
                key = BudgetEntity.OVERALL,
                title = "All spending",
                spentMinor = transactionDao.spendBetween(from, to),
                limitMinor = budget.limitMinor,
            )
        }

        budgets.firstOrNull { it.categoryKey == transaction.category.name }?.let { budget ->
            evaluate(
                key = transaction.category.name,
                title = transaction.category.displayName,
                spentMinor = transactionDao.categorySpendBetween(transaction.category, from, to),
                limitMinor = budget.limitMinor,
            )
        }
    }

    private fun evaluate(key: String, title: String, spentMinor: Long, limitMinor: Long) {
        if (limitMinor <= 0) return
        val percent = (spentMinor * 100 / limitMinor).toInt()
        val crossed = when {
            percent >= 100 -> 100
            percent >= 80 -> 80
            else -> return
        }

        val prefKey = "$key:${monthTag()}"
        if (prefs.getInt(prefKey, 0) >= crossed) return
        prefs.edit().putInt(prefKey, crossed).apply()

        val message = if (crossed >= 100) {
            "$title is over budget: ${format(spentMinor)} of ${format(limitMinor)}."
        } else {
            "$title has used $percent% of its budget. ${format(limitMinor - spentMinor)} left this month."
        }
        notify(key, if (crossed >= 100) "Budget exceeded" else "Budget running low", message)
    }

    private fun notify(key: String, title: String, message: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .apply { pendingIntent?.let(::setContentIntent) }
            .build()

        runCatching { manager.notify(key.hashCode(), notification) }
    }

    private fun format(amountMinor: Long): String = currency.format(amountMinor / 100.0)

    private fun isInCurrentMonth(millis: Long): Boolean = millis >= monthStart() && millis < monthEndExclusive()

    private fun monthTag(): String = Calendar.getInstance().let {
        "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH)}"
    }

    private fun monthStart(): Long = midnightFirstOfMonth().timeInMillis

    private fun monthEndExclusive(): Long =
        midnightFirstOfMonth().apply { add(Calendar.MONTH, 1) }.timeInMillis

    private fun midnightFirstOfMonth(): Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    companion object {
        const val CHANNEL_ID = "budget-alerts"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Warns when spending approaches or passes a budget" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
