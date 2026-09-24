package com.optiontracker.app.domain.duplicate

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {
    @Test
    fun exactMatchIsADuplicate() {
        val match = find(identity(), stored())
        assertNotNull(match)
        assertFalse(match!!.sameOrderId)
        assertTrue(DuplicateDetector.summary(match).contains("an open position"))
        assertTrue(DuplicateDetector.banner(match).startsWith("Possible duplicate of an open position"))
        assertEquals("This looks like a trade you already have", DuplicateDetector.DIALOG_TITLE)
    }

    @Test
    fun premiumWithinOneCentIsADuplicate() {
        assertNotNull(find(identity(premium = 12), stored(premium = 11)))
        assertNotNull(find(identity(premium = 10), stored(premium = 11)))
    }

    @Test
    fun premiumTwoCentsAwayIsNotADuplicate() {
        assertNull(find(identity(premium = 13), stored(premium = 11)))
    }

    @Test
    fun differentStrikeIsNotADuplicate() {
        assertNull(find(identity(strikeCents = 1_200, notes = ""), stored(notes = "")))
    }

    @Test
    fun differentOpenDateIsNotADuplicate() {
        assertNull(find(identity(openedOn = LocalDate.of(2026, 9, 23)), stored()))
    }

    @Test
    fun tickerMatchIgnoresCase() {
        val match = find(identity(ticker = "tsll"), stored(ticker = "TSLL"))
        assertNotNull(match)
    }

    @Test
    fun closedTradeStillWarnsWithClosedCopy() {
        val match = find(identity(), stored(status = PositionStatus.CLOSED))
        assertNotNull(match)
        val summary = DuplicateDetector.summary(match!!)
        val banner = DuplicateDetector.banner(match)
        assertTrue(summary.contains("a closed position"))
        assertTrue(banner.startsWith("Possible duplicate of a closed position"))
        assertFalse(banner.contains("an open position"))
    }

    @Test
    fun openMatchIsPreferredWhenAClosedTradeAlsoMatches() {
        val match = find(
            identity(),
            listOf(
                stored(id = 1, status = PositionStatus.CLOSED),
                stored(id = 2, status = PositionStatus.OPEN),
            ),
        )
        assertEquals(2L, match!!.existing.id)
        assertTrue(DuplicateDetector.banner(match).startsWith("Possible duplicate of an open position"))
    }

    @Test
    fun sameSchwabOrderIdWarnsEvenWhenStrikeDiffers() {
        val notes = "Schwab order 1008019740597"
        val match = find(
            identity(strikeCents = 1_200, notes = notes),
            stored(strikeCents = 1_100, notes = notes),
        )
        assertNotNull(match)
        assertTrue(match!!.sameOrderId)
        assertTrue(DuplicateDetector.summary(match).contains("Same Schwab order 1008019740597"))
    }

    @Test
    fun keyMatchMentionsSharedOrderId() {
        val notes = "Schwab order 1008019740597"
        val match = find(identity(notes = notes), stored(notes = notes))
        assertTrue(match!!.sameOrderId)
        assertTrue(DuplicateDetector.summary(match).contains("Same Schwab order 1008019740597"))
    }

    @Test
    fun savingTheSamePositionDoesNotMatchItself() {
        assertNull(find(identity(), stored(id = 4), ignoreId = 4))
    }

    private fun find(
        candidate: TradeIdentity,
        existing: Position,
        ignoreId: Long = 0L,
    ) = DuplicateDetector.find(candidate, candidate.notes, listOf(existing), ignoreId)

    private fun find(
        candidate: TradeIdentity,
        existing: List<Position>,
        ignoreId: Long = 0L,
    ) = DuplicateDetector.find(candidate, candidate.notes, existing, ignoreId)

    private fun identity(
        ticker: String = "TSLL",
        side: OptionSide = OptionSide.SELL,
        type: OptionType = OptionType.CALL,
        strikeCents: Long = 1_100,
        expiry: LocalDate = LocalDate.of(2026, 9, 25),
        contracts: Int = 100,
        openedOn: LocalDate = LocalDate.of(2026, 9, 22),
        premium: Long = 11,
        notes: String = "",
    ) = TradeIdentity(
        ticker = ticker,
        side = side,
        type = type,
        strikeCents = strikeCents,
        expiry = expiry,
        contracts = contracts,
        openedOn = openedOn,
        entryPremiumCents = premium,
        notes = notes,
    )

    private fun stored(
        id: Long = 1,
        ticker: String = "TSLL",
        side: OptionSide = OptionSide.SELL,
        type: OptionType = OptionType.CALL,
        strikeCents: Long = 1_100,
        expiry: LocalDate = LocalDate.of(2026, 9, 25),
        contracts: Int = 100,
        openedOn: LocalDate = LocalDate.of(2026, 9, 22),
        premium: Long = 11,
        notes: String = "",
        status: PositionStatus = PositionStatus.OPEN,
    ) = Position(
        id = id,
        ticker = ticker,
        side = side,
        type = type,
        strikeCents = strikeCents,
        expiry = expiry,
        contracts = contracts,
        entryPremiumCents = premium,
        entryFeesCents = 0,
        openedOn = openedOn,
        notes = notes,
        status = status,
        exitPremiumCents = if (status == PositionStatus.CLOSED) 0L else null,
        exitFeesCents = if (status == PositionStatus.CLOSED) 0L else null,
        closedOn = if (status == PositionStatus.CLOSED) openedOn else null,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
