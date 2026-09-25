package com.company.stockpro.core.domain.model

@JvmInline
value class Money(val paise: Long) {
    operator fun plus(other: Money): Money = Money(paise + other.paise)
    operator fun minus(other: Money): Money = Money(paise - other.paise)
    operator fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    val toRupeesDouble: Double get() = paise / 100.0

    companion object {
        val ZERO = Money(0L)
        fun fromRupees(rupees: Long): Money = Money(rupees * 100L)
    }
}
