package com.example

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small, bounded diagnostic log dedicated to reorder/highlight investigation.
 * The file is kept in app-private storage and is exported only when the user requests it.
 */
object TrackMoveLogger {
    private const val FILE_NAME = "track_movement_logs.txt"
    private const val MAX_BYTES = 1_500_000L
    private val lock = Any()
    @Volatile var enabled: Boolean = true

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun log(context: Context, event: String, details: String) {
        if (!enabled) return
        synchronized(lock) {
            try {
                val target = file(context)
                if (target.exists() && target.length() > MAX_BYTES) {
                    val old = target.readText()
                    target.writeText("=== LOG ROTATED ===\n" + old.takeLast((MAX_BYTES / 2).toInt()))
                }
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                target.appendText("$timestamp | $event | $details\n")
            } catch (_: Exception) {
                // Diagnostics must never interrupt playback or reordering.
            }
        }
    }

    fun exportTo(context: Context, uri: Uri): Boolean = synchronized(lock) {
        try {
            val content = file(context).takeIf { it.exists() }?.readText()
                ?: "Логи перемещения пока пусты. Открой список треков и попробуй переместить трек.\n"
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(content)
            } ?: return@synchronized false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            try { file(context).writeText("") } catch (_: Exception) {}
        }
    }

    fun sizeBytes(context: Context): Long = try { file(context).length() } catch (_: Exception) { 0L }
}
