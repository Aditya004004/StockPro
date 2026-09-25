package com.company.stockpro.core.domain.model

/**
 * Lifecycle status of general tools, fixtures, gauges, and jigs.
 * prd.md §7 Option b / phases.md § Phase 5
 */
enum class ToolStatus(val displayLabel: String) {
    AVAILABLE("Available"),
    ISSUED("Issued"),
    IN_CALIBRATION("In Calibration"),
    RETIRED("Retired");

    companion object {
        fun fromString(value: String): ToolStatus {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: AVAILABLE
        }
    }
}

/**
 * Health status derived from calibration schedule.
 */
enum class CalibrationStatus(val displayLabel: String) {
    VALID("Valid"),
    DUE_SOON("Due Soon"),
    OVERDUE("Overdue");
}

/**
 * Custody assignment and calibration tracking for tools (prd.md §7 Option b).
 */
data class ToolCustody(
    val itemId: String,
    val status: ToolStatus = ToolStatus.AVAILABLE,
    val assignedToOperator: String? = null,
    val issuedAt: Long? = null,
    val calibrationIntervalDays: Int = 180,
    val lastCalibratedAt: Long? = null,
    val nextCalibrationDueAt: Long? = null,
    val notes: String? = null,
) {
    fun deriveCalibrationStatus(currentTimeEpochMs: Long = System.currentTimeMillis()): CalibrationStatus {
        val dueAt = nextCalibrationDueAt ?: return CalibrationStatus.VALID
        val diffMs = dueAt - currentTimeEpochMs
        return when {
            diffMs < 0 -> CalibrationStatus.OVERDUE
            diffMs <= 7L * 24 * 60 * 60 * 1000L -> CalibrationStatus.DUE_SOON
            else -> CalibrationStatus.VALID
        }
    }

    fun daysUntilDue(currentTimeEpochMs: Long = System.currentTimeMillis()): Long {
        val dueAt = nextCalibrationDueAt ?: return Long.MAX_VALUE
        val diffMs = dueAt - currentTimeEpochMs
        return (diffMs / (24 * 60 * 60 * 1000L))
    }
}
