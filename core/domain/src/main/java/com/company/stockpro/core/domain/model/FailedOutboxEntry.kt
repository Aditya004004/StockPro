package com.company.stockpro.core.domain.model

/**
 * Domain representation of a failed outbox mutation (architecture.md §6).
 * Displayed in the "Needs Attention" dialog or screen.
 */
data class FailedOutboxEntry(
    val id: Long,
    val itemId: String,
    val idempotencyKey: String,
    val errorMessage: String?,
    val createdAt: Long,
)
