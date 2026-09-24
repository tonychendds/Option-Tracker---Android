package com.optiontracker.app.domain.csv

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import java.time.LocalDate
import kotlin.math.abs

/**
 * Writes every option trade, open and closed, as CSV.
 *
 * Premiums are per share and never negative. `fees` is opening fees plus closing
 * fees. `realizedOverride` is dollars when the closed trade has one, and blank
 * otherwise. `closeDate` is filled for closed trades and blank for open trades.
 * Assigned stock lots are not included.
 */
object CsvTradeWriter {
    const val HEADER =
        "status,account,ticker,side,right,strike,openDate,expDate,closeDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride"

    fun suggestedFileName(today: LocalDate): String = "option-tracker-trades-$today.csv"

    fun write(positions: List<Position>): String {
        val body = positions.joinToString(separator = "\n") { csvRow(it) }
        return if (body.isEmpty()) "$HEADER\n" else "$HEADER\n$body\n"
    }

    private fun csvRow(position: Position): String {
        val closed = position.status == PositionStatus.CLOSED
        val fees = position.entryFeesCents + if (closed) position.exitFeesCents ?: 0L else 0L
        val cells = listOf(
            if (closed) "Closed" else "Open",
            position.account,
            position.ticker,
            if (position.side == OptionSide.BUY) "Buy" else "Sell",
            if (position.type == OptionType.CALL) "Call" else "Put",
            Money.toInput(position.strikeCents),
            position.openedOn.toString(),
            position.expiry.toString(),
            if (closed) (position.closedOn ?: position.expiry).toString() else "",
            position.contracts.toString(),
            Money.toInput(position.entryPremiumCents),
            if (closed && position.exitPremiumCents != null) Money.toInput(position.exitPremiumCents) else "",
            Money.toInput(fees),
            position.notes,
            if (closed && position.realizedOverrideCents != null) signedDollars(position.realizedOverrideCents) else "",
        )
        return cells.joinToString(separator = ",") { csvField(it) }
    }

    private fun signedDollars(cents: Long): String {
        val body = Money.toInput(abs(cents))
        return if (cents < 0) "-$body" else body
    }

    private fun csvField(value: String): String {
        val quote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!quote) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
