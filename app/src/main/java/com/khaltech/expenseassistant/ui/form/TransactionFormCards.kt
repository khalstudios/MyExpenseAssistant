package com.khaltech.expenseassistant.ui.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.IncomeColor
import com.khaltech.expenseassistant.ui.SpendColor
import com.khaltech.expenseassistant.ui.formatShortDate
import com.khaltech.expenseassistant.ui.formatTimeOnly
import com.khaltech.expenseassistant.ui.rememberHeroGradient

/**
 * Every field of a transaction on one card: what it was, when, how it was paid and why.
 * Shared so adding a transaction by hand and editing a captured one look and behave the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFieldsCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    direction: Direction,
    onDirectionChange: (Direction) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    categoryName: String,
    categoryBadge: @Composable () -> Unit,
    onEditCategory: () -> Unit,
    occurredAt: Long,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    paymentMode: PaymentMode,
    onPaymentModeChange: (PaymentMode) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    paymentModes: List<PaymentMode> = PaymentMode.entries,
) {
    val isDebit = direction == Direction.DEBIT
    val accent = if (isDebit) SpendColor else IncomeColor
    val hero = rememberHeroGradient()
    FormCard {
        // The two fields that define the transaction keep the gradient; the pickers sit on plain surface.
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                // A leading slot rather than a prefix: the prefix only shows once the label floats,
                // so an untouched field would hide which way the money is going.
                leadingIcon = {
                    Text(
                        if (isDebit) "− ₹" else "+ ₹",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DetailRow(
            label = "Category",
            value = categoryName,
            onClick = onEditCategory,
            leading = categoryBadge,
        )
        RowDivider()
        DetailRow(
            label = "Date",
            value = formatShortDate(occurredAt),
            onClick = onPickDate,
            leading = { RowIcon(Icons.Filled.CalendarMonth) },
        )
        RowDivider()
        DetailRow(
            label = "Time",
            value = formatTimeOnly(occurredAt),
            onClick = onPickTime,
            leading = { RowIcon(Icons.Filled.Schedule) },
        )
        RowDivider()
        PaymentModeRow(paymentMode, onPaymentModeChange, paymentModes)
        RowDivider()
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Notes") },
            placeholder = { Text("What was this for?") },
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: ImageVector = Icons.Filled.ChevronRight,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading()
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            trailing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The row opens the modes underneath it as chips, the way the tags are chosen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentModeRow(
    paymentMode: PaymentMode,
    onPaymentModeChange: (PaymentMode) -> Unit,
    modes: List<PaymentMode> = PaymentMode.entries,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        DetailRow(
            label = "Payment mode",
            value = paymentMode.displayName,
            onClick = { expanded = !expanded },
            leading = { RowIcon(Icons.Filled.Payments) },
            trailing = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        )
        AnimatedVisibility(expanded) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 62.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                modes.forEach { mode ->
                    val selected = mode == paymentMode
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onPaymentModeChange(mode)
                            expanded = false
                        },
                        label = { Text(mode.displayName) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun RowIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Indented to line up with the row labels, the way list dividers do. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(start = 62.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * One chip section above the field holds both: the tags on this transaction, ticked and highlighted,
 * followed by the few others worth offering. Typing narrows the offered ones without hiding the chosen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TagsCard(
    tags: List<String>,
    suggestions: List<String>,
    onTagsChange: (List<String>) -> Unit,
    onOpenTag: ((String) -> Unit)? = null,
) {
    var newTag by remember { mutableStateOf("") }
    val typed = newTag.trim().removePrefix("#")
    val unused = suggestions.filter { s -> tags.none { it.equals(s, ignoreCase = true) } }
    // Idle, the repository already hands these over most-used first, so the head of the list is the
    // relevant part. Typing narrows to what starts with it, then falls back to anything containing it.
    val offered = if (typed.isEmpty()) {
        unused.take(IDLE_TAG_SUGGESTIONS)
    } else {
        val (starts, rest) = unused.partition { it.startsWith(typed, ignoreCase = true) }
        (starts + rest.filter { it.contains(typed, ignoreCase = true) }).take(MATCHING_TAG_SUGGESTIONS)
    }
    val addTag = {
        if (typed.isNotEmpty() && tags.none { it.equals(typed, ignoreCase = true) }) {
            onTagsChange(tags + typed)
        }
        newTag = ""
    }

    FormCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (tags.isNotEmpty() || offered.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Keyed so a chip keeps its own identity as the row reorders; without this the
                    // press ripple stays with the slot and lights up whichever chip moved into it.
                    tags.forEach { tag ->
                        key("tag-$tag") {
                            InputChip(
                                selected = true,
                                onClick = { onOpenTag?.invoke(tag) },
                                label = { Text("#$tag", fontWeight = FontWeight.SemiBold) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove $tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onTagsChange(tags - tag) },
                                    )
                                },
                            )
                        }
                    }
                    offered.forEach { suggestion ->
                        key("suggestion-$suggestion") {
                            AssistChip(
                                onClick = {
                                    onTagsChange(tags + suggestion)
                                    newTag = ""
                                },
                                label = { Text("#$suggestion") },
                            )
                        }
                    }
                }
            }
            if (typed.isNotEmpty() && offered.isEmpty()) {
                Text(
                    "No tag matches “$typed” — add it as a new one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (tags.isEmpty() && offered.isEmpty()) {
                Text(
                    "None on this transaction yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // The field takes the row; the square button beside it commits what was typed.
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                    modifier = Modifier.weight(1f),
                )
                // Square and filled, so it reads as the live action the moment there is something to add.
                Button(
                    onClick = addTag,
                    enabled = typed.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tag")
                }
            }
        }
    }
}

/** Short enough to stay a hint rather than a list; typing widens it a little. */
private const val IDLE_TAG_SUGGESTIONS = 6
private const val MATCHING_TAG_SUGGESTIONS = 8

/** A surface card with no padding of its own, so rows can run edge to edge. */
@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
        content = content,
    )
}
