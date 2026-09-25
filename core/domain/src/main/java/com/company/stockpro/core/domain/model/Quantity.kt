package com.company.stockpro.core.domain.model

@JvmInline
value class Quantity(val milli: Long) {
    operator fun plus(other: Quantity): Quantity = Quantity(milli + other.milli)
    operator fun minus(other: Quantity): Quantity = Quantity(milli - other.milli)
    operator fun compareTo(other: Quantity): Int = milli.compareTo(other.milli)

    val toUnitsDouble: Double get() = milli / 1000.0

    fun formatted(): String {
        return if (milli % 1000L == 0L) {
            (milli / 1000L).toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", toUnitsDouble).trimEnd('0').trimEnd('.')
        }
    }

    companion object {
        val ZERO = Quantity(0L)
        fun fromUnits(units: Long): Quantity = Quantity(units * 1000L)
        fun fromUnits(units: Double): Quantity = Quantity(Math.round(units * 1000.0))
    }
}
