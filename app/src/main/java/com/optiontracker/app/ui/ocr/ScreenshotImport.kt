package com.optiontracker.app.ui.ocr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.optiontracker.app.domain.ocr.BrokerParseResult
import com.optiontracker.app.domain.ocr.BrokerScreenshotParser
import kotlinx.coroutines.launch

@Composable
fun rememberScreenshotImport(
    recognize: suspend (Uri) -> String,
    onResult: (BrokerParseResult) -> Unit,
    onReading: (Boolean) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            onReading(true)
            val result = try {
                BrokerScreenshotParser.parse(recognize(uri))
            } catch (_: Exception) {
                BrokerParseResult.Failed("Couldn't read that image. Enter the position manually.")
            }
            onReading(false)
            onResult(result)
        }
    }
    return {
        picker.launch(arrayOf("image/*"))
    }
}
