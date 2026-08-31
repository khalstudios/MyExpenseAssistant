package com.expenseassistant.ui.category

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.repo.CustomCategoryOption
import com.expenseassistant.di.ServiceLocator

private val CustomCategorySwatches = listOf(
    Color(0xFFF06292), Color(0xFF9575CD), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
    Color(0xFFAED581), Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFF90A4AE),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerSheet(
    merchant: String,
    selected: Category,
    selectedCustomName: String? = null,
    customCategories: List<CustomCategoryOption> = emptyList(),
    onSelect: (Category) -> Unit,
    onSelectCustom: (name: String, colorHex: String, iconKey: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var editingIconFor by remember { mutableStateOf<Category?>(null) }
    val context = LocalContext.current
    val iconStore = remember { ServiceLocator.categoryIconStore(context) }
    val iconOverrides by iconStore.overrides.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Choose a category", style = MaterialTheme.typography.titleLarge)
            Text(
                "Applies to $merchant and future payments to it. Long-press a category to change its icon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Category.entries.forEach { category ->
                    CategoryTile(
                        category = category,
                        isSelected = selectedCustomName == null && category == selected,
                        onClick = { onSelect(category) },
                        onLongClick = { editingIconFor = category },
                    )
                }
            }

            Text(
                "Your categories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            CustomCategoryRow(
                customCategories = customCategories,
                selectedCustomName = selectedCustomName,
                onSelectCustom = onSelectCustom,
                onAddNew = { creating = true },
            )
        }
    }

    if (creating) {
        NewCustomCategoryDialog(
            onConfirm = { name, color, iconKey ->
                onSelectCustom(name, String.format("#%06X", 0xFFFFFF and color.toArgb()), iconKey)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }

    editingIconFor?.let { category ->
        IconPickerDialog(
            title = "Icon for ${category.displayName}",
            selectedKey = iconOverrides[category.name] ?: CategoryIconCatalog.defaultKeyFor(category),
            hasOverride = iconOverrides.containsKey(category.name),
            onSelect = { key ->
                iconStore.setIcon(category.name, key)
                editingIconFor = null
            },
            onResetToDefault = {
                iconStore.clearIcon(category.name)
                editingIconFor = null
            },
            onDismiss = { editingIconFor = null },
        )
    }
}

@Composable
private fun IconPickerDialog(
    title: String,
    selectedKey: String?,
    hasOverride: Boolean,
    onSelect: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { IconGrid(selectedKey = selectedKey, onSelect = onSelect) },
        confirmButton = {
            if (hasOverride) {
                TextButton(onClick = onResetToDefault) { Text("Reset to default") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid(selectedKey: String?, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CategoryIconCatalog.options.forEach { (key, icon) ->
            val isSelected = key == selectedKey
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = key, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomCategoryRow(
    customCategories: List<CustomCategoryOption>,
    selectedCustomName: String?,
    onSelectCustom: (name: String, colorHex: String, iconKey: String) -> Unit,
    onAddNew: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        customCategories.forEach { option ->
            val color = runCatching { Color(android.graphics.Color.parseColor(option.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
            AssistChip(
                onClick = { onSelectCustom(option.name, option.colorHex, option.iconKey ?: "label") },
                label = { Text(option.name) },
                leadingIcon = {
                    Icon(
                        if (option.name == selectedCustomName) Icons.Filled.Check else CategoryIconCatalog.iconFor(option.iconKey),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        AssistChip(
            onClick = onAddNew,
            label = { Text("New category") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
    }
}

@Composable
private fun NewCustomCategoryDialog(onConfirm: (name: String, color: Color, iconKey: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(CustomCategorySwatches.first()) }
    var iconKey by remember { mutableStateOf(CategoryIconCatalog.options.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomCategorySwatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (swatch == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = swatch },
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                IconGrid(selectedKey = iconKey, onSelect = { iconKey = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), color, iconKey) },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryTile(category: Category, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val border = if (isSelected) category.color else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(if (isSelected) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryBadge(category = category, size = 40.dp)
        Text(
            category.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
fun CategoryBadge(
    category: Category,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    showCheck: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(category.color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (showCheck) Icons.Filled.Check else category.resolvedIcon(),
            contentDescription = category.displayName,
            tint = category.color,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/** Overload for rows/headers that should reflect a user-created custom category, if any. */
@Composable
fun CategoryBadge(
    transaction: com.expenseassistant.data.model.TransactionEntity,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(transaction.displayCategoryColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = transaction.displayCategoryIcon,
            contentDescription = transaction.displayCategoryName,
            tint = transaction.displayCategoryColor,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Composable
fun CategoryDot(category: Category, size: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (category.color == Color.Unspecified) MaterialTheme.colorScheme.primary else category.color)
    )
}
