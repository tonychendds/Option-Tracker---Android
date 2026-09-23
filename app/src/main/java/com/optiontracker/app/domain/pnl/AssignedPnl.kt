package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.money.Money

object AssignedPnl {
    fun sharesForContracts(contracts: Int): Int {
        require(contracts >= 0) { "Contracts cannot be negative" }
        return Math.multiplyExact(contracts, OptionPnl.EQUITY_CONTRACT_MULTIPLIER)
    }

    /** (quote − cost basis) × shares, in cents. */
    fun unrealizedCents(quoteCents: Long, costBasisCents: Long, shares: Int): Long {
        require(shares >= 0) { "Shares cannot be negative" }
        val perShare = Math.subtractExact(quoteCents, costBasisCents)
        return Math.multiplyExact(perShare, shares.toLong())
    }

    fun unrealizedFromQuote(quoteLabel: String, costBasisCents: Long, shares: Int): Long? {
        if (quoteLabel.isBlank() || quoteLabel == "—") return null
        val quoteCents = Money.parseCents(quoteLabel) ?: return null
        return try {
            unrealizedCents(quoteCents, costBasisCents, shares)
        } catch (_: ArithmeticException) {
            null
        }
    }
}
