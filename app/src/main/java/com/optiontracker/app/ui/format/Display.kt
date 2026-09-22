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

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

fun sideLabel(side: OptionSide): String = if (side == OptionSide.BUY) "Buy" else "Sell"

fun typeLabel(type: OptionType): String = if (type == OptionType.CALL) "Call" else "Put"

fun contractsLabel(count: Int): String = if (count == 1) "1 contract" else "$count contracts"

@Composable
fun pnlColor(cents: Long): Color = when {
    cents > 0L -> MaterialTheme.colorScheme.primary
    cents < 0L -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
