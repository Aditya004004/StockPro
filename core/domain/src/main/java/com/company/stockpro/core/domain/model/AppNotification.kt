package com.company.stockpro.core.domain.model

/**
 * Categorized notification types driving alerts and badges (prd.md §3.5, §8.2).
 */
enum class NotificationType(val displayLabel: String) {
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    PO_DUE("Purchase Order Due"),
    LARGE_ADJUSTMENT("Large Adjustment"),
    SYNC_FAILURE("Sync Failure"),
    AUDIT_DRIFT("Audit Drift");

    companion object {
        fun fromString(value: String): NotificationType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: LOW_STOCK
        }
    }
}

/**
 * Domain entity representing an alert or event in the notification center.
 */
data class AppNotification(
    val id: String,
    val plantId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val entityRef: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
)
