package com.company.stockpro.core.domain.model

enum class SortOrder(val displayName: String) {
    NAME_ASC("Name"),
    CODE_ASC("Code"),
    QUANTITY_DESC("Quantity (High-Low)"),
    QUANTITY_ASC("Quantity (Low-High)"),
    LAST_MOVED_DESC("Recently Moved")
}
