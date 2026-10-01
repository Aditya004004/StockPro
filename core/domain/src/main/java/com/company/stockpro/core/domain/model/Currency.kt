package com.company.stockpro.core.domain.model

/**
 * Supported currencies for inventory valuation and monetary display.
 *
 * Each entry carries:
 * - [code]: ISO 4217 currency code used in exports and persistence.
 * - [symbol]: Glyph prefixed to formatted amounts.
 * - [displayName]: Human-readable label for the settings picker.
 * - [minorUnitFactor]: Divisor to convert [Money.paise] (minor units) to major units.
 *
 * The [Money] class stores values in minor units (e.g. paise for INR, cents for USD).
 * Use [formatMoney] for display-ready strings that respect the active currency.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val minorUnitFactor: Int = 100,
) {
    INR(code = "INR", symbol = "₹", displayName = "Indian Rupee"),
    USD(code = "USD", symbol = "$", displayName = "US Dollar"),
    EUR(code = "EUR", symbol = "€", displayName = "Euro"),
    GBP(code = "GBP", symbol = "£", displayName = "British Pound");

    /**
     * Formats a [Money] value for UI display.
     * Uses compact notation (K / M for USD/EUR/GBP, Lakh / Cr for INR).
     */
    fun formatMoney(money: Money): String {
        val majorUnits = money.paise.toDouble() / minorUnitFactor
        return formatMajorUnits(majorUnits)
    }

    /**
     * Returns a masked placeholder (e.g. "₹ •••••••••") for hidden valuations.
     */
    fun maskedValue(): String = "$symbol •••••••••"

    private fun formatMajorUnits(value: Double): String = when {
        value == 0.0 -> "$symbol 0"
        this == INR -> formatIndian(value)
        else -> formatWestern(value)
    }

    private fun formatIndian(value: Double): String = when {
        value >= 10_000_000 -> "$symbol ${String.format("%.2f", value / 10_000_000.0)} Cr"
        value >= 100_000 -> "$symbol ${String.format("%.2f", value / 100_000.0)} Lakh"
        else -> "$symbol ${String.format("%,.0f", value)}"
    }

    private fun formatWestern(value: Double): String = when {
        value >= 1_000_000 -> "$symbol ${String.format("%.2f", value / 1_000_000.0)}M"
        value >= 1_000 -> "$symbol ${String.format("%.1f", value / 1_000.0)}K"
        else -> "$symbol ${String.format("%,.2f", value)}"
    }
}
