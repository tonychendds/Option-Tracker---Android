package com.optiontracker.app.domain.validation

import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.OptionPnl
import java.time.LocalDate

object Fields {
    const val TICKER = "ticker"
    const val STRIKE = "strike"
    const val EXPIRY = "expiry"
    const val CONTRACTS = "contracts"
    const val PREMIUM = "premium"
    const val FEES = "fees"
    const val EXIT_PREMIUM = "exitPremium"
    const val EXIT_FEES = "exitFees"
    const val NOTES = "notes"
}

object PositionValidator {
    val tickerPattern = Regex("^[A-Z][A-Z0-9.\\-]{0,9}$")
    const val MAX_CONTRACTS = 100_000
    const val MAX_NOTES = 2_000

    fun validateEntry(
        ticker: String,
        strikeText: String,
        expiry: LocalDate?,
        contractsText: String,
        premiumText: String,
        feesText: String,
        notes: String,
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        val normalizedTicker = ticker.trim().uppercase()
        if (normalizedTicker.isEmpty()) {
            errors[Fields.TICKER] = "Enter a ticker"
        } else if (!tickerPattern.matches(normalizedTicker)) {
            errors[Fields.TICKER] = "Use 1–10 characters: letters, digits, dot, or hyphen"
        }
        val strike = Money.parseCents(strikeText)
        if (strikeText.isBlank()) {
            errors[Fields.STRIKE] = "Enter a strike"
        } else if (strike == null || strike <= 0L) {
            errors[Fields.STRIKE] = "Enter a strike greater than zero, up to 2 decimals"
        }
        if (expiry == null) {
            errors[Fields.EXPIRY] = "Choose an expiration date"
        }
        val contracts = contractsText.trim().toIntOrNull()
        if (contracts == null || contracts !in 1..MAX_CONTRACTS) {
            errors[Fields.CONTRACTS] = "Enter 1 to $MAX_CONTRACTS contracts"
        }
        val premium = Money.parseCents(premiumText)
        if (premiumText.isBlank()) {
            errors[Fields.PREMIUM] = "Enter the premium per share"
        } else if (premium == null) {
            errors[Fields.PREMIUM] = "Enter a premium with up to 2 decimals"
        }
        val fees = parseOptionalFees(feesText)
        if (fees == null) {
            errors[Fields.FEES] = "Enter fees with up to 2 decimals, or leave blank"
        }
        if (notes.length > MAX_NOTES) {
            errors[Fields.NOTES] = "Notes must be $MAX_NOTES characters or fewer"
        }
        if (errors.isEmpty() && premium != null && contracts != null && fees != null) {
            try {
                OptionPnl.notionalCents(premium, contracts)
            } catch (_: ArithmeticException) {
                errors[Fields.PREMIUM] = "That premium and contract count are too large"
            }
        }
        return errors
    }

    fun validateClose(exitPremiumText: String, exitFeesText: String): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        val premium = Money.parseCents(exitPremiumText)
        if (exitPremiumText.isBlank()) {
            errors[Fields.EXIT_PREMIUM] = "Enter the exit premium per share"
        } else if (premium == null) {
            errors[Fields.EXIT_PREMIUM] = "Enter an exit premium with up to 2 decimals"
        }
        if (parseOptionalFees(exitFeesText) == null) {
            errors[Fields.EXIT_FEES] = "Enter fees with up to 2 decimals, or leave blank"
        }
        return errors
    }

    fun modelError(position: Position): String? {
        if (!tickerPattern.matches(position.ticker)) return "Invalid ticker"
        if (position.strikeCents <= 0L || position.strikeCents > Money.MAX_CENTS) return "Invalid strike"
        if (position.contracts !in 1..MAX_CONTRACTS) return "Invalid contracts"
        if (position.entryPremiumCents < 0L || position.entryPremiumCents > Money.MAX_CENTS) {
            return "Invalid premium"
        }
        if (position.entryFeesCents < 0L || position.entryFeesCents > Money.MAX_CENTS) return "Invalid fees"
        if (position.notes.length > MAX_NOTES) return "Notes are too long"
        val exitPremium = position.exitPremiumCents
        if (exitPremium != null && (exitPremium < 0L || exitPremium > Money.MAX_CENTS)) {
            return "Invalid exit premium"
        }
        val exitFees = position.exitFeesCents
        if (exitFees != null && (exitFees < 0L || exitFees > Money.MAX_CENTS)) return "Invalid exit fees"
        return null
    }

    fun parseOptionalFees(text: String): Long? {
        if (text.isBlank()) return 0L
        return Money.parseCents(text)
    }
}
