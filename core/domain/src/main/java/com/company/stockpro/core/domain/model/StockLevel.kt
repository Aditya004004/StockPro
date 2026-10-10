package com.company.stockpro.core.domain.model

enum class StockLevel {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    RESERVED,
    IN_PRODUCTION;

    companion object {
        /**
         * Calculates stock status per PRD & architecture specifications:
         * Let Q = available stock, T = minimum stock threshold (reorder level).
         * Q <= 0       -> OUT_OF_STOCK
         * 0 < Q <= T   -> LOW_STOCK
         * Q > T        -> IN_STOCK
         */
        fun derive(available: Quantity, reorderLevel: Quantity): StockLevel = when {
            available.milli <= 0L -> OUT_OF_STOCK
            available.milli <= reorderLevel.milli -> LOW_STOCK
            else -> IN_STOCK
        }

        fun derive(onHand: Quantity, reserved: Quantity, reorderLevel: Quantity): StockLevel {
            val availableMilli = onHand.milli - reserved.milli
            return when {
                availableMilli <= 0L -> OUT_OF_STOCK
                availableMilli <= reorderLevel.milli -> LOW_STOCK
                else -> IN_STOCK
            }
        }
    }
}
