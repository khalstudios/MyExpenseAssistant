package com.khaltech.expenseassistant.ui.pro

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shows [content] behind a soft lock when the user does not have Pro.
 *
 * The content is deliberately still there and still recognisable. Someone deciding whether to pay
 * should be able to see that there is real data underneath, not an empty placeholder, so the teaser
 * fades and blurs rather than hides. Tapping anywhere on the locked area opens the paywall.
 */
@Composable
fun ProLocked(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pro = LocalPro.current
    if (pro.isPro) {
        content()
        return
    }

    Box(modifier) {
        Box(
            Modifier
                .teaserBlur()
                .alpha(0.45f)
                // Swallows every gesture so nothing underneath can be tapped, scrolled or
                // read out by TalkBack while it is locked.
                .pointerInput(Unit) { detectTapGestures { pro.onUpgrade() } },
        ) {
            content()
        }
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures { pro.onUpgrade() } },
            contentAlignment = Alignment.Center,
        ) {
            UnlockPrompt(title, subtitle, pro.onUpgrade)
        }
    }
}

/**
 * Blur needs a RenderEffect, which arrives in Android 12. Below that the fade alone carries it —
 * calling [blur] there is a silent no-op, so the branch keeps the intent visible in the code.
 */
private fun Modifier.teaserBlur(): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blur(8.dp) else this

@Composable
private fun UnlockPrompt(title: String, subtitle: String, onUpgrade: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.padding(24.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "  $title",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUpgrade) { Text("Unlock Pro") }
        }
    }
}

/** A small "PRO" marker for rows and headers that are locked but not covered. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
