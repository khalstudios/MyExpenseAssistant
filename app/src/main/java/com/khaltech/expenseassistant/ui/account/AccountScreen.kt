package com.khaltech.expenseassistant.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
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
import com.khaltech.expenseassistant.BuildConfig
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.data.prefs.UserProfile
import com.khaltech.expenseassistant.data.backup.BackupFolder
import com.khaltech.expenseassistant.data.prefs.BackupInterval
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.service.PermissionStatus
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.DisclosureDialog
import com.khaltech.expenseassistant.ui.Disclosures
import com.khaltech.expenseassistant.ui.formatMinor
import com.khaltech.expenseassistant.ui.formatTimestamp
import java.util.Locale
import com.khaltech.expenseassistant.ui.rememberHeroGradient
import com.khaltech.expenseassistant.ui.rememberSoftGradient
import com.khaltech.expenseassistant.ui.toMinorUnits
import kotlinx.coroutines.launch

/** The sensitive-access grants offered in the Capture section, each behind a disclosure. */
private enum class CaptureAccess { NOTIFICATIONS, CONTACTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    onOpenBudgets: () -> Unit = {},
    onOpenNeedsReview: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    openAutoBackupSetup: Boolean = false,
    onAutoBackupSetupHandled: () -> Unit = {},
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val count by viewModel.transactionCount.collectAsStateWithLifecycle()
    val earliest by viewModel.earliest.collectAsStateWithLifecycle()
    val autoBackupSettings by viewModel.autoBackupSettings.collectAsStateWithLifecycle()
    val pro = LocalPro.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var editingProfile by remember { mutableStateOf(false) }
    var pendingDisclosure by remember { mutableStateOf<CaptureAccess?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var confirmingClear by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var configuringAutoBackup by remember { mutableStateOf(false) }
    var confirmingBackup by remember { mutableStateOf(false) }
    var choosingBackup by remember { mutableStateOf(false) }
    var selectedBackupInterval by remember { mutableStateOf(BackupInterval.WEEKLY) }
    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }
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

    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val backupLocation by viewModel.backupLocation.collectAsStateWithLifecycle()

    fun announceBackup(succeeded: Boolean) {
        scope.launch {
            snackbarHostState.showMessage(
                if (succeeded) "Backup saved to the ${BackupFolder.NAME} folder" else "Backup failed"
            )
        }
    }

