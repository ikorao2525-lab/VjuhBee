package com.vjuhbee.beecalc.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.TaskCategory
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

/** Состояние экрана календаря. */
data class CalendarUiState(
    val currentMonth: Int,                          // 1..12
    val tasksByMonth: Map<Int, List<CalendarTask>> = emptyMap(),
    val expandedMonths: Set<Int> = emptySet(),
    val editor: TaskEditor? = null
)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as BeeCalcApp).calendarRepository

    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    // Текущий месяц раскрыт сразу — за ним пользователь и пришёл.
    private val expandedMonths = MutableStateFlow(setOf(currentMonth))
    private val editor = MutableStateFlow<TaskEditor?>(null)

    val uiState: StateFlow<CalendarUiState> =
        combine(repository.observeTasks(), expandedMonths, editor) { tasks, expanded, editorState ->
            CalendarUiState(
                currentMonth = currentMonth,
                tasksByMonth = tasks.groupBy { it.month },
                expandedMonths = expanded,
                editor = editorState
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(currentMonth = currentMonth, expandedMonths = setOf(currentMonth))
        )

    init {
        viewModelScope.launch { repository.seedDefaultsIfEmpty() }
    }

    fun toggleMonth(month: Int) {
        expandedMonths.update { if (month in it) it - month else it + month }
    }

    fun setDone(task: CalendarTask, done: Boolean) {
        viewModelScope.launch { repository.updateTask(task.copy(isDone = done)) }
    }

    fun startAdd(month: Int) {
        editor.value = TaskEditor(
            task = CalendarTask(
                id = 0,
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
            if (isNew) repository.addTask(task) else repository.updateTask(task)
            editor.value = null
        }
    }

    fun deleteTask(task: CalendarTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            editor.value = null
        }
    }
}
