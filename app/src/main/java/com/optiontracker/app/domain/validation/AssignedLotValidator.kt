package com.optiontracker.app.domain.validation

import com.optiontracker.app.domain.model.AssignedLot
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.AssignedPnl
import com.optiontracker.app.domain.pnl.OptionPnl
import java.time.LocalDate

enum class ShareEntry {
    SHARES,
    CONTRACTS,
}

object AssignedLotValidator {
    const val MAX_SHARES = PositionValidator.MAX_CONTRACTS * OptionPnl.EQUITY_CONTRACT_MULTIPLIER

    fun validate(
        ticker: String,
        costBasisText: String,
        quantityText: String,
        entry: ShareEntry,
        assignedOn: LocalDate?,
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        val normalizedTicker = ticker.trim().uppercase()
        if (normalizedTicker.isEmpty()) {
            errors[Fields.TICKER] = "Enter a ticker"
        } else if (!PositionValidator.tickerPattern.matches(normalizedTicker)) {
            errors[Fields.TICKER] = "Use 1–10 characters: letters, digits, dot, or hyphen"
        }
        val cost = Money.parseCents(costBasisText)
        if (costBasisText.isBlank()) {
            errors[Fields.STRIKE] = "Enter the cost basis per share"
        } else if (cost == null || cost <= 0L) {
            errors[Fields.STRIKE] = "Enter a price greater than zero, up to 2 decimals"
        }
        val quantity = quantityText.trim().toIntOrNull()
        val shares = sharesFrom(quantity, entry)
        if (entry == ShareEntry.CONTRACTS) {
            if (quantity == null || quantity !in 1..PositionValidator.MAX_CONTRACTS) {
                errors[Fields.CONTRACTS] = "Enter 1 to ${PositionValidator.MAX_CONTRACTS} contracts"
            }
        } else if (quantity == null || shares == null) {
            errors[Fields.CONTRACTS] = "Enter 1 to $MAX_SHARES shares"
        }
        if (assignedOn == null) {
            errors[Fields.CLOSED_ON] = "Choose the assigned date"
        }
        return errors
    }

    fun sharesFrom(quantity: Int?, entry: ShareEntry): Int? {
        if (quantity == null || quantity <= 0) return null
        return try {
            val shares = if (entry == ShareEntry.CONTRACTS) {
                AssignedPnl.sharesForContracts(quantity)
            } else {
                quantity
            }
            if (shares !in 1..MAX_SHARES) null else shares
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun modelError(lot: AssignedLot): String? {
        if (!PositionValidator.tickerPattern.matches(lot.ticker)) return "Invalid ticker"
        if (lot.costBasisCents <= 0L || lot.costBasisCents > Money.MAX_CENTS) return "Invalid cost basis"
        if (lot.shares !in 1..MAX_SHARES) return "Invalid share count"
        return null
    }
}
