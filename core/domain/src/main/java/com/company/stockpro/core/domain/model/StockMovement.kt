package com.company.stockpro.core.domain.model

/**
 * Domain model for a stock movement. Maps to the write-path payload
 * described in architecture.md §6.
 *
 * [idempotencyKey] is client-generated UUID, guaranteed unique per attempt.
 * All quantities use [Quantity] (Long milli-units) — never floating point.
 */
data class StockMovement(
    val id: String = "",
    val itemId: String,
    val itemCode: String = "",
    val itemName: String = "",
    val type: MovementType,
    val delta: Quantity,
    val balanceAfter: Quantity = Quantity.ZERO,
    val unitCost: Money = Money.ZERO,
    val reference: String? = null,
    val reasonCode: String? = null,
    val note: String? = null,
    val locationId: String? = null,
    val fromLocationId: String? = null,
    val toLocationId: String? = null,
    val actorUid: String = "",
    val actorName: String = "",
    val idempotencyKey: String = "",
    val clientCreatedAt: Long = 0L,
    val createdAt: Long = 0L,
    val isPending: Boolean = false,
)
