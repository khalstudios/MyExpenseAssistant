package com.khaltech.expenseassistant.ui.insights

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.formatMinorWhole
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.ui.pro.ProLocked
import com.khaltech.expenseassistant.ui.rememberSoftGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** The period on screen, and where this period is heading. */
private val CurrentColor = Color(0xFFE05555)
private val ProjectionColor = Color(0xFFF4B400)

/**
 * One colour per earlier period, most recent first. A period keeps its colour whichever others are
 * switched on, so August stays blue while July comes and goes. None of them is red or yellow, which
 * belong to this period and its projection.
 */
private val PastColors = listOf(
    Color(0xFF64B5F6),
    Color(0xFF81C784),
    Color(0xFFBA68C8),
    Color(0xFF4DB6AC),
    Color(0xFFA1887F),
    Color(0xFF90A4AE),
)

/** How many of the most recent earlier periods anyone can compare with; the rest are Pro. */
private const val FreeComparablePeriods = 1

/** An earlier period laid over this one: [offset] periods back, split into the same buckets. */
private class PastSeries(val offset: Int, val amounts: List<Long>) {
    val color: Color get() = PastColors[offset - 1]
}

/**
 * One period split into buckets — days for a week or month, months for a year — with any earlier
 * periods being compared split the same way. [elapsed] is how many buckets have happened: all of
 * them for a past period, up to today for the current one.
 */
private class MomentumSeries(
    val current: List<Long>,
    val elapsed: Int,
    /** Most recent first. */
    val past: List<PastSeries>,
    /** Axis labels, keyed by bucket number counted from 1. */
    val labels: List<Pair<Int, String>>,
) {
    /** Buckets across the chart: enough for the longest period on it, so 31 when a 31-day month is compared. */
    val slots: Int get() = max(current.size, past.maxOfOrNull { it.amounts.size } ?: 0)
}

