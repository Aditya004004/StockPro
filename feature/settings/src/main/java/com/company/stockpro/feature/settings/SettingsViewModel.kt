package com.company.stockpro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.repository.InventoryRepository
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
            runCatching { repository.updateUserProfile(name) }
            dismissDialog()
        }
    }

    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            runCatching { repository.updateThemeMode(themeMode) }
            dismissDialog()
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            runCatching { repository.updateLanguage(language) }
            dismissDialog()
        }
    }

    fun updateUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            runCatching { repository.updateUnitSystem(unitSystem) }
            dismissDialog()
        }
    }

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            runCatching { repository.updateNotificationPreferences(preferences) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            runCatching {
                repository.clearCache()
                cacheSizeBytesFlow.value = 0L
            }
            dismissDialog()
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
