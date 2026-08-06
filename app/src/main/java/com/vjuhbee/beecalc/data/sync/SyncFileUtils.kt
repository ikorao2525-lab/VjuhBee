package com.vjuhbee.beecalc.data.sync

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Чтение/запись файла обмена пасекой (SPEC.md §9). */
object SyncFileUtils {

    private const val DIR = "sync"
    private const val EXT = "json"
    private const val FILE_PREFIX = "beecalc-apiary-"

    /** Пишет экспорт во внутренний кэш и возвращает URI для шаринга. */
    fun writeExport(context: Context, json: String): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val fileName = "$FILE_PREFIX${System.currentTimeMillis()}.$EXT"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /** Читает содержимое файла (из URI) как текст. */
    fun readUri(context: Context, uri: Uri): String? =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
}
