package com.vjuhbee.beecalc.ui.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DueDateTest {
    private val today = LocalDate.of(2026, 8, 8)

    @Test fun nullDateHasNoStatus() = assertEquals(DueStatus.NONE, dueStatus(null, today))
    @Test fun todayIsUpcoming() = assertEquals(DueStatus.UPCOMING, dueStatus(today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), today))
    @Test fun dateWithinFutureTwoWeeksIsUpcoming() = assertEquals(DueStatus.UPCOMING, dueStatus(today.plusDays(2).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), today))
    @Test fun dateUpToFourteenDaysLateIsOverdue() = assertEquals(DueStatus.OVERDUE, dueStatus(today.minusDays(14).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), today))
    @Test fun dateOlderThanFourteenDaysIsCritical() = assertEquals(DueStatus.CRITICAL, dueStatus(today.minusDays(15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), today))
}