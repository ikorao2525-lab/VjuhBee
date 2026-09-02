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
    private const val MAX_CACHE_FILES = 10

    /** Пишет экспорт во внутренний кэш и возвращает URI для шаринга. */
    fun writeExport(context: Context, json: String): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        pruneCache(dir)
        val fileName = "$FILE_PREFIX${System.currentTimeMillis()}.$EXT"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun pruneCache(dir: File) {
        dir.listFiles { file -> file.isFile && file.extension == EXT }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_CACHE_FILES - 1)
            ?.forEach { it.delete() }
    }


    /** Сохраняет резервную копию текущей пасеки во внутреннем хранилище приложения. */
    fun writeBackup(context: Context, json: String): File {
        val dir = File(context.filesDir, "sync-backups").apply { mkdirs() }
        val file = File(dir, "beecalc-backup-${System.currentTimeMillis()}.$EXT")
        FileOutputStream(file).use { out -> out.write(json.toByteArray(Charsets.UTF_8)) }
        return file
    }

    data class BackupInfo(val file: File) {
        val name: String get() = file.name
        val sizeBytes: Long get() = file.length()
        val modifiedAt: Long get() = file.lastModified()
    }

    fun listBackups(context: Context): List<BackupInfo> =
        File(context.filesDir, "sync-backups")
            .listFiles { file -> file.isFile && file.extension == EXT }
            ?.sortedByDescending { it.lastModified() }
            ?.map(::BackupInfo)
            ?: emptyList()

    fun readBackup(info: BackupInfo): String? =
        try { info.file.readText(Charsets.UTF_8) } catch (_: Exception) { null }

    fun deleteBackup(info: BackupInfo): Boolean = info.file.delete()
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
