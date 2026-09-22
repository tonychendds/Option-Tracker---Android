package com.optiontracker.app.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Money {
    private val amountPattern = Regex("""\d+(\.\d{1,2})?""")
    const val MAX_CENTS = 1_000_000_000L

    fun parseCents(raw: String): Long? {
        val cleaned = raw.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
        if (!amountPattern.matches(cleaned)) return null
        return centsFromDecimal(cleaned)
    }

    /** Dollars that may be negative, including a leading minus or accounting parentheses. */
    fun parseSignedCents(raw: String): Long? {
        var cleaned = raw.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        var negative = false
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1)
        if (cleaned.startsWith("-")) {
            negative = true
            cleaned = cleaned.substring(1)
        } else if (cleaned.startsWith("(") && cleaned.endsWith(")") && cleaned.length > 2) {
            negative = true
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }
        val cents = parseCents(cleaned) ?: return null
        return if (negative) -cents else cents
    }

    private fun centsFromDecimal(cleaned: String): Long? {
        if (!amountPattern.matches(cleaned)) return null
        return try {
            val cents = BigDecimal(cleaned)
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
            if (cents > MAX_CENTS) null else cents
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun format(cents: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    fun formatSigned(cents: Long): String {
        val formatted = format(cents)
        return if (cents > 0) "+$formatted" else formatted
    }

    fun toInput(cents: Long): String =
        BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString()
}
