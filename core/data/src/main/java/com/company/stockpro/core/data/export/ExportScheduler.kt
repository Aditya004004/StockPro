package com.company.stockpro.core.data.export

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.company.stockpro.core.data.worker.ScheduledExportWorker
import com.company.stockpro.core.domain.model.ExportFrequency
import com.company.stockpro.core.domain.model.ExportSchedule
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * AndroidX WorkManager coordinator for automated background export schedules.
 * Enforces unmetered network and battery-not-low constraints.
 */
class ExportScheduler(
    private val context: Context,
) {

    private val workManager get() = WorkManager.getInstance(context)

    fun scheduleExport(schedule: ExportSchedule) {
        if (!schedule.isEnabled) {
            cancelSchedule(schedule.id)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_ROAMING)
            .build()

        val inputData = Data.Builder()
            .putString(ScheduledExportWorker.KEY_SCHEDULE_ID, schedule.id)
            .putString(ScheduledExportWorker.KEY_DATASET, schedule.dataset.name)
            .putString(ScheduledExportWorker.KEY_FORMAT, schedule.format.name)
            .putString(ScheduledExportWorker.KEY_PLANT_NAME, schedule.plantName)
            .build()

        val (repeatInterval, timeUnit) = when (schedule.frequency) {
            ExportFrequency.DAILY -> 24L to TimeUnit.HOURS
            ExportFrequency.WEEKLY -> 7L to TimeUnit.DAYS
            ExportFrequency.MONTHLY -> 30L to TimeUnit.DAYS
        }

        val initialDelayMillis = computeInitialDelay(schedule.hourOfDay, schedule.minute)

        val periodicWork = PeriodicWorkRequestBuilder<ScheduledExportWorker>(repeatInterval, timeUnit)
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag("export_schedule_${schedule.id}")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "unique_export_schedule_${schedule.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork,
        )
    }

    fun triggerImmediateExport(schedule: ExportSchedule) {
        val inputData = Data.Builder()
            .putString(ScheduledExportWorker.KEY_SCHEDULE_ID, schedule.id)
            .putString(ScheduledExportWorker.KEY_DATASET, schedule.dataset.name)
            .putString(ScheduledExportWorker.KEY_FORMAT, schedule.format.name)
            .putString(ScheduledExportWorker.KEY_PLANT_NAME, schedule.plantName)
            .build()

        val oneTimeWork = OneTimeWorkRequestBuilder<ScheduledExportWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "immediate_export_${schedule.id}",
            ExistingWorkPolicy.REPLACE,
            oneTimeWork,
        )
    }

    fun cancelSchedule(scheduleId: String) {
        workManager.cancelUniqueWork("unique_export_schedule_$scheduleId")
    }

    private fun computeInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