    val backupLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.backupToFolder(uri, ::announceBackup)
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
            contactsAccess = PermissionStatus.isContactsAccessGranted(context)
            viewModel.refreshBackups()
        }
    }

    // Arriving here from the home screen's backup notice opens the setup dialog straight away —
    // or the paywall, the same as the Automatic backups row, if this install is not Pro.
    LaunchedEffect(openAutoBackupSetup) {
        if (openAutoBackupSetup) {
            if (pro.isPro) {
                selectedBackupInterval = autoBackupSettings?.interval ?: BackupInterval.WEEKLY
                configuringAutoBackup = true
            } else {
                pro.onUpgrade()
            }
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
            // Without this the paywall is only reachable by walking into a locked feature, which
            // leaves someone who already decided to buy with nowhere to do it. First on the screen
            // so it is seen rather than scrolled past.
            ProNotice(isPro = pro.isPro, onUpgrade = pro.onUpgrade)

            ProfileHeader(profile) { editingProfile = true }

            // Backups get their own card rather than sitting at the bottom of "Your data". This is
            // the only copy of the user's records, so how to make one and how to bring one back
            // should be findable without reading past the app version number.
            SectionCard("Backup") {
                SettingRow(
                    icon = Icons.Filled.Backup,
                    title = "Back up your data",
                    subtitle = "Save transactions, budgets, and settings to a folder you choose",
                    actionLabel = "Back up",
                    onClick = { confirmingBackup = true },
                )
                // Backing up and restoring by hand stay free: this is the user's own data and
                // losing it must never be the price of not paying. Pro buys not having to remember.
                SettingRow(
                    icon = Icons.Filled.Backup,
                    title = "Automatic backups",
                    subtitle = when {
                        !pro.isPro -> "Pro · Back up on a schedule, without remembering to"
                        autoBackupSettings != null ->
                            "${autoBackupSettings!!.interval.label}; saving to the ${BackupFolder.NAME} folder"
                        else -> "Save a backup daily, weekly, every 2 weeks, or monthly"
                    },
                    actionLabel = when {
                        !pro.isPro -> "Unlock"
                        autoBackupSettings == null -> "Set up"
                        else -> "Change"
                    },
                    onClick = {
                        if (!pro.isPro) {
                            pro.onUpgrade()
                        } else {
                            selectedBackupInterval = autoBackupSettings?.interval ?: BackupInterval.WEEKLY
                            configuringAutoBackup = true
                        }
                    },
                )
                // Only offered to someone who could turn them back on. Without Pro the schedule
                // has already been stopped, so this would be a switch for something not running.
                if (pro.isPro && autoBackupSettings != null) {
                    TextButton(onClick = viewModel::disableAutoBackup) {
                        Text("Turn off automatic backups")
                    }
                }
                SettingRow(
                    icon = Icons.Filled.Restore,
                    title = "Restore from backup",
                    subtitle = if (backups.isEmpty()) "Replace the data currently on this device"
                    else "${backups.size} backup${if (backups.size == 1) "" else "s"} saved in the ${BackupFolder.NAME} folder",
                    actionLabel = "Restore",
                    onClick = {
                        if (backups.isEmpty()) restoreLauncher.launch(arrayOf("application/json", "text/json"))
                        else choosingBackup = true
                    },
                )
            }

            SectionCard("Organise") {
                SettingRow(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    subtitle = "Change colours and icons, rename or delete your own",
                    onClick = onOpenCategories,
                )
                SettingRow(
                    icon = Icons.Filled.Tag,
                    title = "Tags",
                    subtitle = "Every tag you use, and the transactions behind it",
                    onClick = onOpenTags,
                )
            }

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
                InfoRow("App version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
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
                SettingRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy",
                    subtitle = "What is read, what is stored, and what never leaves this phone",
                    actionLabel = "Read",
                    onClick = { showPrivacy = true },
                )
            }

            if (BuildConfig.DEBUG) DebugProCard()
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // Every sensitive access is disclosed in-app before the system grant screen is opened.
    pendingDisclosure?.let { access ->
        val disclosure = when (access) {
            CaptureAccess.NOTIFICATIONS -> Disclosures.Notifications
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
                            if (succeeded) "Backup restored" else "Restore failed: select a Kahan Gaya Paisa backup"
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

    if (choosingBackup) {
        ChooseBackupDialog(
            backups = backups,
            onDismiss = { choosingBackup = false },
            onSelect = { entry ->
                choosingBackup = false
                restoreUri = entry.uri
            },
            onPickFile = {
                choosingBackup = false
                restoreLauncher.launch(arrayOf("application/json", "text/json"))
            },
        )
    }

    if (confirmingBackup) {
        BackupLocationDialog(
            hasLocation = backupLocation != null,
            onDismiss = { confirmingBackup = false },
            onConfirm = {
                confirmingBackup = false
                // Only the first backup needs the picker; after that the folder is already known.
                if (backupLocation == null) backupLocationLauncher.launch(null)
                else viewModel.backupToSavedLocation(::announceBackup)
            },
            onChangeLocation = {
                confirmingBackup = false
                backupLocationLauncher.launch(null)
            },
        )
    }
}

/**
 * Before the first backup this explains the picker; afterwards it confirms the folder already in
 * use, so a routine backup neither reopens the picker nor nests a second folder.
 */
@Composable
private fun BackupLocationDialog(
    hasLocation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onChangeLocation: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Back up your data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasLocation) {
                    Text("Your backup is added to the \"${BackupFolder.NAME}\" folder you chose earlier.")
                    TextButton(onClick = onChangeLocation, contentPadding = PaddingValues(0.dp)) {
                        Text("Change location")
                    }
                } else {
                    Text("Choose where to keep your backups \u2014 Google Drive, this device, or any other storage in the picker.")
                    Text(
                        "A folder named \"${BackupFolder.NAME}\" is created there, and every backup is saved inside it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(if (hasLocation) "Back up" else "Choose location") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
                Text("Choose how often to create a backup, then pick where to keep it \u2014 Google Drive, this device, or any other storage.")
                Text(
                    "A folder named \"${BackupFolder.NAME}\" is created there, and every backup is saved inside it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        confirmButton = { TextButton(onClick = onConfirm) { Text("Choose location") } },
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

/**
 * Pro's state as a notice rather than a card.
 *
 * Every other block on this screen is a titled card of settings, which is the wrong shape for this:
 * it is not a place to change something, it is one thing being announced. A flat tinted band with
 * no elevation and no gradient reads as a banner and stops the offer from looking like a row the
 * user has already dealt with.
 *
 * The light teal of the primary container, with its paired on-container token for the ink, so the
 * band keeps its contrast in light and dark without a colour being picked by eye. Flat and filled
 * rather than carded: no elevation, no gradient, no section title, so it reads as an announcement
 * next to the settings cards around it.
 *
 * One colour for both states on purpose. The wording and the trailing chevron say which state it
 * is, so the band only has to read as a notice rather than as good or bad news.
 */
@Composable
private fun ProNotice(isPro: Boolean, onUpgrade: () -> Unit) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            // Active Pro has nothing to tap, so it is not offered as a target at all.
            .then(if (isPro) Modifier else Modifier.clickable(onClick = onUpgrade))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (isPro) "Pro is active" else "Get Pro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainer,
            )
            Text(
                if (isPro) {
                    "Thank you. Every Pro feature is unlocked on this phone."
                } else {
                    "Your own categories, unlimited tags, recurring payments, automatic backups"
                },
                style = MaterialTheme.typography.bodySmall,
                color = onContainer,
            )
        }
        if (!isPro) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "See what's in Pro",
                tint = onContainer,
            )
        }
    }
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
    /** Null for a row that only reports something and has nothing to tap. */
    actionLabel: String? = "Open",
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
        actionLabel?.let { label ->
            TextButton(onClick = onClick) { Text(label) }
        }
    }
}

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

