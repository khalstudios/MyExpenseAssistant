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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expenseassistant.data.prefs.BackupInterval
import com.expenseassistant.service.PermissionStatus
import com.expenseassistant.service.ScreenCapture
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.DisclosureDialog
import com.expenseassistant.ui.Disclosures
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.rememberSoftGradient
import com.expenseassistant.ui.toMinorUnits
import kotlinx.coroutines.launch

/** The sensitive-access grants offered in the Capture section, each behind a disclosure. */
private enum class CaptureAccess { NOTIFICATIONS, SCREEN_READING, CONTACTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    onOpenBudgets: () -> Unit = {},
    onOpenNeedsReview: () -> Unit = {},
    openAutoBackupSetup: Boolean = false,
    onAutoBackupSetupHandled: () -> Unit = {},
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val count by viewModel.transactionCount.collectAsStateWithLifecycle()
    val earliest by viewModel.earliest.collectAsStateWithLifecycle()
    val autoBackupSettings by viewModel.autoBackupSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var editingProfile by remember { mutableStateOf(false) }
    var pendingDisclosure by remember { mutableStateOf<CaptureAccess?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var confirmingClear by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var configuringAutoBackup by remember { mutableStateOf(false) }
    var selectedBackupInterval by remember { mutableStateOf(BackupInterval.FIFTEEN_DAYS) }
    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }
    var accessibility by remember { mutableStateOf(PermissionStatus.isAccessibilityGranted(context)) }
    var contactsAccess by remember { mutableStateOf(PermissionStatus.isContactsAccessGranted(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsAccess = PermissionStatus.isContactsAccessGranted(context)
    }

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

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.backup(uri) { succeeded ->
            scope.launch { snackbarHostState.showMessage(if (succeeded) "Backup saved" else "Backup failed") }
        }
    }

    val automaticBackupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.enableAutoBackup(uri, selectedBackupInterval) { succeeded ->
            scope.launch {
                snackbarHostState.showMessage(
                    if (succeeded) "Automatic backups enabled" else "Could not access that folder"
                )
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> restoreUri = uri }

    // System settings can change while we are backgrounded, so re-read on every resume.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationAccess = PermissionStatus.isNotificationAccessGranted(context)
            accessibility = PermissionStatus.isAccessibilityGranted(context)
            contactsAccess = PermissionStatus.isContactsAccessGranted(context)
        }
    }

