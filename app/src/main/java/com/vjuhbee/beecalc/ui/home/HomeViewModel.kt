package com.vjuhbee.beecalc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.ui.calendar.DueStatus
import com.vjuhbee.beecalc.ui.calendar.dueStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val hivesCount: Int = 0,
    val upcoming: List<CalendarTask> = emptyList(),
    val overdue: List<CalendarTask> = emptyList(),
    val critical: List<CalendarTask> = emptyList(),
    val hasTasksWithoutDates: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val beeCalcApp = app as BeeCalcApp

    val uiState: StateFlow<HomeUiState> = combine(
        beeCalcApp.calendarRepository.observeTasks(),
        beeCalcApp.hiveRepository.observeHives()
    ) { tasks, hives -> buildState(tasks, hives) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

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