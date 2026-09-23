package com.optiontracker.app.domain.moneyness

import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.money.Money

enum class Moneyness {
    ITM,
    OTM,
    ATM,
}

/**
 * Equity-option moneyness from a delayed underlying price S and strike K.
 *
 * ATM when the prices are equal or within 0.5% of the strike:
 * `|S − K| / K ≤ 0.005`. On a $100 strike that is $99.50 through $100.50.
 * Compared in cents. A missing or unreadable quote is not classified.
 */
object MoneynessClassifier {
    private const val ATM_PARTS_PER_WHOLE = 200L

    fun of(type: OptionType, strikeCents: Long, spotCents: Long): Moneyness? {
        if (strikeCents <= 0L || spotCents < 0L) return null
        val distance = if (spotCents >= strikeCents) spotCents - strikeCents else strikeCents - spotCents
        if (distance <= Long.MAX_VALUE / ATM_PARTS_PER_WHOLE && distance * ATM_PARTS_PER_WHOLE <= strikeCents) {
            return Moneyness.ATM
        }
        val aboveStrike = spotCents > strikeCents
        return when (type) {
            OptionType.CALL -> if (aboveStrike) Moneyness.ITM else Moneyness.OTM
            OptionType.PUT -> if (aboveStrike) Moneyness.OTM else Moneyness.ITM
        }
    }

    fun fromQuote(type: OptionType, strikeCents: Long, quoteLabel: String): Moneyness? {
        if (quoteLabel.isBlank() || quoteLabel == "—") return null
        val spotCents = Money.parseCents(quoteLabel) ?: return null
        return of(type, strikeCents, spotCents)
    }
}
