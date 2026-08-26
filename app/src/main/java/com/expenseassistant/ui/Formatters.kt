package com.expenseassistant.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    currency = Currency.getInstance("INR")
    maximumFractionDigits = 2
}

private val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

fun formatMinor(amountMinor: Long): String = currencyFormat.format(amountMinor / 100.0)

fun formatTimestamp(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
