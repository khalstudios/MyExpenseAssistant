package com.khaltech.expenseassistant.ui.budget

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.khaltech.expenseassistant.ui.toMinorUnits

/**
 * Compact amount editor shared by the budget screen and the home card's daily budget. Saving an
 * empty or zero amount clears the limit.
 */
@Composable
fun BudgetAmountDialog(
    title: String,
    initialMinor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    fieldLabel: String = "Monthly limit",
    hint: String? = null,
) {
    var text by remember {
        mutableStateOf(if (initialMinor > 0) (initialMinor / 100).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() || it == '.' } },
                label = { Text(fieldLabel) },
                prefix = { Text("₹ ") },
                supportingText = hint?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toMinorUnits()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
