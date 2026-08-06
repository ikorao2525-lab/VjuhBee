package com.vjuhbee.beecalc.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Генерация QR-кода улья (SPEC.md §9, v0.4).
 * Кодируем как URI-схему `beecalc://hive/<uuid>` (+ `name`/`note`),
 * чтобы скан открывал приложение, вёл прямо на улей по uuid
 * и позволял импортировать улей с названием/заметкой на другом
 * телефоне (без неё — только uuid).
 */
fun generateHiveQr(
    uuid: String,
    name: String = "",
    note: String = "",
    sizePx: Int = 512
): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val content = buildHiveQrContent(uuid, name, note)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val dark = Color.BLACK
    val light = Color.WHITE

    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            val isDark = matrix[x, y]
            bitmap.setPixel(x, y, if (isDark) dark else light)
        }
    }
    return bitmap
}

/**
 * Данные улья, извлечённые из QR (SPEC.md §9, v0.4).
 * `uuid` — обязательный, `name`/`note` — опциональные и могут быть пустыми.
 */
data class QrHiveData(
    val uuid: String,
    val name: String = "",
    val note: String = ""
)

/** Формирует содержимое QR: `beecalc://hive/<uuid>[?name=..&note=..]`. */
fun buildHiveQrContent(uuid: String, name: String, note: String): String {
    val sb = StringBuilder("beecalc://hive/$uuid")
    val params = mutableListOf<String>()
    if (name.isNotBlank()) params.add("name=" + java.net.URLEncoder.encode(name, "UTF-8"))
    if (note.isNotBlank()) params.add("note=" + java.net.URLEncoder.encode(note, "UTF-8"))
    if (params.isNotEmpty()) sb.append('?').append(params.joinToString("&"))
    return sb.toString()
}

/**
 * Разбирает содержимое QR-кода улья. Возвращает [QrHiveData] с uuid
 * и (если были) название/заметкой. Умеет и старый формат без query.
 */
fun parseHiveQr(text: String): QrHiveData? {
    if (!text.startsWith("beecalc://hive/")) return null
    val rest = text.removePrefix("beecalc://hive/")
    val questionIdx = rest.indexOf('?')
    val uuid = if (questionIdx >= 0) rest.substring(0, questionIdx) else rest
    if (uuid.isBlank()) return null

    var name = ""
    var note = ""
    if (questionIdx >= 0) {
        val query = rest.substring(questionIdx + 1)
        for (pair in query.split('&')) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val key = pair.substring(0, eq)
            val raw = pair.substring(eq + 1)
            val value = try {
                java.net.URLDecoder.decode(raw, "UTF-8")
            } catch (_: Exception) {
                raw
            }
            when (key) {
                "name" -> name = value
                "note" -> note = value
            }
        }
    }
    return QrHiveData(uuid = uuid, name = name, note = note)
}

/**
 * Сохраняет QR-код как PNG во внутренний кэш приложения и возвращает
 * [Uri] для передачи другим приложениям (через FileProvider).
 * Используется кнопками «Поделиться» и «Печать» (SPEC.md §9, v0.4).
 */
fun saveQrToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
    val dir = File(context.cacheDir, "qr").apply { mkdirs() }
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val authority = context.packageName + ".fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

/** Набор типов, которые сканер готов распознавать (только QR, SPEC.md §9 v0.4). */
private val DECODE_HINTS: Map<DecodeHintType, Any> =
    mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

/**
 * Декодирует QR-код из [bitmap]. Возвращает строковое содержимое
 * (ожидается `beecalc://hive/<uuid>`) или null, если код не распознан.
 * Используется в [Analyzer][com.vjuhbee.beecalc.ui.hives.QrAnalyzer].
 */
fun decodeQr(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        MultiFormatReader().apply { setHints(DECODE_HINTS) }
            .decode(binaryBitmap)
            .text
    } catch (_: Exception) {
        null
    }
}