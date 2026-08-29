package com.expenseassistant.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryPickerSheet
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.rememberSoftGradient
import com.expenseassistant.ui.toMinorUnits
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: TransactionEntity,
    onBack: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onCoreChange: (amountMinor: Long, direction: Direction, merchant: String, occurredAt: Long) -> Unit,
    onDelete: () -> Unit,
    tagSuggestions: List<String> = emptyList(),
    onOpenTag: (String) -> Unit = {},
) {
    var pickingCategory by remember { mutableStateOf(false) }
    var editingCore by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editingCore = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit transaction")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete transaction")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AmountHeader(transaction) { pickingCategory = true }
            PaymentModeSection(transaction.paymentMode, onPaymentModeChange)
            DescriptionSection(transaction.description.orEmpty(), onDescriptionChange)
            TagsSection(
                tags = transaction.tags,
                suggestions = tagSuggestions,
                onChange = onTagsChange,
                onOpenTag = onOpenTag,
            )
            MetadataSection(transaction)
        }
    }

    if (editingCore) {
        EditCoreDialog(
            transaction = transaction,
            onDismiss = { editingCore = false },
            onSave = { amountMinor, direction, merchant, occurredAt ->
                onCoreChange(amountMinor, direction, merchant, occurredAt)
                editingCore = false
            },
        )
    }

    if (pickingCategory) {
        CategoryPickerSheet(
            merchant = transaction.merchant,
            selected = transaction.category,
            onSelect = {
                onCategoryChange(it)
                pickingCategory = false
            },
            onDismiss = { pickingCategory = false },
        )
    }
}

@Composable
private fun AmountHeader(transaction: TransactionEntity, onEditCategory: () -> Unit) {
    val isDebit = transaction.direction == Direction.DEBIT
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .background(Color.White.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape)
                    .padding(4.dp),
            ) {
                CategoryBadge(transaction.category, size = 56.dp)
            }
            Text(
                (if (isDebit) "- " else "+ ") + formatMinor(transaction.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = hero.onGradient,
            )
            Text(
                transaction.merchant,
                style = MaterialTheme.typography.titleMedium,
                color = hero.onGradient,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                "${if (isDebit) "Spend" else "Income"} · ${formatTimestamp(transaction.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = hero.onGradientMuted,
            )
            AssistChip(
                onClick = onEditCategory,
                label = { Text(transaction.category.displayName) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFF3E2723)) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    labelColor = Color(0xFF3E2723),
                ),
                border = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PaymentModeSection(selected: PaymentMode, onSelect: (PaymentMode) -> Unit) {
    SectionCard("Payment mode") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    label = { Text(mode.displayName) },
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String, onChange: (String) -> Unit) {
    var draft by remember(description) { mutableStateOf(description) }
    SectionCard("Description") {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("What was this for?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        if (draft != description) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { draft = description }) { Text("Cancel") }
                TextButton(onClick = { onChange(draft) }) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    suggestions: List<String>,
    onChange: (List<String>) -> Unit,
    onOpenTag: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    val unusedSuggestions = suggestions.filter { s -> tags.none { it.equals(s, ignoreCase = true) } }

    SectionCard("Tags") {
        if (tags.isEmpty()) {
            Text(
                "No tags yet. Use a tag like #virtus to group expenses for the same car, trip or person.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Tap a tag to see every expense tagged with it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onOpenTag(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp).clickable { onChange(tags - tag) },
                            )
                        },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                placeholder = { Text("Add a tag") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    if (newTag.isNotBlank()) {
                        onChange(tags + newTag.trim().removePrefix("#"))
                        newTag = ""
                    }
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add tag")
            }
        }
        if (unusedSuggestions.isNotEmpty()) {
            Text("Reuse a tag", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unusedSuggestions.take(8).forEach { suggestion ->
                    AssistChip(
                        onClick = { onChange(tags + suggestion) },
                        label = { Text("#$suggestion") },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSection(transaction: TransactionEntity) {
    SectionCard("Details") {
        MetaRow("Type", if (transaction.direction == Direction.DEBIT) "Spend" else "Income")
        MetaRow("Amount", formatMinor(transaction.amountMinor))
        MetaRow("Date & time", formatTimestamp(transaction.occurredAt))
        MetaRow("Category", transaction.category.displayName)
        MetaRow("Payment mode", transaction.paymentMode.displayName)
        MetaRow("Source", "${transaction.sourceApp} (${transaction.captureSource.name.lowercase()})")
        transaction.referenceId?.let { MetaRow("Reference", it) }
        MetaRow(
            "Auto-categorised",
            if (transaction.userCorrected) "Corrected by you"
            else "${(transaction.categoryConfidence * 100).toInt()}% confidence",
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("Captured text", style = MaterialTheme.typography.labelMedium)
        Text(
            transaction.rawText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCoreDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (amountMinor: Long, direction: Direction, merchant: String, occurredAt: Long) -> Unit,
) {
    var amount by remember {
        mutableStateOf(
            transaction.amountMinor.let { minor ->
                val whole = minor / 100
                val frac = minor % 100
                if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(2, '0')}"
            }
        )
    }
    var direction by remember { mutableStateOf(transaction.direction) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var occurredAt by remember { mutableLongStateOf(transaction.occurredAt) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    label = { Text("Merchant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Text("  ${formatTimestamp(occurredAt)}")
                    }
                    OutlinedButton(onClick = { pickingTime = true }) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Pick time")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amount.toMinorUnits() > 0 && merchant.isNotBlank()) {
                        onSave(amount.toMinorUnits(), direction, merchant.trim(), occurredAt)
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        val pickedCal = Calendar.getInstance().apply { timeInMillis = picked }
                        occurredAt = Calendar.getInstance().apply {
                            timeInMillis = occurredAt
                            set(Calendar.YEAR, pickedCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, pickedCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, pickedCal.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
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
