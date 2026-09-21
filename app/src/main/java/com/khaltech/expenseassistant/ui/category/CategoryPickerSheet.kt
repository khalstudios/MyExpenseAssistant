package com.khaltech.expenseassistant.ui.category

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.ui.pro.FreeCustomCategoryLimit
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.ui.pro.ProPitch
import com.khaltech.expenseassistant.ui.pro.ProPitches

/**
 * Colours offered for any category, arranged as a spectrum so the dialog reads as a palette rather
 * than a jumble: soft tones first (the original eight are among them, so existing custom
 * categories still match a swatch), then deeper ones for anyone who wants more contrast.
 */
val CategoryColorPalette = listOf(
    // Soft
    Color(0xFFEF9A9A), Color(0xFFF06292), Color(0xFFCE93D8), Color(0xFF9575CD),
    Color(0xFF9FA8DA), Color(0xFF90CAF9), Color(0xFF4FC3F7), Color(0xFF80DEEA),
    Color(0xFF4DB6AC), Color(0xFFA5D6A7), Color(0xFFAED581), Color(0xFFE6EE9C),
    Color(0xFFFFD54F), Color(0xFFFFCC80), Color(0xFFFF8A65), Color(0xFFBCAAA4),
    Color(0xFF90A4AE), Color(0xFFB0BEC5),
    // Deep
    Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFF5E35B1),
    Color(0xFF3949AB), Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1),
    Color(0xFF00897B), Color(0xFF43A047), Color(0xFF7CB342), Color(0xFFC0CA33),
    Color(0xFFFDD835), Color(0xFFFFB300), Color(0xFFFB8C00), Color(0xFFF4511E),
    Color(0xFF6D4C41), Color(0xFF546E7A),
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
    var editMode by remember { mutableStateOf(false) }
    val editor = rememberCategoryEditorState()
    val customActions = LocalCustomCategoryActions.current

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (editMode) "Edit categories" else "Choose a category",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { editMode = !editMode }) {
                    if (!editMode) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(if (editMode) "Done" else "  Edit")
                }
            }
            Text(
                if (editMode) {
                    "Tap a category to change its colour or icon. Ones you made can also be renamed or deleted."
                } else {
                    "Applies to $merchant and future payments to it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // The default set, plus every extended category this user is already filing under —
            // and, where the transaction being edited sits in one, that one too, so the picker can
            // always show what it is currently set to.
            val pro = LocalPro.current
            val inUse = LocalCategoriesInUse.current
            val offered = Category.offered(pro.isPro, inUse, current = selected)
            val locked = Category.Extended - offered.toSet()

            if (editMode) {
                CategoryEditList(
                    builtIn = offered,
                    custom = customCategories,
                    onEditBuiltIn = { editor.builtIn = it },
                    onEditCustom = { editor.custom = it },
                )
            } else {
                // One grid for built-in categories and the user's own, most-used first, so the few
                // someone actually files under are the first row rather than scattered through twenty.
                // Unknown always goes last: it collects every payment nobody has sorted yet, and
                // leading with it would be leading with the backlog.
                val usage = LocalCategoryUsage.current
                val entries = offered.map { PickerEntry.BuiltIn(it, usage[it] ?: 0) } +
                    customCategories.map { PickerEntry.Custom(it) }
                val ordered = entries
                    .withIndex()
                    .sortedWith(
                        compareBy<IndexedValue<PickerEntry>> { (it.value as? PickerEntry.BuiltIn)?.category == Category.OTHER }
                            .thenByDescending { it.value.useCount }
                            .thenBy { it.index },
                    )
                    .map { it.value }
                val current = ordered.firstOrNull { entry ->
                    when (entry) {
                        is PickerEntry.BuiltIn -> selectedCustomName == null && entry.category == selected
                        is PickerEntry.Custom -> entry.option.name.equals(selectedCustomName, ignoreCase = true)
                    }
                }
                var showAll by remember { mutableStateOf(false) }
                // Collapsing only pays off when it hides a meaningful number of tiles.
                val collapsible = ordered.size > PickerTopCount + 2
                val shown = when {
                    showAll || !collapsible -> ordered
                    else -> ordered.take(PickerTopCount).let { top ->
                        // The current category is always visible, even when it is rarely used.
                        if (current != null && current !in top) top + current else top
                    }
                }
    
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    shown.forEach { entry ->
                        when (entry) {
                            is PickerEntry.BuiltIn -> CategoryTile(
                                category = entry.category,
                                isSelected = entry == current,
                                onClick = { onSelect(entry.category) },
                                onLongClick = { editor.builtIn = entry.category },
                            )
                            is PickerEntry.Custom -> CustomCategoryTile(
                                option = entry.option,
                                isSelected = entry == current,
                                onClick = {
                                    onSelectCustom(entry.option.name, entry.option.colorHex, entry.option.iconKey ?: "label")
                                },
                                // Where nothing can act on an edit, a long-press does nothing rather
                                // than offering changes that would not happen.
                                onLongClick = { if (customActions != null) editor.custom = entry.option },
                            )
                        }
                    }
                    if (collapsible) {
                        MoreCategoriesTile(
                            expanded = showAll,
                            hiddenCount = ordered.size - shown.size,
                            onClick = { showAll = !showAll },
                        )
                    }
                }
    
                HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 12.dp))
                CreateCustomCategoryChip(
                    onCreate = { creating = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
    
                if (!pro.isPro) {
                    UnlockCategoriesCard(
                        locked = locked,
                        onUpgrade = pro.onUpgrade,
                        onUpgradeFor = pro.onUpgradeFor,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
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

    CategoryEditorDialogs(editor, customCategories)
}

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/**
 * Which category is open for editing, and which custom one is waiting on a delete confirmation.
 * Shared by the picker's Edit mode and the Categories screen, so both edit the same way.
 */
@Stable
class CategoryEditorState {
    var builtIn by mutableStateOf<Category?>(null)
    var custom by mutableStateOf<CustomCategoryOption?>(null)
    var deleting by mutableStateOf<CustomCategoryOption?>(null)
}

@Composable
fun rememberCategoryEditorState(): CategoryEditorState = remember { CategoryEditorState() }

/**
 * The dialogs behind [CategoryEditorState]: colour and icon for a built-in category, and name,
 * colour, icon or deletion for one the user made. Draws nothing while nothing is open.
 */
@Composable
fun CategoryEditorDialogs(state: CategoryEditorState, customCategories: List<CustomCategoryOption>) {
    val context = LocalContext.current
    val iconStore = remember { ServiceLocator.categoryIconStore(context) }
    val iconOverrides by iconStore.overrides.collectAsState()
    val colorStore = remember { ServiceLocator.categoryColorStore(context) }
    val colorOverrides by colorStore.overrides.collectAsState()
    val customActions = LocalCustomCategoryActions.current

    state.deleting?.let { option ->
        if (customActions != null) {
            DeleteCustomCategoryDialog(
                option = option,
                countTransactions = customActions.countTransactions,
                onConfirm = {
                    customActions.delete(option.name)
                    state.deleting = null
                },
                onDismiss = { state.deleting = null },
            )
        }
    }

    state.builtIn?.let { category ->
        val defaultIcon = CategoryIconCatalog.defaultKeyFor(category)
        val defaultColor = category.defaultColor
        EditCategoryDialog(
            title = category.displayName,
            // Built-in names stay fixed: they appear in exports, budgets and notifications, which
            // should always mean the same thing.
            initialName = null,
            initialColor = colorFromHex(colorOverrides[category.name]) ?: defaultColor,
            colorChoices = (listOf(defaultColor) + CategoryColorPalette).distinct(),
            initialIconKey = iconOverrides[category.name] ?: defaultIcon,
            onSave = { _, color, iconKey ->
                // Choosing the default again clears the override, so a later change to the default
                // reaches this user too.
                if (color == defaultColor) colorStore.clearColor(category.name)
                else colorStore.setColor(category.name, color.toHex())
                if (iconKey == defaultIcon) iconStore.clearIcon(category.name)
                else iconStore.setIcon(category.name, iconKey)
                state.builtIn = null
            },
            onReset = if (iconOverrides.containsKey(category.name) || colorOverrides.containsKey(category.name)) {
                {
                    colorStore.clearColor(category.name)
                    iconStore.clearIcon(category.name)
                    state.builtIn = null
                }
            } else {
                null
            },
            onDismiss = { state.builtIn = null },
        )
    }

    state.custom?.let { option ->
        if (customActions != null) {
            val initialColor = colorFromHex(option.colorHex) ?: CategoryColorPalette.first()
            EditCategoryDialog(
                title = "Edit category",
                initialName = option.name,
                // A name already taken by another of the user's categories would silently merge
                // the two, so it is refused rather than guessed at.
                isNameTaken = { name ->
                    customCategories.any {
                        !it.name.equals(option.name, ignoreCase = true) && it.name.equals(name.trim(), ignoreCase = true)
                    }
                },
                initialColor = initialColor,
                colorChoices = (CategoryColorPalette + initialColor).distinct(),
                initialIconKey = option.iconKey ?: "label",
                onSave = { name, color, iconKey ->
                    customActions.update(option.name, name ?: option.name, color.toHex(), iconKey)
                    state.custom = null
                },
                onDelete = {
                    state.custom = null
                    state.deleting = option
                },
                onDismiss = { state.custom = null },
            )
        }
    }
}

/**
 * Every category in one list, the user's own first, so each can be opened and edited without
 * hunting through the grid. [showCounts] adds how many transactions each holds.
 */
@Composable
fun CategoryEditList(
    builtIn: List<Category>,
    custom: List<CustomCategoryOption>,
    onEditBuiltIn: (Category) -> Unit,
    onEditCustom: (CustomCategoryOption) -> Unit,
    showCounts: Boolean = false,
) {
    val canEditCustom = LocalCustomCategoryActions.current != null
    val usage = LocalCategoryUsage.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (custom.isNotEmpty() && canEditCustom) {
            EditListHeader("Yours")
            custom.forEach { option ->
                val color = colorFromHex(option.colorHex) ?: MaterialTheme.colorScheme.primary
                EditListRow(
                    name = option.name,
                    icon = CategoryIconCatalog.iconFor(option.iconKey),
                    color = color,
                    count = option.useCount.takeIf { showCounts },
                    onClick = { onEditCustom(option) },
                )
            }
            EditListHeader("Built-in", Modifier.padding(top = 12.dp))
        }
        builtIn.forEach { category ->
            EditListRow(
                name = category.displayName,
                icon = category.resolvedIcon(),
                color = category.color,
                count = (usage[category] ?: 0).takeIf { showCounts },
                onClick = { onEditBuiltIn(category) },
            )
        }
    }
}

@Composable
private fun EditListHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun EditListRow(name: String, icon: ImageVector, color: Color, count: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            count?.let {
                Text(
                    if (it == 1) "1 transaction" else "$it transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Edit $name",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Colour and icon for any category, plus the name for one the user made. [initialName] null means
 * the name is fixed and is shown as the title instead of in a field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditCategoryDialog(
    title: String,
    initialName: String?,
    initialColor: Color,
    colorChoices: List<Color>,
    initialIconKey: String,
    onSave: (name: String?, color: Color, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
    isNameTaken: (String) -> Boolean = { false },
    /** Shown for a built-in category that has been changed. */
    onReset: (() -> Unit)? = null,
    /** Shown for a category the user made. */
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName.orEmpty()) }
    var color by remember { mutableStateOf(initialColor) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    val nameTaken = initialName != null && isNameTaken(name)
    val canSave = initialName == null || (name.isNotBlank() && !nameTaken)

    AlertDialog(
        onDismissRequest = onDismiss,
        // Delete lives in the title row, top left, where it is always in view: under the icon grid,
        // which is taller than the dialog, it was scrolled out of sight.
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onDelete?.let {
                    IconButton(onClick = it, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(title)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (initialName != null) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        isError = nameTaken,
                        supportingText = if (nameTaken) {
                            { Text("You already have a category with this name") }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                ColorSwatches(choices = colorChoices, selected = color, onSelect = { color = it })
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                IconGrid(selectedKey = iconKey, onSelect = { iconKey = it })
            }
        },
        // Reset sits with the other buttons for the same reason Delete sits in the title.
        confirmButton = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                onReset?.let {
                    TextButton(onClick = it) { Text("Reset") }
                }
                Box(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = { onSave(if (initialName != null) name.trim() else null, color, iconKey) },
                    enabled = canSave,
                ) { Text("Save") }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(choices: List<Color>, selected: Color, onSelect: (Color) -> Unit) {
    // Wraps rather than scrolls, so every choice is visible in a narrow dialog.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        choices.forEach { swatch ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        width = if (swatch == selected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(swatch) },
            )
        }
    }
}

@Composable
private fun DeleteCustomCategoryDialog(
    option: CustomCategoryOption,
    countTransactions: suspend (String) -> Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Null until counted, so the dialog never briefly claims the category holds nothing.
    val count by produceState<Int?>(null, option.name) { value = countTransactions(option.name) }
    val fallback = Category.OTHER.displayName
    val consequence = when (val n = count) {
        null -> ""
        1 -> " The 1 transaction filed under it will move to $fallback."
        else -> " The $n transactions filed under it will move to $fallback."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"${option.name}\"?") },
        text = { Text("This removes the category from every transaction, not just this one.$consequence") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = count != null) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
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

/** A tile in the picker's grid: one of the built-in categories, or one the user made. */
private sealed interface PickerEntry {
    val useCount: Int

    data class BuiltIn(val category: Category, override val useCount: Int) : PickerEntry
    data class Custom(val option: CustomCategoryOption) : PickerEntry {
        override val useCount: Int get() = option.useCount
    }
}

/**
 * Categories the user already made keep working whatever their entitlement, so a lapsed or restored
 * account never finds its own filing system disabled. A free user may make a couple of their own;
 * only the one after that is Pro.
 */
@Composable
private fun CreateCustomCategoryChip(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val pro = LocalPro.current
    val freeLeft = (FreeCustomCategoryLimit - LocalCustomCategoryCount.current).coerceAtLeast(0)
    val canCreate = pro.isPro || freeLeft > 0
    AssistChip(
        onClick = { if (canCreate) onCreate() else pro.onUpgradeFor(ProPitches.CustomCategories) },
        label = {
            Text(
                if (!pro.isPro && freeLeft > 0) "Create your own category · $freeLeft free"
                else "Create your own category",
            )
        },
        leadingIcon = {
            Icon(
                if (canCreate) Icons.Filled.Add else Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = modifier,
    )
}

/** Tiles shown before the grid collapses the rest behind "More". Two rows on a typical phone. */
private const val PickerTopCount = 8

@Composable
private fun MoreCategoriesTile(expanded: Boolean, hiddenCount: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (expanded) "Show less" else "$hiddenCount more",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * What Pro adds to categories, shown to free users at the moment they are choosing one.
 *
 * The locked categories are drawn as real tiles in their own colours, not as grey chips, so the
 * user sees exactly what they would be filing under. Tapping one opens the paywall headed with
 * that category's name, which is the reason they tapped it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnlockCategoriesCard(
    locked: List<Category>,
    onUpgrade: () -> Unit,
    onUpgradeFor: (ProPitch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Unlock more categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    "Unlimited categories of your own, plus these ready-made ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }

        if (locked.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                locked.forEach { category ->
                    LockedCategoryTile(
                        category = category,
                        onClick = { onUpgradeFor(ProPitches.extendedCategory(category.displayName)) },
                    )
                }
            }
        }

        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Get Pro")
        }
    }
}

@Composable
private fun LockedCategoryTile(category: Category, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            CategoryBadge(category = category, size = 36.dp)
            // A small padlock on the corner says "locked" without greying out the category itself.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Pro",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        Text(
            category.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun NewCustomCategoryDialog(onConfirm: (name: String, color: Color, iconKey: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(CategoryColorPalette.first()) }
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
                ColorSwatches(choices = CategoryColorPalette, selected = color, onSelect = { color = it })
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

/** A category the user made, drawn like a built-in one so the grid reads as a single set. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomCategoryTile(
    option: CustomCategoryOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(option.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val border = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                CategoryIconCatalog.iconFor(option.iconKey),
                contentDescription = option.name,
                tint = color,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            option.name,
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
    transaction: com.khaltech.expenseassistant.data.model.TransactionEntity,
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
