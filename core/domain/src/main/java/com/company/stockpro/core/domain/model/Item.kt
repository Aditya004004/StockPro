package com.company.stockpro.core.domain.model

data class Item(
    val id: String,
    val code: String,
    val name: String,
    val itemClass: ItemClass,
    val categoryName: String,
    val unitOfMeasure: String,
    val onHand: Quantity,
    val reserved: Quantity = Quantity.ZERO,
    val inProduction: Quantity = Quantity.ZERO,
    val reorderLevel: Quantity,
    val parLevel: Quantity,
    val unitCost: Money,
    val locationCode: String,
    val barcode: String? = null,
    val imagePath: String? = null,
    val isActive: Boolean = true,
    val lastMovementAt: Long = 0L
) {
    val stockLevel: StockLevel
        get() = StockLevel.derive(available, reorderLevel)

    val available: Quantity
        get() = Quantity((onHand.milli - reserved.milli).coerceAtLeast(0L))
}
