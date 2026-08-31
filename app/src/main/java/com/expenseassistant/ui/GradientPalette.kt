package com.expenseassistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Cards get a soft drop shadow so they lift off the background, unlike the flat reference. */
val CardElevation = 2.dp

val SpendColor = Color(0xFFEF6C6C)
val IncomeColor = Color(0xFF4DD0A7)

/** Muted gradient + matching text colours for hero cards (home summary, transaction header). */
data class HeroGradient(val brush: Brush, val onGradient: Color, val onGradientMuted: Color)

/** Mostly-solid pale green base holding for 80% of the card, then a short fade into pink; inverted in dark mode. */
@Composable
fun rememberHeroGradient(): HeroGradient {
    return if (isSystemInDarkTheme()) {
        val textColor = Color(0xFFF0E8EC)
        HeroGradient(
            brush = Brush.linearGradient(
                0f to Color(0xFF1C231E), 0.8f to Color(0xFF1C231E), 1f to Color(0xFF3A2430),
            ),
            onGradient = textColor,
            onGradientMuted = textColor.copy(alpha = 0.65f),
        )
    } else {
        val textColor = Color(0xFF33362F)
        HeroGradient(
            brush = Brush.linearGradient(
                0f to Color(0xFFF3FBF3), 0.8f to Color(0xFFF3FBF3), 1f to Color(0xFFF9DFEB),
            ),
            onGradient = textColor,
            onGradientMuted = textColor.copy(alpha = 0.65f),
        )
    }
}

/** Flat card fill; kept as a brush so every call site stays unchanged. */
@Composable
fun rememberSoftGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(listOf(surface, surface))
}