    // Arriving here from the home screen's backup notice opens the setup dialog straight away.
    LaunchedEffect(openAutoBackupSetup) {
        if (openAutoBackupSetup) {
            selectedBackupInterval = autoBackupSettings?.interval ?: BackupInterval.FIFTEEN_DAYS
            configuringAutoBackup = true
            onAutoBackupSetupHandled()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileHeader(profile) { editingProfile = true }

            SectionCard("Planning") {
                SettingRow(
                    icon = Icons.Filled.Savings,
                    title = "Monthly budgets",
                    subtitle = "Set limits overall and per category",
                    onClick = onOpenBudgets,
                )
                SettingRow(
                    icon = Icons.Filled.RateReview,
                    title = "Needs review",
                    subtitle = "Transactions we couldn't categorise confidently",
                    onClick = onOpenNeedsReview,
                )
            }

            SectionCard("Capture") {
                SettingRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notification access",
                    subtitle = if (notificationAccess) "Enabled" else "Disabled",
                    onClick = { pendingDisclosure = CaptureAccess.NOTIFICATIONS },
                )
                if (ScreenCapture.AVAILABLE) {
                    SettingRow(
                        icon = Icons.Filled.Accessibility,
                        title = "Screen reading",
                        subtitle = if (accessibility) "Enabled" else "Disabled",
                        onClick = { pendingDisclosure = CaptureAccess.SCREEN_READING },
                    )
                }
                SettingRow(
                    icon = Icons.Filled.Contacts,
                    title = "Contact names",
                    subtitle = if (contactsAccess) "Enabled" else "Match payments to your phone contacts",
                    onClick = { pendingDisclosure = CaptureAccess.CONTACTS },
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
                    icon = Icons.Filled.Backup,
                    title = "Back up your data",
                    subtitle = "Save transactions, budgets, and settings; choose Google Drive in the picker",
                    actionLabel = "Back up",
                    onClick = { backupLauncher.launch(viewModel.suggestedBackupFileName()) },
                )
                SettingRow(
                    icon = Icons.Filled.Backup,
                    title = "Automatic backups",
                    subtitle = autoBackupSettings?.let { "${it.interval.label}; backup folder selected" }
                        ?: "Save a backup every 15 days or monthly",
                    actionLabel = if (autoBackupSettings == null) "Set up" else "Change",
                    onClick = {
                        selectedBackupInterval = autoBackupSettings?.interval ?: BackupInterval.FIFTEEN_DAYS
                        configuringAutoBackup = true
                    },
                )
                if (autoBackupSettings != null) {
                    TextButton(onClick = viewModel::disableAutoBackup) {
                        Text("Turn off automatic backups")
                    }
                }
                SettingRow(
                    icon = Icons.Filled.Restore,
                    title = "Restore from backup",
                    subtitle = "Replace the data currently on this device",
                    actionLabel = "Restore",
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json")) },
                )
                SettingRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "Delete all transactions",
                    subtitle = "Cannot be undone",
                    onClick = { confirmingClear = true },
                )
                SettingRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy",
                    subtitle = "What is read, what is stored, and what never leaves this phone",
                    actionLabel = "Read",
                    onClick = { showPrivacy = true },
                )
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // Every sensitive access is disclosed in-app before the system grant screen is opened.
    pendingDisclosure?.let { access ->
        val disclosure = when (access) {
            CaptureAccess.NOTIFICATIONS -> Disclosures.Notifications
            CaptureAccess.SCREEN_READING -> Disclosures.ScreenReading
            CaptureAccess.CONTACTS -> Disclosures.Contacts
        }
        DisclosureDialog(
            disclosure = disclosure,
            onDismiss = { pendingDisclosure = null },
            onAccept = {
                pendingDisclosure = null
                when (access) {
                    CaptureAccess.NOTIFICATIONS ->
                        runCatching { context.startActivity(PermissionStatus.notificationAccessIntent()) }
                    CaptureAccess.SCREEN_READING ->
                        runCatching { context.startActivity(PermissionStatus.accessibilityIntent()) }
                    CaptureAccess.CONTACTS ->
                        contactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }
            },
        )
    }

    if (showPrivacy) {
        PrivacyDialog(onDismiss = { showPrivacy = false })
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

    restoreUri?.let { uri ->
        RestoreBackupDialog(
            onDismiss = { restoreUri = null },
            onConfirm = {
                restoreUri = null
                viewModel.restore(uri) { succeeded ->
                    scope.launch {
                        snackbarHostState.showMessage(
                            if (succeeded) "Backup restored" else "Restore failed: select an Expense Assistant backup"
                        )
                    }
                }
            },
        )
    }

    if (configuringAutoBackup) {
        AutoBackupDialog(
            selectedInterval = selectedBackupInterval,
            onIntervalSelected = { selectedBackupInterval = it },
            onDismiss = { configuringAutoBackup = false },
            onConfirm = {
                configuringAutoBackup = false
                automaticBackupFolderLauncher.launch(null)
            },
        )
    }
}

@Composable
private fun AutoBackupDialog(
    selectedInterval: BackupInterval,
    onIntervalSelected: (BackupInterval) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up automatic backups") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose how often to create a backup. You will select its folder next.")
                BackupInterval.entries.forEach { interval ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedInterval == interval,
                            onClick = { onIntervalSelected(interval) },
                        )
                        TextButton(onClick = { onIntervalSelected(interval) }) { Text(interval.label) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Choose folder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RestoreBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore this backup?") },
        text = { Text("This replaces your current transactions, budgets, learned categories, profile, and category icons.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
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
                .fillMaxWidth()
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
