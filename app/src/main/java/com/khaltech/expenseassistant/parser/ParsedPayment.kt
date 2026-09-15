package com.khaltech.expenseassistant.parser

import com.khaltech.expenseassistant.data.model.Direction

data class ParsedPayment(
    val amountMinor: Long,
    val currency: String,
    val direction: Direction,
    val merchantRaw: String?,
    val referenceId: String?,
    val rawText: String,
    val sourcePackage: String,
    val sourceApp: String,
    val occurredAt: Long,
)
