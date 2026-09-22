package com.khaltech.expenseassistant.ui.account

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khaltech.expenseassistant.data.export.CsvExporter
import com.khaltech.expenseassistant.data.backup.AutoBackupScheduler
import com.khaltech.expenseassistant.data.backup.BackupArchive
import com.khaltech.expenseassistant.data.backup.BackupFolder
import com.khaltech.expenseassistant.data.prefs.AutoBackupSettings
import com.khaltech.expenseassistant.data.prefs.BackupInterval
import com.khaltech.expenseassistant.data.prefs.UserProfile
import com.khaltech.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(app: Application) : AndroidViewModel(app) {

    private val preferences = ServiceLocator.userPreferences(app)
    private val repository = ServiceLocator.repository(app)
    private val budgetRepository = ServiceLocator.budgetRepository(app)
    private val recurringPlans = ServiceLocator.recurringPlanRepository(app)
    private val backupArchive = ServiceLocator.backupArchive(app)

    private val _profile = MutableStateFlow(preferences.load())
    val profile: StateFlow<UserProfile> = _profile

    private val _autoBackupSettings = MutableStateFlow(preferences.autoBackupSettings())
    val autoBackupSettings: StateFlow<AutoBackupSettings?> = _autoBackupSettings

    /** Backups found in the remembered location, newest first, for the restore picker. */
    private val _backups = MutableStateFlow<List<BackupFolder.Entry>>(emptyList())
    val backups: StateFlow<List<BackupFolder.Entry>> = _backups

    /** The location already chosen for backups, so later backups need not ask for it again. */
    private val _backupLocation = MutableStateFlow(savedBackupLocation())
    val backupLocation: StateFlow<Uri?> = _backupLocation

    /** Null once the user revokes the grant in system settings, which sends them back to the picker. */
    private fun savedBackupLocation(): Uri? {
        val uri = preferences.backupLocationUri()?.let(Uri::parse) ?: return null
        return uri.takeIf { BackupFolder.hasAccess(getApplication(), it) }
    }

    /** Backs up straight into the folder already chosen, without opening the picker again. */
    fun backupToSavedLocation(onResult: (Boolean) -> Unit) {
        val treeUri = _backupLocation.value
        if (treeUri == null) onResult(false) else backupToFolder(treeUri, onResult)
    }

    val transactionCount: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _earliest = MutableStateFlow<Long?>(null)
    val earliest: StateFlow<Long?> = _earliest

    init {
        viewModelScope.launch { _earliest.value = repository.earliestTimestamp() }
    }

    fun save(profile: UserProfile) {
        preferences.save(profile)
        _profile.value = profile
    }

    fun clearAllTransactions(alsoResetSettings: Boolean, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            repository.deleteAll()
            if (alsoResetSettings) {
                repository.clearLearnedRules()
                budgetRepository.clearAll()
                recurringPlans.clearAll()
            }
        }
        _earliest.value = repository.earliestTimestamp()
        onResult(result.isSuccess)
    }

    fun exportCsv(uri: Uri, onResult: (Int) -> Unit) = viewModelScope.launch {
        val transactions = repository.allTransactions()
        val written = runCatching {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(CsvExporter.toCsv(transactions).toByteArray())
                } ?: error("Could not open $uri")
            }
        }
        onResult(if (written.isSuccess) transactions.size else -1)
    }

    fun suggestedFileName(): String = CsvExporter.fileName()

    /**
     * Writes a dated backup into the app's own folder under [treeUri], creating that folder when it
     * is not there yet, so the user only has to pick where the folder should live.
     */
    fun backupToFolder(treeUri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val context = getApplication<Application>()
        val result = runCatching {
            val contents = withContext(Dispatchers.IO) { backupArchive.export() }
            withContext(Dispatchers.IO) {
                BackupFolder.write(context, treeUri, BackupArchive.fileName(), contents)
            }
            rememberBackupLocation(treeUri)
        }
        refreshBackups()
        onResult(result.isSuccess)
    }

    /**
     * Holds on to the picked location so later restores can list what is already there without
     * asking for it again.
     */
    private fun rememberBackupLocation(treeUri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        preferences.saveBackupLocationUri(treeUri.toString())
        _backupLocation.value = treeUri
    }

    fun refreshBackups() = viewModelScope.launch {
        val treeUri = savedBackupLocation().also { _backupLocation.value = it }
        _backups.value = if (treeUri == null) emptyList() else withContext(Dispatchers.IO) {
            BackupFolder.list(getApplication(), treeUri)
        }
    }

    fun restore(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val contents = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reader.readText()
                } ?: error("Could not open $uri")
            }
            backupArchive.restore(contents)
        }
        if (result.isSuccess) {
            _profile.value = preferences.load()
            _earliest.value = repository.earliestTimestamp()
        }
        onResult(result.isSuccess)
    }

    fun enableAutoBackup(folderUri: Uri, interval: BackupInterval, onResult: (Boolean) -> Unit) {
        val context = getApplication<Application>()
        val result = runCatching {
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            preferences.saveBackupLocationUri(folderUri.toString())
            AutoBackupSettings(interval, folderUri.toString()).also { settings ->
                preferences.saveAutoBackup(settings)
                AutoBackupScheduler.schedule(context, settings)
                _autoBackupSettings.value = settings
            }
        }
        refreshBackups()
        onResult(result.isSuccess)
    }

    fun disableAutoBackup() {
        val context = getApplication<Application>()
        preferences.clearAutoBackup()
        AutoBackupScheduler.cancel(context)
        _autoBackupSettings.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AccountViewModel(checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]))
            }
        }
    }
}