/**
 * Debug builds only: switches this device between the free and Pro tiers without buying anything
 * or reinstalling. Leaving it on "Actual" gives the real answer: grandfathered, bought, or free.
 */
@Composable
private fun DebugProCard() {
    val context = LocalContext.current
    val billing = remember { ServiceLocator.billing(context) }
    val entitlements = remember { ServiceLocator.entitlementStore(context) }
    var forcePro by remember { mutableStateOf(entitlements.debugOverride()) }

    SectionCard("Debug") {
        Text(
            "Pro tier on this device. Debug builds only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Pair<String, Boolean?>>("Actual" to null, "Free" to false, "Pro" to true).forEach { (label, value) ->
                FilterChip(
                    selected = forcePro == value,
                    onClick = {
                        forcePro = value
                        billing.setDebugOverride(value)
                    },
                    label = { Text(label) },
                )
            }
        }
    }
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

/**
 * The backups already sitting in the app's folder, newest first. Each one is drawn as a card with a
 * file icon and a trailing chevron, because a plain list of names did not read as tappable.
 */
@Composable
private fun ChooseBackupDialog(
    backups: List<BackupFolder.Entry>,
    onDismiss: () -> Unit,
    onSelect: (BackupFolder.Entry) -> Unit,
    onPickFile: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tap a backup to restore it. Newest first; this replaces the data on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Capped so a long history cannot push the dialog's buttons off screen.
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(backups, key = { it.uri.toString() }) { entry ->
                        BackupRow(entry = entry, onClick = { onSelect(entry) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onPickFile) { Text("Pick a file\u2026") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BackupRow(entry: BackupFolder.Entry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                // The date leads: it is what someone picking a backup is actually choosing between.
                Text(
                    formatTimestamp(entry.savedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    "${entry.name} \u00b7 ${formatBackupSize(entry.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatBackupSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
