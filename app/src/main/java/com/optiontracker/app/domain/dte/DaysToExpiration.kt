package com.optiontracker.app.domain.dte

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calendar days from [today] until [expiry].
 *
 * This is the usual options DTE: whole calendar days, not trading sessions.
 * The expiration date itself is **0** (`0d` / expires today). Tomorrow is 1.
 * A date before [today] is negative; the chip shows [EXPIRED_LABEL] instead of
 * a negative count.
 */
fun daysToExpiration(today: LocalDate, expiry: LocalDate): Long =
    ChronoUnit.DAYS.between(today, expiry)

const val EXPIRED_LABEL = "Expired"

/** Compact chip text: `14d`, `1d`, `0d`, or [EXPIRED_LABEL]. */
fun dteLabel(days: Long): String = if (days < 0) EXPIRED_LABEL else "${days}d"

/** Spoken form of [dteLabel] for the same day count. */
fun dteDescription(days: Long): String = when {
    days < 0 -> EXPIRED_LABEL
    days == 0L -> "Expires today"
    days == 1L -> "1 day to expiration"
    else -> "$days days to expiration"
}

/**
 * Grey until the contract is inside a week. Amber from 7 through 2 days.
 * Red on expiration day, the day before, and after expiry.
 */
enum class DteTone {
    NEUTRAL,
    SOON,
    URGENT,
}

fun dteTone(days: Long): DteTone = when {
    days <= 1L -> DteTone.URGENT
    days <= 7L -> DteTone.SOON
    else -> DteTone.NEUTRAL
}
