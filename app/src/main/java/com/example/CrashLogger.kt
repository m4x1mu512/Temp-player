package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val CRASH_LOG_FILE_NAME = "crash.log"
    private const val FULL_EXPORT_LOG_FILE_NAME = "app_logs.txt"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var isInitialized = false

    /**
     * Инициализирует обработчик необработанных исключений.
     * Должен вызываться как можно раньше, например в Application.onCreate.
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(appContext, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            } finally {
                // Передаем управление стандартному системному обработчику
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Записывает детальный отчёт об ошибке в файл crash.log во внутреннем хранилище.
     */
    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val appVersion = getAppVersion(context)
        val threadName = thread.name

        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()

        val logEntry = buildString {
            appendLine("==================== CRASH REPORT ====================")
            appendLine("Timestamp: $timestamp")
            appendLine("App Version: $appVersion")
            appendLine("Package: ${context.packageName}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: $threadName (ID: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("Stack trace:")
            appendLine(stackTrace)
            appendLine("======================================================")
            appendLine()
        }

        val logFile = getCrashLogFile(context)
        FileWriter(logFile, true).use { writer ->
            writer.write(logEntry)
            writer.flush()
        }
        Log.e(TAG, "Crash report recorded into ${logFile.absolutePath}")
    }

    /**
     * Получает версию приложения из PackageInfo с учётом версий Android.
     */
    fun getAppVersion(context: Context): String {
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Возвращает дескриптор файла crash.log во внутреннем хранилище приложения.
     */
    fun getCrashLogFile(context: Context): File {
        return File(context.filesDir, CRASH_LOG_FILE_NAME)
    }

    /**
     * Возвращает дескриптор файла app_logs.txt для экспорта диагностического отчёта.
     */
    fun getExportLogFile(context: Context): File {
        return File(context.filesDir, FULL_EXPORT_LOG_FILE_NAME)
    }

    /**
     * Проверяет, существует ли файл лога крашей и не пустой ли он.
     */
    fun hasCrashLog(context: Context): Boolean {
        val file = getCrashLogFile(context)
        return file.exists() && file.length() > 0L
    }

    /**
     * Читает содержимое лога крашей.
     */
    fun readCrashLog(context: Context): String? {
        val file = getCrashLogFile(context)
        return if (file.exists() && file.length() > 0L) {
            file.readText()
        } else {
            null
        }
    }

    /**
     * Очищает файл лога крашей.
     */
    fun clearCrashLog(context: Context): Boolean {
        val file = getCrashLogFile(context)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Считывает последние строки из logcat для текущего процесса.
     */
    private fun getRecentLogcat(): String {
        return try {
            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "--pid=$pid"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            var line: String?
            var lineCount = 0
            val maxLines = 1500

            while (reader.readLine().also { line = it } != null) {
                log.appendLine(line)
                lineCount++
                if (lineCount >= maxLines) break
            }
            reader.close()
            process.destroy()
            if (log.isNotEmpty()) log.toString() else "Logcat пуст или недоступен"
        } catch (e: Exception) {
            "Не удалось получить logcat: ${e.message}"
        }
    }

    /**
     * Генерирует комплексный файл отчёта app_logs.txt, включающий:
     * - Информацию об устройстве и версии
     * - Историю крашей (если были)
     * - Последние логи текущего сеанса приложения из logcat
     */
    fun generateFullLogReport(context: Context): File {
        val exportFile = getExportLogFile(context)
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val crashLog = readCrashLog(context)
        val logcatLogs = getRecentLogcat()

        val reportContent = buildString {
            appendLine("=================================================================")
            appendLine("           ОТЧЁТ ДИАГНОСТИКИ ПРИЛОЖЕНИЯ (APP LOGS)")
            appendLine("=================================================================")
            appendLine("Дата создания: $timestamp")
            appendLine("Приложение: ${context.packageName}")
            appendLine("Версия: ${getAppVersion(context)}")
            appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Архитектура: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Память процесса (heap): ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB / ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB")
            appendLine("=================================================================")
            appendLine()
            appendLine("----------------- 1. ИСТОРИЯ КРАШЕЙ (CRASH LOG) -----------------")
            if (crashLog != null && crashLog.isNotBlank()) {
                appendLine(crashLog)
            } else {
                appendLine("Необработанных критических падений не зафиксировано.")
            }
            appendLine("-----------------------------------------------------------------")
            appendLine()
            appendLine("----------------- 2. СИСТЕМНЫЕ ЛОГИ (LOGCAT) --------------------")
            appendLine(logcatLogs)
            appendLine("-----------------------------------------------------------------")
            appendLine("КОНЕЦ ОТЧЁТА")
        }

        exportFile.writeText(reportContent)
        return exportFile
    }

    /**
     * Выгружает файл логов (.txt) через стандартный системный диалог Android (ACTION_SEND).
     * Позволяет сохранить файл на диск, отправить в мессенджер, по почте или скопировать.
     */
    fun exportLogs(context: Context) {
        try {
            val logFile = generateFullLogReport(context)
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, logFile)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Логи приложения ${context.packageName} - app_logs.txt")
                putExtra(Intent.EXTRA_TEXT, "Диагностический файл логов приложения во вложении (app_logs.txt).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Выгрузить логи приложения").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка выгрузки логов", e)
            Toast.makeText(context, "Не удалось выгрузить логи: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Отправляет файл crash.log через системный диалог выбора приложения (ACTION_SEND)
     * с использованием FileProvider.
     */
    fun shareCrashLog(context: Context) {
        exportLogs(context)
    }
}

/**
 * Удобная функция верхнего уровня для отправки/выгрузки логов из любого места.
 */
fun shareCrashLog(context: Context) {
    CrashLogger.exportLogs(context)
}
