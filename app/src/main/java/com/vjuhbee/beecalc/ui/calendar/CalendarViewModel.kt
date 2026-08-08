package com.vjuhbee.beecalc.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.YearHarvestTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** Открытый редактор работы: новая или существующая. */
data class TaskEditor(
    val task: CalendarTask,
    val isNew: Boolean
)

/** Ключ раскрытого месяца: год + месяц. */
data class MonthKey(val year: Int, val month: Int)

/** Состояние экрана календаря. */
data class CalendarUiState(
    val currentYear: Int,
    val currentMonth: Int,                                       // 1..12
    val years: List<Int> = emptyList(),                          // по убыванию: текущий сверху
    val tasksByYearMonth: Map<Int, Map<Int, List<CalendarTask>>> = emptyMap(),
    val harvestByYear: Map<Int, YearHarvestTotals> = emptyMap(), // итоги сбора за год
    val expandedYears: Set<Int> = emptySet(),
    val expandedMonths: Set<MonthKey> = emptySet(),
    val deletedTasks: List<CalendarTask> = emptyList(),
    val hives: List<Hive> = emptyList(),
    val editor: TaskEditor? = null
)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as BeeCalcApp).calendarRepository
    private val hiveRepository = (app as BeeCalcApp).hiveRepository

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    // Текущий год и месяц раскрыты сразу — за ними пользователь и пришёл.
    private val expandedYears = MutableStateFlow(setOf(currentYear))
    private val expandedMonths = MutableStateFlow(setOf(MonthKey(currentYear, currentMonth)))
    private val editor = MutableStateFlow<TaskEditor?>(null)

    val uiState: StateFlow<CalendarUiState> =
        combine(
            repository.observeTasks(),
            repository.observeDeleted(),
            expandedYears,
            expandedMonths,
            editor
        ) { tasks, deleted, years, months, editorState ->
            val byYear = tasks.groupBy { it.year }
            CalendarUiState(
                currentYear = currentYear,
                currentMonth = currentMonth,
                years = (byYear.keys + currentYear).sortedDescending(),
                tasksByYearMonth = byYear.mapValues { (_, yearTasks) -> yearTasks.groupBy { it.month } },
                harvestByYear = byYear.mapValues { (_, yearTasks) ->
                    YearHarvestTotals(
                        honeyKg = yearTasks.sumOf { it.honeyKg ?: 0.0 },
                        honeyLiters = yearTasks.sumOf { it.honeyLiters ?: 0.0 },
                        pollenKg = yearTasks.sumOf { it.pollenKg ?: 0.0 },
                        beeBreadKg = yearTasks.sumOf { it.beeBreadKg ?: 0.0 },
                        propolisGrams = yearTasks.sumOf { it.propolisGrams ?: 0.0 },
                        waxKg = yearTasks.sumOf { it.waxKg ?: 0.0 },
                        royalJellyGrams = yearTasks.sumOf { it.royalJellyGrams ?: 0.0 }
                    )
                },
                expandedYears = years,
                expandedMonths = months,
                deletedTasks = deleted,
                editor = editorState
            )
        }.combine(hiveRepository.observeHives()) { state, hives ->
            state.copy(hives = hives)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(
                currentYear = currentYear,
                currentMonth = currentMonth,
                expandedYears = setOf(currentYear),
                expandedMonths = setOf(MonthKey(currentYear, currentMonth))
            )
        )

    init {
        viewModelScope.launch { repository.prepareYear(currentYear) }
    }

    fun toggleYear(year: Int) {
        expandedYears.update { if (year in it) it - year else it + year }
    }

    fun toggleMonth(year: Int, month: Int) {
        val key = MonthKey(year, month)
        expandedMonths.update { if (key in it) it - key else it + key }
    }

    /** Раскрыть сразу конкретный год и месяц (переход из экрана улья, v0.5). */
    fun expandTo(year: Int, month: Int) {
        expandedYears.update { it + year }
        expandedMonths.update { it + MonthKey(year, month) }
    }

    fun setDone(task: CalendarTask, done: Boolean) {
        viewModelScope.launch { repository.updateTask(task.copy(isDone = done)) }
    }

    fun startAdd(year: Int, month: Int) {
        editor.value = TaskEditor(
            task = CalendarTask(
                id = 0,
                year = year,
                month = month,
                title = "",
                shortDescription = "",
                category = TaskCategory.OTHER
            ),
            isNew = true
        )
    }

    fun startEdit(task: CalendarTask) {
        editor.value = TaskEditor(task = task, isNew = false)
    }

    fun closeEditor() {
        editor.value = null
    }

    fun saveTask(task: CalendarTask, isNew: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSave = if (isNew && task.uuid.isBlank()) {
                task.copy(uuid = java.util.UUID.randomUUID().toString(), updatedAt = now)
            } else {
                task.copy(updatedAt = now)
            }
            if (isNew) repository.addTask(toSave) else repository.updateTask(toSave)
            editor.value = null
        }
    }

    /** «Удалить» отправляет работу в корзину — её можно вернуть. */
    fun deleteTask(task: CalendarTask) {
        viewModelScope.launch {
            repository.moveToTrash(task.copy(updatedAt = System.currentTimeMillis()))
            editor.value = null
        }
    }

    fun restoreTask(task: CalendarTask) {
        viewModelScope.launch {
            repository.restoreFromTrash(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteForever(task: CalendarTask) {
        viewModelScope.launch { repository.deleteForever(task) }
    }
}
