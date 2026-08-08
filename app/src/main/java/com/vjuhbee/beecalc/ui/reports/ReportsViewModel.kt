package com.vjuhbee.beecalc.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit
import com.vjuhbee.beecalc.model.Treatment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

 data class ReportRange(
    val startMillis: Long?,
    val endMillis: Long?,
    val label: String
)

data class ReportData(
    val hive: Hive?,
    val range: ReportRange,
    val tasks: List<CalendarTask>,
    val inspections: List<Inspection>,
    val treatments: List<Treatment>,
    val harvestTotals: List<HarvestSummary> = emptyList(),
    val harvestByMonth: Map<Pair<Int, Int>, List<HarvestSummary>> = emptyMap()
)

data class HarvestSummary(
    val product: HarvestProduct,
    val unit: HarvestUnit,
    val amount: Double
)

data class ReportsUiState(
    val hives: List<Hive> = emptyList(),
    val selectedHiveUuid: String? = null,
    val report: ReportData? = null
)

class ReportsViewModel(app: Application) : AndroidViewModel(app) {
    private val beeCalcApp = app as BeeCalcApp
    private val selectedHiveUuid = MutableStateFlow<String?>(null)
    private val loadedRecords = MutableStateFlow(Records())

    val uiState: StateFlow<ReportsUiState> = combine(
        beeCalcApp.hiveRepository.observeHives(), selectedHiveUuid, loadedRecords
    ) { hives, selected, records ->
        ReportsUiState(hives = hives, selectedHiveUuid = selected, report = records.report)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    init {
        viewModelScope.launch {
            loadedRecords.value = Records(
                hives = beeCalcApp.hiveRepository.allHives(),
                tasks = beeCalcApp.calendarRepository.allTasks(),
                inspections = beeCalcApp.hiveRepository.allInspections(),
                treatments = beeCalcApp.hiveRepository.allTreatments()
            )
        }
    }

    fun selectHive(uuid: String?) { selectedHiveUuid.value = uuid }

    fun generate(kind: ReportKind, period: ReportPeriod, customStart: Long? = null, customEnd: Long? = null) {
        val records = loadedRecords.value
        val hive = if (kind == ReportKind.HIVE) {
            records.hives.firstOrNull { it.uuid == selectedHiveUuid.value }
        } else null
        val range = period.toRange(customStart, customEnd)
        val hiveUuid = hive?.uuid
        val hiveId = hive?.id
        loadedRecords.value = records.copy(
            report = ReportData(
                hive = hive,
                range = range,
                tasks = records.tasks.filter { task ->
                    !task.isDeleted && (kind == ReportKind.APIARY || hiveUuid in task.linkedHiveUuids) &&
                        task.monthStartMillis().inRange(range)
                },
                inspections = records.inspections.filter { item ->
                    (hiveId == null || item.hiveId == hiveId) && item.date.inRange(range)
                },
                treatments = records.treatments.filter { item ->
                    (hiveId == null || item.hiveId == hiveId) && item.date.inRange(range)
                }
,
                harvestTotals = harvestItemsFor(records.tasks, kind, hiveUuid, range).summarize(),
                harvestByMonth = harvestItemsFor(records.tasks, kind, hiveUuid, range).groupBy { it.first.year to it.first.month }
                    .mapValues { (_, items) -> items.flatMap { it.second }.let { harvestItems -> harvestItems.groupBy { it.product to it.unit }.map { (key, values) -> HarvestSummary(key.first, key.second, values.sumOf { it.amount }) }.sortedWith(compareBy({ it.product.name }, { it.unit.code })) } }
            )
        )
    }

    private fun harvestItemsFor(
        tasks: List<CalendarTask>,
        kind: ReportKind,
        hiveUuid: String?,
        range: ReportRange
    ): List<Pair<CalendarTask, List<HarvestItem>>> = tasks
        .filter { task ->
            !task.isDeleted &&
                (kind == ReportKind.APIARY || hiveUuid in task.linkedHiveUuids) &&
                task.monthStartMillis().inRange(range)
        }
        .map { it to it.harvestItems }

    private fun List<Pair<CalendarTask, List<HarvestItem>>>.summarize(): List<HarvestSummary> =
        flatMap { it.second }
            .groupBy { it.product to it.unit }
            .map { (key, items) -> HarvestSummary(key.first, key.second, items.sumOf { it.amount }) }
            .sortedWith(compareBy({ it.product.name }, { it.unit.code }))
    private data class Records(
        val hives: List<Hive> = emptyList(), val tasks: List<CalendarTask> = emptyList(),
        val inspections: List<Inspection> = emptyList(), val treatments: List<Treatment> = emptyList(),
        val report: ReportData? = null
    )

    private fun ReportPeriod.toRange(customStart: Long?, customEnd: Long?): ReportRange {
        if (this == ReportPeriod.ALL_TIME) return ReportRange(null, null, "Весь доступный архив")
        if (this == ReportPeriod.CUSTOM && customStart != null && customEnd != null) {
            return ReportRange(customStart, customEnd, formatRange(customStart, customEnd))
        }
        val end = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = end }
        val months = when (this) {
            ReportPeriod.MONTH -> 1
            ReportPeriod.QUARTER -> 3
            ReportPeriod.HALF_YEAR -> 6
            ReportPeriod.YEAR -> 12
            else -> 1
        }
        calendar.add(Calendar.MONTH, -months)
        return ReportRange(calendar.timeInMillis, end, formatRange(calendar.timeInMillis, end))
    }

    private fun CalendarTask.monthStartMillis(): Long = Calendar.getInstance().apply {
        clear(); set(year, month - 1, 1, 0, 0, 0)
    }.timeInMillis

    private fun Long.inRange(range: ReportRange): Boolean =
        (range.startMillis == null || this >= range.startMillis) &&
            (range.endMillis == null || this <= range.endMillis)

    private fun formatRange(start: Long, end: Long): String {
        val format = java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return "${format.format(start)} — ${format.format(end)}"
    }
}

