package com.vjuhbee.beecalc.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

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