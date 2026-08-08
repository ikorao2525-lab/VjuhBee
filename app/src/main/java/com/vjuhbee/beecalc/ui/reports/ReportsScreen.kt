package com.vjuhbee.beecalc.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.HarvestProduct

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var reportKind by remember { mutableStateOf(ReportKind.HIVE) }
    var period by remember { mutableStateOf(ReportPeriod.YEAR) }
    var customStart by remember { mutableStateOf<Long?>(null) }
    var customEnd by remember { mutableStateOf<Long?>(null) }
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showReportPage by remember { mutableStateOf(false) }

    if (showReportPage && state.report != null) {
        ReportPage(report = state.report!!, kind = reportKind, onBack = { showReportPage = false })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.reports_back))
        }
        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.reports_intro), style = MaterialTheme.typography.bodyLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reports_kind_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportKind.entries.forEach { kind ->
                        FilterChip(
                            selected = reportKind == kind,
                            onClick = { reportKind = kind },
                            label = { Text(stringResource(if (kind == ReportKind.HIVE) R.string.reports_kind_hive else R.string.reports_kind_apiary)) }
                        )
                    }
                }
                if (reportKind == ReportKind.HIVE) {
                    Text(stringResource(R.string.reports_hive_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.hives.forEach { hive ->
                            FilterChip(state.selectedHiveUuid == hive.uuid, { viewModel.selectHive(hive.uuid) }, label = { Text(hive.name) })
                        }
                    }
                    if (state.hives.isEmpty()) Text(stringResource(R.string.reports_no_hives))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reports_period_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { selectedPeriod ->
                        FilterChip(
                            selected = period == selectedPeriod,
                            onClick = { period = selectedPeriod },
                            label = { Text(stringResource(selectedPeriod.titleRes())) }
                        )
                    }
                }
                if (period == ReportPeriod.CUSTOM) {
                    OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.START }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(customStart?.let(::formatDate) ?: stringResource(R.string.reports_custom_start))
                    }
                    OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.END }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(customEnd?.let(::formatDate) ?: stringResource(R.string.reports_custom_end))
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.generate(reportKind, period, customStart, customEnd); showReportPage = true },
            enabled = (reportKind == ReportKind.APIARY || state.selectedHiveUuid != null) &&
                (period != ReportPeriod.CUSTOM || (customStart != null && customEnd != null && customStart!! <= customEnd!!)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.reports_generate)) }

    }

    datePickerTarget?.let { target ->
        val pickerState = rememberDatePickerState(if (target == DatePickerTarget.START) customStart else customEnd)
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { value -> if (target == DatePickerTarget.START) customStart = value else customEnd = value }
                    datePickerTarget = null
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { datePickerTarget = null }) { Text(stringResource(R.string.editor_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun ReportPage(
    report: ReportData,
    kind: ReportKind,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pendingPdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        val source = pendingPdfUri
        if (destination != null && source != null) {
            context.contentResolver.openInputStream(source)?.use { input ->
                context.contentResolver.openOutputStream(destination)?.use { output -> input.copyTo(output) }
            }
        }
        pendingPdfUri = null
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reports_back_to_selection))
            }
            OutlinedButton(onClick = {
                    val uri = createReportPdf(context, report, kind)
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, context.getString(R.string.reports_share_title)))
                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reports_share_pdf))
            }
        }
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                pendingPdfUri = createReportPdf(context, report, kind)
                savePdfLauncher.launch("beecalc-report-${System.currentTimeMillis()}.pdf")
            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reports_save_pdf))
            }
            OutlinedButton(onClick = {
                val uri = createReportPdf(context, report, kind)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, context.getString(R.string.reports_print_pdf)))
            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reports_print_pdf))
            }
        }
        Text(
            stringResource(R.string.reports_preview_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (kind == ReportKind.HIVE) report.hive?.name.orEmpty()
            else stringResource(R.string.reports_kind_apiary),
            style = MaterialTheme.typography.titleMedium
        )
        Text(stringResource(R.string.reports_preview_period, report.range.label))
        Text(stringResource(R.string.reports_preview_harvest, report.harvestTotals.size), fontWeight = FontWeight.Bold)
        report.harvestTotals.forEach { item ->
            Text(harvestProductName(item.product) + ": " + item.amount.toString() + " " + item.unit.label)
        }
Text(stringResource(R.string.reports_preview_tasks, report.tasks.size), fontWeight = FontWeight.Bold)
        report.tasks.groupBy { it.year to it.month }.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second }).forEach { (key, tasks) ->
            Text(reportMonthLabel(key.first, key.second), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            tasks.forEach { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        if (task.shortDescription.isNotBlank()) Text(task.shortDescription)
                        Text(if (task.isDone) stringResource(R.string.reports_task_done) else stringResource(R.string.reports_task_pending))
                        task.harvestItems.forEach { item -> Text(harvestProductName(item.product) + ": " + item.amount.toString() + " " + item.unit.label) }
                        task.fullDescription?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                }
            }
        }
        Text(stringResource(R.string.reports_preview_inspections, report.inspections.size), fontWeight = FontWeight.Bold)
        report.inspections.forEach { item ->
            Text(stringResource(R.string.reports_inspection_item, formatDate(item.date), item.frames, item.brood))
            item.note.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
        Text(stringResource(R.string.reports_preview_treatments, report.treatments.size), fontWeight = FontWeight.Bold)
        report.treatments.forEach { item ->
            Text(stringResource(R.string.reports_treatment_item, formatDate(item.date), item.medicine, item.dose))
            item.note.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
    }
}
enum class DatePickerTarget { START, END }
enum class ReportKind { HIVE, APIARY }
enum class ReportPeriod { MONTH, QUARTER, HALF_YEAR, YEAR, ALL_TIME, CUSTOM }

private fun ReportPeriod.titleRes(): Int = when (this) {
    ReportPeriod.MONTH -> R.string.reports_period_month
    ReportPeriod.QUARTER -> R.string.reports_period_quarter
    ReportPeriod.HALF_YEAR -> R.string.reports_period_half_year
    ReportPeriod.YEAR -> R.string.reports_period_year
    ReportPeriod.ALL_TIME -> R.string.reports_period_all
    ReportPeriod.CUSTOM -> R.string.reports_period_custom
}

private fun harvestProductName(product: HarvestProduct): String = when (product) {
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

private fun formatDate(millis: Long): String = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(millis)
private fun reportMonthKey(millis: Long): Pair<Int, Int> {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return calendar.get(java.util.Calendar.YEAR) to calendar.get(java.util.Calendar.MONTH) + 1
}
private fun reportMonthLabel(year: Int, month: Int): String {
    val names = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
    return "${names[month - 1]} $year"
}
