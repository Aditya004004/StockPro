package com.company.stockpro.core.domain.model

/**
 * Domain model representing an audit record of a low-stock threshold modification.
 */
data class ThresholdAudit(
    val id: String,
    val itemId: String,
    val sku: String,
    val plantId: String,
    val oldThreshold: Quantity,
    val newThreshold: Quantity,
    val operator: String,
    val timestamp: Long,
)
