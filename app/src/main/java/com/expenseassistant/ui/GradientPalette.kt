package com.expenseassistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Bold gradient + matching text colours for hero cards (home summary, transaction header). */
data class HeroGradient(val brush: Brush, val onGradient: Color, val onGradientMuted: Color)

@Composable
fun rememberHeroGradient(): HeroGradient {
    return if (isSystemInDarkTheme()) {
        // Unchanged from the palette that already reads well in dark mode.
        HeroGradient(
            brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
            onGradient = MaterialTheme.colorScheme.onPrimary,
            onGradientMuted = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
    } else {
        // A lighter, airier pair than the deep brand teal/coral so it doesn't look heavy in light mode.
        val textColor = Color(0xFF1B3A36)
        HeroGradient(
            brush = Brush.linearGradient(listOf(Color(0xFF4DB6AC), Color(0xFFFFAB91))),
            onGradient = textColor,
            onGradientMuted = textColor.copy(alpha = 0.75f),
        )
    }
}

/** Faint gradient for ordinary cards, just enough to look shinier than a flat fill. */
@Composable
fun rememberSoftGradient(): Brush {
    val dark = isSystemInDarkTheme()
    val start = MaterialTheme.colorScheme.surface
    val end = if (dark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.025f)
    }
    return Brush.linearGradient(listOf(start, end))
}
