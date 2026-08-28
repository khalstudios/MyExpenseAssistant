package com.expenseassistant.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.Category
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.toMinorUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var editingOverall by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Budgets apply to each calendar month. Leave a limit empty to remove it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                BudgetEditRow(
                    title = "All spending",
                    limitMinor = budgets[BudgetEntity.OVERALL] ?: 0L,
                    badge = null,
                    onClick = { editingOverall = true },
                )
            }
            item {
                Text(
                    "Per category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(Category.entries.filter { it != Category.INCOME }, key = { it.name }) { category ->
                BudgetEditRow(
                    title = category.displayName,
                    limitMinor = budgets[category.name] ?: 0L,
                    badge = { CategoryBadge(category, size = 38.dp) },
                    onClick = { editing = category },
                )
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
private fun BudgetEditRow(
    title: String,
    limitMinor: Long,
    badge: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            badge?.invoke()
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (limitMinor > 0) formatMinor(limitMinor) else "Not set",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (limitMinor > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (limitMinor > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
