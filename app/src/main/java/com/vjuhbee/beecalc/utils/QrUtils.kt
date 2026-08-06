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
 * Генерация QR-кода улья из его uuid (SPEC.md §9, v0.4).
 * Кодируем как URI-схему `beecalc://hive/<uuid>`, чтобы скан
 * открывал приложение и вёл прямо на улей.
 *
 * Чистая функция без UI — её легко тестировать отдельно.
 */
fun generateHiveQr(uuid: String, sizePx: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val content = "beecalc://hive/$uuid"
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