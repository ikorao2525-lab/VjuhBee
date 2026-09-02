package com.vjuhbee.beecalc.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local, bounded diagnostics without user-entered content. */
class DiagnosticLogger(context: Context) {
    private val file = File(context.filesDir, "diagnostics.log")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Synchronized fun debug(event: String) = write("DEBUG", event)
    @Synchronized fun info(event: String) = write("INFO", event)
    @Synchronized fun warning(event: String) = write("WARNING", event)

    @Synchronized
    fun error(event: String, throwable: Throwable? = null) {
        val suffix = throwable?.let { error ->
            val frames = error.stackTrace.take(12).joinToString(";") { frame ->
                "${frame.className}.${frame.methodName}:${frame.lineNumber}"
            }
            ": ${error.javaClass.simpleName} [$frames]"
        }.orEmpty()
        write("ERROR", event + suffix)
    }

    @Synchronized fun markStartupStarted() = File(file.parentFile, STARTUP_MARKER).writeText("started")

    @Synchronized
    fun consumeInterruptedStartup(): Boolean {
        val marker = File(file.parentFile, STARTUP_MARKER)
        if (!marker.exists()) return false
        marker.delete()
        return true
    }

    @Synchronized fun markStartupComplete() = File(file.parentFile, STARTUP_MARKER).delete()

    @Synchronized
    fun read(): String {
        prune()
        return if (file.exists()) file.readText() else ""
    }

    @Synchronized fun clear() { if (file.exists()) file.writeText("") }

    private fun write(level: String, event: String) {
        file.appendText("${dateFormat.format(Date())} [$level] ${event.take(240)}\n")
        if (file.length() > MAX_BYTES) prune()
    }

    private fun prune() {
        if (!file.exists()) return
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        val lines = file.readLines().filter { line ->
            val timestamp = runCatching { dateFormat.parse(line.take(19))?.time }.getOrNull()
            timestamp == null || timestamp >= cutoff
        }
        val text = lines.joinToString("\n", postfix = if (lines.isNotEmpty()) "\n" else "")
        val bytes = text.toByteArray(Charsets.UTF_8)
        file.writeBytes(if (bytes.size > MAX_BYTES) bytes.takeLast(MAX_BYTES).toByteArray() else bytes)
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024
        const val RETENTION_MILLIS = 14L * 24 * 60 * 60 * 1000
        const val STARTUP_MARKER = "startup.marker"
    }
}
