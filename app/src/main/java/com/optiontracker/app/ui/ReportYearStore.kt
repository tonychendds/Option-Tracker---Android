package com.optiontracker.app.ui

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Year shared by Home and History. Defaults to the current calendar year. */
class ReportYearStore(today: LocalDate = LocalDate.now()) {
    private val selected = MutableStateFlow(today.year)
    val year: StateFlow<Int> = selected.asStateFlow()

    fun select(year: Int) {
        if (year in 1970..9999) selected.value = year
    }
}
