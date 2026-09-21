package com.khaltech.expenseassistant.ui.insights

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.formatMinorWhole
import com.khaltech.expenseassistant.ui.rememberSoftGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** The period on screen, last period when comparing, and where this period is heading. */
private val CurrentColor = Color(0xFFE05555)
private val PreviousColor = Color(0xFF64B5F6)
private val ProjectionColor = Color(0xFFF4B400)

private enum class MoneyFlow(val label: String, val direction: Direction) {
    SPENDING("Spending", Direction.DEBIT),
    INCOME("Income", Direction.CREDIT),
}

/**
 * One period split into buckets — days for a week or month, months for a year — with the period
 * before it split the same way. [elapsed] is how many buckets have happened: all of them for a past
 * period, up to today for the current one.
 */
private class MomentumSeries(
    val current: List<Long>,
    val elapsed: Int,
    val previous: List<Long>,
    /** Axis labels, keyed by bucket number counted from 1. */
    val labels: List<Pair<Int, String>>,
)

@Composable
fun SpendingTrendsCard(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    var flow by rememberSaveable { mutableStateOf(MoneyFlow.SPENDING) }
    var compare by rememberSaveable { mutableStateOf(false) }
    val series = state.momentumSeries(flow.direction)
    val previousLabel = Periods.label(Periods.shift(state.selection, -1))
    val hasData = series.current.sum() > 0 || (compare && series.previous.sum() > 0)

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
                    text = "  Momentum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FlowToggle(selected = flow, onSelect = { flow = it })
            }

            if (!hasData) {
                Text(
                    text = if (flow == MoneyFlow.SPENDING) {
                        "Your spending trend will appear here once transactions are recorded."
                    } else {
                        "Money coming in will be charted here once some is recorded."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ChartTitle("Total")
                // Income arrives in lumps — a salary, a refund — so extending its pace to the end
                // of the period would predict nonsense. Only spending is projected.
                CumulativeChart(
                    series = series,
                    compare = compare,
                    project = flow == MoneyFlow.SPENDING && state.isCurrentPeriod,
                )
                ChartTitle(if (state.range == AnalyticsRange.YEAR) "Month-wise" else "Day-wise")
                BucketBars(series = series, compare = compare)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            CompareRow(previousLabel = previousLabel, checked = compare, onCheckedChange = { compare = it })
        }
    }
}

@Composable
private fun ChartTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun FlowToggle(selected: MoneyFlow, onSelect: (MoneyFlow) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A tint of the text colour rather than a surface colour, so the track shows on the card's soft
    // gradient in both light and dark themes, with the chosen option raised out of it.
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.onSurface.copy(alpha = 0.07f))
            .padding(4.dp),
    ) {
        MoneyFlow.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .then(if (isSelected) Modifier.shadow(3.dp, RoundedCornerShape(50)) else Modifier)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun CompareRow(previousLabel: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                append("Compare with ")
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = colors.onSurface)) {
                    append(previousLabel)
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Colours the canvases need, read in composition because a canvas cannot read the theme. */
private class ChartPalette(
    val grid: Color,
    val axis: Color,
    val label: Color,
    val markerFill: Color,
    val average: Color,
)

@Composable
private fun rememberChartPalette(): ChartPalette {
    val colors = MaterialTheme.colorScheme
    return ChartPalette(
        grid = colors.outlineVariant.copy(alpha = 0.7f),
        axis = colors.onSurfaceVariant.copy(alpha = 0.45f),
        label = colors.onSurfaceVariant,
        markerFill = colors.surface,
        average = colors.onSurface.copy(alpha = 0.75f),
    )
}

/**
 * Running totals: the line starts at nothing on the axis and each point is the total at the end of
 * that bucket. Last period, when compared, runs the whole way in blue; this one runs to today, and
 * for spending a dashed line carries today's pace on to the end of the period.
 */
@Composable
private fun CumulativeChart(series: MomentumSeries, compare: Boolean, project: Boolean) {
    val palette = rememberChartPalette()
    val slots = series.current.size
    val currentTotals = series.current.take(series.elapsed).runningFold(0L) { total, amount -> total + amount }
    val previousTotals = series.previous.runningFold(0L) { total, amount -> total + amount }
    val reached = currentTotals.last()
    val projected = if (project && series.elapsed < slots && reached > 0) reached * slots / series.elapsed else null
    val xSlots = if (compare) max(slots, series.previous.size) else slots
    val top = headroom(
        maxOf(reached, projected ?: 0L, if (compare) previousTotals.last() else 0L),
    )

    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val frame = drawFrame(top, xSlots, series.labels, labelOffset = 0f, palette = palette)
        if (compare) drawTotalsLine(frame, previousTotals, PreviousColor, palette.markerFill)
        projected?.let { end ->
            val from = Offset(frame.x(series.elapsed.toFloat()), frame.y(reached))
            val to = Offset(frame.x(slots.toFloat()), frame.y(end))
            drawLine(
                color = ProjectionColor,
                start = from,
                end = to,
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
            )
            drawMarker(to, ProjectionColor, palette.markerFill)
        }
        drawTotalsLine(frame, currentTotals, CurrentColor, palette.markerFill)
        // Where the line hands over to the projection, the point takes the projection's colour.
        if (projected != null) drawMarker(Offset(frame.x(series.elapsed.toFloat()), frame.y(reached)), ProjectionColor, palette.markerFill)
    }
}

