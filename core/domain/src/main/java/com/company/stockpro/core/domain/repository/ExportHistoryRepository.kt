package com.company.stockpro.core.domain.repository

import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.ExportSchedule
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface governing export audit logs, tamper-evident history records,
 * and scheduled automated export profiles.
 */
interface ExportHistoryRepository {

    /** Reactive stream of all export history entries, ordered by timestamp DESC. */
    fun observeExportHistory(): Flow<List<ExportHistoryRecord>>

    /** Records a newly executed export event with its SHA-256 checksum. */
    suspend fun recordExport(record: ExportHistoryRecord): DataResult<Unit>

    /** Deletes an individual export history record and deletes its corresponding local file. */
    suspend fun deleteExportHistory(id: String): DataResult<Unit>

    /** Clears all export history records. */
    suspend fun clearExportHistory(): DataResult<Unit>

    /** Reactive stream of scheduled export profiles. */
    fun observeSchedules(): Flow<List<ExportSchedule>>

    /** Creates or updates a scheduled export profile. */
    suspend fun saveSchedule(schedule: ExportSchedule): DataResult<Unit>

    /** Enables or disables an automated export schedule. */
    suspend fun toggleSchedule(id: String, isEnabled: Boolean): DataResult<Unit>

    /** Updates the last execution timestamp for a schedule. */
    suspend fun updateScheduleLastRun(id: String, timestamp: Long): DataResult<Unit>

    /** Permanently removes a scheduled export profile. */
    suspend fun deleteSchedule(id: String): DataResult<Unit>
}
