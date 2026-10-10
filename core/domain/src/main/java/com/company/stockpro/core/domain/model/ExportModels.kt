package com.company.stockpro.core.domain.model

/**
 * Supported enterprise export formats with MIME types and file extensions.
 */
enum class ExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
) {
    CSV(
        label = "CSV Spreadsheet (.csv)",
        extension = "csv",
        mimeType = "text/csv",
    ),
    EXCEL(
        label = "Excel Workbook (.xlsx)",
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ),
    PDF(
        label = "PDF Report (.pdf)",
        extension = "pdf",
        mimeType = "application/pdf",
    ),
    JSON(
        label = "ERP JSON (.json)",
        extension = "json",
        mimeType = "application/json",
    ),
    XML(
        label = "Enterprise XML (.xml)",
        extension = "xml",
        mimeType = "application/xml",
    );

    companion object {
        fun fromExtension(ext: String): ExportFormat {
            val clean = ext.trim().removePrefix(".").lowercase()
            return entries.firstOrNull { it.extension == clean } ?: CSV
        }
    }
}

/**
 * Available datasets for extraction and reporting.
 */
enum class ExportDataset(
    val displayTitle: String,
    val description: String,
) {
    MASTER_INVENTORY(
        displayTitle = "Master Item Catalogue",
        description = "Complete item registry including SKU codes, classifications, unit costs, stock levels, and valuations.",
    ),
    STOCK_LEDGER(
        displayTitle = "Stock Movement Ledger",
        description = "Append-only chronological audit trail of all receipts, issues, adjustments, and location transfers.",
    ),
    LOW_STOCK_ALERT(
        displayTitle = "Critical & Low Stock Items",
        description = "Items currently at or below minimum reorder thresholds requiring prompt replenishment.",
    ),
    EXECUTIVE_SUMMARY(
        displayTitle = "Executive & Financial Summary",
        description = "Aggregated KPI snapshot: total valuation, turnover metrics, inbound/outbound flow, and category breakdowns.",
    );

    companion object {
        fun fromString(value: String): ExportDataset {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: MASTER_INVENTORY
        }
    }
}

/**
 * Configuration payload defining export scope, security context, and rendering rules.
 */
data class ExportConfig(
    val dataset: ExportDataset,
    val format: ExportFormat,
    val plantId: String = "",
    val plantName: String = "Pune Plant 1",
    val warehouseName: String = "Main Warehouse",
    val currency: Currency = Currency.INR,
    val userRole: Role = Role.INVENTORY_MANAGER,
    val initiatedBy: String = "System User",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val includeCharts: Boolean = true,
)

/**
 * Historical record of an executed export operation.
 * Includes cryptographic SHA-256 checksum for audit and integrity verification.
 */
data class ExportHistoryRecord(
    val id: String,
    val timestamp: Long,
    val dataset: ExportDataset,
    val format: ExportFormat,
    val recordCount: Int,
    val fileSizeBytes: Long,
    val filePath: String,
    val fileName: String,
    val sha256Checksum: String,
    val initiatedBy: String,
    val plantName: String,
    val status: String = "SUCCESS",
)

/**
 * Frequency options for scheduled background report generation.
 */
enum class ExportFrequency(val displayTitle: String) {
    DAILY("Daily (06:00 AM)"),
    WEEKLY("Weekly (Monday 06:00 AM)"),
    MONTHLY("Monthly (1st of Month)");

    companion object {
        fun fromString(value: String): ExportFrequency {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: DAILY
        }
    }
}

/**
 * Scheduled background export profile managed via AndroidX WorkManager.
 */
data class ExportSchedule(
    val id: String,
    val dataset: ExportDataset,
    val format: ExportFormat,
    val frequency: ExportFrequency,
    val hourOfDay: Int = 6,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long? = null,
    val plantName: String = "Pune Plant 1",
    val recipientEmail: String? = null,
)
