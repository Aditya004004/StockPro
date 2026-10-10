package com.company.stockpro.core.domain.repository

import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.ExportSchedule
import com.company.stockpro.core.domain.util.DataResult

/**
 * High-level orchestration contract for executing on-demand exports,
 * sharing generated reports via Android ShareSheet, and managing background automation.
 */
interface ExportManager {

    /**
     * Executes an on-demand export according to [config], saves the file,
     * hashes it with SHA-256, and records it in history.
     */
    suspend fun executeExport(config: ExportConfig): DataResult<ExportHistoryRecord>

    /**
     * Launches the system ShareSheet to share the generated file.
     */
    fun shareExport(record: ExportHistoryRecord)

    /**
     * Registers a periodic background export schedule with WorkManager.
     */
    suspend fun scheduleExport(schedule: ExportSchedule): DataResult<Unit>

    /**
     * Triggers an immediate one-time background execution of [schedule].
     */
    suspend fun triggerScheduleNow(schedule: ExportSchedule): DataResult<Unit>

    /**
     * Cancels an active periodic export schedule.
     */
    suspend fun cancelSchedule(scheduleId: String): DataResult<Unit>
}
