package com.optiontracker.app.domain.model

import java.time.LocalDate

enum class OptionSide {
    BUY,
    SELL,
}

enum class OptionType {
    CALL,
    PUT,
}

enum class PositionStatus {
    OPEN,
    CLOSED,
}

/**
 * A single equity option position entered by hand.
 *
 * Premiums are stored as US cents per share. The contract notional is
 * premium × contracts × 100. See [com.optiontracker.app.domain.pnl.OptionPnl].
 */
data class Position(
    val id: Long,
    val ticker: String,
    val side: OptionSide,
    val type: OptionType,
    val strikeCents: Long,
    val expiry: LocalDate,
    val contracts: Int,
    val entryPremiumCents: Long,
    val entryFeesCents: Long,
    val openedOn: LocalDate,
    val notes: String,
    val status: PositionStatus,
    val exitPremiumCents: Long?,
    val exitFeesCents: Long?,
    val closedOn: LocalDate?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val account: String = "",
    val realizedOverrideCents: Long? = null,
)
