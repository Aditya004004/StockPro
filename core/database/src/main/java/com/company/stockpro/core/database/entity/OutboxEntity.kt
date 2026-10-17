package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room outbox queue for offline-first mutations.
 *
 * State machine: PENDING → POSTING → DONE | FAILED
 * - PENDING: enqueued locally, awaiting drain.
 * - POSTING: OutboxWorker is currently calling the Cloud Function.
 * - DONE: server confirmed (2xx); entry can be cleaned up.
 * - FAILED: server rejected (4xx); needs user attention.
 *
 * Retry is only for 5xx/network errors (stays PENDING with bumped retryCount).
 * Ordering is FIFO per item to ensure balanceAfter consistency.
 */
@Entity(
    tableName = "outbox",
    indices = [
        Index(value = ["status", "item_id", "created_at"]),
        Index(value = ["status"]),
    ],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,
    val status: String = STATUS_PENDING,
    val payload: String,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_POSTING = "POSTING"
        const val STATUS_DONE = "DONE"
        const val STATUS_FAILED = "FAILED"
    }
}
