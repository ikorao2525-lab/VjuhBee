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

private data class FilterState(val query: String = "", val status: TaskStatusFilter = TaskStatusFilter.ALL, val category: TaskCategory? = null, val important: Boolean = false)

/** Состояние экрана календаря. */
enum class TaskStatusFilter { ALL, ACTIVE, DONE }

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
    val editor: TaskEditor? = null,
    val searchQuery: String = "", val statusFilter: TaskStatusFilter = TaskStatusFilter.ALL, val categoryFilter: TaskCategory? = null, val importantOnly: Boolean = false
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
    private val filters = MutableStateFlow(FilterState())

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
                    yearTasks.flatMap { it.harvestItems }
                        .groupBy { it.product to it.unit }
                        .entries
                        .fold(YearHarvestTotals()) { totals, (key, values) ->
                            val amount = values.sumOf { it.amount }
                            when (key.first) {
                                com.vjuhbee.beecalc.model.HarvestProduct.HONEY -> if (key.second == com.vjuhbee.beecalc.model.HarvestUnit.LITER) totals.copy(honeyLiters = amount) else totals.copy(honeyKg = amount)
                                com.vjuhbee.beecalc.model.HarvestProduct.POLLEN -> totals.copy(pollenKg = amount)
                                com.vjuhbee.beecalc.model.HarvestProduct.BEE_BREAD -> totals.copy(beeBreadKg = amount)
                                com.vjuhbee.beecalc.model.HarvestProduct.PROPOLIS -> totals.copy(propolisGrams = amount)
                                com.vjuhbee.beecalc.model.HarvestProduct.WAX -> totals.copy(waxKg = amount)
                                com.vjuhbee.beecalc.model.HarvestProduct.ROYAL_JELLY -> totals.copy(royalJellyGrams = amount)
                                else -> totals
                            }
                        }
                },
                expandedYears = years,
                expandedMonths = months,
                deletedTasks = deleted,
                editor = editorState
            )
        }.combine(filters) { state, filter ->
            val filteredByYear = state.tasksByYearMonth.mapValues { (_, months) ->
                months.mapValues { (_, tasks) -> tasks.filter { task ->
                    (filter.query.isBlank() || task.title.contains(filter.query, true) || task.shortDescription.contains(filter.query, true)) &&
                        (filter.status == TaskStatusFilter.ALL || (filter.status == TaskStatusFilter.ACTIVE && !task.isDone) || (filter.status == TaskStatusFilter.DONE && task.isDone)) &&
                        (filter.category == null || task.category == filter.category) &&
                        (!filter.important || task.importance == com.vjuhbee.beecalc.model.Importance.HIGH)
                } }
            }
            state.copy(tasksByYearMonth = filteredByYear, searchQuery = filter.query, statusFilter = filter.status, categoryFilter = filter.category, importantOnly = filter.important)        }.combine(hiveRepository.observeHives()) { state, hives ->
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

    fun setSearchQuery(value: String) { filters.update { it.copy(query = value) } }
    fun setStatusFilter(value: TaskStatusFilter) { filters.update { it.copy(status = value) } }
    fun setCategoryFilter(value: TaskCategory?) { filters.update { it.copy(category = value) } }
    fun setImportantOnly(value: Boolean) { filters.update { it.copy(important = value) } }
    fun clearFilters() { filters.value = FilterState() }

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
        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    isDone = done,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
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
