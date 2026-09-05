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

/** Soft illuminated gradient + matching text colours for hero cards. */
data class HeroGradient(val brush: Brush, val onGradient: Color, val onGradientMuted: Color)

/** Cool graphite in dark mode and clean blue-white in light mode, with restrained teal and rose light. */
@Composable
fun rememberHeroGradient(): HeroGradient {
    return if (isSystemInDarkTheme()) {
        val textColor = Color(0xFFF3F4F6)
        HeroGradient(
            brush = Brush.linearGradient(
                0f to Color(0xFF202126),
                0.42f to Color(0xFF202629),
                1f to Color(0xFF332931),
            ),
            onGradient = textColor,
            onGradientMuted = textColor.copy(alpha = 0.62f),
        )
    } else {
        val textColor = Color(0xFF20242C)
        HeroGradient(
            brush = Brush.linearGradient(
                0f to Color(0xFFFFFFFF),
                0.42f to Color(0xFFF9FCFC),
                1f to Color(0xFFFFF4F6),
            ),
            onGradient = textColor,
            onGradientMuted = textColor.copy(alpha = 0.62f),
        )
    }
}

/** Flat card fill; kept as a brush so every call site stays unchanged. */
@Composable
fun rememberSoftGradient(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(listOf(surface, surface))
}
