package com.khaltech.expenseassistant.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.repo.MerchantSummary
import com.khaltech.expenseassistant.ui.category.color
import com.khaltech.expenseassistant.ui.category.resolvedIcon
import com.khaltech.expenseassistant.ui.formatMinor

/**
 * Every merchant the user has paid, most-paid first, reached from Profile. Tapping one opens every
 * payment made to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantsScreen(
    merchants: List<MerchantSummary>,
    onBack: () -> Unit,
    onOpenMerchant: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(merchants, query) {
        val needle = query.trim()
        if (needle.isEmpty()) merchants
        else merchants.filter { it.name.contains(needle, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merchants") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (merchants.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No merchants yet. Everyone you pay will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search merchants") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Tap a merchant to see every payment made to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        "No merchant matches “${query.trim()}”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shown, key = { it.key }) { merchant ->
                MerchantRow(merchant, onClick = { onOpenMerchant(merchant.key) })
            }
        }
    }
}

@Composable
private fun MerchantRow(merchant: MerchantSummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MerchantAvatar(merchant)
        Column(Modifier.weight(1f)) {
            Text(merchant.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val payments = if (merchant.paymentCount == 1) "1 payment" else "${merchant.paymentCount} payments"
            Text(
                "$payments · ${formatMinor(merchant.spentMinor)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open ${merchant.name}",
            tint = colors.onSurfaceVariant,
        )
    }
}

/** The icon and colour of the category its latest payment is filed under. */
@Composable
private fun MerchantAvatar(merchant: MerchantSummary) {
    val tint = merchant.category.color
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(merchant.category.resolvedIcon(), contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}
