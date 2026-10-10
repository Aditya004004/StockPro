package com.company.stockpro.feature.settings

import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings

enum class ExportTab(val title: String) {
    QUICK_EXPORT("Quick Export"),
    ADVANCED_EXPORT("Advanced Export"),
    HISTORY("Audit History"),
}

enum class DatePreset(val label: String, val daysBack: Int?) {
    ALL_TIME("All Time", null),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
}

data class ExportDatabaseUiState(
    val selectedTab: ExportTab = ExportTab.QUICK_EXPORT,
    val userSettings: UserSettings = UserSettings(
        profile = UserProfile(
            uid = "u1",
            displayName = "User",
            email = "user@company.com",
            role = Role.INVENTORY_MANAGER,
            plantId = "plant_01",
            plantName = "Pune Plant 1",
        )
    ),
    val isExporting: Boolean = false,
    val exportProgressMessage: String? = null,
    val lastExportedRecord: ExportHistoryRecord? = null,
    val historyRecords: List<ExportHistoryRecord> = emptyList(),
    val selectedDataset: ExportDataset = ExportDataset.MASTER_INVENTORY,
    val selectedFormat: ExportFormat = ExportFormat.CSV,
    val datePreset: DatePreset = DatePreset.ALL_TIME,
    val includeCharts: Boolean = true,
    val showClearHistoryDialog: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)
