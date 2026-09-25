package com.company.stockpro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val activeDialogFlow = MutableStateFlow<SettingsDialog?>(null)
    private val cacheSizeBytesFlow = MutableStateFlow(0L)
    private val exportedCsvFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.observeUserSettings(),
        repository.observeSyncStatus(),
        activeDialogFlow,
        cacheSizeBytesFlow,
        exportedCsvFlow,
    ) { userSettings, syncStatus, activeDialog, cacheSize, exportedCsv ->
        SettingsUiState.Content(
            userSettings = userSettings,
            syncStatus = syncStatus,
            cacheSizeBytes = cacheSize,
            activeDialog = activeDialog,
            exportedCsv = exportedCsv,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    fun showDialog(dialog: SettingsDialog) {
        activeDialogFlow.value = dialog
    }

    fun dismissDialog() {
        activeDialogFlow.value = null
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            try {
                repository.updateUserProfile(name)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
        }
    }

    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                repository.updateThemeMode(themeMode)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            try {
                repository.updateLanguage(language)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
        }
    }

    fun updateUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            try {
                repository.updateUnitSystem(unitSystem)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
        }
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            try {
                repository.updateNotificationPreferences(preferences)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                repository.clearCache()
                cacheSizeBytesFlow.value = 0L
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            dismissDialog()
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.deleteAccount()) {
                is DataResult.Success -> {
                    dismissDialog()
                    onSuccess()
                }
                is DataResult.Error -> {
                    val msg = when (val text = result.message) {
                        is com.company.stockpro.core.domain.util.UiText.Dynamic -> text.value
                        is com.company.stockpro.core.domain.util.UiText.StringResource -> "Failed to delete account."
                    }
                    onError(msg)
                }
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val csv = buildString {
                appendLine("StockPro Master Data & Settings Export")
                appendLine("Exported At,${System.currentTimeMillis()}")
                appendLine("Export Type,System Configuration")
                appendLine("Status,Complete")
            }
            exportedCsvFlow.value = csv
        }
    }

    fun clearExportedCsv() {
        exportedCsvFlow.value = null
    }
}