@Composable
fun SpendingTrendsCard(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    val pro = LocalPro.current
    // Bit k-1 set means the period k back is on the chart. A bitmask saves without a custom Saver,
    // and an offset means the same thing in any range, so the choice survives switching ranges.
    var shownMask by rememberSaveable { mutableIntStateOf(0) }
    fun isShown(offset: Int) = shownMask and (1 shl (offset - 1)) != 0
    // Last period is free; the older ones need Pro. Choices made under Pro stay remembered but
    // stop drawing if it lapses.
    val shown = (1..Periods.ComparablePeriods).filter { isShown(it) && (it <= FreeComparablePeriods || pro.isPro) }
    val series = state.momentumSeries(Direction.DEBIT, shown)
    val hasData = series.current.sum() > 0 || series.past.any { it.amounts.sum() > 0 }

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

            if (!hasData) {
                Text(
                    text = "Your spending trend will appear here once transactions are recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ChartTitle("Total")
                CumulativeChart(series = series, project = state.isCurrentPeriod)
                ChartTitle(if (state.range == AnalyticsRange.YEAR) "Month-wise" else "Day-wise")
                BucketBars(series = series)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            val onToggle = { offset: Int -> shownMask = shownMask xor (1 shl (offset - 1)) }
            Column {
                CompareHeader()
                (1..FreeComparablePeriods).forEach { offset ->
                    CompareToggle(state.selection, offset, checked = isShown(offset), onToggle = onToggle)
                }
                ProLocked(
                    title = "Compare with earlier ${state.range.label.lowercase()}s",
                    subtitle = "Lay up to six of them over this one and see how your spending pace compares.",
                ) {
                    Column {
                        (FreeComparablePeriods + 1..Periods.ComparablePeriods).forEach { offset ->
                            CompareToggle(state.selection, offset, checked = isShown(offset), onToggle = onToggle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun CompareHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("Compare with", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** A switch for the period [offset] back, with the colour its line takes, so the rows double as the legend. */
@Composable
private fun CompareToggle(selection: PeriodSelection, offset: Int, checked: Boolean, onToggle: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle(offset) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(PastColors[offset - 1]),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            Periods.label(Periods.shift(selection, -offset)),
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) colors.onSurface else colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        // The row carries the toggle, so the switch itself stays out of the way of TalkBack.
        Switch(checked = checked, onCheckedChange = null)
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
 * that bucket. Each earlier period being compared runs the whole way in its own colour; this one
 * runs to today, and a dashed line carries today's pace on to the end of the period.
 */
@Composable
private fun CumulativeChart(series: MomentumSeries, project: Boolean) {
    val palette = rememberChartPalette()
    val slots = series.current.size
    val currentTotals = series.current.take(series.elapsed).runningFold(0L) { total, amount -> total + amount }
    val pastTotals = series.past.map { past -> past to past.amounts.runningFold(0L) { total, amount -> total + amount } }
    val reached = currentTotals.last()
    val projected = if (project && series.elapsed < slots && reached > 0) reached * slots / series.elapsed else null
    val top = headroom(
        maxOf(reached, projected ?: 0L, pastTotals.maxOfOrNull { it.second.last() } ?: 0L),
    )

    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val frame = drawFrame(top, series.slots, series.labels, labelOffset = 0f, palette = palette)
        // Oldest first, so the more recent periods draw over them.
        pastTotals.asReversed().forEach { (past, totals) -> drawTotalsLine(frame, totals, past.color, palette.markerFill) }
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
 * One bar per bucket, with each compared period's bar standing beside this period's in its own
 * colour. Every period gets its own dashed average. This one's is labelled on the right; an earlier
 * one's is labelled on the left only when it is the only one on, because six labels would pile up.
 */
@Composable
private fun BucketBars(series: MomentumSeries) {
    val palette = rememberChartPalette()
    val slots = series.slots
    val counted = series.current.take(series.elapsed)
    val currentAverage = counted.sum() / series.elapsed
    val top = headroom(
        maxOf(counted.maxOrNull() ?: 0L, series.past.maxOfOrNull { it.amounts.maxOrNull() ?: 0L } ?: 0L),
    )
    val bars = 1 + series.past.size

    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        // Bars sit in the middle of their slot, so the labels shift half a slot to sit under them.
        val frame = drawFrame(top, slots, series.labels, labelOffset = -0.5f, palette = palette)
        val slotWidth = (frame.right - frame.left) / slots
        val gap = 1.dp.toPx()
        val barWidth = if (bars == 1) {
            min(slotWidth * 0.45f, 6.dp.toPx())
        } else {
            min(slotWidth * 0.8f / bars - gap, 4.dp.toPx()).coerceAtLeast(1.dp.toPx())
        }

        // position runs 0 (this period) to bars - 1 (the oldest compared), centred on the slot.
        fun bar(index: Int, amount: Long, color: Color, position: Int) {
            if (amount <= 0L) return
            val centre = frame.x(index + 0.5f) + (position - (bars - 1) / 2f) * (barWidth + gap)
            val barTop = frame.y(amount)
            drawRoundRect(
                color = color,
                topLeft = Offset(centre - barWidth / 2f, barTop),
                size = Size(barWidth, frame.bottom - barTop),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }

        counted.forEachIndexed { index, amount -> bar(index, amount, CurrentColor, 0) }
        series.past.forEachIndexed { position, past ->
            past.amounts.forEachIndexed { index, amount -> bar(index, amount, past.color, position + 1) }
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13.sp.toPx() }
        series.past.forEach { past ->
            val average = if (past.amounts.isEmpty()) 0L else past.amounts.sum() / past.amounts.size
            if (average > 0) {
                drawAverage(frame, average, past.color, labelPaint, alignRight = false, labelled = series.past.size == 1)
            }
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

private fun DrawScope.drawAverage(
    frame: Frame,
    average: Long,
    color: Color,
    paint: Paint,
    alignRight: Boolean,
    labelled: Boolean = true,
) {
    val y = frame.y(average)
    drawLine(
        color = color,
        start = Offset(frame.left, y),
        end = Offset(frame.right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
    )
    if (!labelled) return
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

/** [shown] are the earlier periods to lay over this one, as offsets back from it. */
private fun AnalyticsUiState.momentumSeries(direction: Direction, shown: List<Int>): MomentumSeries {
    val current = bucketAmounts(selection, transactions, direction)
    val past = shown.sorted().map { offset ->
        PastSeries(offset, bucketAmounts(Periods.shift(selection, -offset), previousTransactions, direction))
    }
    val elapsed = if (!isCurrentPeriod) {
        current.size
    } else when (range) {
        AnalyticsRange.YEAR -> Calendar.getInstance().get(Calendar.MONTH) + 1
        else -> Periods.elapsedDays(selection)
    }
    return MomentumSeries(
        current = current,
        elapsed = elapsed.coerceIn(1, current.size),
        past = past,
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
