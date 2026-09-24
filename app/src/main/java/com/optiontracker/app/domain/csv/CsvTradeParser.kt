package com.optiontracker.app.domain.csv

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.OptionPnl
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

data class CsvImportResult(
    val positions: List<Position>,
    val openCount: Int,
    val closedCount: Int,
    val skippedCount: Int,
    val errors: List<String>,
    val fileError: String? = null,
) {
    val replacedExisting: Boolean get() = fileError == null && positions.isNotEmpty()

    companion object {
        fun unreadable(message: String) = CsvImportResult(
            positions = emptyList(),
            openCount = 0,
            closedCount = 0,
            skippedCount = 0,
            errors = emptyList(),
            fileError = message,
        )
    }
}

/**
 * Parses a Google Sheet CSV export.
 *
 * Closed rows use `closeDate` when that column is present and filled. Files from
 * before that column existed, and rows that leave it blank, still use the
 * expiration date as the close date. The single `fees` column is stored as
 * opening fees and exit fees are zero, so a calculated P/L subtracts that amount
 * once. An export writes opening fees plus closing fees into that same column.
 *
 * When `realizedOverride` is present on a closed row, that dollar amount is the
 * realized P/L shown in the app. Entry and exit premiums are still stored, but
 * they are not used for the displayed result.
 *
 * Strike, premiums, fees, and the override may have more than two decimal places.
 * Those amounts are rounded half-up to the nearest cent. A blank account is allowed.
 */
object CsvTradeParser {
    val requiredColumns = listOf(
        "status",
        "account",
        "ticker",
        "side",
        "right",
        "strike",
        "opendate",
        "expdate",
        "contracts",
        "entrypremium",
        "exitpremium",
        "fees",
        "notes",
        "realizedoverride",
    )

    fun parse(text: String): CsvImportResult {
        val records = parseRecords(text.removePrefix("\uFEFF"))
        if (records.isEmpty() || records.first().all { it.isBlank() }) {
            return CsvImportResult.unreadable("The file needs a header row.")
        }
        val header = records.first().map { normalizeHeader(it) }
        val missing = requiredColumns.filter { it !in header }
        if (missing.isNotEmpty()) {
            return CsvImportResult.unreadable(
                "Missing column${if (missing.size == 1) "" else "s"}: ${missing.joinToString(", ") { columnLabel(it) }}.",
            )
        }
        val trackedColumns = requiredColumns + "closedate"
        if (header.filter { it in trackedColumns }.groupingBy { it }.eachCount().any { it.value > 1 }) {
            return CsvImportResult.unreadable("The header row repeats a column name.")
        }
        val index = header.withIndex().associate { it.value to it.index }
        val positions = mutableListOf<Position>()
        val errors = mutableListOf<String>()
        var skipped = 0
        records.drop(1).forEachIndexed { offset, cells ->
            val rowNumber = offset + 2
            if (cells.all { it.isBlank() }) return@forEachIndexed
            when (val mapped = mapRow(cells, index)) {
                is RowMapping.Ok -> positions += mapped.position
                is RowMapping.Bad -> {
                    skipped += 1
                    errors += "Row $rowNumber: ${mapped.message}"
                }
            }
        }
        return CsvImportResult(
            positions = positions,
            openCount = positions.count { it.status == PositionStatus.OPEN },
            closedCount = positions.count { it.status == PositionStatus.CLOSED },
            skippedCount = skipped,
            errors = errors,
        )
    }

