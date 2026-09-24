package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus

/**
 * Equity-option profit and loss.
 *
 * Premiums are quoted per share. One standard equity contract covers 100 shares,
 * so cash notional is:
 *
 *     premium per share × contracts × 100
 *
 * Fees are the total commission for that leg, in cents, and always reduce P/L.
 *
 * Realized P/L when a position is closed:
 *
 *     Buy to open:  (exit notional − entry notional) − entry fees − exit fees
 *     Sell to open: (entry notional − exit notional) − entry fees − exit fees
 *
 * Closing does not ask for a side. A buy is closed by selling, and a sell is
 * closed by buying. Home still treats an open position as premium cash flow only.
 * Positions can show a separate unrealized mark from the delayed option premium.
 */
object OptionPnl {
    const val EQUITY_CONTRACT_MULTIPLIER = 100

    fun notionalCents(premiumPerShareCents: Long, contracts: Int): Long {
        require(premiumPerShareCents >= 0) { "Premium cannot be negative" }
        require(contracts >= 0) { "Contracts cannot be negative" }
        return Math.multiplyExact(
            Math.multiplyExact(premiumPerShareCents, contracts.toLong()),
            EQUITY_CONTRACT_MULTIPLIER.toLong(),
        )
    }

    /**
     * Cash effect of opening the trade. Credits are positive, debits are negative.
     * A buy pays the notional plus fees. A sell receives the notional minus fees.
     */
    fun entryCashFlowCents(
        side: OptionSide,
        premiumPerShareCents: Long,
        contracts: Int,
        feesCents: Long,
    ): Long {
        require(feesCents >= 0) { "Fees cannot be negative" }
        val notional = notionalCents(premiumPerShareCents, contracts)
        return when (side) {
            OptionSide.BUY -> Math.negateExact(Math.addExact(notional, feesCents))
            OptionSide.SELL -> Math.subtractExact(notional, feesCents)
        }
    }

    fun realizedPnlCents(
        side: OptionSide,
        contracts: Int,
        entryPremiumCents: Long,
        entryFeesCents: Long,
        exitPremiumCents: Long,
        exitFeesCents: Long,
    ): Long {
        require(entryFeesCents >= 0 && exitFeesCents >= 0) { "Fees cannot be negative" }
        val entryNotional = notionalCents(entryPremiumCents, contracts)
        val exitNotional = notionalCents(exitPremiumCents, contracts)
        val gross = when (side) {
            OptionSide.BUY -> Math.subtractExact(exitNotional, entryNotional)
            OptionSide.SELL -> Math.subtractExact(entryNotional, exitNotional)
        }
        return Math.subtractExact(Math.subtractExact(gross, entryFeesCents), exitFeesCents)
    }

    /**
     * Unrealized premium P/L for an open contract. Fees are not included.
     *
     * Short: (entry premium − current premium) × contracts × 100
     * Long:  (current premium − entry premium) × contracts × 100
     */
    fun unrealizedPremiumCents(
        side: OptionSide,
        contracts: Int,
        entryPremiumCents: Long,
        currentPremiumCents: Long,
    ): Long {
        val entry = notionalCents(entryPremiumCents, contracts)
        val current = notionalCents(currentPremiumCents, contracts)
        return when (side) {
            OptionSide.SELL -> Math.subtractExact(entry, current)
            OptionSide.BUY -> Math.subtractExact(current, entry)
        }
    }
}

fun Position.entryNotionalCents(): Long =
    OptionPnl.notionalCents(entryPremiumCents, contracts)

fun Position.entryCashFlowCents(): Long =
    OptionPnl.entryCashFlowCents(side, entryPremiumCents, contracts, entryFeesCents)

fun Position.exitNotionalCents(): Long? {
    val premium = exitPremiumCents ?: return null
    return OptionPnl.notionalCents(premium, contracts)
}

fun Position.realizedPnlCents(): Long? {
    if (status != PositionStatus.CLOSED) return null
    realizedOverrideCents?.let { return it }
    val exitPremium = exitPremiumCents ?: return null
    val exitFees = exitFeesCents ?: 0L
    return OptionPnl.realizedPnlCents(
        side = side,
        contracts = contracts,
        entryPremiumCents = entryPremiumCents,
        entryFeesCents = entryFeesCents,
        exitPremiumCents = exitPremium,
        exitFeesCents = exitFees,
    )
}
