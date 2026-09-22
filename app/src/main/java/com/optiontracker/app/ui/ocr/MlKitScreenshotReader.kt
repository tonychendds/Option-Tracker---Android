package com.optiontracker.app.ui.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.optiontracker.app.domain.ocr.OcrLayout
import com.optiontracker.app.domain.ocr.OcrLine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** On-device text recognition. The image is not uploaded. */
class MlKitScreenshotReader(private val context: Context) {
    suspend fun recognize(uri: Uri): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) continuation.resume(readingOrderText(result))
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            recognizer.close()
        }
    }

    /**
     * ML Kit often returns the left labels and the right values as separate
     * blocks, so [com.google.mlkit.vision.text.Text.getText] is not row order.
     * Lines that share a vertical position are joined before parsing.
     */
    private fun readingOrderText(result: com.google.mlkit.vision.text.Text): String {
        val lines = result.textBlocks.flatMap { block -> block.lines }.map { line ->
            val box = line.boundingBox
            OcrLine(
                text = line.text,
                left = box?.left ?: 0,
                top = box?.top ?: 0,
                height = box?.height() ?: 0,
            )
        }
        val positioned = lines.any { it.left != 0 || it.top != 0 || it.height != 0 }
        val joined = if (positioned) OcrLayout.joinLines(lines) else ""
        return if (joined.isNotBlank()) joined else result.text
    }
}
