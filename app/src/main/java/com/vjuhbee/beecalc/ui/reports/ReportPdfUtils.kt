package com.vjuhbee.beecalc.ui.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vjuhbee.beecalc.model.HarvestProduct
import java.io.File
import java.io.FileOutputStream

data class PdfLine(val text: String, val isMonthHeader: Boolean = false)

fun createReportPdf(context: Context, report: ReportData, kind: ReportKind): android.net.Uri {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = android.graphics.Color.BLACK }
    var y = 36f
    val lines = buildList {
        add(PdfLine("BeeCalc — отчёт"))
        add(PdfLine(if (kind == ReportKind.HIVE) "Улей: ${report.hive?.name.orEmpty()}" else "Вся пасека"))
        add(PdfLine("Период: ${report.range.label}"))
        add(PdfLine(""))
        add(PdfLine("Итого урожая: ${report.harvestTotals.size} поз."))
        report.harvestTotals.forEach { summary -> add(PdfLine("• ${reportProductName(summary.product)}: ${summary.amount} ${summary.unit.label}")) }
        add(PdfLine(""))
        add(PdfLine("Урожай по месяцам"))
        report.harvestByMonth.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second }).forEach { (month, summaries) ->
            add(PdfLine(reportMonthLabel(month.first, month.second), true))
            summaries.forEach { summary -> add(PdfLine("• ${reportProductName(summary.product)}: ${summary.amount} ${summary.unit.label}")) }
        }
        add(PdfLine(""))
        add(PdfLine("Работы: ${report.tasks.size}"))
        report.tasks.groupBy { it.year to it.month }.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second }).forEach { (month, tasks) ->
            add(PdfLine(reportMonthLabel(month.first, month.second), true))
            tasks.forEach { task -> add(PdfLine("• ${task.title} — ${if (task.isDone) "выполнено" else "не выполнено"}")) }
        }
        add(PdfLine(""))
        add(PdfLine("Осмотры: ${report.inspections.size}"))
        report.inspections.forEach { add(PdfLine("• рамок: ${it.frames}, расплод: ${it.brood}")) }
        add(PdfLine(""))
        add(PdfLine("Обработки: ${report.treatments.size}"))
        report.treatments.forEach { add(PdfLine("• ${it.medicine} ${it.dose}")) }
    }
    lines.forEach { line ->
        val lineHeight = if (line.isMonthHeader) 28f else 18f
        if (y > pageHeight - 36f - lineHeight) {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = 36f
        }
        if (line.isMonthHeader) {
            paint.isFakeBoldText = true
            paint.textSize = 14f
            page.canvas.drawLine(32f, y - 10f, pageWidth - 32f, y - 10f, paint)
            page.canvas.drawText(line.text.take(90), 32f, y + 8f, paint)
            paint.isFakeBoldText = false
            paint.textSize = 12f
        } else {
            page.canvas.drawText(line.text.take(90), 32f, y, paint)
        }
        y += lineHeight
    }
    document.finishPage(page)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "beecalc-report-${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
}

private fun reportProductName(product: HarvestProduct): String = when (product) {
    HarvestProduct.HONEY -> "Мёд"
    HarvestProduct.POLLEN -> "Пыльца"
    HarvestProduct.BEE_BREAD -> "Перга"
    HarvestProduct.PROPOLIS -> "Прополис"
    HarvestProduct.WAX -> "Воск"
    HarvestProduct.ROYAL_JELLY -> "Маточное молочко"
    HarvestProduct.CAPPINGS -> "Забрус"
    HarvestProduct.WAX_MERVA -> "Мерва"
    HarvestProduct.BEE_VENOM -> "Пчелиный яд"
    HarvestProduct.WINTER_BEES -> "Подмор"
    HarvestProduct.QUEENS -> "Матки"
    HarvestProduct.NUCLEUS_COLONIES -> "Отводки"
    HarvestProduct.PACKAGE_BEES -> "Пчелопакеты"
}

private fun reportMonthLabel(year: Int, month: Int): String {
    val names = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    return "${names[month - 1]} $year"
}