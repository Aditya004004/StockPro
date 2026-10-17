package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room mirror of an append-only ledger entry.
 * Mirrors Firestore `ledger/{entryId}` (architecture.md §4.3).
 *
 * [isPending] is true for optimistic entries that haven't been confirmed
 * by the server yet. Once the outbox receives a 2xx, the entry is replaced
 * with the server-authoritative version.
 */
@Entity(
    tableName = "stock_ledger",
    indices = [
        Index(value = ["item_id", "created_at"]),
        Index(value = ["created_at"]),
        Index(value = ["idempotency_key"]),
        Index(value = ["from_location_id"]),
        Index(value = ["to_location_id"]),
    ],
)
data class StockLedgerEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "item_code")
    val itemCode: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    val type: String,
    @ColumnInfo(name = "delta_milli")
    val deltaMilli: Long,
    @ColumnInfo(name = "balance_after_milli")
    val balanceAfterMilli: Long = 0L,
    @ColumnInfo(name = "unit_cost_paise")
    val unitCostPaise: Long = 0L,
    val reference: String? = null,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "from_location_id")
    val fromLocationId: String? = null,
    @ColumnInfo(name = "to_location_id")
    val toLocationId: String? = null,
    @ColumnInfo(name = "actor_uid")
    val actorUid: String = "",
    @ColumnInfo(name = "actor_name")
    val actorName: String = "",
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,
    @ColumnInfo(name = "client_created_at")
    val clientCreatedAt: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "is_pending")
    val isPending: Boolean = false,
)
