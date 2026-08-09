package com.vjuhbee.beecalc.ui.reports

import android.net.Uri

data class ReportQrData(
    val version: Int,
    val kind: ReportKind,
    val hiveUuid: String?,
    val startMillis: Long?,
    val endMillis: Long?,
    val checksum: String
)

fun buildReportQrContent(report: ReportData, kind: ReportKind, checksum: String): String {
    return Uri.Builder().scheme("beecalc").authority("report").appendPath("check")
        .appendQueryParameter("v", "1")
        .appendQueryParameter("kind", kind.name)
        .appendQueryParameter("hiveUuid", report.hive?.uuid.orEmpty())
        .appendQueryParameter("start", report.range.startMillis?.toString().orEmpty())
        .appendQueryParameter("end", report.range.endMillis?.toString().orEmpty())
        .appendQueryParameter("checksum", checksum)
        .build().toString()
}

fun parseReportQr(raw: String): ReportQrData? {
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
    if (uri.scheme != "beecalc" || uri.authority != "report" || uri.path != "/check") return null
    val version = uri.getQueryParameter("v")?.toIntOrNull() ?: return null
    val kind = uri.getQueryParameter("kind")?.let { runCatching { ReportKind.valueOf(it) }.getOrNull() } ?: return null
    val checksum = uri.getQueryParameter("checksum").orEmpty()
    if (checksum.isBlank()) return null
    return ReportQrData(
        version = version,
        kind = kind,
        hiveUuid = uri.getQueryParameter("hiveUuid")?.takeIf { it.isNotBlank() },
        startMillis = uri.getQueryParameter("start")?.toLongOrNull(),
        endMillis = uri.getQueryParameter("end")?.toLongOrNull(),
        checksum = checksum
    )
}
