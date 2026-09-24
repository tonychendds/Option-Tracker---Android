package com.optiontracker.app.domain.duplicate

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import java.time.LocalDate
import kotlin.math.abs

/** Fields compared when deciding whether a new save repeats a stored trade. */
data class TradeIdentity(
    val ticker: String,
    val side: OptionSide,
    val type: OptionType,
    val strikeCents: Long,
    val expiry: LocalDate,
    val contracts: Int,
    val openedOn: LocalDate,
    val entryPremiumCents: Long,
    val notes: String,
)

data class DuplicateMatch(
    val existing: Position,
    val sameOrderId: Boolean,
    val orderId: String?,
)

/**
 * A trade is a duplicate when ticker, side, call/put, strike, expiration,
 * contracts, and open date match and the entry premium is within $0.01.
 * Open and closed trades both count. The same Schwab order id in the notes
 * is a stronger signal and matches even when another field differs.
 */
object DuplicateDetector {
    const val DIALOG_TITLE = "This looks like a trade you already have"
    private const val PREMIUM_TOLERANCE_CENTS = 1L
    private val orderId = Regex("""(?i)\bSchwab order\s+(\d+)\b""")

    fun find(
        candidate: TradeIdentity?,
        notes: String,
        existing: List<Position>,
        ignoreId: Long = 0L,
    ): DuplicateMatch? {
        val pool = existing.filter { ignoreId == 0L || it.id != ignoreId }
        if (pool.isEmpty()) return null
        val identity = candidate
        val keyMatches = if (identity == null) {
            emptyList()
        } else {
            pool.filter { matchesKey(identity, it) }
        }
        val candidateOrder = schwabOrderId(if (identity != null) identity.notes else notes)
        val chosen = if (keyMatches.isNotEmpty()) {
            keyMatches.sortedWith(compareBy<Position>({ statusRank(it) }, { orderRank(it, candidateOrder) }, { it.id })).first()
        } else if (candidateOrder != null) {
            pool.filter { schwabOrderId(it.notes) == candidateOrder }
                .sortedWith(compareBy<Position>({ statusRank(it) }, { it.id }))
                .firstOrNull()
        } else {
            null
        } ?: return null
        val matchedOrder = schwabOrderId(chosen.notes)
        val sameOrder = candidateOrder != null && candidateOrder == matchedOrder
        return DuplicateMatch(
            existing = chosen,
            sameOrderId = sameOrder,
            orderId = if (sameOrder) candidateOrder else null,
        )
    }

    fun matchesKey(candidate: TradeIdentity, existing: Position): Boolean {
        if (!candidate.ticker.trim().equals(existing.ticker.trim(), ignoreCase = true)) return false
        if (candidate.side != existing.side) return false
        if (candidate.type != existing.type) return false
        if (candidate.strikeCents != existing.strikeCents) return false
        if (candidate.expiry != existing.expiry) return false
        if (candidate.contracts != existing.contracts) return false
        if (candidate.openedOn != existing.openedOn) return false
        return abs(candidate.entryPremiumCents - existing.entryPremiumCents) <= PREMIUM_TOLERANCE_CENTS
    }

    fun summary(match: DuplicateMatch): String {
        val position = match.existing
        val status = if (position.status == PositionStatus.OPEN) "an open position" else "a closed position"
        val contracts = if (position.contracts == 1) "1 contract" else "${position.contracts} contracts"
        val base = buildString {
            append(sideWord(position.side))
            append(" ")
            append(typeWord(position.type))
            append(" ")
            append(position.ticker)
            append(" ")
            append(Money.format(position.strikeCents))
            append(", ")
            append(contracts)
            append(", opened ")
            append(position.openedOn)
            append(", expiring ")
            append(position.expiry)
            append(". Already saved as ")
            append(status)
            append(".")
        }
        val order = match.orderId
        return if (match.sameOrderId && order != null) "$base Same Schwab order $order." else base
    }

    fun banner(match: DuplicateMatch): String {
        val lead = if (match.existing.status == PositionStatus.OPEN) {
            "Possible duplicate of an open position"
        } else {
            "Possible duplicate of a closed position"
        }
        val position = match.existing
        return "$lead. ${sideWord(position.side)} ${typeWord(position.type)} ${position.ticker} ${Money.format(position.strikeCents)}, opened ${position.openedOn}."
    }

    fun schwabOrderId(notes: String): String? = orderId.find(notes)?.groupValues?.get(1)

    private fun statusRank(position: Position): Int =
        if (position.status == PositionStatus.OPEN) 0 else 1

    private fun orderRank(position: Position, candidateOrder: String?): Int =
        if (candidateOrder != null && schwabOrderId(position.notes) == candidateOrder) 0 else 1

    private fun sideWord(side: OptionSide): String = if (side == OptionSide.BUY) "Buy" else "Sell"

    private fun typeWord(type: OptionType): String = if (type == OptionType.CALL) "Call" else "Put"
}
