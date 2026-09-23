package com.optiontracker.app.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesDollarsAndCents() {
        assertEquals(250L, Money.parseCents("2.50"))
        assertEquals(250L, Money.parseCents("$2.50"))
        assertEquals(120L, Money.parseCents("1.2"))
        assertEquals(100L, Money.parseCents("1"))
        assertEquals(125_000L, Money.parseCents("1,250.00"))
        assertEquals(0L, Money.parseCents("0"))
    }

    @Test
    fun rejectsBadMoney() {
        assertNull(Money.parseCents(""))
        assertNull(Money.parseCents("abc"))
        assertNull(Money.parseCents("-1"))
        assertNull(Money.parseCents("1.234"))
        assertNull(Money.parseCents("1.2.3"))
    }

    @Test
    fun parsesSignedDollars() {
        assertEquals(14_800L, Money.parseSignedCents("148.00"))
        assertEquals(14_800L, Money.parseSignedCents("+$148"))
        assertEquals(-5_000L, Money.parseSignedCents("-50"))
        assertEquals(-1_250L, Money.parseSignedCents("($12.50)"))
        assertNull(Money.parseSignedCents(""))
        assertNull(Money.parseSignedCents("nope"))
    }

    @Test
    fun roundsSpreadsheetPrecisionToCents() {
        assertEquals(1_100L, Money.parseRoundedCents("11.0"))
        assertEquals(0L, Money.parseRoundedCents("0.0000"))
        assertEquals(727L, Money.parseRoundedCents("7.2727"))
        assertEquals(5L, Money.parseRoundedCents("0.046"))
        assertEquals(2L, Money.parseRoundedCents("0.0202"))
        assertEquals(685_000L, Money.parseSignedRoundedCents("6850.00"))
        assertEquals(-500L, Money.parseSignedRoundedCents("-5.004"))
        assertNull(Money.parseRoundedCents(""))
        assertNull(Money.parseCents("7.2727"))
    }

    @Test
    fun formatsUsd() {
        assertEquals("$2.50", Money.format(250))
        assertEquals("-$1.30", Money.format(-130))
        assertEquals("+$148.00", Money.formatSigned(14_800))
        assertEquals("-$1.00", Money.formatSigned(-100))
        assertEquals("$0.00", Money.formatSigned(0))
    }

    @Test
    fun formatsChartScale() {
        assertEquals("$0", Money.formatChart(0))
        assertEquals("$0", Money.formatChart(40))
        assertEquals("+$60", Money.formatChart(6_000))
        assertEquals("+$148", Money.formatChart(14_800))
        assertEquals("+$2k", Money.formatChart(200_000))
        assertEquals("+$16.7k", Money.formatChart(1_665_199))
        assertEquals("+$129k", Money.formatChart(12_881_799))
        assertEquals("-$4.2k", Money.formatChart(-420_000))
        assertEquals("+$1.2M", Money.formatChart(120_000_000))
    }
}
