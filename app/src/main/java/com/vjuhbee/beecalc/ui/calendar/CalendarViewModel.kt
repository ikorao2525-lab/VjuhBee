package com.vjuhbee.beecalc.ui.calendar

import androidx.lifecycle.ViewModel
import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.StaticCalendarRepository
import com.vjuhbee.beecalc.model.CalendarTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

/** Состояние экрана календаря. */
data class CalendarUiState(
    val currentMonth: Int,                     // 1..12
    val tasksByMonth: Map<Int, List<CalendarTask>>,
    val expandedMonths: Set<Int>
)

class CalendarViewModel(
    repository: CalendarRepository = StaticCalendarRepository
) : ViewModel() {

    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            currentMonth = currentMonth,
            tasksByMonth = repository.getTasks().groupBy { it.month },
            // Текущий месяц раскрыт сразу — за ним пользователь и пришёл.
            expandedMonths = setOf(currentMonth)
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState

    fun toggleMonth(month: Int) {
        _uiState.update { state ->
            val expanded = if (month in state.expandedMonths) {
                state.expandedMonths - month
            } else {
                state.expandedMonths + month
            }
            state.copy(expandedMonths = expanded)
        }
    }
}
