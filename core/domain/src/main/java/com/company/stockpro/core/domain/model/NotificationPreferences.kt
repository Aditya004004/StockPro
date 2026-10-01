package com.company.stockpro.core.domain.model

/**
 * Granular per-type notification preferences (prd.md §8.2 Gap 10).
 * Prevents users disabling all notifications when only one alert type is noisy.
 */
data class NotificationPreferences(
    val lowStockEnabled: Boolean = true,
    val outOfStockEnabled: Boolean = true,
    val poDueEnabled: Boolean = true,
    val largeAdjustmentEnabled: Boolean = true,
    val syncFailureEnabled: Boolean = true,
    val expiryWarningEnabled: Boolean = true,
) {
    val criticalStockEnabled: Boolean get() = outOfStockEnabled

    fun isTypeEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.LOW_STOCK -> lowStockEnabled
        NotificationType.OUT_OF_STOCK -> outOfStockEnabled
        NotificationType.PO_DUE -> poDueEnabled
        NotificationType.LARGE_ADJUSTMENT -> largeAdjustmentEnabled
        NotificationType.SYNC_FAILURE -> syncFailureEnabled
        NotificationType.AUDIT_DRIFT -> largeAdjustmentEnabled
    }
}
