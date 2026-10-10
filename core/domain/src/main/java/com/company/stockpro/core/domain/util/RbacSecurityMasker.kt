package com.company.stockpro.core.domain.util

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement

/**
 * Enterprise Role-Based Access Control (RBAC) security utility.
 * Enforces financial data masking across reports, exports, and shared documents.
 * Operators, Store Keepers, and non-managerial roles have unit costs and valuations masked.
 */
object RbacSecurityMasker {

    private const val MASKED_STRING = "***"

    /**
     * Determines whether the given [role] is permitted to view valuation and financial unit costs.
     */
    fun canViewValuation(role: Role): Boolean = role.canViewValuation

    /**
     * Masks financial fields of an [Item] if the user [role] does not have valuation privileges.
     */
    fun maskItem(item: Item, role: Role): Item {
        return if (canViewValuation(role)) {
            item
        } else {
            item.copy(unitCost = Money(0L))
        }
    }

    /**
     * Masks financial fields of a [StockMovement] if the user [role] does not have valuation privileges.
     */
    fun maskMovement(movement: StockMovement, role: Role): StockMovement {
        return if (canViewValuation(role)) {
            movement
        } else {
            movement.copy(unitCost = Money(0L))
        }
    }

    /**
     * Formats total valuation in minor units (paise) for display/export.
     * Returns masked placeholder if [role] lacks valuation visibility.
     */
    fun formatValuation(paise: Long, currency: Currency, role: Role): String {
        return if (canViewValuation(role)) {
            currency.formatMoney(Money(paise))
        } else {
            currency.maskedValue()
        }
    }

    /**
     * Formats unit cost for display/export.
     * Returns "***" if [role] lacks valuation visibility.
     */
    fun formatUnitCost(money: Money, currency: Currency, role: Role): String {
        return if (canViewValuation(role)) {
            currency.formatMoney(money)
        } else {
            MASKED_STRING
        }
    }

    /**
     * Returns the raw valuation value in paise or 0L if masked.
     */
    fun getSecureValuationPaise(paise: Long, role: Role): Long {
        return if (canViewValuation(role)) paise else 0L
    }
}
