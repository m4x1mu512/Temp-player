package com.example

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Small, bounded diagnostic log dedicated to reorder/highlight investigation.
 * The file is kept in app-private storage and is exported only when the user requests it.
 */
object TrackMoveLogger {
    private const val FILE_NAME = "track_movement_logs.txt"
    private const val MAX_BYTES = 1_500_000L
    private val lock = Any()
    private val threadNumber = AtomicInteger()
    private val writer = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(256),
        ThreadFactory { task ->
            Thread(task, "TrackMoveLogger-" + threadNumber.incrementAndGet()).apply { isDaemon = true }
        },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )
    @Volatile var enabled: Boolean = true

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun log(context: Context, event: String, details: String) {
        log(context, event) { details }
    }

    fun log(context: Context, event: String, details: () -> String) {
        if (!enabled) return
        val appContext = context.applicationContext
        try {
            writer.execute {
                if (!enabled) return@execute
                val message = try { details() } catch (_: Exception) { "details unavailable" }
                synchronized(lock) {
                    try {
                        val target = file(appContext)
                        if (target.exists() && target.length() > MAX_BYTES) {
                            val old = target.readText()
                            target.writeText("=== LOG ROTATED ===\n" + old.takeLast((MAX_BYTES / 2).toInt()))
                        }
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                        target.appendText("$timestamp | $event | $message\n")
                    } catch (_: Exception) {
                        // Diagnostics must never interrupt playback or reordering.
                    }
                }
            }
        } catch (_: Exception) {
            // A saturated diagnostic queue must never affect playback or reordering.
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
