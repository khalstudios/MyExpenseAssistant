package com.khaltech.expenseassistant.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.recurring.Cadence
import com.khaltech.expenseassistant.recurring.RecurringExpense
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.CategoryPickerSheet
import com.khaltech.expenseassistant.ui.formatShortDate
import com.khaltech.expenseassistant.ui.toMinorUnits
import java.util.Locale

/** What the user filled in, ready for the repository. [id] is 0 for something newly added. */
data class RecurringPlanInput(
    val id: Long,
    val merchant: String,
    val category: Category,
    val amountMinor: Long,
    val cadence: Cadence,
    val nextDueAt: Long,
)

/**
 * Enters or edits a repeating payment.
 *
 * Three things arrive here. A blank form, for a payment the app could not have seen — rent by
 * standing instruction, a subscription on a card the phone gets no alerts for, the milk bill in
 * cash. The user's own saved entry, to correct. And a *detected* one, which opens prefilled with
 * what the history suggested: saving it turns the app's guess into the user's own record, which
 * then takes precedence over anything detection has to say about that merchant.
 *
 * Only the facts the recurring card shows are asked for. Anything more would make this a second
 * transaction form, and it is not one — nothing here is recorded as spending.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecurringScreen(
    onBack: () -> Unit,
    onSave: (RecurringPlanInput) -> Unit,
    /** The row being opened, or null to add one from scratch. */
    existing: RecurringExpense? = null,
    onDelete: (() -> Unit)? = null,
) {
    var merchant by remember { mutableStateOf(existing?.merchant.orEmpty()) }
    var amount by remember {
        mutableStateOf(existing?.typicalAmountMinor?.let { formatMinorForInput(it) } ?: "")
    }
    var category by remember { mutableStateOf(existing?.category ?: Category.OTHER) }
    var cadence by remember { mutableStateOf(existing?.cadence ?: Cadence.MONTHLY) }
    var nextDueAt by remember {
        mutableLongStateOf(existing?.nextExpectedAt ?: System.currentTimeMillis())
    }

    var pickingCategory by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val canSave = merchant.isNotBlank() && amount.toMinorUnits() > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add recurring payment" else "Recurring payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (canSave) {
                        onSave(
                            RecurringPlanInput(
                                // A detected row has no stored plan yet, so saving it creates one.
                                id = existing?.manualId ?: 0L,
                                merchant = merchant.trim(),
                                category = category,
                                amountMinor = amount.toMinorUnits(),
                                cadence = cadence,
                                nextDueAt = nextDueAt,
                            )
                        )
                    }
                },
                containerColor = if (canSave) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (canSave) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Save recurring payment")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("What is it for") },
                        placeholder = { Text("Rent, Netflix, gym") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("Amount each time") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pickingCategory = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryBadge(category, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Category", style = MaterialTheme.typography.labelMedium)
                            Text(category.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("How often", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Cadence.entries.forEach { option ->
                            FilterChip(
                                selected = cadence == option,
                                onClick = { cadence = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pickingDate = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Next payment due", style = MaterialTheme.typography.labelMedium)
                            Text(formatShortDate(nextDueAt), style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "Later dates are worked out from this one, so it keeps pointing at the next payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Keeps the last card clear of the floating save button.
            Spacer(Modifier.height(72.dp))
        }
    }

    if (pickingCategory) {
        CategoryPickerSheet(
            merchant = merchant.ifBlank { "this payment" },
            selected = category,
            onSelect = {
                category = it
                pickingCategory = false
            },
            onDismiss = { pickingCategory = false },
        )
    }

    if (confirmingDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Remove ${merchant.ifBlank { "this payment" }}?") },
            text = {
                Text(
                    "It leaves the recurring list and will not be detected again until three more " +
                        "payments at this interval. No transactions are deleted.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Keep") } },
        )
    }

    if (pickingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = nextDueAt)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { nextDueAt = it }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    }
}

/**
 * Paise back into the plain number the amount field accepts.
 *
 * [formatMinor] is for reading and carries a currency symbol and grouping commas, neither of which
 * survives [toMinorUnits] on the way back in. A whole-rupee amount drops the decimals so the field
 * opens showing "499" rather than "499.00".
 */
private fun formatMinorForInput(amountMinor: Long): String =
    if (amountMinor % 100 == 0L) (amountMinor / 100).toString()
    else String.format(Locale.US, "%.2f", amountMinor / 100.0)
