package com.expenseassistant.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.data.repo.CustomCategoryOption
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.IncomeColor
import com.expenseassistant.ui.SpendColor
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryPickerSheet
import com.expenseassistant.ui.category.displayCategoryName
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatShortDate
import com.expenseassistant.ui.formatTimeOnly
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.toMinorUnits
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: TransactionEntity,
    onBack: () -> Unit,
    onSave: (TransactionEdits) -> Unit,
    onDelete: () -> Unit,
    customCategories: List<CustomCategoryOption> = emptyList(),
    tagSuggestions: List<String> = emptyList(),
    onOpenTag: (String) -> Unit = {},
) {
    // Seeded per transaction id only, so the entity flow re-emitting after a save never clobbers typing.
    var amount by remember(transaction.id) { mutableStateOf(amountInput(transaction.amountMinor)) }
    var direction by remember(transaction.id) { mutableStateOf(transaction.direction) }
    var merchant by remember(transaction.id) { mutableStateOf(transaction.merchant) }
    var occurredAt by remember(transaction.id) { mutableLongStateOf(transaction.occurredAt) }
    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    var customName by remember(transaction.id) { mutableStateOf(transaction.customCategoryName) }
    var customColor by remember(transaction.id) { mutableStateOf(transaction.customCategoryColor) }
    var customIcon by remember(transaction.id) { mutableStateOf(transaction.customCategoryIcon) }
    var paymentMode by remember(transaction.id) { mutableStateOf(transaction.paymentMode) }
    var description by remember(transaction.id) { mutableStateOf(transaction.description.orEmpty()) }
    var tags by remember(transaction.id) { mutableStateOf(transaction.tags) }

    var pickingCategory by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }

    val edits = TransactionEdits(
        amountMinor = amount.toMinorUnits(),
        direction = direction,
        merchant = merchant.trim(),
        occurredAt = occurredAt,
        category = category,
        customCategoryName = customName,
        customCategoryColor = customColor,
        customCategoryIcon = customIcon,
        paymentMode = paymentMode,
        description = description.takeIf { it.isNotBlank() },
        tags = tags,
    )
    val isDirty = edits != transaction.toEdits()
    val isValid = edits.amountMinor > 0 && edits.merchant.isNotBlank()

    // Category visuals are entity-driven, so preview the draft through a throwaway copy.
    val preview = transaction.copy(
        category = category,
        customCategoryName = customName,
        customCategoryColor = customColor,
        customCategoryIcon = customIcon,
    )

    fun leave(action: () -> Unit) {
        if (isDirty) pendingExit = action else action()
    }

    BackHandler(enabled = isDirty) { pendingExit = onBack }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = { leave(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmingDelete = true }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete transaction")
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isDirty,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isValid) {
                            onSave(edits)
                            onBack()
                        }
                    },
                    containerColor = if (isValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isValid) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save changes")
                }
            }
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
            AmountCard(
                amount = amount,
                onAmountChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                direction = direction,
                onDirectionChange = { direction = it },
                merchant = merchant,
                onMerchantChange = { merchant = it },
                preview = preview,
                onEditCategory = { pickingCategory = true },
            )
            DateTimeCard(
                occurredAt = occurredAt,
                onPickDate = { pickingDate = true },
                onPickTime = { pickingTime = true },
            )
            PaymentModeAndTagsCard(
                paymentMode = paymentMode,
                onPaymentModeChange = { paymentMode = it },
                tags = tags,
                suggestions = tagSuggestions,
                onTagsChange = { tags = it },
                onOpenTag = { tag -> leave { onOpenTag(tag) } },
            )
            NotesCard(description) { description = it }
            MetadataCard(transaction)
            // Keeps the last card clear of the floating save button.
            Spacer(Modifier.height(72.dp))
        }
    }

    if (pickingCategory) {
        CategoryPickerSheet(
            merchant = merchant,
            selected = category,
            selectedCustomName = customName,
            customCategories = customCategories,
            onSelect = {
                category = it
                customName = null
                customColor = null
                customIcon = null
                pickingCategory = false
            },
            onSelectCustom = { name, colorHex, iconKey ->
                category = Category.OTHER
                customName = name
                customColor = colorHex
                customIcon = iconKey
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

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete transaction?") },
            text = {
                Text("${formatMinor(transaction.amountMinor)} at ${transaction.merchant} will be removed permanently.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }

    pendingExit?.let { exit ->
        AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text("Discard changes?") },
            text = { Text("Your edits haven't been saved yet.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingExit = null
                    exit()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { pendingExit = null }) { Text("Keep editing") } },
        )
    }
}

/** Amount, direction, merchant and category edited on one card since they define the transaction. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    direction: Direction,
    onDirectionChange: (Direction) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    preview: TransactionEntity,
    onEditCategory: () -> Unit,
) {
    val isDebit = direction == Direction.DEBIT
    val accent = if (isDebit) SpendColor else IncomeColor
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(Direction.DEBIT, Direction.CREDIT).forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == direction,
                        onClick = { onDirectionChange(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(if (entry == Direction.DEBIT) "Spend" else "Income") },
                    )
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                prefix = { Text(if (isDebit) "\u2212 \u20b9" else "+ \u20b9") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = accent,
                    unfocusedTextColor = accent,
                    focusedBorderColor = accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = merchant,
                onValueChange = onMerchantChange,
                label = { Text(if (isDebit) "Paid to" else "Received from") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditCategory),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryBadge(preview, size = 36.dp)
                Text(
                    preview.displayCategoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Change category",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeCard(
    occurredAt: Long,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    SectionCard("Date & time") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(formatShortDate(occurredAt), maxLines = 1)
            }
            OutlinedButton(onClick = onPickTime, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(formatTimeOnly(occurredAt), maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PaymentModeAndTagsCard(
    paymentMode: PaymentMode,
    onPaymentModeChange: (PaymentMode) -> Unit,
    tags: List<String>,
    suggestions: List<String>,
    onTagsChange: (List<String>) -> Unit,
    onOpenTag: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    val unusedSuggestions = suggestions.filter { s -> tags.none { it.equals(s, ignoreCase = true) } }

    SectionCard("Payment mode") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == paymentMode,
                    onClick = { onPaymentModeChange(mode) },
                    label = { Text(mode.displayName) },
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (tags.isNotEmpty()) {
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
                                modifier = Modifier.size(16.dp).clickable { onTagsChange(tags - tag) },
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
                        onTagsChange(tags + newTag.trim().removePrefix("#"))
                        newTag = ""
                    }
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add tag")
            }
        }
        if (unusedSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unusedSuggestions.take(8).forEach { suggestion ->
                    AssistChip(
                        onClick = { onTagsChange(tags + suggestion) },
                        label = { Text("#$suggestion") },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(description: String, onChange: (String) -> Unit) {
    SectionCard("Notes") {
        OutlinedTextField(
            value = description,
            onValueChange = onChange,
            placeholder = { Text("What was this for?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MetadataCard(transaction: TransactionEntity) {
    SectionCard("Metadata") {
        MetaRow("Source", "${transaction.sourceApp} (${transaction.captureSource.name.lowercase()})")
        transaction.referenceId?.let { MetaRow("Reference", it) }
        MetaRow("Captured on", formatTimestamp(transaction.createdAt))
        MetaRow(
            "Auto-categorised",
            if (transaction.userCorrected) "Corrected by you"
            else "${(transaction.categoryConfidence * 100).toInt()}% confidence",
        )
        if (transaction.rawText.isNotBlank()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Captured text", style = MaterialTheme.typography.labelMedium)
            Text(
                transaction.rawText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private fun amountInput(amountMinor: Long): String {
    val whole = amountMinor / 100
    val frac = amountMinor % 100
    return if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(2, '0')}"
}

/** Saved state in the same shape the draft produces, so dirty checks ignore trimming and blanks. */
private fun TransactionEntity.toEdits() = TransactionEdits(
    amountMinor = amountMinor,
    direction = direction,
    merchant = merchant.trim(),
    occurredAt = occurredAt,
    category = category,
    customCategoryName = customCategoryName,
    customCategoryColor = customCategoryColor,
    customCategoryIcon = customCategoryIcon,
    paymentMode = paymentMode,
    description = description?.takeIf { it.isNotBlank() },
    tags = tags,
)
