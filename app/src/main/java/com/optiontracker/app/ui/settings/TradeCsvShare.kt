package com.optiontracker.app.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes the CSV into the cache and opens the Android share sheet. */
fun shareTradeCsv(context: Context, csv: String, fileName: String) {
    val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "-")
    val directory = File(context.cacheDir, "exports")
    if (!directory.exists() && !directory.mkdirs()) {
        error("Could not prepare the share file")
    }
    val file = File(directory, safeName)
    file.writeText(csv, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, safeName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, safeName, uri)
    }
    val chooser = Intent.createChooser(send, "Share trades").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(chooser)
}
