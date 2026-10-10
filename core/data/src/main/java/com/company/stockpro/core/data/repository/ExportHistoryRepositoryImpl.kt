package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.ExportHistoryEntity
import com.company.stockpro.core.database.entity.ExportScheduleEntity
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportFrequency
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.ExportSchedule
import com.company.stockpro.core.domain.repository.ExportHistoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Concrete implementation of [ExportHistoryRepository] backed by Room database.
 */
class ExportHistoryRepositoryImpl(
    private val database: StockProDatabase,
) : ExportHistoryRepository {

    private val historyDao get() = database.exportHistoryDao()
    private val scheduleDao get() = database.exportScheduleDao()

    override fun observeExportHistory(): Flow<List<ExportHistoryRecord>> {
        return historyDao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun recordExport(record: ExportHistoryRecord): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            historyDao.insert(record.toEntity())
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to record export history: ${e.localizedMessage}"))
        }
    }

    override suspend fun deleteExportHistory(id: String): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = historyDao.getById(id)
            if (entity != null) {
                val file = File(entity.filePath)
                if (file.exists()) {
                    file.delete()
                }
                historyDao.deleteById(id)
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to delete export record: ${e.localizedMessage}"))
        }
    }

    override suspend fun clearExportHistory(): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val allRecords = historyDao.getAll()
            for (record in allRecords) {
                try {
                    val file = File(record.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {
                    // Continue deleting remaining files even if one file is locked
                }
            }
            historyDao.clearAll()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to clear export history: ${e.localizedMessage}"))
        }
    }

    override fun observeSchedules(): Flow<List<ExportSchedule>> {
        return scheduleDao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveSchedule(schedule: ExportSchedule): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.insert(schedule.toEntity())
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to save schedule: ${e.localizedMessage}"))
        }
    }

    override suspend fun toggleSchedule(id: String, isEnabled: Boolean): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.toggleEnabled(id, isEnabled)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to toggle schedule: ${e.localizedMessage}"))
        }
    }

    override suspend fun updateScheduleLastRun(id: String, timestamp: Long): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.updateLastRun(id, timestamp)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to update schedule run: ${e.localizedMessage}"))
        }
    }

    override suspend fun deleteSchedule(id: String): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.deleteById(id)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(UiText.Dynamic("Failed to delete schedule: ${e.localizedMessage}"))
        }
    }
}

private fun ExportHistoryEntity.toDomain(): ExportHistoryRecord = ExportHistoryRecord(
    id = id,
    timestamp = timestamp,
    dataset = ExportDataset.fromString(dataset),
    format = ExportFormat.fromExtension(format),
    recordCount = recordCount,
    fileSizeBytes = fileSizeBytes,
    filePath = filePath,
    fileName = fileName,
    sha256Checksum = sha256Checksum,
    initiatedBy = initiatedBy,
    plantName = plantName,
    status = status,
)

private fun ExportHistoryRecord.toEntity(): ExportHistoryEntity = ExportHistoryEntity(
    id = id,
    timestamp = timestamp,
    dataset = dataset.name,
    format = format.extension,
    recordCount = recordCount,
    fileSizeBytes = fileSizeBytes,
    filePath = filePath,
    fileName = fileName,
    sha256Checksum = sha256Checksum,
    initiatedBy = initiatedBy,
    plantName = plantName,
    status = status,
)

private fun ExportScheduleEntity.toDomain(): ExportSchedule = ExportSchedule(
    id = id,
    dataset = ExportDataset.fromString(dataset),
    format = ExportFormat.fromExtension(format),
    frequency = ExportFrequency.fromString(frequency),
    hourOfDay = hourOfDay,
    minute = minute,
    isEnabled = isEnabled,
    lastRunTimestamp = lastRunTimestamp,
    plantName = plantName,
    recipientEmail = recipientEmail,
)

private fun ExportSchedule.toEntity(): ExportScheduleEntity = ExportScheduleEntity(
    id = id,
    dataset = dataset.name,
    format = format.extension,
    frequency = frequency.name,
    hourOfDay = hourOfDay,
    minute = minute,
    isEnabled = isEnabled,
    lastRunTimestamp = lastRunTimestamp,
    plantName = plantName,
    recipientEmail = recipientEmail,
)
