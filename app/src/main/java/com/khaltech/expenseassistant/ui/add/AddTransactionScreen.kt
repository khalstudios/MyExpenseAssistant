package com.khaltech.expenseassistant.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.data.repo.MerchantSuggestion
import com.khaltech.expenseassistant.data.repo.NoteSuggestion
import com.khaltech.expenseassistant.data.repo.mergeTags
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.CategoryIconCatalog
import com.khaltech.expenseassistant.ui.category.CategoryPickerSheet
import com.khaltech.expenseassistant.ui.form.TagsCard
import com.khaltech.expenseassistant.ui.form.TransactionFieldsCard
import com.khaltech.expenseassistant.ui.toMinorUnits
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSave: (ManualTransactionInput) -> Unit,
    customCategories: List<CustomCategoryOption> = emptyList(),
    tagSuggestions: List<String> = emptyList(),
    merchantSuggestions: List<MerchantSuggestion> = emptyList(),
    noteSuggestions: List<NoteSuggestion> = emptyList(),
) {
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.OTHER) }
    var customCategoryName by remember { mutableStateOf<String?>(null) }
    var customCategoryColor by remember { mutableStateOf<String?>(null) }
    var customCategoryIcon by remember { mutableStateOf<String?>(null) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    val openedAt = remember { System.currentTimeMillis() }
    var occurredAt by remember { mutableLongStateOf(openedAt) }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }

    var pickingCategory by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }

    val canSave = amount.toMinorUnits() > 0 && merchant.isNotBlank()
    // Anything touched since the screen opened is worth a warning before it is thrown away.
    val isDirty = amount.isNotBlank() || merchant.isNotBlank() || description.isNotBlank() ||
        tags.isNotEmpty() || customCategoryName != null || category != Category.OTHER ||
        paymentMode != PaymentMode.CASH || direction != Direction.DEBIT || occurredAt != openedAt

    fun leave(action: () -> Unit) {
        if (isDirty) pendingExit = action else action()
    }

    BackHandler(enabled = isDirty) { pendingExit = onBack }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Add transaction") },
                navigationIcon = {
                    IconButton(onClick = { leave(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (canSave) {
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
                                tags = tags,
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
                Icon(Icons.Filled.Save, contentDescription = "Save transaction")
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
            TransactionFieldsCard(
                amount = amount,
                onAmountChange = { input -> amount = input.filter { it.isDigit() || it == '.' } },
                direction = direction,
                onDirectionChange = { direction = it },
                merchant = merchant,
                onMerchantChange = { merchant = it },
                categoryName = customCategoryName ?: category.displayName,
                categoryBadge = {
                    ChosenCategoryBadge(category, customCategoryName, customCategoryColor, customCategoryIcon)
                },
                onEditCategory = { pickingCategory = true },
                occurredAt = occurredAt,
                onPickDate = { pickingDate = true },
                onPickTime = { pickingTime = true },
                paymentMode = paymentMode,
                onPaymentModeChange = { paymentMode = it },
                description = description,
                onDescriptionChange = { description = it },
                // Nothing was captured, so there is no unknown mode to fall back to.
                paymentModes = PaymentMode.entries.filter { it != PaymentMode.UNKNOWN },
                merchantSuggestions = merchantSuggestions,
                onMerchantSuggestionPicked = { picked ->
                    merchant = picked.name
                    category = picked.category
                    customCategoryName = picked.customCategoryName
                    customCategoryColor = picked.customCategoryColor
                    customCategoryIcon = picked.customCategoryIcon
                    tags = mergeTags(tags, picked.tags)
                },
                noteSuggestions = noteSuggestions,
            )
            TagsCard(
                tags = tags,
                suggestions = tagSuggestions,
                onTagsChange = { tags = it },
            )
            // Keeps the last card clear of the floating save button.
            Spacer(Modifier.height(72.dp))
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

    pendingExit?.let { exit ->
        AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text("Discard changes?") },
            text = { Text("This transaction hasn't been saved yet.") },
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

/** A custom category has no entity to read its look from, so its colour and icon are drawn here. */
@Composable
private fun ChosenCategoryBadge(
    category: Category,
    customName: String?,
    customColor: String?,
    customIcon: String?,
) {
    if (customName == null) {
        CategoryBadge(category, size = 32.dp)
        return
    }
    val tint = runCatching { Color(android.graphics.Color.parseColor(customColor)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Box(
        Modifier
            .size(32.dp)
            .background(tint.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            CategoryIconCatalog.iconFor(customIcon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
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
