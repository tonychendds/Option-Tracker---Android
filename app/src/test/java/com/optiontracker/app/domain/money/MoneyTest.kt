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
    fun formatsUsd() {
        assertEquals("$2.50", Money.format(250))
        assertEquals("-$1.30", Money.format(-130))
        assertEquals("+$148.00", Money.formatSigned(14_800))
        assertEquals("-$1.00", Money.formatSigned(-100))
        assertEquals("$0.00", Money.formatSigned(0))
    }
}
