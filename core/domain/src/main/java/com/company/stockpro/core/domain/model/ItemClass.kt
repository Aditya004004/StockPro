package com.company.stockpro.core.domain.model

enum class ItemClass {
    COMPONENT,
    RAW_MATERIAL,
    FINISHED_GOOD,
    TOOL,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): ItemClass =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
