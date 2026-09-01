package com.expenseassistant.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberSoftGradient
import com.expenseassistant.ui.toMinorUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    onOpenCategory: (Category) -> Unit = {},
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var editingOverall by remember { mutableStateOf(false) }

    val categories = remember { Category.entries.filter { it != Category.INCOME } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly budgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Set monthly budget limits. Tap a category name to view its transactions, or tap the limit chip to edit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                val overallLimit = budgets[BudgetEntity.OVERALL] ?: 0L
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rememberSoftGradient())
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp).padding(4.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "All spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Overall monthly cap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(
                            onClick = { editingOverall = true },
                            label = {
                                Text(
                                    if (overallLimit > 0) formatMinor(overallLimit) else "+ Set limit",
                                    fontWeight = if (overallLimit > 0) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                        )
                    }
                }
            }

            item {
                Text(
                    "Category budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(rememberSoftGradient())
                            .padding(vertical = 4.dp),
                    ) {
                        categories.forEachIndexed { index, category ->
                            val limitMinor = budgets[category.name] ?: 0L
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenCategory(category) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CategoryBadge(category, size = 38.dp)
                                    Column {
                                        Text(
                                            category.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "View transactions \u203a",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                AssistChip(
                                    onClick = { editing = category },
                                    label = {
                                        Text(
                                            if (limitMinor > 0) formatMinor(limitMinor) else "+ Set",
                                            fontWeight = if (limitMinor > 0) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                )
                            }
                            if (index < categories.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingOverall) {
        BudgetAmountDialog(
            title = "Budget for all spending",
            initialMinor = budgets[BudgetEntity.OVERALL] ?: 0L,
            onDismiss = { editingOverall = false },
            onConfirm = {
                viewModel.setOverall(it)
                editingOverall = false
            },
        )
    }

    editing?.let { category ->
        BudgetAmountDialog(
            title = "Budget for ${category.displayName}",
            initialMinor = budgets[category.name] ?: 0L,
            onDismiss = { editing = null },
            onConfirm = {
                viewModel.setCategory(category, it)
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetAmountDialog(
    title: String,
    initialMinor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
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
                label = { Text("Monthly limit") },
                prefix = { Text("\u20b9 ") },
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
