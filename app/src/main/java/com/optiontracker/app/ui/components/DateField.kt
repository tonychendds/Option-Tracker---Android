package com.optiontracker.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.optiontracker.app.ui.format.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDate: (LocalDate) -> Unit,
    error: String?,
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = date?.let(::formatDate) ?: "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            isError = error != null,
            supportingText = { Text(error ?: "Tap to choose a date") },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (error != null) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.outline
                },
                disabledLabelColor = if (error != null) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                },
                disabledSupportingTextColor = if (error != null) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { show = true },
        )
    }
    if (show) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDate(it.toLocalDateUtc()) }
                        show = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
