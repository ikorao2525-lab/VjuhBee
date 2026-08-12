package com.vjuhbee.beecalc.ui.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DueStatus { NONE, UPCOMING, OVERDUE, CRITICAL }

fun dueStatus(dueDateMillis: Long?, today: LocalDate = LocalDate.now()): DueStatus {
    if (dueDateMillis == null) return DueStatus.NONE
    val due = Instant.ofEpochMilli(dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val daysLate = ChronoUnit.DAYS.between(due, today)
    return when {
        daysLate < -14 -> DueStatus.NONE
        daysLate <= 0 -> DueStatus.UPCOMING
        daysLate <= 14 -> DueStatus.OVERDUE
        else -> DueStatus.CRITICAL
    }
}

fun DueStatus.labelRes(): Int? = when (this) {
    DueStatus.UPCOMING -> com.vjuhbee.beecalc.R.string.due_status_upcoming
    DueStatus.OVERDUE -> com.vjuhbee.beecalc.R.string.due_status_overdue
    DueStatus.CRITICAL -> com.vjuhbee.beecalc.R.string.due_status_critical
    DueStatus.NONE -> null
}
/** Совместимость для экранов, ещё не переведённых на stringResource. */
fun DueStatus.label(): String = when (this) {
    DueStatus.UPCOMING -> " · скоро"
    DueStatus.OVERDUE -> " · просрочено"
    DueStatus.CRITICAL -> " · критично"
    DueStatus.NONE -> ""
}