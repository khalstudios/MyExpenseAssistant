package com.khaltech.expenseassistant.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.data.repo.MerchantSuggestion
import com.khaltech.expenseassistant.data.repo.NoteSuggestion
import com.khaltech.expenseassistant.data.repo.mergeTags
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.CategoryPickerSheet
import com.khaltech.expenseassistant.ui.category.displayCategoryName
import com.khaltech.expenseassistant.ui.form.TagsCard
import com.khaltech.expenseassistant.ui.form.TransactionFieldsCard
import com.khaltech.expenseassistant.ui.formatMinor
import com.khaltech.expenseassistant.ui.formatTimestamp
import com.khaltech.expenseassistant.ui.toMinorUnits
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
    merchantSuggestions: List<MerchantSuggestion> = emptyList(),
    noteSuggestions: List<NoteSuggestion> = emptyList(),
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
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TransactionFieldsCard(
                amount = amount,
                onAmountChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                direction = direction,
                onDirectionChange = { direction = it },
                merchant = merchant,
                onMerchantChange = { merchant = it },
                categoryName = preview.displayCategoryName,
                categoryBadge = { CategoryBadge(preview, size = 32.dp) },
                onEditCategory = { pickingCategory = true },
                occurredAt = occurredAt,
                onPickDate = { pickingDate = true },
                onPickTime = { pickingTime = true },
                paymentMode = paymentMode,
                onPaymentModeChange = { paymentMode = it },
                description = description,
                onDescriptionChange = { description = it },
                merchantSuggestions = merchantSuggestions,
                onMerchantSuggestionPicked = { picked ->
                    merchant = picked.name
                    category = picked.category
                    customName = picked.customCategoryName
                    customColor = picked.customCategoryColor
                    customIcon = picked.customCategoryIcon
                    tags = mergeTags(tags, picked.tags)
                },
                noteSuggestions = noteSuggestions,
            )
            TagsCard(
                tags = tags,
                suggestions = tagSuggestions,
                onTagsChange = { tags = it },
                onOpenTag = { tag -> leave { onOpenTag(tag) } },
                showEmptyHint = true,
            )
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MetadataCard(transaction: TransactionEntity) {
    SectionCard("Metadata") {
        // The name the payment was captured with, once a rename or a learned name has replaced it.
        transaction.merchantRaw?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(transaction.merchant.trim(), ignoreCase = true) }
            ?.let { MetaRow("Original name", it) }
        MetaRow("Source","${transaction.sourceApp} (${transaction.captureSource.name.lowercase()})")
        accountLabel(transaction)?.let { MetaRow("Account", it) }
        transaction.transactionType?.let { MetaRow("Type", it.displayName) }
        transaction.availableBalanceMinor?.let {
            val label = if (transaction.accountType == AccountType.CREDIT_CARD) "Available limit" else "Balance after"
            MetaRow(label, formatMinor(it))
        }
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

/** "HDFC Bank · Credit card ••1234", or whichever parts the bank alert stated. */
private fun accountLabel(transaction: TransactionEntity): String? {
    val account = transaction.accountType?.let { type ->
        listOfNotNull(type.displayName, transaction.accountLast4?.let { "••$it" }).joinToString(" ")
    }
    return listOfNotNull(transaction.bankName, account).joinToString(" · ").takeIf { it.isNotEmpty() }
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
