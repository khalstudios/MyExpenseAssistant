package com.khaltech.expenseassistant.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.billing.ProOffer
import com.khaltech.expenseassistant.billing.ProProduct

/**
 * The one place Pro is sold.
 *
 * Both products unlock the same thing, so the sheet leads with the one-time purchase and offers the
 * yearly plan underneath for people who would rather not pay it all at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    offers: List<ProOffer>,
    onBuy: (ProOffer) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "  Kahan Gaya Paisa Pro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "  $feature",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            val lifetime = offers.firstOrNull { it.product == ProProduct.LIFETIME }
            val yearly = offers.firstOrNull { it.product == ProProduct.YEARLY }

            if (lifetime == null && yearly == null) {
                // Play has not answered yet, or cannot be reached. Never show a made-up price.
                Text(
                    "Prices are still loading. Check your connection and try again in a moment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }

            lifetime?.let { offer ->
                Button(onClick = { onBuy(offer) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pay once · ${offer.formattedPrice}")
                }
                Text(
                    "One payment, yours for good. No renewals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            yearly?.let { offer ->
                OutlinedButton(onClick = { onBuy(offer) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Or ${offer.formattedPrice} a year")
                }
            }

            Text(
                "Your transactions stay on this phone either way. Pro only unlocks more of what the " +
                    "app can do with them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text("Already bought it? Restore")
            }
        }
    }
}
