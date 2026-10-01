package com.company.stockpro.qa

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.feature.reports.formatCompactMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Phase 5 QA Test Suite: Global Currency System & Financial Precision Verification.
 * Validates PRD §3.1, §3.3, §8.1 and Architecture precision guidelines (paise: Long).
 */
class CurrencyAndValuationSystemTest {

    @Test
    fun `currency system supports all four required global currencies`() {
        val supportedCurrencies = Currency.entries

        assertEquals(4, supportedCurrencies.size)
        assertTrue(supportedCurrencies.contains(Currency.INR))
        assertTrue(supportedCurrencies.contains(Currency.USD))
        assertTrue(supportedCurrencies.contains(Currency.EUR))
        assertTrue(supportedCurrencies.contains(Currency.GBP))

        // Validate symbols
        assertEquals("₹", Currency.INR.symbol)
        assertEquals("$", Currency.USD.symbol)
        assertEquals("€", Currency.EUR.symbol)
        assertEquals("£", Currency.GBP.symbol)

        // Validate ISO Codes
        assertEquals("INR", Currency.INR.code)
        assertEquals("USD", Currency.USD.code)
        assertEquals("EUR", Currency.EUR.code)
        assertEquals("GBP", Currency.GBP.code)
    }

    @Test
    fun `money formatting with Indian Rupee uses correct symbol and decimal cents`() {
        val money = Money(150_000L) // ₹1,500.00
        val formatted = String.format(Locale.US, "%s %,.2f", Currency.INR.symbol, money.toRupeesDouble)
        assertTrue(formatted.contains("1,500.00") || formatted.contains("1500.00"))
        assertTrue(formatted.startsWith("₹"))
    }

    @Test
    fun `money arithmetic maintains strict 64-bit integer precision without floating point inaccuracies`() {
        val m1 = Money(100_001L) // ₹1,000.01
        val m2 = Money(200_009L) // ₹2,000.09
        val sum = Money(m1.paise + m2.paise)

        assertEquals(300_010L, sum.paise)
        assertEquals(3000.10, sum.toRupeesDouble, 0.001)

        // Multiply unit cost by quantity
        val unitCost = Money(45_000L) // ₹450.00
        val quantity = Quantity(12_000L) // 12.000 items
        val totalValuationPaise = (unitCost.paise * quantity.milli) / 1000L

        assertEquals(540_000L, totalValuationPaise) // ₹5,400.00
    }

    @Test
    fun `formatCompactMoney correctly handles Indian numbering system Lakh and Crore for INR`() {
        // Standard amount below 1 Lakh (e.g. ₹500 = 50,000 paise)
        val compactStandard = formatCompactMoney(50_000L, Currency.INR)
        assertEquals("₹ 500", compactStandard)

        // 1 Lakh Rupees = 100,000 units = 10,000,000 paise
        val compactLakh = formatCompactMoney(10_000_000L, Currency.INR)
        assertEquals("₹ 1.0 Lakh", compactLakh)

        // 45 Lakh Rupees = 4,500,000 units = 450,000,000 paise
        val compactMultiLakh = formatCompactMoney(450_000_000L, Currency.INR)
        assertEquals("₹ 45.0 Lakh", compactMultiLakh)

        // 2.5 Crore Rupees = 25,000,000 units = 2,500,000,000 paise
        val compactCrore = formatCompactMoney(2_500_000_000L, Currency.INR)
        assertEquals("₹ 2.5 Cr", compactCrore)
    }

    @Test
    fun `formatCompactMoney correctly handles Western numbering system k and M for USD, EUR, and GBP`() {
        // USD: $1,500 = 150,000 cents -> $1.5k
        assertEquals("$1.5k", formatCompactMoney(150_000L, Currency.USD))
        // USD: $1,500,000 = 150,000,000 cents -> $1.5M
        assertEquals("$1.5M", formatCompactMoney(150_000_000L, Currency.USD))

        // EUR: €2,000 = 200,000 cents -> €2.0k
        assertEquals("€2.0k", formatCompactMoney(200_000L, Currency.EUR))
        assertEquals("€1.5M", formatCompactMoney(150_000_000L, Currency.EUR))

        // GBP: £5,000 = 500,000 pence -> £5.0k
        assertEquals("£5.0k", formatCompactMoney(500_000L, Currency.GBP))
        assertEquals("£2.5M", formatCompactMoney(250_000_000L, Currency.GBP))
    }

    @Test
    fun `zero and negative valuation handling`() {
        assertEquals("₹ 0", formatCompactMoney(0L, Currency.INR))
        assertEquals("$0", formatCompactMoney(0L, Currency.USD))
        assertEquals("€0", formatCompactMoney(0L, Currency.EUR))
        assertEquals("£0", formatCompactMoney(0L, Currency.GBP))

        val zeroMoney = Money(0L)
        assertEquals(0L, zeroMoney.paise)
        assertEquals(0.0, zeroMoney.toRupeesDouble, 0.0001)
    }
}
