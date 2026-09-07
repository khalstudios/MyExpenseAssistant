package com.expenseassistant.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.color
import com.expenseassistant.ui.formatMinor

data class PieSlice(
    val category: Category,
    val amountMinor: Long,
    val fraction: Float,
    val transactionCount: Int = 0,
)

@Composable
fun CategoryRankedBarChart(
    slices: List<PieSlice>,
    totalMinor: Long,
    onOpenCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topSlices = slices.take(8)
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
                text = "${(slice.fraction * 100).toInt()}%",
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

@Composable
fun CategoryPieChart(
    slices: List<PieSlice>,
    totalMinor: Long,
    modifier: Modifier = Modifier,
) {
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "pieSweep",
    )
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val thickness = size.minDimension * 0.16f
            val inset = thickness / 2f
            val arcSize = Size(size.width - thickness, size.height - thickness)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = thickness),
            )

            var startAngle = -90f
            slices.forEach { slice ->
                val fullSweep = slice.fraction * 360f
                val gap = minOf(4f, fullSweep * 0.18f)
                val sweep = (fullSweep - gap) * sweepProgress
                if (sweep > 0f) {
                    drawArc(
                        color = slice.category.color,
                        startAngle = startAngle + (gap / 2f),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = thickness, cap = StrokeCap.Round),
                    )
                }
                startAngle += fullSweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total spent", style = MaterialTheme.typography.labelMedium)
            Text(
                formatMinor(totalMinor),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${slices.size} ${if (slices.size == 1) "category" else "categories"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                        "${(slice.fraction * 100).toInt()}% \u00b7 ${slice.transactionCount} " +
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
