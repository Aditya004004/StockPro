package com.company.stockpro.feature.settings

import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.util.UiText

sealed interface SettingsDialog {
    data object EditProfile : SettingsDialog
    data object ChangePassword : SettingsDialog
    data object Appearance : SettingsDialog
    data object Language : SettingsDialog
    data object Units : SettingsDialog
    data object NotificationPreferences : SettingsDialog
    data class ClearCacheConfirm(val estimatedBytes: Long) : SettingsDialog
    data object LogoutConfirm : SettingsDialog
    data object HelpFaq : SettingsDialog
    data object Feedback : SettingsDialog
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val userSettings: UserSettings,
        val syncStatus: SyncStatus = SyncStatus.Synced(System.currentTimeMillis()),
        val cacheSizeBytes: Long = 2560000L,
        val activeDialog: SettingsDialog? = null,
        val appVersionName: String = "1.0.0",
        val appVersionCode: Int = 1,
        val exportedCsv: String? = null,
    ) : SettingsUiState

    data class Error(val message: UiText) : SettingsUiState
}
