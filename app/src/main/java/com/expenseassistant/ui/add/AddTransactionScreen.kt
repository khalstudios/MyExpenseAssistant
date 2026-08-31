package com.expenseassistant.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.repo.CustomCategoryOption
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryPickerSheet
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.toMinorUnits
import java.util.Calendar

data class ManualTransactionInput(
    val amountMinor: Long,
    val direction: Direction,
    val merchant: String,
    val category: Category,
    val customCategoryName: String? = null,
    val customCategoryColor: String? = null,
    val customCategoryIcon: String? = null,
    val paymentMode: PaymentMode,
    val occurredAt: Long,
    val description: String,
    val tags: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSave: (ManualTransactionInput) -> Unit,
    customCategories: List<CustomCategoryOption> = emptyList(),
) {
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.OTHER) }
    var customCategoryName by remember { mutableStateOf<String?>(null) }
    var customCategoryColor by remember { mutableStateOf<String?>(null) }
    var customCategoryIcon by remember { mutableStateOf<String?>(null) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var occurredAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var description by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }

    var pickingCategory by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    val canSave = amount.toMinorUnits() > 0 && merchant.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(Direction.DEBIT, Direction.CREDIT).forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == direction,
                        onClick = { direction = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(if (entry == Direction.DEBIT) "Spend" else "Income") },
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount") },
                prefix = { Text("\u20b9 ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text(if (direction == Direction.DEBIT) "Paid to" else "Received from") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(onClick = { pickingCategory = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (customCategoryName != null) {
                        val customColor = runCatching {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(customCategoryColor))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        Icon(
                            com.expenseassistant.ui.category.CategoryIconCatalog.iconFor(customCategoryIcon),
                            contentDescription = null,
                            tint = customColor,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        CategoryBadge(category, size = 32.dp)
                    }
                    Text(customCategoryName ?: category.displayName)
                }
            }

            Text("Payment mode", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMode.entries.filter { it != PaymentMode.UNKNOWN }.forEach { mode ->
                    FilterChip(
                        selected = mode == paymentMode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode.displayName) },
                    )
                }
            }

            Text("When", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text("  ${formatTimestamp(occurredAt)}")
                }
                OutlinedButton(onClick = { pickingTime = true }) {
                    Icon(Icons.Filled.Schedule, contentDescription = "Pick time")
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = tagText,
                onValueChange = { tagText = it },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        ManualTransactionInput(
                            amountMinor = amount.toMinorUnits(),
                            direction = direction,
                            merchant = merchant.trim(),
                            category = category,
                            customCategoryName = customCategoryName,
                            customCategoryColor = customCategoryColor,
                            customCategoryIcon = customCategoryIcon,
                            paymentMode = paymentMode,
                            occurredAt = occurredAt,
                            description = description,
                            tags = tagText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save transaction")
            }
        }
    }

    if (pickingCategory) {
        CategoryPickerSheet(
            merchant = merchant.ifBlank { "this transaction" },
            selected = category,
            selectedCustomName = customCategoryName,
            customCategories = customCategories,
            onSelect = {
                category = it
                customCategoryName = null
                customCategoryColor = null
                customCategoryIcon = null
                pickingCategory = false
            },
            onSelectCustom = { name, colorHex, iconKey ->
                category = Category.OTHER
                customCategoryName = name
                customCategoryColor = colorHex
                customCategoryIcon = iconKey
                pickingCategory = false
            },
            onDismiss = { pickingCategory = false },
        )
    }

    if (pickingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        occurredAt = mergeDate(picked, occurredAt)
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (pickingTime) {
        val calendar = Calendar.getInstance().apply { timeInMillis = occurredAt }
        val timeState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    occurredAt = Calendar.getInstance().apply {
                        timeInMillis = occurredAt
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                    }.timeInMillis
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) },
        )
    }
}

/** Keeps the already-chosen clock time when only the date changes. */
private fun mergeDate(dateMillis: Long, existing: Long): Long {
    val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
    return Calendar.getInstance().apply {
        timeInMillis = existing
        set(Calendar.YEAR, date.get(Calendar.YEAR))
        set(Calendar.MONTH, date.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
