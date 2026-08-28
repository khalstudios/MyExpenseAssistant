package com.expenseassistant.data.export

import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    private val headers = listOf(
        "Date", "Time", "Type", "Amount", "Currency", "Merchant", "Category",
        "Payment mode", "Description", "Tags", "Source", "Captured via", "Reference",
    )

    fun toCsv(transactions: List<TransactionEntity>): String = buildString {
        appendLine(headers.joinToString(","))
        transactions.forEach { transaction ->
            val date = Date(transaction.occurredAt)
            appendLine(
                listOf(
                    dateFormat.format(date),
                    timeFormat.format(date),
                    if (transaction.direction == Direction.DEBIT) "Spend" else "Income",
                    "%.2f".format(Locale.US, transaction.amountMinor / 100.0),
                    transaction.currency,
                    transaction.merchant,
                    transaction.category.displayName,
                    transaction.paymentMode.displayName,
                    transaction.description.orEmpty(),
                    transaction.tags.joinToString("; "),
                    transaction.sourceApp,
                    transaction.captureSource.name.lowercase(),
                    transaction.referenceId.orEmpty(),
                ).joinToString(",") { it.escapeCsv() }
            )
        }
    }

    fun fileName(): String =
        "expense-assistant-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.csv"

    private fun String.escapeCsv(): String {
        val needsQuoting = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
