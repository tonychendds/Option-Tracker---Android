package com.optiontracker.app.domain.moneyness

import com.optiontracker.app.domain.model.OptionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneynessTest {
    private val strike = 10_000L

    @Test
    fun callIsItmAboveTheBandAndOtmBelowIt() {
        assertEquals(Moneyness.ITM, MoneynessClassifier.of(OptionType.CALL, strike, 10_051))
        assertEquals(Moneyness.OTM, MoneynessClassifier.of(OptionType.CALL, strike, 9_949))
    }

    @Test
    fun putIsItmBelowTheBandAndOtmAboveIt() {
        assertEquals(Moneyness.ITM, MoneynessClassifier.of(OptionType.PUT, strike, 9_949))
        assertEquals(Moneyness.OTM, MoneynessClassifier.of(OptionType.PUT, strike, 10_051))
    }

    @Test
    fun halfPercentBandAndExactStrikeAreAtm() {
        assertEquals(Moneyness.ATM, MoneynessClassifier.of(OptionType.CALL, strike, strike))
        assertEquals(Moneyness.ATM, MoneynessClassifier.of(OptionType.PUT, strike, strike))
        assertEquals(Moneyness.ATM, MoneynessClassifier.of(OptionType.CALL, strike, 10_050))
        assertEquals(Moneyness.ATM, MoneynessClassifier.of(OptionType.PUT, strike, 9_950))
        assertEquals(Moneyness.ATM, MoneynessClassifier.fromQuote(OptionType.CALL, strike, "$100.50"))
    }

    @Test
    fun missingQuoteIsNotClassified() {
        assertNull(MoneynessClassifier.fromQuote(OptionType.CALL, strike, ""))
        assertNull(MoneynessClassifier.fromQuote(OptionType.PUT, strike, "—"))
        assertNull(MoneynessClassifier.fromQuote(OptionType.CALL, strike, "n/a"))
        assertNull(MoneynessClassifier.of(OptionType.CALL, 0, 10_000))
    }
}
