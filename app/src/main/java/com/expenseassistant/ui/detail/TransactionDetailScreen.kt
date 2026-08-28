package com.expenseassistant.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryPickerSheet
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: TransactionEntity,
    onBack: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onDelete: () -> Unit,
) {
    var pickingCategory by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF6F2EF),
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            TagsSection(transaction.tags, onTagsChange)
            MetadataSection(transaction)
        }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryBadge(transaction.category, size = 56.dp)
            Text(
                (if (isDebit) "- " else "+ ") + formatMinor(transaction.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                transaction.merchant,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                "${if (isDebit) "Spend" else "Income"} · ${formatTimestamp(transaction.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(
                onClick = onEditCategory,
                label = { Text(transaction.category.displayName) },
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
private fun TagsSection(tags: List<String>, onChange: (List<String>) -> Unit) {
    var newTag by remember { mutableStateOf("") }

    SectionCard("Tags") {
        if (tags.isEmpty()) {
            Text(
                "No tags yet. Use tags like reimbursable, family or trip.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onChange(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $tag") },
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
                        onChange(tags + newTag)
                        newTag = ""
                    }
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add tag")
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
