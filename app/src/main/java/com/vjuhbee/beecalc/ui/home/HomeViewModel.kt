package com.vjuhbee.beecalc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.vjuhbee.beecalc.ui.calendar.DueStatus
import com.vjuhbee.beecalc.ui.calendar.dueStatus
import java.time.LocalDate

data class HomeUiState(
    val hivesCount: Int = 0,
    val upcoming: List<CalendarTask> = emptyList(),
    val overdue: List<CalendarTask> = emptyList(),
    val critical: List<CalendarTask> = emptyList(),
    val hasTasksWithoutDates: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val beeCalcApp = app as BeeCalcApp
    private val userApiaryRepository = beeCalcApp.userAndApiaryRepository

    val uiState: StateFlow<HomeUiState> =
        userApiaryRepository.observeActiveApiaryUuid().flatMapLatest { apiaryUuid ->
            val tasksFlow = if (apiaryUuid.isNullOrBlank()) {
                beeCalcApp.calendarRepository.observeTasks()
            } else {
                beeCalcApp.calendarRepository.observeTasksForApiary(apiaryUuid)
            }
            val hivesFlow = if (apiaryUuid.isNullOrBlank()) {
                beeCalcApp.hiveRepository.observeHives()
            } else {
                beeCalcApp.hiveRepository.observeHivesForApiary(apiaryUuid)
            }
            combine(tasksFlow, hivesFlow) { tasks, hives -> buildState(tasks, hives) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun markDone(task: CalendarTask) {
        viewModelScope.launch {
            beeCalcApp.calendarRepository.updateTask(task.copy(isDone = true, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun buildState(tasks: List<CalendarTask>, hives: List<Hive>): HomeUiState {
        val today = LocalDate.now()
        val active = tasks.filter { !it.isDeleted && !it.isDone }
        val dated = active.mapNotNull { task ->
            task.dueDateMillis?.let { task to dueStatus(it, today) }
        }
        return HomeUiState(
            hivesCount = hives.size,
            upcoming = dated.filter { it.second == DueStatus.UPCOMING }.map { it.first },
            overdue = dated.filter { it.second == DueStatus.OVERDUE }.map { it.first },
            critical = dated.filter { it.second == DueStatus.CRITICAL }.map { it.first },
            hasTasksWithoutDates = active.any { it.dueDateMillis == null }
        )
    }
}
