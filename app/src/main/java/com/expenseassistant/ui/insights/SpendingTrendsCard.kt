package com.expenseassistant.ui.insights

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.category.color
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberSoftGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class TrendBucket(val label: String, val spendMinor: Long, val categorySpendMinor: Map<Category, Long>)

private data class TrendSeries(
    val color: Color,
    val valuesMinor: List<Long>,
    val finalLabel: String,
)

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
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
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
                val categorySeries = buckets.categoryTrendSeries()
                if (categorySeries.size > 1) {
                    TrendSectionHeader("Category momentum", "Top categories")
                    CategoryMomentumChart(categorySeries)
                    TrendAxisLabels(buckets)
                }
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
private fun CategoryMomentumChart(series: List<TrendSeries>) {
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val axis = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maximum = max(series.maxOfOrNull { it.valuesMinor.maxOrNull() ?: 0L } ?: 0L, 1L).toFloat()

    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val leftPadding = 42.dp.toPx()
        val rightPadding = 88.dp.toPx()
        val topPadding = 12.dp.toPx()
        val bottomPadding = 26.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11.sp.toPx()
            color = labelColor.toArgb()
        }

        repeat(4) { index ->
            val y = topPadding + chartHeight * index / 3f
            drawLine(
                color = grid,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
            val value = maximum * (1f - index / 3f)
            drawContext.canvas.nativeCanvas.drawText(
                formatMinor(value.toLong()),
                0f,
                y + 4.dp.toPx(),
                labelPaint,
            )
        }

        drawLine(axis, Offset(leftPadding, topPadding), Offset(leftPadding, topPadding + chartHeight), strokeWidth = 1.5.dp.toPx())
        drawLine(axis, Offset(leftPadding, topPadding + chartHeight), Offset(leftPadding + chartWidth, topPadding + chartHeight), strokeWidth = 1.5.dp.toPx())

        series.forEach { trend ->
            val path = Path()
            trend.valuesMinor.forEachIndexed { index, amount ->
                val x = leftPadding + chartWidth * index / (trend.valuesMinor.size - 1).coerceAtLeast(1)
                val y = topPadding + chartHeight * (1f - amount / maximum)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = trend.color.copy(alpha = 0.28f), style = Stroke(width = 7.dp.toPx()))
            drawPath(path = path, color = trend.color, style = Stroke(width = 3.dp.toPx()))
            trend.valuesMinor.forEachIndexed { index, amount ->
                val x = leftPadding + chartWidth * index / (trend.valuesMinor.size - 1).coerceAtLeast(1)
                val y = topPadding + chartHeight * (1f - amount / maximum)
                drawCircle(trend.color, radius = 3.2.dp.toPx(), center = Offset(x, y))
            }

            val lastValue = trend.valuesMinor.lastOrNull() ?: 0L
            val labelY = topPadding + chartHeight * (1f - lastValue / maximum)
            labelPaint.color = trend.color.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                trend.finalLabel,
                leftPadding + chartWidth + 10.dp.toPx(),
                labelY + 4.dp.toPx(),
                labelPaint,
            )
        }
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
        val periodTransactions = transactions
            .asSequence()
            .filter { it.direction == Direction.DEBIT }
            .filter { it.occurredAt >= bucketStart.timeInMillis && it.occurredAt < bucketEnd.timeInMillis }
            .toList()
        TrendBucket(
            label = labelFormat.format(bucketStart.time),
            spendMinor = periodTransactions.sumOf { it.amountMinor },
            categorySpendMinor = periodTransactions
                .groupBy { it.category }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amountMinor } },
        )
    }
}

private fun List<TrendBucket>.categoryTrendSeries(): List<TrendSeries> {
    val topCategories = flatMap { bucket -> bucket.categorySpendMinor.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, amounts) -> amounts.sum() }
        .toList()
        .sortedByDescending { it.second }
        .take(4)
        .map { it.first }

    if (topCategories.isEmpty()) return emptyList()

    val categorySeries = topCategories.map { category ->
        val cumulative = runningAmounts { bucket -> bucket.categorySpendMinor[category] ?: 0L }
        TrendSeries(
            color = category.color,
            valuesMinor = cumulative,
            finalLabel = "${category.displayName.take(10)} ${formatMinor(cumulative.last())}",
        )
    }
    val allCumulative = runningAmounts { it.spendMinor }
    return categorySeries + TrendSeries(
        color = Color(0xFF9E9E9E),
        valuesMinor = allCumulative,
        finalLabel = "All ${formatMinor(allCumulative.last())}",
    )
}

private fun List<TrendBucket>.runningAmounts(valueForBucket: (TrendBucket) -> Long): List<Long> {
    var total = 0L
    return map { bucket ->
        total += valueForBucket(bucket)
        total
    }
}