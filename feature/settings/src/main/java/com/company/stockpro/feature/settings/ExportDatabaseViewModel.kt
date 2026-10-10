package com.company.stockpro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.ExportHistoryRepository
import com.company.stockpro.core.domain.repository.ExportManager
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportDatabaseViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val exportManager: ExportManager,
    private val historyRepository: ExportHistoryRepository,
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(ExportTab.QUICK_EXPORT)
    private val isExportingFlow = MutableStateFlow(false)
    private val exportProgressFlow = MutableStateFlow<String?>(null)
    private val lastRecordFlow = MutableStateFlow<ExportHistoryRecord?>(null)
    private val selectedDatasetFlow = MutableStateFlow(ExportDataset.MASTER_INVENTORY)
    private val selectedFormatFlow = MutableStateFlow(ExportFormat.CSV)
    private val datePresetFlow = MutableStateFlow(DatePreset.ALL_TIME)
    private val includeChartsFlow = MutableStateFlow(true)
    private val showClearHistoryDialogFlow = MutableStateFlow(false)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val isErrorFlow = MutableStateFlow(false)

    val uiState: StateFlow<ExportDatabaseUiState> = combine(
        inventoryRepository.observeUserSettings(),
        historyRepository.observeExportHistory(),
        combine(
            selectedTabFlow,
            isExportingFlow,
            exportProgressFlow,
            lastRecordFlow,
            selectedDatasetFlow,
        ) { tab, isExporting, progress, lastRec, dataset ->
            Tuple5(tab, isExporting, progress, lastRec, dataset)
        },
        combine(
            selectedFormatFlow,
            datePresetFlow,
            includeChartsFlow,
            showClearHistoryDialogFlow,
            combine(statusMessageFlow, isErrorFlow) { status, isErr ->
                status to isErr
            }
        ) { format, preset, charts, clearDialog, statusPair ->
            Tuple5(format, preset, charts, clearDialog, statusPair)
        }
    ) { userSettings, history, t1, t2 ->
        val (tab, isExporting, progress, lastRec, dataset) = t1
        val (format, preset, charts, clearDialog, statusPair) = t2
        val (status, isErr) = statusPair

        ExportDatabaseUiState(
            selectedTab = tab,
            userSettings = userSettings,
            isExporting = isExporting,
            exportProgressMessage = progress,
            lastExportedRecord = lastRec,
            historyRecords = history,
            selectedDataset = dataset,
            selectedFormat = format,
            datePreset = preset,
            includeCharts = charts,
            showClearHistoryDialog = clearDialog,
            statusMessage = status,
            isError = isErr,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExportDatabaseUiState(),
    )

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    fun selectTab(tab: ExportTab) {
        selectedTabFlow.value = tab
    }

    fun setDataset(dataset: ExportDataset) {
        selectedDatasetFlow.value = dataset
    }

    fun setFormat(format: ExportFormat) {
        selectedFormatFlow.value = format
    }

    fun setDatePreset(preset: DatePreset) {
        datePresetFlow.value = preset
    }

    fun setIncludeCharts(include: Boolean) {
        includeChartsFlow.value = include
    }

    fun setShowClearHistoryDialog(show: Boolean) {
        showClearHistoryDialogFlow.value = show
    }

    fun dismissStatus() {
        statusMessageFlow.value = null
        isErrorFlow.value = false
    }

    fun quickExport(
        dataset: ExportDataset = ExportDataset.MASTER_INVENTORY,
        format: ExportFormat = ExportFormat.CSV,
    ) {
        triggerExport(dataset = dataset, format = format, includeCharts = true)
    }

    fun executeAdvancedExport() {
        val state = uiState.value
        val (startMillis, endMillis) = computeDateRange(state.datePreset)

        triggerExport(
            dataset = state.selectedDataset,
            format = state.selectedFormat,
            startDateMillis = startMillis,
            endDateMillis = endMillis,
            includeCharts = state.includeCharts,
        )
    }

    private fun triggerExport(
        dataset: ExportDataset,
        format: ExportFormat,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        includeCharts: Boolean = true,
    ) {
        val userSettings = uiState.value.userSettings
        val config = ExportConfig(
            dataset = dataset,
            format = format,
            plantId = userSettings.profile.plantId,
            plantName = userSettings.profile.plantName,
            warehouseName = userSettings.profile.warehouseName,
            currency = userSettings.currency,
            userRole = userSettings.profile.role,
            initiatedBy = userSettings.profile.displayName,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            includeCharts = includeCharts,
        )

        viewModelScope.launch {
            isExportingFlow.value = true
            exportProgressFlow.value = "Streaming ${dataset.displayTitle} into ${format.name}..."
            when (val result = exportManager.executeExport(config)) {
                is DataResult.Success -> {
                    lastRecordFlow.value = result.data
                    isExportingFlow.value = false
                    exportProgressFlow.value = null
                    statusMessageFlow.value = "Generated ${result.data.fileName} (${result.data.recordCount} records)"
                    isErrorFlow.value = false
                }
                is DataResult.Error -> {
                    isExportingFlow.value = false
                    exportProgressFlow.value = null
                    statusMessageFlow.value = "Export failed: ${result.message}"
                    isErrorFlow.value = true
                }
            }
        }
    }

    fun shareRecord(record: ExportHistoryRecord) {
        exportManager.shareExport(record)
    }

    fun deleteHistoryRecord(id: String) {
        viewModelScope.launch {
            historyRepository.deleteExportHistory(id)
            if (lastRecordFlow.value?.id == id) {
                lastRecordFlow.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearExportHistory()
            lastRecordFlow.value = null
            showClearHistoryDialogFlow.value = false
            statusMessageFlow.value = "Export audit history cleared."
        }
    }

    private fun computeDateRange(preset: DatePreset): Pair<Long?, Long?> {
        val days = preset.daysBack ?: return null to null
        val now = System.currentTimeMillis()
        val start = now - (days * 24L * 60L * 60L * 1000L)
        return start to now
    }
}
