package com.company.stockpro.core.domain.model

@JvmInline
value class Quantity(val milli: Long) {
    operator fun plus(other: Quantity): Quantity = Quantity(milli + other.milli)
    operator fun minus(other: Quantity): Quantity = Quantity(milli - other.milli)
    operator fun compareTo(other: Quantity): Int = milli.compareTo(other.milli)

    val toUnitsDouble: Double get() = milli / 1000.0

    companion object {
        val ZERO = Quantity(0L)
        fun fromUnits(units: Long): Quantity = Quantity(units * 1000L)
    }
}
