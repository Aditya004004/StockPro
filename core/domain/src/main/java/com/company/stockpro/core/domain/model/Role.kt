package com.company.stockpro.core.domain.model

/**
 * User roles within StockPro organization and plant hierarchy.
 * Governs the security and permission matrix (architecture.md §5.3).
 */
enum class Role(val displayTitle: String) {
    STORE_KEEPER("Store Keeper"),
    PRODUCTION_SUPERVISOR("Production Supervisor"),
    QA("Quality Assurance"),
    INVENTORY_MANAGER("Inventory Manager"),
    PLANT_MANAGER("Plant Manager"),
    ORG_ADMIN("Organization Admin");

    val canManageTeam: Boolean
        get() = this in setOf(PLANT_MANAGER, ORG_ADMIN)

    val canViewValuation: Boolean
        get() = this in setOf(INVENTORY_MANAGER, PLANT_MANAGER, ORG_ADMIN)

    val canAdjustStock: Boolean
        get() = this in setOf(INVENTORY_MANAGER, PLANT_MANAGER, ORG_ADMIN)

    val canExportData: Boolean
        get() = this in setOf(INVENTORY_MANAGER, PLANT_MANAGER, ORG_ADMIN)

    val canDeactivateItem: Boolean
        get() = this in setOf(PLANT_MANAGER, ORG_ADMIN)

    companion object {
        fun fromString(value: String): Role {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: STORE_KEEPER
        }
    }
}
