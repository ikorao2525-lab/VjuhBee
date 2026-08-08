package com.vjuhbee.beecalc.ui.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.HarvestProduct
import java.io.File
import java.io.FileOutputStream

fun createReportPdf(context: Context, report: ReportData, kind: ReportKind): android.net.Uri {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = android.graphics.Color.BLACK }
    var y = 36f
    val lines = buildList {
        add("BeeCalc — отчёт")
        add(if (kind == ReportKind.HIVE) "Улей: ${report.hive?.name.orEmpty()}" else "Вся пасека")
        add("Период: ${report.range.label}")
        add("")
        add("Сбор продукции: ${report.harvestTotals.size} поз.")
        report.harvestTotals.forEach { item -> add("• ${reportProductName(item.product)}: ${item.amount} ${item.unit.label}") }
        add("")
        add("Работы: ${report.tasks.size}")
        report.tasks.forEach { task ->
            add("• ${task.title} — ${if (task.isDone) "выполнено" else "не выполнено"}")
        }
        add("")
        add("Осмотры: ${report.inspections.size}")
        report.inspections.forEach { add("• рамок: ${it.frames}, расплод: ${it.brood}") }
        add("")
        add("Обработки: ${report.treatments.size}")
        report.treatments.forEach { add("• ${it.medicine} ${it.dose}") }
    }
    lines.forEach { line ->
        if (y > pageHeight - 30) return@forEach
        page.canvas.drawText(line.take(90), 32f, y, paint)
        y += 18f
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
