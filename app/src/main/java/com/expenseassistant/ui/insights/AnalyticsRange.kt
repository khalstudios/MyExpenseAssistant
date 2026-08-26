package com.expenseassistant.ui.insights

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AnalyticsRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

/** A concrete period: the [range] plus any timestamp that falls inside it. */
data class PeriodSelection(val range: AnalyticsRange, val anchorMillis: Long) {
    companion object {
        fun now(range: AnalyticsRange) = PeriodSelection(range, System.currentTimeMillis())
    }
}

object Periods {

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayMonthFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    fun start(selection: PeriodSelection): Long = startCalendar(selection).timeInMillis

    fun endExclusive(selection: PeriodSelection): Long =
        startCalendar(selection).apply { addOne(selection.range, 1) }.timeInMillis

    fun previousStart(selection: PeriodSelection): Long =
        startCalendar(selection).apply { addOne(selection.range, -1) }.timeInMillis

    fun shift(selection: PeriodSelection, delta: Int): PeriodSelection =
        selection.copy(
            anchorMillis = startCalendar(selection).apply { addOne(selection.range, delta) }.timeInMillis
        )

    fun withRange(selection: PeriodSelection, range: AnalyticsRange): PeriodSelection =
        PeriodSelection(range, selection.anchorMillis.coerceAtMost(System.currentTimeMillis()))

    fun jumpTo(range: AnalyticsRange, year: Int, monthIndex: Int): PeriodSelection {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return PeriodSelection(range, calendar.timeInMillis)
    }

    fun isCurrent(selection: PeriodSelection): Boolean {
        val now = System.currentTimeMillis()
        return now >= start(selection) && now < endExclusive(selection)
    }

    fun canGoForward(selection: PeriodSelection): Boolean = !isCurrent(selection)

    fun label(selection: PeriodSelection): String {
        val startDate = Date(start(selection))
        return when (selection.range) {
            AnalyticsRange.WEEK ->
                "${dayMonthFormat.format(startDate)} \u2013 ${dayMonthFormat.format(Date(endExclusive(selection) - 1))}"

            AnalyticsRange.MONTH -> monthYearFormat.format(startDate)
            AnalyticsRange.YEAR -> yearFormat.format(startDate)
        }
    }

    /** Days counted so far; a completed period always counts in full. */
    fun elapsedDays(selection: PeriodSelection): Int {
        if (!isCurrent(selection)) return totalDays(selection)
        val elapsed = ((System.currentTimeMillis() - start(selection)) / DAY_MILLIS).toInt() + 1
        return elapsed.coerceIn(1, totalDays(selection))
    }

    fun totalDays(selection: PeriodSelection): Int = when (selection.range) {
        AnalyticsRange.WEEK -> 7
        AnalyticsRange.MONTH -> startCalendar(selection).getActualMaximum(Calendar.DAY_OF_MONTH)
        AnalyticsRange.YEAR -> startCalendar(selection).getActualMaximum(Calendar.DAY_OF_YEAR)
    }

    fun comparisonLabel(range: AnalyticsRange): String = when (range) {
        AnalyticsRange.WEEK -> "the previous week"
        AnalyticsRange.MONTH -> "the previous month"
        AnalyticsRange.YEAR -> "the previous year"
    }

    fun monthStart(selection: PeriodSelection): Long =
        calendarAt(selection.anchorMillis).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    fun monthEndExclusive(selection: PeriodSelection): Long = calendarAt(selection.anchorMillis).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }.timeInMillis

    fun yearOf(selection: PeriodSelection): Int = calendarAt(selection.anchorMillis).get(Calendar.YEAR)

    fun monthIndexOf(selection: PeriodSelection): Int = calendarAt(selection.anchorMillis).get(Calendar.MONTH)

    private fun startCalendar(selection: PeriodSelection): Calendar =
        calendarAt(selection.anchorMillis).apply {
            when (selection.range) {
                AnalyticsRange.WEEK -> {
                    val diff = get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
                    add(Calendar.DAY_OF_MONTH, -(if (diff < 0) diff + 7 else diff))
                }

                AnalyticsRange.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                AnalyticsRange.YEAR -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }

    private fun Calendar.addOne(range: AnalyticsRange, amount: Int) = when (range) {
        AnalyticsRange.WEEK -> add(Calendar.DAY_OF_MONTH, 7 * amount)
        AnalyticsRange.MONTH -> add(Calendar.MONTH, amount)
        AnalyticsRange.YEAR -> add(Calendar.YEAR, amount)
    }

    private fun calendarAt(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
