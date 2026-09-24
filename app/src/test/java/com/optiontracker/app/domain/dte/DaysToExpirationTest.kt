package com.optiontracker.app.domain.dte

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DaysToExpirationTest {
    private val today = LocalDate.of(2026, 9, 24)

    @Test
    fun expirationDayIsZero() {
        assertEquals(0L, daysToExpiration(today, today))
        assertEquals("0d", dteLabel(0))
        assertEquals("Expires today", dteDescription(0))
        assertEquals(DteTone.URGENT, dteTone(0))
    }

    @Test
    fun tomorrowIsOneDay() {
        assertEquals(1L, daysToExpiration(today, today.plusDays(1)))
        assertEquals("1d", dteLabel(1))
        assertEquals("1 day to expiration", dteDescription(1))
        assertEquals(DteTone.URGENT, dteTone(1))
    }

    @Test
    fun laterDatesCountCalendarDays() {
        assertEquals(2L, daysToExpiration(today, LocalDate.of(2026, 9, 26)))
        assertEquals(14L, daysToExpiration(today, LocalDate.of(2026, 10, 8)))
        assertEquals(1L, daysToExpiration(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1)))
        assertEquals("14d", dteLabel(14))
        assertEquals("14 days to expiration", dteDescription(14))
        assertEquals(DteTone.SOON, dteTone(2))
        assertEquals(DteTone.SOON, dteTone(7))
        assertEquals(DteTone.NEUTRAL, dteTone(8))
    }

    @Test
    fun pastExpiryIsExpired() {
        assertEquals(-1L, daysToExpiration(today, today.minusDays(1)))
        assertEquals(-10L, daysToExpiration(today, LocalDate.of(2026, 9, 14)))
        assertEquals(EXPIRED_LABEL, dteLabel(-1))
        assertEquals(EXPIRED_LABEL, dteLabel(-10))
        assertEquals(EXPIRED_LABEL, dteDescription(-1))
        assertEquals(DteTone.URGENT, dteTone(-1))
    }
}
