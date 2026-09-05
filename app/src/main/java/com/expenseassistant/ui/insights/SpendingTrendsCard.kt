package com.expenseassistant.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Direction
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberSoftGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class TrendBucket(val label: String, val spendMinor: Long)

@Composable
fun SpendingTrendsCard(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    val buckets = state.spendingTrendBuckets()
    val totalSpend = buckets.sumOf { it.spendMinor }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "  Spending momentum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (totalSpend == 0L) {
                Text(
                    text = "Your spending trend will appear here once transactions are recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TrendSectionHeader("Cumulative spend", formatMinor(totalSpend))
                CumulativeSpendChart(buckets)
                TrendAxisLabels(buckets)
                TrendSectionHeader("${state.range.label}-wise spending", "Avg. ${formatMinor(totalSpend / buckets.size)}")
                PeriodSpendBars(buckets)
                TrendAxisLabels(buckets)
            }
        }
    }
}

@Composable
private fun TrendSectionHeader(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CumulativeSpendChart(buckets: List<TrendBucket>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val fill = primary.copy(alpha = 0.12f)
    val cumulative = buckets.runningFold(0L) { total, bucket -> total + bucket.spendMinor }.drop(1)
    val maximum = max(cumulative.maxOrNull() ?: 0L, 1L).toFloat()

    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val horizontalPadding = 6.dp.toPx()
        val verticalPadding = 10.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        repeat(4) { index ->
            val y = verticalPadding + chartHeight * index / 3f
            drawLine(
                color = grid,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
        }

        val linePath = Path()
        cumulative.forEachIndexed { index, amount ->
            val x = horizontalPadding + chartWidth * index / (cumulative.size - 1).coerceAtLeast(1)
            val y = verticalPadding + chartHeight * (1f - amount / maximum)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(size.width - horizontalPadding, size.height - verticalPadding)
            lineTo(horizontalPadding, size.height - verticalPadding)
            close()
        }
        drawPath(fillPath, fill)
        drawPath(linePath, primary, style = Stroke(width = 3.dp.toPx()))
        cumulative.forEachIndexed { index, amount ->
            val x = horizontalPadding + chartWidth * index / (cumulative.size - 1).coerceAtLeast(1)
            val y = verticalPadding + chartHeight * (1f - amount / maximum)
            drawCircle(primary, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun PeriodSpendBars(buckets: List<TrendBucket>) {
    val primary = MaterialTheme.colorScheme.primary
    val averageColor = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val maximum = max(buckets.maxOfOrNull { it.spendMinor } ?: 0L, 1L).toFloat()
    val average = buckets.map { it.spendMinor }.average().toFloat()

    Canvas(Modifier.fillMaxWidth().height(126.dp)) {
        val horizontalPadding = 6.dp.toPx()
        val verticalPadding = 10.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        repeat(3) { index ->
            val y = verticalPadding + chartHeight * index / 2f
            drawLine(
                color = grid,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
        }
        val averageY = verticalPadding + chartHeight * (1f - average / maximum)
        drawLine(
            color = averageColor,
            start = Offset(horizontalPadding, averageY),
            end = Offset(size.width - horizontalPadding, averageY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        )

        val slotWidth = chartWidth / buckets.size
        val barWidth = (slotWidth * 0.58f).coerceAtMost(18.dp.toPx())
        buckets.forEachIndexed { index, bucket ->
            val barHeight = chartHeight * bucket.spendMinor / maximum
            drawRoundRect(
                color = primary.copy(alpha = if (bucket.spendMinor == 0L) 0.18f else 0.82f),
                topLeft = Offset(horizontalPadding + slotWidth * index + (slotWidth - barWidth) / 2f, verticalPadding + chartHeight - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(if (bucket.spendMinor == 0L) 2.dp.toPx() else 0f)),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun TrendAxisLabels(buckets: List<TrendBucket>) {
    val middle = buckets[buckets.lastIndex / 2].label
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(buckets.first().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(middle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(buckets.last().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun AnalyticsUiState.spendingTrendBuckets(): List<TrendBucket> {
    val start = Calendar.getInstance().apply { timeInMillis = Periods.start(selection) }
    val bucketCount = when (range) {
        AnalyticsRange.WEEK -> 7
        AnalyticsRange.MONTH -> Periods.totalDays(selection)
        AnalyticsRange.YEAR -> 12
    }
    val labelFormat = SimpleDateFormat(
        when (range) {
            AnalyticsRange.WEEK -> "EEE"
            AnalyticsRange.MONTH -> "d"
            AnalyticsRange.YEAR -> "MMM"
        },
        Locale.getDefault(),
    )
    return List(bucketCount) { index ->
        val bucketStart = (start.clone() as Calendar).apply {
            when (range) {
                AnalyticsRange.WEEK, AnalyticsRange.MONTH -> add(Calendar.DAY_OF_MONTH, index)
                AnalyticsRange.YEAR -> add(Calendar.MONTH, index)
            }
        }
        val bucketEnd = (bucketStart.clone() as Calendar).apply {
            when (range) {
                AnalyticsRange.WEEK, AnalyticsRange.MONTH -> add(Calendar.DAY_OF_MONTH, 1)
                AnalyticsRange.YEAR -> add(Calendar.MONTH, 1)
            }
        }
        TrendBucket(
            label = labelFormat.format(bucketStart.time),
            spendMinor = transactions
                .asSequence()
                .filter { it.direction == Direction.DEBIT }
                .filter { it.occurredAt >= bucketStart.timeInMillis && it.occurredAt < bucketEnd.timeInMillis }
                .sumOf { it.amountMinor },
        )
    }
}