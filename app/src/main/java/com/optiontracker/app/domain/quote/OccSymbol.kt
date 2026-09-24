package com.optiontracker.app.domain.quote

import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import java.time.LocalDate
import java.util.Locale

/**
 * Yahoo's option symbol is the OCC contract:
 * root + yyMMdd + C or P + strike in thousandths of a dollar, 8 digits.
 * `NVDA` 230.00 call on 2026-09-25 is `NVDA260925C00230000`.
 * Dots and hyphens are dropped from the root (`BRK.B` → `BRKB`).
 */
object OccSymbol {
    private const val MAX_THOUSANDTHS = 99_999_999L

    fun yahoo(ticker: String, type: OptionType, strikeCents: Long, expiry: LocalDate): String? {
        val root = quoteKey(ticker).replace(".", "").replace("-", "")
        if (root.isEmpty() || root.any { !it.isLetterOrDigit() }) return null
        if (strikeCents <= 0L) return null
        val thousandths = try {
            Math.multiplyExact(strikeCents, 10L)
        } catch (_: ArithmeticException) {
            return null
        }
        if (thousandths > MAX_THOUSANDTHS) return null
        val date = String.format(
            Locale.US,
            "%02d%02d%02d",
            expiry.year % 100,
            expiry.monthValue,
            expiry.dayOfMonth,
        )
        val right = if (type == OptionType.CALL) "C" else "P"
        val strike = String.format(Locale.US, "%08d", thousandths)
        return "$root$date$right$strike"
    }

    /** Entry premium stays visible. “Now” appears once a delayed premium is known or has failed. */
    fun premiumLine(position: Position, board: QuoteBoard): String {
        val entry = "entry ${Money.format(position.entryPremiumCents)}"
        val symbol = yahoo(position.ticker, position.type, position.strikeCents, position.expiry)
            ?: return entry
        val now = underlyingQuoteLabel(symbol, board)
        return when {
            now == "—" -> "$entry · now —"
            now.isEmpty() -> entry
            else -> "$entry · now $now"
        }
    }

    /** Delayed option premium in cents, or null while loading or after a failed quote. */
    fun currentPremiumCents(position: Position, board: QuoteBoard): Long? {
        val symbol = yahoo(position.ticker, position.type, position.strikeCents, position.expiry)
            ?: return null
        val now = underlyingQuoteLabel(symbol, board)
        if (now.isEmpty() || now == "—") return null
        return Money.parseRoundedCents(now)
    }
}
