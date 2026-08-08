package com.vjuhbee.beecalc.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.ui.calendar.DueStatus
import com.vjuhbee.beecalc.ui.calendar.label
import com.vjuhbee.beecalc.ui.calendar.dueStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onCalendarClick: () -> Unit,
    onHivesClick: () -> Unit,
    onQrClick: () -> Unit,
    onReportsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_hives_count, state.hivesCount))
                    if (state.hasTasksWithoutDates) {
                        Text(stringResource(R.string.home_undated_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { QuickActions(onCalendarClick, onHivesClick, onQrClick, onReportsClick) }
        item { TaskSection(R.string.home_upcoming, state.upcoming, DueStatus.UPCOMING) }
        item { TaskSection(R.string.home_overdue, state.overdue, DueStatus.OVERDUE) }
        item { TaskSection(R.string.home_critical, state.critical, DueStatus.CRITICAL) }
        if (state.upcoming.isEmpty() && state.overdue.isEmpty() && state.critical.isEmpty()) {
            item { Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun QuickActions(onCalendar: () -> Unit, onHives: () -> Unit, onQr: () -> Unit, onReports: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_quick_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCalendar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_calendar)) }
                OutlinedButton(onClick = onHives, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_hives)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onQr, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_qr)) }
                Button(onClick = onReports, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.home_reports)) }
            }
        }
    }
}

@Composable
private fun TaskSection(titleRes: Int, tasks: List<CalendarTask>, status: DueStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        tasks.forEach { HomeTaskRow(it, status) }
    }
}

@Composable
private fun HomeTaskRow(task: CalendarTask, status: DueStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(task.shortDescription)
            task.dueDateMillis?.let {
                Text(
                    stringResource(R.string.home_due_date, formatHomeDate(it), status.label()),
                    color = if (status == DueStatus.UPCOMING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatHomeDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))