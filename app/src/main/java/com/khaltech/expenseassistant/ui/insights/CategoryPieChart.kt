package com.khaltech.expenseassistant.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.color
import com.khaltech.expenseassistant.ui.formatMinor
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class PieSlice(
    val category: Category,
    val amountMinor: Long,
    val fraction: Float,
    val transactionCount: Int = 0,
)

/** How many bars the ranked chart draws; the detailed breakdown under it lists every category. */
const val RankedBarCount = 8

@Composable
fun CategoryRankedBarChart(
    slices: List<PieSlice>,
    totalMinor: Long,
    onOpenCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topSlices = slices.take(RankedBarCount)
    val maxFraction = topSlices.maxOfOrNull { it.fraction }?.coerceAtLeast(0.01f) ?: 1f

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Total spent", style = MaterialTheme.typography.labelMedium)
                Text(
                    formatMinor(totalMinor),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "Share of spending",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            topSlices.forEach { slice ->
                RankedCategoryBar(slice = slice, maxFraction = maxFraction, onOpenCategory = onOpenCategory)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 118.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("0", "25", "50", "75", "100%").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RankedCategoryBar(slice: PieSlice, maxFraction: Float, onOpenCategory: (Category) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenCategory(slice.category) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = slice.category.displayName,
            modifier = Modifier.widthIn(min = 108.dp, max = 118.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(slice.category.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth((slice.fraction / maxFraction).coerceIn(0.04f, 1f))
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(slice.category.color.copy(alpha = 0.86f)),
            )
            Text(
                text = "${(slice.fraction * 100).roundToInt()}%",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = formatMinor(slice.amountMinor),
            modifier = Modifier.widthIn(min = 58.dp, max = 76.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Thick donut starting at twelve o'clock, with slices divided by thin lines in the card colour and
 * each slice's share printed inside it when there is room.
 */
@Composable
fun CategoryPieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    separatorColor: Color = MaterialTheme.colorScheme.surface,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.titleSmall.copy(
        color = Color.White,
        fontWeight = FontWeight.Medium,
    )
    val drawn = slices.filter { it.fraction > 0f }
    // Read out here: the user's colour choices come from composition, which the canvas can't see.
    val sliceColors = drawn.map { it.category.color }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                contentDescription = drawn.joinToString(prefix = "Spending by category: ") {
                    "${it.category.displayName} ${percentLabel(it.fraction)}"
                }
            }
    ) {
        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius * DONUT_HOLE_RATIO
        val thickness = outerRadius - innerRadius
        val ringRadius = innerRadius + thickness / 2f

        // Filled wedges with the hole punched out afterwards; a thick stroked arc leaves a visible
        // seam where the renderer joins its segments.
        var startAngle = -90f
        drawn.forEachIndexed { index, slice ->
            val sweep = slice.fraction * 360f
            drawArc(
                color = sliceColors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )
            startAngle += sweep
        }

        // Straight dividers keep an even width however thin the slice, unlike angular gaps.
        if (drawn.size > 1) {
            val dividerWidth = 2.dp.toPx()
            startAngle = -90f
            drawn.forEach { slice ->
                val direction = unitVector(startAngle)
                drawLine(
                    color = separatorColor,
                    start = center,
                    end = center + direction * (outerRadius + 1f),
                    strokeWidth = dividerWidth,
                )
                startAngle += slice.fraction * 360f
            }
        }
        drawCircle(color = separatorColor, radius = innerRadius, center = center)

        startAngle = -90f
        drawn.forEach { slice ->
            val sweep = slice.fraction * 360f
            if (slice.fraction >= MIN_LABEL_FRACTION) {
                val layout = textMeasurer.measure(percentLabel(slice.fraction), labelStyle)
                // Only label a slice whose ring segment is wide enough to hold the text.
                val arcLength = ringRadius * Math.toRadians(sweep.toDouble()).toFloat()
                if (arcLength > layout.size.width + 8.dp.toPx() && thickness > layout.size.height) {
                    val labelCenter = center + unitVector(startAngle + sweep / 2f) * ringRadius
                    drawText(
                        textLayoutResult = layout,
                        topLeft = labelCenter - Offset(layout.size.width / 2f, layout.size.height / 2f),
                    )
                }
            }
            startAngle += sweep
        }
    }
}

private const val DONUT_HOLE_RATIO = 0.5f
private const val MIN_LABEL_FRACTION = 0.05f

private fun percentLabel(fraction: Float): String =
    String.format(Locale.getDefault(), "%.1f%%", fraction * 100f)

private fun unitVector(angleDegrees: Float): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    return Offset(cos(radians).toFloat(), sin(radians).toFloat())
}

@Composable
fun CategorySpendList(
    slices: List<PieSlice>,
    onClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        slices.forEach { slice ->
            Row(
                Modifier.fillMaxWidth().clickable { onClick(slice.category) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryBadge(slice.category, size = 38.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(slice.category.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatMinor(slice.amountMinor),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { slice.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = slice.category.color,
                        trackColor = slice.category.color.copy(alpha = 0.15f),
                    )
                    Text(
                        "${(slice.fraction * 100).roundToInt()}% \u00b7 ${slice.transactionCount} " +
                            if (slice.transactionCount == 1) "transaction" else "transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No spending recorded this month yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