    private fun mapRow(cells: List<String>, index: Map<String, Int>): RowMapping {
        fun cell(name: String): String = cells.getOrNull(index.getValue(name))?.trim().orEmpty()

        val status = when (cell("status").lowercase(Locale.US)) {
            "open" -> PositionStatus.OPEN
            "closed" -> PositionStatus.CLOSED
            else -> return RowMapping.Bad("Status must be Open or Closed")
        }
        val account = cell("account")
        if (account.length > PositionValidator.MAX_ACCOUNT || account.any { it.isISOControl() }) {
            return RowMapping.Bad("Account must be ${PositionValidator.MAX_ACCOUNT} characters or fewer")
        }
        val ticker = cell("ticker").uppercase(Locale.US)
        if (!PositionValidator.tickerPattern.matches(ticker)) {
            return RowMapping.Bad("Enter a ticker such as AAPL or BRK.B")
        }
        val side = when (cell("side").lowercase(Locale.US)) {
            "buy" -> OptionSide.BUY
            "sell" -> OptionSide.SELL
            else -> return RowMapping.Bad("Side must be Buy or Sell")
        }
        val type = when (cell("right").lowercase(Locale.US)) {
            "call" -> OptionType.CALL
            "put" -> OptionType.PUT
            else -> return RowMapping.Bad("Right must be Call or Put")
        }
        val strike = Money.parseRoundedCents(cell("strike"))
        if (strike == null || strike <= 0L) {
            return RowMapping.Bad("Strike must be a positive amount")
        }
        val openedOn = parseDate(cell("opendate")) ?: return RowMapping.Bad("Open date must be yyyy-MM-dd")
        val expiry = parseDate(cell("expdate")) ?: return RowMapping.Bad("Expiration must be yyyy-MM-dd")
        val contracts = cell("contracts").toIntOrNull()
        if (contracts == null || contracts !in 1..PositionValidator.MAX_CONTRACTS) {
            return RowMapping.Bad("Contracts must be a whole number from 1 to ${PositionValidator.MAX_CONTRACTS}")
        }
        val entryPremium = Money.parseRoundedCents(cell("entrypremium"))
        if (entryPremium == null) {
            return RowMapping.Bad("Entry premium must be a non-negative amount")
        }
        val feesText = cell("fees")
        val fees = if (feesText.isEmpty()) 0L else Money.parseRoundedCents(feesText)
        if (fees == null) return RowMapping.Bad("Fees must be a non-negative amount")
        val notes = cell("notes")
        if (notes.length > PositionValidator.MAX_NOTES) {
            return RowMapping.Bad("Notes must be ${PositionValidator.MAX_NOTES} characters or fewer")
        }
        val overrideText = cell("realizedoverride")
        val realizedOverride = if (overrideText.isEmpty()) {
            null
        } else {
            Money.parseSignedRoundedCents(overrideText) ?: return RowMapping.Bad("Realized P/L must be a dollar amount")
        }
        val exitText = cell("exitpremium")
        val exitPremium = if (exitText.isEmpty()) {
            null
        } else {
            Money.parseRoundedCents(exitText) ?: return RowMapping.Bad("Exit premium must be a non-negative amount")
        }
        if (status == PositionStatus.CLOSED && exitPremium == null && realizedOverride == null) {
            return RowMapping.Bad("A closed trade needs an exit premium or a realized P/L")
        }
        try {
            OptionPnl.notionalCents(entryPremium, contracts)
            if (exitPremium != null) OptionPnl.notionalCents(exitPremium, contracts)
        } catch (_: ArithmeticException) {
            return RowMapping.Bad("Premium and contract count are too large")
        }
        val closed = status == PositionStatus.CLOSED
        val closedOn = if (!closed) {
            null
        } else {
            val closeIndex = index["closedate"]
            val closeText = if (closeIndex == null) "" else cells.getOrNull(closeIndex)?.trim().orEmpty()
            when {
                closeText.isEmpty() -> expiry
                else -> parseDate(closeText) ?: return RowMapping.Bad("Close date must be yyyy-MM-dd")
            }
        }
        return RowMapping.Ok(
            Position(
                id = 0L,
                ticker = ticker,
                side = side,
                type = type,
                strikeCents = strike,
                expiry = expiry,
                contracts = contracts,
                entryPremiumCents = entryPremium,
                entryFeesCents = fees,
                openedOn = openedOn,
                notes = notes,
                status = status,
                exitPremiumCents = if (closed) exitPremium else null,
                exitFeesCents = if (closed) 0L else null,
                closedOn = closedOn,
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
                account = account,
                realizedOverrideCents = if (closed) realizedOverride else null,
            ),
        )
    }

    private fun parseDate(raw: String): LocalDate? {
        if (raw.isEmpty()) return null
        return try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeHeader(raw: String): String =
        raw.trim().removePrefix("\uFEFF").lowercase(Locale.US).replace(" ", "")

    private fun columnLabel(name: String): String = when (name) {
        "opendate" -> "openDate"
        "expdate" -> "expDate"
        "closedate" -> "closeDate"
        "entrypremium" -> "entryPremium"
        "exitpremium" -> "exitPremium"
        "realizedoverride" -> "realizedOverride"
        else -> name
    }

    private sealed interface RowMapping {
        data class Ok(val position: Position) : RowMapping
        data class Bad(val message: String) : RowMapping
    }

    internal fun parseRecords(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val character = text[index]
            when {
                inQuotes && character == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index += 2
                }
                character == '"' -> {
                    inQuotes = !inQuotes
                    index += 1
                }
                character == ',' && !inQuotes -> {
                    row.add(field.toString())
                    field.clear()
                    index += 1
                }
                (character == '\n' || character == '\r') && !inQuotes -> {
                    if (character == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index += 1
                    }
                    row.add(field.toString())
                    field.clear()
                    rows.add(row)
                    row = mutableListOf()
                    index += 1
                }
                else -> {
                    field.append(character)
                    index += 1
                }
            }
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
