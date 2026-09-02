package com.vjuhbee.beecalc.ui.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.HarvestProduct
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f
private const val MARGIN = 28f
private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

fun createReportPdf(context: Context, report: ReportData, kind: ReportKind, userName: String = "Пчеловод", apiaryName: String = "Вся пасека"): android.net.Uri {
    val document = PdfDocument()
    val layout = PdfLayout(document, context)
    layout.header(report, kind, userName, apiaryName)
    layout.summary(report)
    reportMonths(report).forEach { month ->
        layout.monthSpacer()
        layout.monthHeader(reportMonthLabel(month.first, month.second))
        val tasks = report.tasks.filter { it.year == month.first && it.month == month.second }
        val inspections = report.inspections.filter { reportMonthKey(it.date) == month }
        val treatments = report.treatments.filter { reportMonthKey(it.date) == month }
        val harvest = report.harvestByMonth[month].orEmpty()
        if (tasks.isNotEmpty()) layout.tasksTable(tasks)
        if (inspections.isNotEmpty()) layout.inspectionsTable(inspections)
        if (treatments.isNotEmpty()) layout.treatmentsTable(treatments)
        if (harvest.isNotEmpty()) layout.harvestTable(harvest)
    }
    if (reportMonths(report).isEmpty()) layout.empty("Нет записей за выбранный период")
    layout.qrFooter(report, kind)
    layout.finish()
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    dir.listFiles { candidate -> candidate.isFile && candidate.extension == "pdf" }
        ?.sortedByDescending { it.lastModified() }
        ?.drop(9)
        ?.forEach { it.delete() }
    val file = File(dir, "beecalc-report-${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
}

private class PdfLayout(private val document: PdfDocument, private val context: Context) {
    private val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 30, 30); textSize = 9f }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 8f }
    private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 65, 55); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
    private val userNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 65, 55); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 65, 55); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(190, 205, 198); strokeWidth = 1f }
    private val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(228, 239, 233) }
    private var pageNumber = 0
    private var page: PdfDocument.Page = startPage()
    private var canvas: Canvas = page.canvas
    private var y = MARGIN

    private fun startPage(): PdfDocument.Page {
        pageNumber += 1
        return document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create())
    }
    private fun newPage() { footer(); document.finishPage(page); page = startPage(); canvas = page.canvas; y = MARGIN }
    private fun ensure(height: Float) { if (y + height > PAGE_HEIGHT - 32f) newPage() }

    fun header(report: ReportData, kind: ReportKind, userName: String, apiaryName: String) {
        ensure(78f)
        drawLogo(canvas, context, MARGIN, y, 38)
        canvas.drawText("BeeCalc", MARGIN + 48f, y + 18f, title)
        canvas.drawText("Отчёт пасеки", MARGIN + 48f, y + 36f, normal)
        userNamePaint.color = Color.rgb(25, 65, 55)
        userNamePaint.textSize = 12f
        userNamePaint.typeface = Typeface.DEFAULT_BOLD
        userNamePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(userName.ifBlank { "Пчеловод" }, PAGE_WIDTH / 2f, y + 25f, userNamePaint)
        userNamePaint.textAlign = Paint.Align.LEFT
        rightText(if (kind == ReportKind.HIVE) "Улей: ${report.hive?.name.orEmpty()}" else "Пасека: $apiaryName", PAGE_WIDTH - MARGIN, y + 15f, bold)
        rightText("Период: ${report.range.label}", PAGE_WIDTH - MARGIN, y + 31f, small)
        rightText("Сформирован: ${formatDate(System.currentTimeMillis())}", PAGE_WIDTH - MARGIN, y + 46f, small)
        y += 62f; canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line); y += 14f
    }
    fun summary(report: ReportData) {
        ensure(48f); canvas.drawText("Сводка", MARGIN, y + 11f, bold)
        listOf("Работы: ${report.tasks.size}", "Осмотры: ${report.inspections.size}", "Обработки: ${report.treatments.size}", "Урожай: ${report.harvestTotals.size} поз.").forEachIndexed { i, value -> canvas.drawText(value, MARGIN + i * CONTENT_WIDTH / 4f, y + 29f, normal) }; y += 43f
    }
    fun monthSpacer() { ensure(30f); y += 20f; canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line); y += 10f }
    fun monthHeader(label: String) { ensure(30f); canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25f, headerFill); canvas.drawText(label.uppercase(Locale.getDefault()), MARGIN + 8f, y + 17f, bold); y += 34f }
    fun tasksTable(tasks: List<CalendarTask>) {
        section("Работы"); val widths = listOf(55f, 250f, 75f, 125f); tableHeader(listOf("Дата", "Работа", "Статус", "Ульи"), widths)
        tasks.forEach { task -> tableRow(listOf(task.dueDateMillis?.let(::formatDate) ?: "${task.month}.${task.year}", task.title + task.shortDescription.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty(), if (task.isDone) "Готово" else "Открыта", task.linkedHiveUuids.size.takeIf { it > 0 }?.let { "$it шт." } ?: "—"), widths) }
    }
    fun inspectionsTable(items: List<com.vjuhbee.beecalc.model.Inspection>) { section("Осмотры"); val widths = listOf(65f, 55f, 65f, 65f, 310f); tableHeader(listOf("Дата", "Рамок", "Расплод", "Матка", "Заметка"), widths); items.forEach { tableRow(listOf(formatDate(it.date), it.frames.toString(), it.brood.toString(), if (it.queenSeen) "Да" else "Нет", it.note), widths) } }
    fun treatmentsTable(items: List<com.vjuhbee.beecalc.model.Treatment>) { section("Обработки"); val widths = listOf(65f, 170f, 100f, 225f); tableHeader(listOf("Дата", "Препарат", "Доза", "Заметка"), widths); items.forEach { tableRow(listOf(formatDate(it.date), it.medicine, it.dose, it.note), widths) } }
    fun harvestTable(items: List<HarvestSummary>) { section("Урожай"); val widths = listOf(300f, 130f, 130f); tableHeader(listOf("Продукт", "Количество", "Единица"), widths); items.forEach { tableRow(listOf(reportProductName(it.product), formatNumber(it.amount), it.unit.label), widths, setOf(1)) } }
    fun empty(text: String) { ensure(30f); canvas.drawText(text, MARGIN, y + 12f, normal); y += 26f }
    private fun section(label: String) { ensure(26f); canvas.drawText(label, MARGIN, y + 12f, bold); y += 19f }
    private fun tableHeader(labels: List<String>, widths: List<Float>) { ensure(22f); var x = MARGIN; canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, headerFill); labels.forEachIndexed { i, label -> canvas.drawText(label, x + 4f, y + 14f, bold); x += widths[i] }; y += 20f }
    private fun tableRow(values: List<String>, widths: List<Float>, numeric: Set<Int> = emptySet()) {
        val wrapped = values.mapIndexed { i, value -> wrap(value, normal, widths[i] - 8f).ifEmpty { listOf("") } }; val rowHeight = wrapped.maxOf { it.size } * 12f + 7f
        if (y + rowHeight > PAGE_HEIGHT - 32f) { newPage(); tableHeader(values.indices.map { "" }, widths) }
        var x = MARGIN; wrapped.forEachIndexed { i, lines -> lines.forEachIndexed { row, text -> if (numeric.contains(i)) rightText(text, x + widths[i] - 4f, y + 13f + row * 12f, small) else canvas.drawText(text, x + 4f, y + 13f + row * 12f, normal) }; x += widths[i] }
        canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, line); y += rowHeight
    }
    fun qrFooter(report: ReportData, kind: ReportKind) { val qrSize = 72f; ensure(qrSize + 42f); y += 8f; canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line); y += 10f; val checksum = calculateReportChecksum(report, kind); val qr = makeQr(buildReportQrContent(report, kind, checksum), qrSize.toInt()); canvas.drawBitmap(qr, MARGIN, y, null); canvas.drawText("QR-код контрольной суммы отчёта", MARGIN + qrSize + 12f, y + 20f, bold); canvas.drawText("Проверка выполняется в BeeCalc", MARGIN + qrSize + 12f, y + 36f, small); canvas.drawText("SHA-256: ${checksum.take(16)}…", MARGIN + qrSize + 12f, y + 51f, small); qr.recycle(); y += qrSize + 8f }
    fun finish() { footer(); document.finishPage(page) }
    private fun footer() { canvas.drawLine(MARGIN, PAGE_HEIGHT - 24f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 24f, line); rightText("Страница $pageNumber", PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 10f, small) }
    private fun rightText(text: String, right: Float, baseline: Float, paint: Paint) { canvas.drawText(text, right - paint.measureText(text), baseline, paint) }
}

