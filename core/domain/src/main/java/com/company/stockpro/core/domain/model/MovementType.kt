package com.company.stockpro.core.domain.model

/**
 * Types of stock movements in the ledger.
 * Matches the Firestore `ledger.type` field (architecture.md §4.3).
 *
 * Every enum carries [Unknown] and parses safely (rules.md §3.4).
 */
enum class MovementType {
    RECEIPT,
    ISSUE,
    PRODUCTION,
    ADJUSTMENT,
    TRANSFER_IN,
    TRANSFER_OUT,
    SCRAP,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): MovementType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
