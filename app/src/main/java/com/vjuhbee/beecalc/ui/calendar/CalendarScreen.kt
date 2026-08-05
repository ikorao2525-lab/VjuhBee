package com.vjuhbee.beecalc.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory

/**
 * Сезонный календарь работ (SPEC.md §5.2).
 * Месяцы сворачиваются, текущий подсвечен и раскрыт при входе.
 */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthNames = stringArrayResource(R.array.month_names)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..12).forEach { month ->
            val tasks = state.tasksByMonth[month].orEmpty()
            val isCurrent = month == state.currentMonth
            val isExpanded = month in state.expandedMonths

            item(key = "month_$month") {
                MonthHeader(
                    name = monthNames[month - 1],
                    taskCount = tasks.size,
                    isCurrent = isCurrent,
                    isExpanded = isExpanded,
                    onClick = { viewModel.toggleMonth(month) }
                )
            }
            if (isExpanded) {
                items(tasks, key = { "task_${it.id}" }) { task ->
                    TaskCard(task)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    name: String,
    taskCount: Int,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCurrent) stringResource(R.string.calendar_current_month, name) else name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isExpanded) "−" else "+ $taskCount",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun TaskCard(task: CalendarTask) {
    // Полное описание раскрывается по нажатию, если оно есть.
    var showFull by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = task.fullDescription != null) { showFull = !showFull }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryLabel(task.category)
                if (task.importance == Importance.HIGH) {
                    ImportantLabel()
                }
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = task.shortDescription,
                style = MaterialTheme.typography.bodyLarge
            )
            if (showFull && task.fullDescription != null) {
                Text(
                    text = task.fullDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (task.fullDescription != null) {
                Text(
                    text = stringResource(R.string.calendar_tap_for_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Категория показывается текстом, не только цветом (SPEC.md §8). */
@Composable
private fun CategoryLabel(category: TaskCategory) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = stringResource(categoryNameRes(category)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ImportantLabel() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = stringResource(R.string.calendar_important),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun categoryNameRes(category: TaskCategory): Int = when (category) {
    TaskCategory.INSPECTION -> R.string.category_inspection
    TaskCategory.FEEDING -> R.string.category_feeding
    TaskCategory.TREATMENT -> R.string.category_treatment
    TaskCategory.MAINTENANCE -> R.string.category_maintenance
    TaskCategory.HARVEST -> R.string.category_harvest
    TaskCategory.SEASONAL -> R.string.category_seasonal
    TaskCategory.OTHER -> R.string.category_other
}
