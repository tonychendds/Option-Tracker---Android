package com.optiontracker.app.domain.validation

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionValidatorTest {
    @Test
    fun acceptsANormalEquityOption() {
        val errors = PositionValidator.validateEntry(
            ticker = "brk.b",
            strikeText = "450.50",
            expiry = LocalDate.of(2026, 10, 16),
            contractsText = "2",
            premiumText = "1.25",
            feesText = "",
            notes = "earnings",
        )
        assertTrue(errors.isEmpty())
        assertEquals(0L, PositionValidator.parseOptionalFees(""))
    }

    @Test
    fun rejectsMissingAndNonsenseFields() {
        val errors = PositionValidator.validateEntry(
            ticker = "",
            strikeText = "0",
            expiry = null,
            contractsText = "0",
            premiumText = "1.234",
            feesText = "-1",
            notes = "",
        )
        assertTrue(errors.containsKey(Fields.TICKER))
        assertTrue(errors.containsKey(Fields.STRIKE))
        assertTrue(errors.containsKey(Fields.EXPIRY))
        assertTrue(errors.containsKey(Fields.CONTRACTS))
        assertTrue(errors.containsKey(Fields.PREMIUM))
        assertTrue(errors.containsKey(Fields.FEES))
    }

    @Test
    fun closeRequiresExitPremium() {
        val missing = PositionValidator.validateClose("", "")
        assertTrue(missing.containsKey(Fields.EXIT_PREMIUM))
        val ok = PositionValidator.validateClose("0", "0.65")
        assertTrue(ok.isEmpty())
    }
}