private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> { if (text.isBlank()) return listOf(""); val result = mutableListOf<String>(); var current = ""; text.split(Regex("\\s+")).forEach { word -> val candidate = if (current.isEmpty()) word else "$current $word"; if (paint.measureText(candidate) <= maxWidth) current = candidate else { if (current.isNotEmpty()) result += current; current = word } }; if (current.isNotEmpty()) result += current; return result }
private fun drawLogo(canvas: Canvas, context: Context, x: Float, y: Float, size: Int) { val drawable = context.getDrawable(R.drawable.ic_launcher) ?: return; drawable.setBounds(x.toInt(), y.toInt(), (x + size).toInt(), (y + size).toInt()); drawable.draw(canvas) }
private fun makeQr(content: String, size: Int): Bitmap { val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)); val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE); return bitmap }
fun calculateReportChecksum(report: ReportData, kind: ReportKind): String {
    val canonical = buildString {
        append(kind).append('|').append(report.hive?.uuid.orEmpty()).append('|')
        append(report.range.startMillis ?: "").append('|').append(report.range.endMillis ?: "").append('|')
        report.tasks.sortedBy { it.uuid }.forEach { task ->
            append(task.uuid).append(':').append(task.year).append(':').append(task.month).append(':')
            append(task.dueDateMillis).append(':').append(task.title).append(':').append(task.shortDescription).append(':')
            append(task.fullDescription).append(':').append(task.category).append(':').append(task.importance).append(':')
            append(task.tags.joinToString(",")).append(':').append(task.isDone).append(':').append(task.isDeleted).append(':')
            append(task.linkedHiveUuids.sorted().joinToString(",")).append('|')
        }
        report.inspections.sortedBy { it.uuid }.forEach { item ->
            append(item.uuid).append(':').append(item.date).append(':').append(item.frames).append(':')
            append(item.brood).append(':').append(item.queenSeen).append(':').append(item.note).append('|')
        }
        report.treatments.sortedBy { it.uuid }.forEach { item ->
            append(item.uuid).append(':').append(item.date).append(':').append(item.medicine).append(':')
            append(item.dose).append(':').append(item.note).append('|')
        }
        report.harvestTotals.sortedWith(compareBy({ it.product.name }, { it.unit.code })).forEach { item ->
            append(item.product).append(':').append(item.amount).append(':').append(item.unit.code).append('|')
        }
    }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
}private fun reportProductName(product: HarvestProduct): String = when (product) { HarvestProduct.HONEY -> "Мёд"; HarvestProduct.POLLEN -> "Пыльца"; HarvestProduct.BEE_BREAD -> "Перга"; HarvestProduct.PROPOLIS -> "Прополис"; HarvestProduct.WAX -> "Воск"; HarvestProduct.ROYAL_JELLY -> "Маточное молочко"; HarvestProduct.CAPPINGS -> "Забрус"; HarvestProduct.WAX_MERVA -> "Мерва"; HarvestProduct.BEE_VENOM -> "Пчелиный яд"; HarvestProduct.WINTER_BEES -> "Подмор"; HarvestProduct.QUEENS -> "Матки"; HarvestProduct.NUCLEUS_COLONIES -> "Отводки"; HarvestProduct.PACKAGE_BEES -> "Пчелопакеты" }
private fun reportMonths(report: ReportData): List<Pair<Int, Int>> = (report.tasks.map { it.year to it.month } + report.inspections.map { reportMonthKey(it.date) } + report.treatments.map { reportMonthKey(it.date) } + report.harvestByMonth.keys).distinct().sortedWith(compareBy({ it.first }, { it.second }))
private fun reportMonthKey(millis: Long): Pair<Int, Int> { val c = Calendar.getInstance().apply { timeInMillis = millis }; return c.get(Calendar.YEAR) to c.get(Calendar.MONTH) + 1 }
private fun reportMonthLabel(year: Int, month: Int): String = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")[month - 1] + " " + year
private fun formatDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
private fun formatNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