/**
 * One bar per bucket. When comparing, last period's bar stands just right of this period's, and
 * each period gets its own dashed average: this one's labelled on the right, last one's on the left
 * in blue, so the two labels never sit on top of each other.
 */
@Composable
private fun BucketBars(series: MomentumSeries, compare: Boolean) {
    val palette = rememberChartPalette()
    val slots = if (compare) max(series.current.size, series.previous.size) else series.current.size
    val counted = series.current.take(series.elapsed)
    val currentAverage = counted.sum() / series.elapsed
    val previousAverage = if (series.previous.isEmpty()) 0L else series.previous.sum() / series.previous.size
    val top = headroom(
        maxOf(counted.maxOrNull() ?: 0L, if (compare) series.previous.maxOrNull() ?: 0L else 0L),
    )

    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        // Bars sit in the middle of their slot, so the labels shift half a slot to sit under them.
        val frame = drawFrame(top, slots, series.labels, labelOffset = -0.5f, palette = palette)
        val slotWidth = (frame.right - frame.left) / slots
        val barWidth = min(slotWidth * (if (compare) 0.3f else 0.45f), if (compare) 4.dp.toPx() else 6.dp.toPx())
        val gap = 1.dp.toPx()

        fun bar(index: Int, amount: Long, color: Color, shift: Float) {
            if (amount <= 0L) return
            val centre = frame.x(index + 0.5f) + shift
            val barTop = frame.y(amount)
            drawRoundRect(
                color = color,
                topLeft = Offset(centre - barWidth / 2f, barTop),
                size = Size(barWidth, frame.bottom - barTop),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }

        counted.forEachIndexed { index, amount ->
            bar(index, amount, CurrentColor, if (compare) -(barWidth / 2f + gap) else 0f)
        }
        if (compare) {
            series.previous.forEachIndexed { index, amount -> bar(index, amount, PreviousColor, barWidth / 2f + gap) }
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13.sp.toPx() }
        if (compare && previousAverage > 0) {
            drawAverage(frame, previousAverage, PreviousColor, labelPaint, alignRight = false)
        }
        if (currentAverage > 0) {
            drawAverage(frame, currentAverage, palette.average, labelPaint, alignRight = true)
        }
    }
}

/** Where the plotting area sits inside the canvas, and how to map values onto it. */
private class Frame(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    private val maxValue: Float,
    private val slots: Int,
) {
    fun x(position: Float): Float = left + (right - left) * position / slots
    fun y(value: Long): Float = bottom - (bottom - top) * (value / maxValue)
}

private const val GridLines = 5

/** Grid, axes and labels, drawn the same way under both charts. */
private fun DrawScope.drawFrame(
    maxValue: Float,
    slots: Int,
    labels: List<Pair<Int, String>>,
    labelOffset: Float,
    palette: ChartPalette,
): Frame {
    val yPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12.sp.toPx()
        color = palette.label.toArgb()
        textAlign = Paint.Align.RIGHT
    }
    val xPaint = Paint(yPaint).apply { textAlign = Paint.Align.CENTER }
    val yLabels = (1..GridLines).map { compactAmount((maxValue * it / GridLines).toLong()) }
    val left = (yLabels.maxOfOrNull { yPaint.measureText(it) } ?: 0f) + 12.dp.toPx()
    // Room on the right for half of the last date label, which is centred on the axis's end.
    val right = size.width - (labels.lastOrNull()?.let { xPaint.measureText(it.second) / 2f } ?: 0f) - 2.dp.toPx()
    val top = 8.dp.toPx()
    val bottom = size.height - 26.dp.toPx()
    val frame = Frame(left, right, top, bottom, maxValue, slots)

    (1..GridLines).forEach { index ->
        val y = bottom - (bottom - top) * index / GridLines
        drawLine(
            color = palette.grid,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
        drawContext.canvas.nativeCanvas.drawText(yLabels[index - 1], left - 8.dp.toPx(), y + 4.dp.toPx(), yPaint)
    }
    drawLine(palette.axis, Offset(left, top), Offset(left, bottom), strokeWidth = 1.2.dp.toPx())
    drawLine(palette.axis, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.2.dp.toPx())
    labels.forEach { (position, text) ->
        drawContext.canvas.nativeCanvas.drawText(
            text,
            frame.x(position + labelOffset),
            bottom + 20.dp.toPx(),
            xPaint,
        )
    }
    return frame
}

