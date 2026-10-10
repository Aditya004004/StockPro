package com.company.stockpro.core.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages filesystem persistence, cryptographic hashing, and Android ShareSheet URI generation
 * for generated export artifacts.
 */
class ExportFileManager(
    private val context: Context,
) {

    private val exportDirectory: File
        get() {
            val dir = File(context.filesDir, "exports")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun executeExport(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
    ): ExportHistoryRecord = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val safeDataset = config.dataset.name.lowercase()
        val fileName = "StockPro_${safeDataset}_$timestamp.${config.format.extension}"
        val targetFile = File(exportDirectory, fileName)

        val recordCount = FileOutputStream(targetFile).use { fos ->
            ExportEngineDispatcher.generateExport(config, fetcher, fos)
        }

        val fileSize = targetFile.length()
        val sha256 = ChecksumUtil.calculateSha256(targetFile)

        ExportHistoryRecord(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            dataset = config.dataset,
            format = config.format,
            recordCount = recordCount,
            fileSizeBytes = fileSize,
            filePath = targetFile.absolutePath,
            fileName = fileName,
            sha256Checksum = sha256,
            initiatedBy = config.initiatedBy,
            plantName = config.plantName,
            status = "SUCCESS",
        )
    }

    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createShareIntent(record: ExportHistoryRecord): Intent {
        val file = File(record.filePath)
        if (!file.exists()) {
            throw FileNotFoundException("Export file no longer exists at ${record.filePath}")
        }
        val uri = getShareableUri(file)

        return Intent(Intent.ACTION_SEND).apply {
            type = record.format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "StockPro Report: ${record.dataset.displayTitle}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the ${record.dataset.displayTitle} export generated from StockPro at plant ${record.plantName}.\nSHA-256 Checksum: ${record.sha256Checksum}"
            )
            clipData = ClipData.newRawUri("StockPro Export", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
