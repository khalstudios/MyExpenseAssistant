package com.expenseassistant.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expenseassistant.data.prefs.UserProfile
import com.expenseassistant.service.PermissionStatus
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.rememberSoftGradient
import com.expenseassistant.ui.toMinorUnits
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val count by viewModel.transactionCount.collectAsStateWithLifecycle()
    val earliest by viewModel.earliest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var editingProfile by remember { mutableStateOf(false) }
    var confirmingClear by remember { mutableStateOf(false) }
    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }
    var accessibility by remember { mutableStateOf(PermissionStatus.isAccessibilityGranted(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportCsv(uri) { exported ->
            scope.launch {
                snackbarHostState.showMessage(
                    if (exported >= 0) "Exported $exported transactions" else "Export failed"
                )
            }
        }
    }

    // System settings can change while we are backgrounded, so re-read on every resume.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationAccess = PermissionStatus.isNotificationAccessGranted(context)
            accessibility = PermissionStatus.isAccessibilityGranted(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileHeader(profile) { editingProfile = true }

            SectionCard("Capture") {
                SettingRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notification access",
                    subtitle = if (notificationAccess) "Enabled" else "Disabled",
                    onClick = { runCatching { context.startActivity(PermissionStatus.notificationAccessIntent()) } },
                )
                SettingRow(
                    icon = Icons.Filled.Accessibility,
                    title = "Screen reading",
                    subtitle = if (accessibility) "Enabled" else "Disabled",
                    onClick = { runCatching { context.startActivity(PermissionStatus.accessibilityIntent()) } },
                )
            }

            SectionCard("Planning") {
                SettingRow(
                    icon = Icons.Filled.Savings,
                    title = "Monthly budgets",
                    subtitle = "Set limits overall and per category",
                    onClick = onOpenBudgets,
                )
            }

            SectionCard("Your data") {
                InfoRow("Transactions recorded", count.toString())
                InfoRow("Tracking since", earliest?.let { formatTimestamp(it) } ?: "No data yet")
                InfoRow("Stored", "On this device only")
                SettingRow(
                    icon = Icons.Filled.Download,
                    title = "Export to CSV",
                    subtitle = "Save every transaction as a spreadsheet file",
                    actionLabel = "Export",
                    onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                )
                SettingRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "Delete all transactions",
                    subtitle = "Cannot be undone",
                    onClick = { confirmingClear = true },
                )
            }
        }
    }

    if (editingProfile) {
        ProfileDialog(
            profile = profile,
            onDismiss = { editingProfile = false },
            onSave = {
                viewModel.save(it)
                editingProfile = false
            },
        )
    }

    if (confirmingClear) {
        ClearDataDialog(
            onDismiss = { confirmingClear = false },
            onConfirm = { alsoResetSettings ->
                viewModel.clearAllTransactions(alsoResetSettings)
                confirmingClear = false
            },
        )
    }
}

@Composable
private fun ClearDataDialog(onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var alsoResetSettings by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete all transactions?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Every recorded transaction will be removed from this device. This cannot be undone.")
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(checked = alsoResetSettings, onCheckedChange = { alsoResetSettings = it })
                    Column(Modifier.weight(1f)) {
                        Text("Also reset budgets and learned categories")
                        Text(
                            "Clears your monthly limits and forgets every merchant you have re-categorised.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoResetSettings) }) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProfileHeader(profile: UserProfile, onEdit: () -> Unit) {
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            Modifier
                .background(hero.brush)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = hero.onGradient,
                    modifier = Modifier.size(32.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name.ifBlank { "Set up your profile" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = hero.onGradient,
                )
                if (profile.email.isNotBlank()) {
                    Text(profile.email, style = MaterialTheme.typography.bodySmall, color = hero.onGradientMuted)
                }
                if (profile.monthlyIncomeMinor > 0) {
                    Text(
                        "Monthly income ${formatMinor(profile.monthlyIncomeMinor)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = hero.onGradientMuted,
                    )
                }
            }
            TextButton(onClick = onEdit) { Text("Edit", color = hero.onGradient) }
        }
    }
}

@Composable
private fun ProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var income by remember {
        mutableStateOf(if (profile.monthlyIncomeMinor > 0) (profile.monthlyIncomeMinor / 100).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = income,
                    onValueChange = { input -> income = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monthly income") },
                    prefix = { Text("\u20b9 ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    UserProfile(
                        name = name.trim(),
                        email = email.trim(),
                        monthlyIncomeMinor = income.toMinorUnits(),
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String = "Open",
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) { Text(actionLabel) }
    }
}

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
