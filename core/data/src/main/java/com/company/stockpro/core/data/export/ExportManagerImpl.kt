package com.company.stockpro.core.data.export

import android.content.Context
import android.content.Intent
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.ExportSchedule
import com.company.stockpro.core.domain.repository.ExportHistoryRepository
import com.company.stockpro.core.domain.repository.ExportManager
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [ExportManager] coordinating file engines,
 * audit logging, ShareSheet intents, and WorkManager task scheduling.
 */
class ExportManagerImpl(
    private val context: Context,
    private val fetcher: StreamingDataFetcher,
    private val fileManager: ExportFileManager,
    private val historyRepository: ExportHistoryRepository,
    private val scheduler: ExportScheduler,
) : ExportManager {

    override suspend fun executeExport(config: ExportConfig): DataResult<ExportHistoryRecord> = withContext(Dispatchers.IO) {
        try {
            val record = fileManager.executeExport(config, fetcher)
            historyRepository.recordExport(record)
            DataResult.Success(record)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Export failed: ${e.localizedMessage}"))
        }
    }

    override fun shareExport(record: ExportHistoryRecord) {
        try {
            val shareIntent = fileManager.createShareIntent(record).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share ${record.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Unable to share report: ${e.localizedMessage}",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    override suspend fun scheduleExport(schedule: ExportSchedule): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            historyRepository.saveSchedule(schedule)
            scheduler.scheduleExport(schedule)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to schedule export: ${e.localizedMessage}"))
        }
    }

    override suspend fun triggerScheduleNow(schedule: ExportSchedule): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduler.triggerImmediateExport(schedule)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to trigger export: ${e.localizedMessage}"))
        }
    }

    override suspend fun cancelSchedule(scheduleId: String): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduler.cancelSchedule(scheduleId)
            historyRepository.toggleSchedule(scheduleId, false)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to cancel schedule: ${e.localizedMessage}"))
        }
    }
}
