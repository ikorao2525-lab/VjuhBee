package com.vjuhbee.beecalc.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.ui.calendar.CalendarViewModel
import com.vjuhbee.beecalc.ui.calendar.DueStatus
import com.vjuhbee.beecalc.ui.calendar.TaskEditorDialog
import com.vjuhbee.beecalc.ui.calendar.label
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onCalendarClick: () -> Unit,
    onHivesClick: () -> Unit,
    onQrClick: () -> Unit,
    onReportsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val calendarState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingDone by remember { mutableStateOf<Set<String>>(emptySet()) }
    var upcomingExpanded by rememberSaveable { mutableStateOf(true) }
    var overdueExpanded by rememberSaveable { mutableStateOf(false) }
    var criticalExpanded by rememberSaveable { mutableStateOf(false) }
    var fadingDone by remember { mutableStateOf<Set<String>>(emptySet()) }
    val completionJobs = remember { mutableMapOf<String, Job>() }

    fun startCompletion(task: CalendarTask) {
        if (task.uuid in pendingDone) {
            completionJobs.remove(task.uuid)?.cancel()
            pendingDone = pendingDone - task.uuid
            fadingDone = fadingDone - task.uuid
            return
        }
        pendingDone = pendingDone + task.uuid
        completionJobs[task.uuid] = scope.launch {
            delay(2_500)
            fadingDone = fadingDone + task.uuid
            delay(500)
            viewModel.markDone(task)
            pendingDone = pendingDone - task.uuid
            fadingDone = fadingDone - task.uuid
            completionJobs.remove(task.uuid)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_hives_count, state.hivesCount))
                    if (state.hasTasksWithoutDates) Text(stringResource(R.string.home_undated_note), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { QuickActions(onCalendarClick, onHivesClick, onQrClick, onReportsClick) }
        item { TaskSection(R.string.home_upcoming, state.upcoming, DueStatus.UPCOMING, upcomingExpanded, { upcomingExpanded = !upcomingExpanded }, calendarViewModel::startEdit, ::startCompletion, pendingDone, fadingDone) }
        item { TaskSection(R.string.home_overdue, state.overdue, DueStatus.OVERDUE, overdueExpanded, { overdueExpanded = !overdueExpanded }, calendarViewModel::startEdit, ::startCompletion, pendingDone, fadingDone) }
        item { TaskSection(R.string.home_critical, state.critical, DueStatus.CRITICAL, criticalExpanded, { criticalExpanded = !criticalExpanded }, calendarViewModel::startEdit, ::startCompletion, pendingDone, fadingDone) }
        if (state.upcoming.isEmpty() && state.overdue.isEmpty() && state.critical.isEmpty()) item { Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge) }
    }

    calendarState.editor?.let { editor ->
        TaskEditorDialog(
            editor = editor,
            monthNames = stringArrayResource(R.array.month_names),
            hives = calendarState.hives,
            onSave = { calendarViewModel.saveTask(it, editor.isNew) },
            onDelete = { calendarViewModel.deleteTask(it) },
            onDismiss = { calendarViewModel.closeEditor() }
        )
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
private fun TaskSection(titleRes: Int, tasks: List<CalendarTask>, status: DueStatus, expanded: Boolean, onToggle: () -> Unit, onOpen: (CalendarTask) -> Unit, onDone: (CalendarTask) -> Unit, pending: Set<String>, fading: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(titleRes) + " (" + tasks.size + ")", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggle) { Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null) }
        }
        if (expanded) {
            tasks.forEach { task ->
                AnimatedVisibility(visible = task.uuid !in fading, exit = fadeOut()) {
                    HomeTaskRow(task, status, onOpen, onDone, task.uuid in pending)
                }
            }
        }
    }
}

@Composable
private fun HomeTaskRow(task: CalendarTask, status: DueStatus, onOpen: (CalendarTask) -> Unit, onDone: (CalendarTask) -> Unit, isPending: Boolean) {
    key(task.uuid, isPending) {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(isPending) {
            progress.snapTo(0f)
            if (isPending) progress.animateTo(1f, animationSpec = tween(3_000))
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(task) }) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textDecoration = if (isPending) TextDecoration.LineThrough else TextDecoration.None)
                    Text(task.shortDescription)
                    task.dueDateMillis?.let { Text(stringResource(R.string.home_due_date, formatHomeDate(it), status.label()), color = if (status == DueStatus.UPCOMING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                }
                if (isPending) {
                    CircularProgressIndicator(progress = { progress.value }, modifier = Modifier.size(24.dp).padding(2.dp), strokeWidth = 3.dp)
                }
                Checkbox(checked = isPending, onCheckedChange = { onDone(task) }, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun formatHomeDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
