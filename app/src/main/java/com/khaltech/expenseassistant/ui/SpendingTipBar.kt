package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TipBackground = Color.White
private val TipText = Color(0xFF20242C)
private val TipMutedText = Color(0xFF6B7280)
private val TipAccent = Color(0xFF0E7C6B)

/**
 * A brief note about today, shown at the top of the home feed on opening the app. It closes itself
 * after a few seconds; the cross dismisses it immediately.
 */
@Composable
fun SpendingTipBar(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    // Fixed light colours rather than the theme's: the tip sits on a dimming scrim, so it has to
    // read as lit in dark mode too.
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = TipBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = TipAccent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = TipText,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = TipMutedText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
