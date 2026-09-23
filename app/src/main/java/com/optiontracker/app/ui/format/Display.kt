package com.optiontracker.app.ui.format

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

private val shortMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM", Locale.US)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

fun formatMonthShort(month: YearMonth): String = month.format(shortMonthFormatter)

fun formatHitRate(rate: Double?): String {
    if (rate == null) return "—"
    return "${round(rate * 100).toInt()}%"
}

fun sideLabel(side: OptionSide): String = if (side == OptionSide.BUY) "Buy" else "Sell"

/** Buy pays the premium. Sell receives it. The amount itself stays positive. */
fun premiumCashFlowLabel(side: OptionSide): String = if (side == OptionSide.BUY) "Debit" else "Credit"

fun premiumCashFlowHint(side: OptionSide): String = if (side == OptionSide.BUY) {
    "Debit — you pay this premium. Quoted per share. Total = premium × contracts × 100."
} else {
    "Credit — you receive this premium. Quoted per share. Total = premium × contracts × 100."
}

/** Closing pays or receives the opposite of the opening side. */
fun exitPremiumCashFlowLabel(openSide: OptionSide): String =
    if (openSide == OptionSide.BUY) "Credit" else "Debit"

fun exitPremiumCashFlowHint(openSide: OptionSide): String = if (openSide == OptionSide.BUY) {
    "Credit — you receive this premium when selling to close. Quoted per share. Total = premium × contracts × 100."
} else {
    "Debit — you pay this premium when buying to close. Quoted per share. Total = premium × contracts × 100."
}

fun typeLabel(type: OptionType): String = if (type == OptionType.CALL) "Call" else "Put"

fun contractsLabel(count: Int): String = if (count == 1) "1 contract" else "$count contracts"

@Composable
fun pnlColor(cents: Long): Color = when {
    cents > 0L -> MaterialTheme.colorScheme.primary
    cents < 0L -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
