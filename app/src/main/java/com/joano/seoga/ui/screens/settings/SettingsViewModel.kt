package com.joano.seoga.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joano.seoga.R
import com.joano.seoga.data.backup.BackupManager
import com.joano.seoga.data.backup.RestorePreview
import com.joano.seoga.data.preferences.UserPreferencesRepository
import com.joano.seoga.data.preferences.UserSettings
import com.joano.seoga.data.repository.LibraryRepository
import com.joano.seoga.ui.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupOpState {
    object Idle : BackupOpState()
    data class Loading(val message: UiText) : BackupOpState()
    data class Success(val message: UiText) : BackupOpState()
    data class Error(val message: UiText) : BackupOpState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)
    private val libraryRepository = LibraryRepository.getInstance(application)
    private val backupManager = BackupManager(application)

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val totalWorks: StateFlow<Int> = libraryRepository.totalWorksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalChapters: StateFlow<Int?> = libraryRepository.totalChaptersRead
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCollections: StateFlow<Int> = libraryRepository.totalCollectionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _opState = MutableStateFlow<BackupOpState>(BackupOpState.Idle)
    val opState: StateFlow<BackupOpState> = _opState.asStateFlow()

    private val _pendingRestorePreview = MutableStateFlow<Pair<Uri, RestorePreview>?>(null)
    val pendingRestorePreview: StateFlow<Pair<Uri, RestorePreview>?> = _pendingRestorePreview.asStateFlow()

    fun setMangaDexEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseMangaDexSearch(enabled)
        }
    }

    fun setAniListEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseAniListSearch(enabled)
        }
    }

    fun setAdultContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeAdultContent(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun exportToUri(uri: Uri) {
        _opState.value = BackupOpState.Loading(UiText.DynamicString("Exporting..."))
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val result = backupManager.exportBackup(stream)
                    result.fold(
                        onSuccess = {
                            _opState.value = BackupOpState.Success(UiText.StringResource(R.string.backup_export_success))
                        },
                        onFailure = { e ->
                            _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Export failed"))
                        }
                    )
                } ?: run {
                    _opState.value = BackupOpState.Error(UiText.StringResource(R.string.backup_export_error))
                }
            } catch (e: Exception) {
                _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Error"))
            }
        }
    }

    fun inspectBackupFile(uri: Uri) {
        _opState.value = BackupOpState.Loading(UiText.DynamicString("Checking file..."))
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = backupManager.readBackupPreview(stream)
                    result.fold(
                        onSuccess = { preview ->
                            _opState.value = BackupOpState.Idle
                            _pendingRestorePreview.value = Pair(uri, preview)
                        },
                        onFailure = { e ->
                            _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Invalid file"))
                        }
                    )
                } ?: run {
                    _opState.value = BackupOpState.Error(UiText.StringResource(R.string.backup_restore_error))
                }
            } catch (e: Exception) {
                _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Error"))
            }
        }
    }

    fun confirmRestore(replaceExisting: Boolean) {
        val pending = _pendingRestorePreview.value ?: return
        val uri = pending.first
        _pendingRestorePreview.value = null
        _opState.value = BackupOpState.Loading(UiText.DynamicString("Restoring..."))

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = backupManager.restoreBackup(stream, replaceExisting)
                    result.fold(
                        onSuccess = { workCount ->
                            _opState.value = BackupOpState.Success(
                                UiText.StringResource(R.string.backup_restore_success, workCount, 0)
                            )
                        },
                        onFailure = { e ->
                            _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Restore failed"))
                        }
                    )
                } ?: run {
                    _opState.value = BackupOpState.Error(UiText.StringResource(R.string.backup_restore_error))
                }
            } catch (e: Exception) {
                _opState.value = BackupOpState.Error(UiText.DynamicString(e.localizedMessage ?: "Error"))
            }
        }
    }

    fun dismissRestorePreview() {
        _pendingRestorePreview.value = null
    }

    fun dismissOpState() {
        _opState.value = BackupOpState.Idle
    }
}
