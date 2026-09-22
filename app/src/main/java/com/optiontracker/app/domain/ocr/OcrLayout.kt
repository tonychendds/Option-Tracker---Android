package com.optiontracker.app.domain.ocr

import kotlin.math.abs

/** One OCR line with its position on the screenshot. */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val height: Int,
)

/**
 * Puts fragments that share a row onto one line.
 * Schwab draws the label on the left and the value on the right, and ML Kit
 * often returns those as separate lines. Joining by vertical position restores
 * "Quantity 100" and "Trade Date 09/22/2026".
 */
object OcrLayout {
    fun joinLines(lines: List<OcrLine>): String {
        val usable = lines.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return ""
        val sorted = usable.sortedWith(compareBy({ it.top }, { it.left }))
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val row = rows.lastOrNull()
            if (row == null || !sameRow(row.first(), line)) {
                rows += mutableListOf(line)
            } else {
                row += line
            }
        }
        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text.trim() }.trim()
        }
    }

    private fun sameRow(anchor: OcrLine, next: OcrLine): Boolean {
        val band = maxOf(anchor.height, next.height, 20) / 2
        return abs(anchor.top - next.top) <= band
    }
}
