package com.company.stockpro.core.domain.model

enum class StockLevel {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    RESERVED,
    IN_PRODUCTION;

    companion object {
        fun derive(onHand: Quantity, reorderLevel: Quantity): StockLevel = when {
            onHand.milli <= 0 -> OUT_OF_STOCK
            onHand.milli <= reorderLevel.milli -> LOW_STOCK
            else -> IN_STOCK
        }
    }
}
