package com.vjuhbee.beecalc.ui.reports

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.ui.hives.QrScannerScreen
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun ReportQrScannerScreen(onBack: () -> Unit, viewModel: ReportQrViewModel = viewModel()) {
    var result by remember { mutableStateOf<ReportQrData?>(null) }
    var invalid by remember { mutableStateOf(false) }
    var verification by remember { mutableStateOf<ReportVerification?>(null) }

    if (result == null && !invalid) {
        QrScannerScreen(onBack = onBack, onQrScanned = { raw ->
            result = parseReportQr(raw)
            invalid = result == null
            result?.let { data -> viewModel.verify(data) { verification = it } }
        })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.report_qr_title), style = MaterialTheme.typography.headlineSmall)
        Text(if (invalid) stringResource(R.string.report_qr_invalid) else stringResource(R.string.report_qr_valid_payload))
        result?.let { data ->
            Text("Тип отчёта: ${data.kind}")
            Text("Контрольная сумма: ${data.checksum.take(16)}…")
        }
        verification?.let { state ->
            Text(
                stringResource(state.messageRes),
                color = if (state == ReportVerification.MATCH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = { result = null; invalid = false; verification = null },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.report_qr_scan)) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.calculator_back))
        }
    }
}

enum class ReportVerification(val messageRes: Int) {
    MATCH(R.string.report_qr_match),
    DIFFERENT(R.string.report_qr_different),
    NOT_FOUND(R.string.report_qr_not_found)
}

class ReportQrViewModel(app: Application) : AndroidViewModel(app) {
    private val beeCalcApp = app as BeeCalcApp

    fun verify(data: ReportQrData, onResult: (ReportVerification) -> Unit) {
        viewModelScope.launch {
            val hives = beeCalcApp.hiveRepository.allHives()
            val tasks = beeCalcApp.calendarRepository.allTasks()
            val inspections = beeCalcApp.hiveRepository.allInspections()
            val treatments = beeCalcApp.hiveRepository.allTreatments()
            val hive = data.hiveUuid?.let { uuid -> hives.firstOrNull { it.uuid == uuid } }

            if (data.kind == ReportKind.HIVE && hive == null) {
                onResult(ReportVerification.NOT_FOUND)
                return@launch
            }

            val selectedTasks = tasks.filter { task ->
                !task.isDeleted &&
                    (data.kind == ReportKind.APIARY || data.hiveUuid in task.linkedHiveUuids) &&
                    task.monthStartMillisForQr().inQrRange(data.startMillis, data.endMillis)
            }
            val selectedInspections = inspections.filter { item ->
                (data.kind == ReportKind.APIARY || item.hiveId == hive?.id) &&
                    item.date.inQrRange(data.startMillis, data.endMillis)
            }
            val selectedTreatments = treatments.filter { item ->
                (data.kind == ReportKind.APIARY || item.hiveId == hive?.id) &&
                    item.date.inQrRange(data.startMillis, data.endMillis)
            }
            val harvestTotals = selectedTasks.flatMap { it.harvestItems }
                .groupBy { it.product to it.unit }
                .map { (key, items) -> HarvestSummary(key.first, key.second, items.sumOf { it.amount }) }

            val localReport = ReportData(
                hive = hive,
                range = ReportRange(data.startMillis, data.endMillis, "QR"),
                tasks = selectedTasks,
                inspections = selectedInspections,
                treatments = selectedTreatments,
                harvestTotals = harvestTotals
            )
            onResult(
                if (calculateReportChecksum(localReport, data.kind) == data.checksum) {
                    ReportVerification.MATCH
                } else {
                    ReportVerification.DIFFERENT
                }
            )
        }
    }
}

private fun CalendarTask.monthStartMillisForQr(): Long = Calendar.getInstance().apply {
    clear()
    set(year, month - 1, 1, 0, 0, 0)
}.timeInMillis

private fun Long.inQrRange(start: Long?, end: Long?): Boolean =
    (start == null || this >= start) && (end == null || this <= end)
