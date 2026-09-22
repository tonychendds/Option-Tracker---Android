package com.optiontracker.app.domain.ocr

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class BrokerDraft(
    val ticker: String,
    val side: OptionSide,
    val type: OptionType,
    val strikeText: String,
    val expiry: LocalDate,
    val contractsText: String,
    val premiumText: String,
    val feesText: String,
    val openedOn: LocalDate,
    val notes: String,
    val summary: String,
    val closingLeg: Boolean,
)

sealed interface BrokerParseResult {
    data class Ready(val draft: BrokerDraft) : BrokerParseResult
    data class Failed(val message: String) : BrokerParseResult
}

/**
 * Reads broker trade-details text, starting with Charles Schwab mobile web.
 * Premium is the per-share Price, not principal or total.
 * Fees are commission plus industry, regulatory, and similar fee lines.
 * A closing action is prefilled for review and is not applied as a close.
 */
object BrokerScreenshotParser {
    private val symbol = Regex(
        """(?i)\b([A-Z][A-Z0-9.\-]{0,9})\s+(\d{1,2}\s*/\s*\d{1,2}\s*/\s*\d{4}|\d{4}-\d{2}-\d{2})\s+\$?\s*(\d+(?:\.\d+)?)\s*([CP]|CALL|PUT)\b""",
    )
    private val action = Regex("""(?i)\b(buy|sell)\s+to\s+(open|close)\b""")
    private val quantity = Regex("""(?i)\b(?:Quantity|Qty)\b\s*:?\s*([\d,]+(?:\.0+)?)""")
    private val price = Regex("""(?i)\bPrice\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)""")
    private val tradeDate = Regex(
        """(?i)\bTrade\s+Date\b\s*:?\s*(\d{1,2}\s*/\s*\d{1,2}\s*/\s*\d{4}|\d{4}-\d{2}-\d{2})""",
    )
    private val orderId = Regex("""(?i)\bOrder\s*(?:Id|ID|#)\b\s*:?\s*(\d+)""")
    private val feeLabels = listOf(
        Regex("""(?i)\bCommission\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bIndustry\s+Fee\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bRegulatory\s+Fee\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bReg\s+Fee\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bTransaction\s+Fee\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bOCC\s+Fee\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)\bFees\b\s*:?\s*\$?\s*([\d,]+(?:\.\d+)?)"""),
    )
    private val usDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/uuuu")

    const val UNREADABLE = "Couldn't read a trade from that screenshot. Enter the position manually."

    fun parse(text: String): BrokerParseResult {
        val normalized = text
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t\\r\\n]+"), " ")
            .replace(Regex(" +"), " ")
            .trim()
        if (normalized.isEmpty()) {
            return BrokerParseResult.Failed("Couldn't read any text from that image. Enter the position manually.")
        }

        val symbolMatch = symbol.find(normalized)
        val actionMatch = action.find(normalized)
        val quantityMatch = quantity.find(normalized)
        val priceMatch = price.find(normalized)
        val dateMatch = tradeDate.find(normalized)

        val missing = buildList {
            if (symbolMatch == null) add("symbol")
            if (actionMatch == null) add("action")
            if (quantityMatch == null) add("quantity")
            if (priceMatch == null) add("price")
            if (dateMatch == null) add("trade date")
        }
        if (missing.isNotEmpty()) {
            return BrokerParseResult.Failed(
                "Couldn't read a trade from that screenshot. Missing ${missing.joinToString(", ")}. Enter the position manually.",
            )
        }

        val ticker = symbolMatch!!.groupValues[1].uppercase()
        if (!PositionValidator.tickerPattern.matches(ticker)) {
            return BrokerParseResult.Failed(UNREADABLE)
        }
        val expiry = parseDate(symbolMatch.groupValues[2]) ?: return BrokerParseResult.Failed(UNREADABLE)
        val strikeCents = Money.parseRoundedCents(symbolMatch.groupValues[3])
        if (strikeCents == null || strikeCents <= 0L) return BrokerParseResult.Failed(UNREADABLE)
        val type = when (symbolMatch.groupValues[4].uppercase()) {
            "C", "CALL" -> OptionType.CALL
            "P", "PUT" -> OptionType.PUT
            else -> return BrokerParseResult.Failed(UNREADABLE)
        }
        val side = if (actionMatch!!.groupValues[1].equals("sell", ignoreCase = true)) {
            OptionSide.SELL
        } else {
            OptionSide.BUY
        }
        val closing = actionMatch.groupValues[2].equals("close", ignoreCase = true)
        val contracts = quantityMatch!!.groupValues[1].replace(",", "").replace(Regex("""\.0+$"""), "").toIntOrNull()
        if (contracts == null || contracts !in 1..PositionValidator.MAX_CONTRACTS) {
            return BrokerParseResult.Failed(UNREADABLE)
        }
        val premiumCents = Money.parseRoundedCents(priceMatch!!.groupValues[1])
            ?: return BrokerParseResult.Failed(UNREADABLE)
        val openedOn = parseDate(dateMatch!!.groupValues[1]) ?: return BrokerParseResult.Failed(UNREADABLE)
        val feeParts = feeLabels.mapNotNull { pattern ->
            pattern.find(normalized)?.groupValues?.get(1)?.let(Money::parseRoundedCents)
        }
        val feesCents = feeParts.sum()
        val order = orderId.find(normalized)?.groupValues?.get(1)
        val notes = buildString {
            append(if (order == null) "Schwab" else "Schwab order $order")
            if (closing) {
                append("\nDetected as ")
                append(if (side == OptionSide.SELL) "Sell" else "Buy")
                append(" to Close. Saving adds a new open position and does not close an existing trade.")
            }
        }.take(PositionValidator.MAX_NOTES)

        val strikeText = Money.toInput(strikeCents)
        val sideWord = if (side == OptionSide.SELL) "Sell" else "Buy"
        val typeWord = if (type == OptionType.CALL) "Call" else "Put"
        val summary = buildString {
            append("Detected ")
            append(sideWord)
            if (closing) append(" to Close")
            append(" ")
            append(typeWord)
            append(" ")
            append(ticker)
            append(" ")
            append(strikeText)
            append(" expiring ")
            append(expiry)
            append(". Review the fields, then tap Save.")
            if (closing) {
                append(" Saving adds a new open position and does not close an existing trade.")
            }
        }

        return BrokerParseResult.Ready(
            BrokerDraft(
                ticker = ticker,
                side = side,
                type = type,
                strikeText = strikeText,
                expiry = expiry,
                contractsText = contracts.toString(),
                premiumText = Money.toInput(premiumCents),
                feesText = if (feeParts.isEmpty()) "" else Money.toInput(feesCents),
                openedOn = openedOn,
                notes = notes,
                summary = summary,
                closingLeg = closing,
            ),
        )
    }

    private fun parseDate(raw: String): LocalDate? {
        val cleaned = raw.replace(" ", "")
        return try {
            if (cleaned.contains("-")) {
                LocalDate.parse(cleaned)
            } else {
                LocalDate.parse(cleaned, usDate)
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
