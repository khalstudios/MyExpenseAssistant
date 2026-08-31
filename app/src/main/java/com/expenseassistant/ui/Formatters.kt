package com.expenseassistant.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    currency = Currency.getInstance("INR")
    maximumFractionDigits = 2
}

private val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
private val dayHeaderFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
private val dayHeaderWithYearFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
private val timeOnlyFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
private val monthYearNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val shortMonthFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val yearNameFormat = SimpleDateFormat("yyyy", Locale.getDefault())

fun formatMinor(amountMinor: Long): String = currencyFormat.format(amountMinor / 100.0)

fun formatTimestamp(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatTimeOnly(epochMillis: Long): String = timeOnlyFormat.format(Date(epochMillis))

/** Groups transactions into day buckets; the value is midnight of that day. */
fun startOfDay(epochMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = epochMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** "22 August", or "22 August 2025" once the date falls outside the current year. */
fun formatDayHeader(epochMillis: Long): String {
    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val year = Calendar.getInstance().apply { timeInMillis = epochMillis }.get(Calendar.YEAR)
    val format = if (year == thisYear) dayHeaderFormat else dayHeaderWithYearFormat
    return format.format(Date(epochMillis))
}

/** Parses user-typed rupee amounts such as "250" or "1250.75" into paise. */
fun String.toMinorUnits(): Long =
    trim().toBigDecimalOrNull()?.movePointRight(2)?.toLong()?.coerceAtLeast(0) ?: 0L

/** "August 2026" for the summary heading. */
fun currentMonthYearName(): String = monthYearNameFormat.format(Date())

/** "Jan - Aug 2026", or just "Jan 2026" in January, covering the year so far. */
fun yearToDateLabel(): String {
    val now = Calendar.getInstance()
    val currentMonth = shortMonthFormat.format(now.time)
    val year = yearNameFormat.format(now.time)
    if (now.get(Calendar.MONTH) == Calendar.JANUARY) return "$currentMonth $year"
    val january = (now.clone() as Calendar).apply { set(Calendar.MONTH, Calendar.JANUARY) }
    return "${shortMonthFormat.format(january.time)} - $currentMonth $year"
}
