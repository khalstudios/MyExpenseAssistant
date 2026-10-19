package com.khaltech.expenseassistant.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * A car and a house, each with a spanner badge in the corner: the maintenance categories.
 *
 * Built from the Material shapes rather than drawn from scratch, so they sit in the same visual
 * family as every other category icon. The badge is cut out of the base shape rather than drawn on
 * top of it, because an icon is tinted a single colour and an overlapping spanner would merge into it.
 */
object RepairIcons {
    val Vehicle: ImageVector by lazy { withSpanner("VehicleMaintenance", Icons.Filled.DirectionsCar) }
    val House: ImageVector by lazy { withSpanner("HouseMaintenance", Icons.Filled.Home) }

    // The whole viewport clockwise, then the badge circle anticlockwise, so the non-zero clip
    // keeps everything except the circle.
    private const val BADGE_CLIP =
        "M0,0H24V24H0Z M18,11a7,7 0 1,0 0,14a7,7 0 1,0 0,-14Z"

    // The spanner shrunk to fit inside the badge with a clear gap from the cut edge.
    private const val SPANNER_SCALE = 0.4f
    private const val SPANNER_OFFSET = 18f - 12f * SPANNER_SCALE

    private fun withSpanner(name: String, base: ImageVector): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            group(clipPathData = addPathNodes(BADGE_CLIP)) { addPaths(base.root) }
            group(
                scaleX = SPANNER_SCALE,
                scaleY = SPANNER_SCALE,
                translationX = SPANNER_OFFSET,
                translationY = SPANNER_OFFSET,
            ) { addPaths(Icons.Filled.Build.root) }
        }.build()

    private fun ImageVector.Builder.addPaths(group: VectorGroup) {
        group.forEach { node ->
            when (node) {
                is VectorPath -> addPath(pathData = node.pathData, pathFillType = node.pathFillType, fill = SolidColor(Color.Black))
                is VectorGroup -> addPaths(node)
            }
        }
    }
}
