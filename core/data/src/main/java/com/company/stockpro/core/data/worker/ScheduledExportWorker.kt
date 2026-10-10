package com.company.stockpro.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.company.stockpro.core.data.export.ExportFileManager
import com.company.stockpro.core.data.repository.ExportHistoryRepositoryImpl
import com.company.stockpro.core.data.repository.StreamingDataFetcherImpl
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.repository.ExportHistoryRepository
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker responsible for automated periodic inventory report generation.
 * Executes on [Dispatchers.IO] under battery and unmetered network constraints.
 * Posts a system notification with a direct share action on completion.
 */
class ScheduledExportWorker(
    appContext: Context,
    params: WorkerParameters,
    private val database: StockProDatabase,
    private val fetcher: StreamingDataFetcher,
    private val fileManager: ExportFileManager,
    private val historyRepo: ExportHistoryRepository,
) : CoroutineWorker(appContext, params) {

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        StockProDatabase.getInstance(appContext),
        StreamingDataFetcherImpl(StockProDatabase.getInstance(appContext)),
        ExportFileManager(appContext),
        ExportHistoryRepositoryImpl(StockProDatabase.getInstance(appContext)),
    )

    companion object {
        const val TAG = "ScheduledExportWorker"
        const val CHANNEL_ID = "stockpro_exports"
        const val KEY_SCHEDULE_ID = "key_schedule_id"
        const val KEY_DATASET = "key_dataset"
        const val KEY_FORMAT = "key_format"
        const val KEY_PLANT_NAME = "key_plant_name"
        const val KEY_PLANT_ID = "key_plant_id"
        const val KEY_USER_ROLE = "key_user_role"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val scheduleId = inputData.getString(KEY_SCHEDULE_ID) ?: ""
        val datasetStr = inputData.getString(KEY_DATASET) ?: ExportDataset.MASTER_INVENTORY.name
        val formatStr = inputData.getString(KEY_FORMAT) ?: ExportFormat.EXCEL.name
        val plantName = inputData.getString(KEY_PLANT_NAME) ?: "Pune Plant 1"
        val plantId = inputData.getString(KEY_PLANT_ID) ?: ""
        val roleStr = inputData.getString(KEY_USER_ROLE) ?: Role.STORE_KEEPER.name
        val userRole = Role.fromString(roleStr)

        val dataset = ExportDataset.fromString(datasetStr)
        val format = ExportFormat.entries.firstOrNull { it.name.equals(formatStr, ignoreCase = true) }
            ?: ExportFormat.EXCEL

        val config = ExportConfig(
            dataset = dataset,
            format = format,
            plantId = plantId,
            plantName = plantName,
            warehouseName = "Main Warehouse",
            currency = Currency.INR,
            userRole = userRole,
            initiatedBy = "Automated Scheduler",
            includeCharts = true,
        )

        try {
            val record = fileManager.executeExport(config, fetcher)
            historyRepo.recordExport(record)

            if (scheduleId.isNotBlank()) {
                historyRepo.updateScheduleLastRun(scheduleId, System.currentTimeMillis())
            }

            postCompletionNotification(record)
            Log.i(TAG, "Successfully generated scheduled export: ${record.fileName} (${record.fileSizeBytes} bytes)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Scheduled export failed: ${e.localizedMessage}", e)
            Result.retry()
        }
    }

    private fun postCompletionNotification(record: com.company.stockpro.core.domain.model.ExportHistoryRecord) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled Reports & Exports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when scheduled inventory export files are ready"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "export_database")
            putExtra("export_record_id", record.id)
        }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                applicationContext,
                record.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            val shareIntent = fileManager.createShareIntent(record)
            PendingIntent.getActivity(
                applicationContext,
                record.id.hashCode(),
                Intent.createChooser(shareIntent, "Share ${record.fileName}"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Scheduled Report Ready: ${record.dataset.displayTitle}")
            .setContentText("Generated ${record.recordCount} items (${record.fileName})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Automated report '${record.dataset.displayTitle}' (${record.format.name}) generated successfully for ${record.plantName}. Tap to view in StockPro.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            notificationManager.notify(record.id.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.localizedMessage}")
        }
    }
}
