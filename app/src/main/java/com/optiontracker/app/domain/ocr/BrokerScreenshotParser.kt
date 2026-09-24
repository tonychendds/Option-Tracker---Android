package com.optiontracker.app.domain.ocr

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

data class BrokerDraft(
    val ticker: String,
    val side: OptionSide?,
    val type: OptionType,
    val strikeText: String,
    val expiry: LocalDate,
    val contractsText: String,
    val premiumText: String,
    val feesText: String,
    val openedOn: LocalDate?,
    val notes: String,
    val summary: String,
    val closingLeg: Boolean,
    val missingFields: List<String> = emptyList(),
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
 *
 * Labels and values may sit on one line, on adjacent lines, or in two columns
 * whose text arrives as a block of labels followed by a block of values.
 * When the symbol line is readable, whatever else was found is returned for
 * review and [BrokerDraft.missingFields] names the gaps.
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
    private val tokenPattern = Regex(
        """\d{1,2}/\d{1,2}/\d{4}|\d{4}-\d{2}-\d{2}|\$[\d,]+(?:\.\d+)?|\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d+(?:\.\d+)?|[A-Za-z]+|#""",
    )
    private val usDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/uuuu")

    const val UNREADABLE = "Couldn't read a trade from that screenshot. Enter the position manually."
    private const val NET_TOLERANCE_CENTS = 5L

    fun parse(text: String): BrokerParseResult {
        val flattened = text
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t\\r]+"), " ")
            .trim()
        if (flattened.isEmpty()) {
            return BrokerParseResult.Failed("Couldn't read any text from that image. Enter the position manually.")
        }
        val normalized = flattened
            .replace(Regex("[\\n]+"), " ")
            .replace(Regex(" +"), " ")
            .trim()

        val symbolMatch = symbol.find(normalized)
            ?: return BrokerParseResult.Failed(
                "Couldn't read a trade from that screenshot. Missing symbol. Enter the position manually.",
            )
        val ticker = symbolMatch.groupValues[1].uppercase()
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

        val actionMatch = action.find(normalized)
        val quantityMatch = quantity.find(normalized)
        val priceMatch = price.find(normalized)
        val dateMatch = tradeDate.find(normalized)
        val labeledOrder = orderId.find(normalized)?.groupValues?.get(1)
        val labeledFees = feeLabels.mapNotNull { pattern ->
            pattern.find(normalized)?.groupValues?.get(1)?.let(Money::parseRoundedCents)
        }
        val recovered = recoverValues(normalized, symbolMatch.range)

        val side = when {
            actionMatch != null && actionMatch.groupValues[1].equals("sell", ignoreCase = true) -> OptionSide.SELL
            actionMatch != null -> OptionSide.BUY
            else -> recovered.side
        }
        val closing = when {
            actionMatch != null -> actionMatch.groupValues[2].equals("close", ignoreCase = true)
            else -> recovered.closing
        }
        val contracts = quantityMatch?.groupValues?.get(1)
            ?.replace(",", "")
            ?.replace(Regex("""\.0+$"""), "")
            ?.toIntOrNull()
            ?.takeIf { it in 1..PositionValidator.MAX_CONTRACTS }
            ?: recovered.contracts?.takeIf { it in 1..PositionValidator.MAX_CONTRACTS }
        val premiumCents = priceMatch?.groupValues?.get(1)?.let(Money::parseRoundedCents)
            ?: recovered.premiumCents
        val openedOn = dateMatch?.groupValues?.get(1)?.let(::parseDate) ?: recovered.openedOn
        val columnFees = if (contracts != null && premiumCents != null) {
            feesAfterPrincipal(recovered.monies, premiumCents * contracts.toLong() * 100L)
        } else {
            null
        }
        val feesCents = columnFees ?: labeledFees.takeIf { it.isNotEmpty() }?.sum()
        val order = labeledOrder ?: recovered.orderId

        val missing = buildList {
            if (side == null) add("action")
            if (contracts == null) add("quantity")
            if (premiumCents == null) add("price")
            if (openedOn == null) add("trade date")
        }

        val notes = buildString {
            append(if (order == null) "Schwab" else "Schwab order $order")
            if (closing && side != null) {
                append("\nDetected as ")
                append(if (side == OptionSide.SELL) "Sell" else "Buy")
                append(" to Close. Saving adds a new open position and does not close an existing trade.")
            }
        }.take(PositionValidator.MAX_NOTES)

        val strikeText = Money.toInput(strikeCents)
        val typeWord = if (type == OptionType.CALL) "Call" else "Put"
        val summary = buildString {
            append("Detected ")
            if (side == OptionSide.SELL) append("Sell ")
            if (side == OptionSide.BUY) append("Buy ")
            if (closing && side != null) append("to Close ")
            append(typeWord)
            append(" ")
            append(ticker)
            append(" ")
            append(strikeText)
            append(" expiring ")
            append(expiry)
            append(". ")
            if (missing.isEmpty()) {
                append("Review the fields, then tap Save.")
                if (closing) {
                    append(" Saving adds a new open position and does not close an existing trade.")
                }
            } else {
                append("Missing ")
                append(missing.joinToString(", "))
                append(". Fill those in, then tap Save.")
            }
        }

        return BrokerParseResult.Ready(
            BrokerDraft(
                ticker = ticker,
                side = side,
                type = type,
                strikeText = strikeText,
                expiry = expiry,
                contractsText = contracts?.toString().orEmpty(),
                premiumText = premiumCents?.let(Money::toInput).orEmpty(),
                feesText = if (feesCents == null || feesCents == 0L) "" else Money.toInput(feesCents),
                openedOn = openedOn,
                notes = notes,
                summary = summary,
                closingLeg = closing && side != null,
                missingFields = missing,
            ),
        )
    }

    private data class RecoveredValues(
        val side: OptionSide?,
        val closing: Boolean,
        val contracts: Int?,
        val premiumCents: Long?,
        val monies: List<Long>,
        val openedOn: LocalDate?,
        val orderId: String?,
    )

    /**
     * Walks tokens that are not part of the symbol line.
     * Schwab's value column is a date, a date, an order id, an action, a
     * contract count, then Price, Principal, fee lines, and Total.
     */
    private fun recoverValues(text: String, symbolSpan: IntRange): RecoveredValues {
        val masked = text.replaceRange(symbolSpan, " ".repeat(symbolSpan.last - symbolSpan.first + 1))
        val tokens = tokenPattern.findAll(masked).map { it.value }.toList()
        var side: OptionSide? = null
        var closing = false
        var openedOn: LocalDate? = null
        var order: String? = null
        var contracts: Int? = null
        var pendingInt: Int? = null
        val monies = mutableListOf<Long>()

        var index = 0
        while (index < tokens.size) {
            val actionAt = actionAt(tokens, index)
            if (actionAt != null) {
                if (side == null) {
                    side = actionAt.first
                    closing = actionAt.second
                }
                index += 3
                continue
            }
            val token = tokens[index]
            val date = if (token.contains('/') || token.contains('-')) parseDate(token) else null
            if (date != null) {
                if (openedOn == null) openedOn = date
                index++
                continue
            }
            val money = asMoneyCents(token)
            if (money != null) {
                if (contracts == null && pendingInt != null) contracts = pendingInt
                monies += money
                pendingInt = null
                index++
                continue
            }
            if (token.all(Char::isDigit) && token.length >= 8) {
                if (order == null) order = token
                index++
                continue
            }
            val whole = asWholeInt(token)
            if (whole != null && monies.isEmpty()) {
                pendingInt = whole
            }
            index++
        }

        return RecoveredValues(
            side = side,
            closing = closing,
            contracts = contracts,
            premiumCents = monies.firstOrNull(),
            monies = monies,
            openedOn = openedOn,
            orderId = order,
        )
    }

    private fun actionAt(tokens: List<String>, index: Int): Pair<OptionSide, Boolean>? {
        if (index + 2 >= tokens.size) return null
        val verb = tokens[index]
        val middle = tokens[index + 1]
        val effect = tokens[index + 2]
        if (!middle.equals("to", ignoreCase = true)) return null
        val side = when {
            verb.equals("sell", ignoreCase = true) -> OptionSide.SELL
            verb.equals("buy", ignoreCase = true) -> OptionSide.BUY
            else -> return null
        }
        val closing = when {
            effect.equals("close", ignoreCase = true) -> true
            effect.equals("open", ignoreCase = true) -> false
            else -> return null
        }
        return side to closing
    }

    /** Dollar amounts and numbers that include cents. A bare integer is not money. */
    private fun asMoneyCents(token: String): Long? {
        if (!token.contains('$') && !token.contains('.')) return null
        return Money.parseRoundedCents(token)
    }

    private fun asWholeInt(token: String): Int? {
        if (token.contains('$') || token.contains('.')) return null
        val digits = token.replace(",", "")
        if (digits.length >= 8 || digits.isEmpty() || !digits.all(Char::isDigit)) return null
        return digits.toIntOrNull()?.takeIf { it in 1..PositionValidator.MAX_CONTRACTS }
    }

    /**
     * Fee lines sit after Principal. The last amount is the net Total when it
     * matches principal minus those fees (a sell) or principal plus those fees (a buy).
     */
    private fun feesAfterPrincipal(monies: List<Long>, principal: Long): Long? {
        if (principal <= 0L) return null
        val principalIndex = monies.indexOfFirst { abs(it - principal) <= NET_TOLERANCE_CENTS }
        if (principalIndex < 0) return null
        val after = monies.drop(principalIndex + 1)
        if (after.isEmpty()) return 0L
        if (after.size == 1) {
            val only = after.first()
            if (abs(only - principal) <= NET_TOLERANCE_CENTS) return 0L
            return only
        }
        val middle = after.dropLast(1)
        val middleSum = middle.sum()
        val last = after.last()
        val sellNet = principal - middleSum
        val buyNet = principal + middleSum
        return if (abs(last - sellNet) <= NET_TOLERANCE_CENTS || abs(last - buyNet) <= NET_TOLERANCE_CENTS) {
            middleSum
        } else {
            after.sum()
        }
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
