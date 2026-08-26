package com.expenseassistant.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormatSymbols
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodNavigator(
    label: String,
    range: AnalyticsRange,
    canGoForward: Boolean,
    isCurrentPeriod: Boolean,
    onRangeChange: (AnalyticsRange) -> Unit,
    onShift: (Int) -> Unit,
    onJumpTo: (year: Int, monthIndex: Int) -> Unit,
    onResetToCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AnalyticsRange.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = entry == range,
                    onClick = { onRangeChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, AnalyticsRange.entries.size),
                    label = { Text(entry.label) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onShift(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period")
            }
            TextButton(onClick = { showPicker = true }) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { onShift(1) }, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next period")
            }
        }
        if (!isCurrentPeriod) {
            TextButton(onClick = onResetToCurrent, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to current ${range.label.lowercase()}")
            }
        }
    }

    if (showPicker) {
        PeriodPickerDialog(
            range = range,
            onDismiss = { showPicker = false },
            onPick = { year, monthIndex ->
                onJumpTo(year, monthIndex)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPickerDialog(
    range: AnalyticsRange,
    onDismiss: () -> Unit,
    onPick: (year: Int, monthIndex: Int) -> Unit,
) {
    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    var year by remember { mutableIntStateOf(thisYear) }
    val months = remember { DateFormatSymbols.getInstance().shortMonths.filter { it.isNotBlank() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (range == AnalyticsRange.YEAR) {
                TextButton(onClick = { onPick(year, 0) }) { Text("Select") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        title = { Text(if (range == AnalyticsRange.YEAR) "Pick a year" else "Pick a month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                    }
                    Text(year.toString(), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { year++ }, enabled = year < thisYear) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }
                if (range != AnalyticsRange.YEAR) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(months) { month ->
                            val index = months.indexOf(month)
                            FilterChip(
                                selected = false,
                                onClick = { onPick(year, index) },
                                label = { Text(month) },
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}
