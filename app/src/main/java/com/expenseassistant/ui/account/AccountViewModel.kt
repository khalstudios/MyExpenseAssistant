package com.expenseassistant.ui.account

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expenseassistant.data.export.CsvExporter
import com.expenseassistant.data.backup.AutoBackupScheduler
import com.expenseassistant.data.backup.BackupArchive
import com.expenseassistant.data.prefs.AutoBackupSettings
import com.expenseassistant.data.prefs.BackupInterval
import com.expenseassistant.data.prefs.UserProfile
import com.expenseassistant.di.ServiceLocator
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
    private val backupArchive = ServiceLocator.backupArchive(app)

    private val _profile = MutableStateFlow(preferences.load())
    val profile: StateFlow<UserProfile> = _profile

    private val _autoBackupSettings = MutableStateFlow(preferences.autoBackupSettings())
    val autoBackupSettings: StateFlow<AutoBackupSettings?> = _autoBackupSettings

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

    fun clearAllTransactions(alsoResetSettings: Boolean) = viewModelScope.launch {
        repository.deleteAll()
        if (alsoResetSettings) {
            repository.clearLearnedRules()
            budgetRepository.clearAll()
        }
        _earliest.value = null
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

    fun suggestedBackupFileName(): String = BackupArchive.fileName()

    fun backup(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val contents = withContext(Dispatchers.IO) { backupArchive.export() }
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(contents)
                } ?: error("Could not open $uri")
            }
        }
        onResult(result.isSuccess)
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
            AutoBackupSettings(interval, folderUri.toString()).also { settings ->
                preferences.saveAutoBackup(settings)
                AutoBackupScheduler.schedule(context, settings)
                _autoBackupSettings.value = settings
            }
        }
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
