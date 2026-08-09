package com.vjuhbee.beecalc.ui.hives

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Какой диалог открыт на экране улья. */
sealed interface HiveDialog {
    data object EditHive : HiveDialog
    data object AddInspection : HiveDialog
    data object AddTreatment : HiveDialog
    data object ShowQr : HiveDialog
}

enum class HistoryTypeFilter { ALL, INSPECTIONS, TREATMENTS }

private data class HistoryFilterState(val query: String = "", val type: HistoryTypeFilter = HistoryTypeFilter.ALL, val year: Int? = null)

data class HiveDetailUiState(
    val hive: Hive? = null,
    val inspections: List<Inspection> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val linkedTasks: List<CalendarTask> = emptyList(),
    val dialog: HiveDialog? = null,
    val isDeleted: Boolean = false,
    val historyQuery: String = "",
    val historyTypeFilter: HistoryTypeFilter = HistoryTypeFilter.ALL,
    val historyYear: Int? = null
)

class HiveDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repository = (app as BeeCalcApp).hiveRepository
    private val calendarRepository = (app as BeeCalcApp).calendarRepository

    /** id улья приходит из маршрута hive/{hiveId}. */
    private val hiveId: Int = checkNotNull(savedStateHandle["hiveId"])

    private val dialog = MutableStateFlow<HiveDialog?>(null)
    private val deleted = MutableStateFlow(false)
    private val historyFilters = MutableStateFlow(HistoryFilterState())

    /**
     * Работы календаря, привязанные к этому улью (SPEC.md §5.2, v0.5):
     * любая задача, в linkedHiveUuids которой есть uuid улья (все годы —
     * архив для отчётности).
     */
    private val linkedTasks: Flow<List<CalendarTask>> =
        combine(repository.observeHive(hiveId), calendarRepository.observeTasks()) { hive, tasks ->
            val uuid = hive?.uuid ?: return@combine emptyList()
            tasks.filter { uuid in it.linkedHiveUuids }
        }

    val uiState: StateFlow<HiveDetailUiState> =
        combine(
            listOf(
                repository.observeHive(hiveId),
                repository.observeInspections(hiveId),
                repository.observeTreatments(hiveId),
                linkedTasks,
                combine(dialog, deleted) { d, del -> d to del },
                historyFilters
            )
        ) { values ->
            val hive = values[0] as Hive?
            val inspections = values[1] as List<Inspection>
            val treatments = values[2] as List<Treatment>
            val linked = values[3] as List<CalendarTask>
            val dialogState = values[4] as Pair< HiveDialog?, Boolean>
            val filter = values[5] as HistoryFilterState
            fun yearOf(date: Long): Int = java.util.Calendar.getInstance().apply { timeInMillis = date }.get(java.util.Calendar.YEAR)
            val filteredInspections = inspections.filter { filter.type != HistoryTypeFilter.TREATMENTS && (filter.query.isBlank() || it.note.contains(filter.query, true)) && (filter.year == null || yearOf(it.date) == filter.year) }
            val filteredTreatments = treatments.filter { filter.type != HistoryTypeFilter.INSPECTIONS && (filter.query.isBlank() || it.medicine.contains(filter.query, true) || it.note.contains(filter.query, true)) && (filter.year == null || yearOf(it.date) == filter.year) }
            HiveDetailUiState(
                hive = hive,
                inspections = filteredInspections,
                treatments = filteredTreatments,
                linkedTasks = linked,
                dialog = dialogState.first,
                isDeleted = dialogState.second,
                historyQuery = filter.query,
                historyTypeFilter = filter.type,
                historyYear = filter.year
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HiveDetailUiState()
        )
    fun setHistoryQuery(value: String) { historyFilters.update { it.copy(query = value) } }
    fun setHistoryTypeFilter(value: HistoryTypeFilter) { historyFilters.update { it.copy(type = value) } }
    fun setHistoryYear(value: Int?) { historyFilters.update { it.copy(year = value) } }
    fun clearHistoryFilters() { historyFilters.value = HistoryFilterState() }

    fun openDialog(newDialog: HiveDialog) {
        dialog.value = newDialog
    }

    fun closeDialog() {
        dialog.value = null
    }

    fun saveHive(hive: Hive) {
        viewModelScope.launch {
            repository.updateHive(hive.copy(updatedAt = System.currentTimeMillis()))
            dialog.value = null
        }
    }

    fun deleteHive(hive: Hive) {
        viewModelScope.launch {
            repository.deleteHive(hive)
            deleted.value = true   // сигнал экрану вернуться к списку
        }
    }

    fun addInspection(inspection: Inspection) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addInspection(
                inspection.copy(
                    hiveId = hiveId,
                    uuid = if (inspection.uuid.isBlank()) java.util.UUID.randomUUID().toString() else inspection.uuid,
                    updatedAt = now
                )
            )
            dialog.value = null
        }
    }

    fun deleteInspection(inspection: Inspection) {
        viewModelScope.launch { repository.deleteInspection(inspection) }
    }

    fun addTreatment(treatment: Treatment) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addTreatment(
                treatment.copy(
                    hiveId = hiveId,
                    uuid = if (treatment.uuid.isBlank()) java.util.UUID.randomUUID().toString() else treatment.uuid,
                    updatedAt = now
                )
            )
            dialog.value = null
        }
    }

    fun deleteTreatment(treatment: Treatment) {
        viewModelScope.launch { repository.deleteTreatment(treatment) }
    }

    /**
     * Отвязать работу от этого улья (SPEC.md §5.2, v0.5).
     * Убираем uuid улья из linkedHiveUuids работы; работа в календаре остаётся.
     */
    fun unlinkTask(task: CalendarTask, hiveUuid: String) {
        viewModelScope.launch {
            val hive = repository.observeHive(hiveId).firstOrNull() ?: return@launch
            if (hive.uuid != hiveUuid) return@launch
            val updated = task.copy(
                linkedHiveUuids = task.linkedHiveUuids - hiveUuid,
                updatedAt = System.currentTimeMillis()
            )
            calendarRepository.updateTask(updated)
        }
    }
}