private fun DrawScope.drawTotalsLine(frame: Frame, totals: List<Long>, color: Color, markerFill: Color) {
    if (totals.size < 2) return
    val points = totals.mapIndexed { index, total -> Offset(frame.x(index.toFloat()), frame.y(total)) }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
    points.forEach { drawMarker(it, color, markerFill) }
}

/** A ringed point, hollow so the line reads through it. */
private fun DrawScope.drawMarker(centre: Offset, color: Color, fill: Color) {
    drawCircle(fill, radius = 4.dp.toPx(), center = centre)
    drawCircle(color, radius = 4.dp.toPx(), center = centre, style = Stroke(width = 1.8.dp.toPx()))
}

private fun DrawScope.drawAverage(frame: Frame, average: Long, color: Color, paint: Paint, alignRight: Boolean) {
    val y = frame.y(average)
    drawLine(
        color = color,
        start = Offset(frame.left, y),
        end = Offset(frame.right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
    )
    paint.color = color.toArgb()
    paint.textAlign = if (alignRight) Paint.Align.RIGHT else Paint.Align.LEFT
    drawContext.canvas.nativeCanvas.drawText(
        "Avg: ${formatMinorWhole(average)}",
        if (alignRight) frame.right else frame.left + 6.dp.toPx(),
        y - 6.dp.toPx(),
        paint,
    )
}

/** A little room above the highest point, so the line never runs along the top edge. */
private fun headroom(maxMinor: Long): Float = max(maxMinor, 100L) * 1.15f

/** "8.2K", "65K", "1.2M": short enough for an axis, trailing ".0" dropped. */
private fun compactAmount(amountMinor: Long): String {
    val rupees = amountMinor / 100.0
    fun short(value: Double, suffix: String) =
        String.format(Locale.US, "%.1f", value).removeSuffix(".0") + suffix
    return when {
        rupees >= 1_000_000 -> short(rupees / 1_000_000, "M")
        rupees >= 1_000 -> short(rupees / 1_000, "K")
        else -> String.format(Locale.US, "%.0f", rupees)
    }
}

private fun AnalyticsUiState.momentumSeries(direction: Direction): MomentumSeries {
    val current = bucketAmounts(selection, transactions, direction)
    val previous = bucketAmounts(Periods.shift(selection, -1), previousTransactions, direction)
    val elapsed = if (!isCurrentPeriod) {
        current.size
    } else when (range) {
        AnalyticsRange.YEAR -> Calendar.getInstance().get(Calendar.MONTH) + 1
        else -> Periods.elapsedDays(selection)
    }
    return MomentumSeries(
        current = current,
        elapsed = elapsed.coerceIn(1, current.size),
        previous = previous,
        labels = axisLabels(selection, current.size),
    )
}

private fun bucketCount(selection: PeriodSelection): Int = when (selection.range) {
    AnalyticsRange.WEEK -> 7
    AnalyticsRange.MONTH -> Periods.totalDays(selection)
    AnalyticsRange.YEAR -> 12
}

private fun bucketStart(selection: PeriodSelection, index: Int): Calendar =
    Calendar.getInstance().apply {
        timeInMillis = Periods.start(selection)
        when (selection.range) {
            AnalyticsRange.WEEK, AnalyticsRange.MONTH -> add(Calendar.DAY_OF_MONTH, index)
            AnalyticsRange.YEAR -> add(Calendar.MONTH, index)
        }
    }

private fun bucketAmounts(
    selection: PeriodSelection,
    transactions: List<TransactionEntity>,
    direction: Direction,
): List<Long> {
    val count = bucketCount(selection)
    val bounds = (0..count).map { bucketStart(selection, it).timeInMillis }
    val sums = LongArray(count)
    transactions.forEach { transaction ->
        if (transaction.direction != direction) return@forEach
        val index = bounds.indexOfLast { it <= transaction.occurredAt }
        if (index in 0 until count) sums[index] += transaction.amountMinor
    }
    return sums.toList()
}

/** Every fifth day for a month, every day for a week, every other month for a year. */
private fun axisLabels(selection: PeriodSelection, count: Int): List<Pair<Int, String>> {
    val (pattern, step) = when (selection.range) {
        AnalyticsRange.WEEK -> "EEE" to 1
        AnalyticsRange.MONTH -> "dd/MM" to 5
        AnalyticsRange.YEAR -> "MMM" to 2
    }
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return (step..count step step).map { position -> position to format.format(bucketStart(selection, position - 1).time) }
}
