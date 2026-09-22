package com.optiontracker.app.ui.ocr

import com.optiontracker.app.domain.ocr.BrokerParseResult

/** Holds one screenshot parse until the add screen reads it. Nothing is saved here. */
class ScreenshotDraftStore {
    private var pending: BrokerParseResult? = null

    fun offer(result: BrokerParseResult) {
        pending = result
    }

    fun consume(): BrokerParseResult? = pending.also { pending = null }
}
