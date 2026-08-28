package com.expenseassistant.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
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

    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val thickness = size.minDimension * 0.18f
            val inset = thickness / 2f
            val arcSize = Size(size.width - thickness, size.height - thickness)
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = slice.fraction * 360f * sweepProgress
                drawArc(
                    color = slice.category.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = thickness),
                )
                startAngle += slice.fraction * 360f
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total spent", style = MaterialTheme.typography.labelSmall)
            Text(
                formatMinor(totalMinor),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
