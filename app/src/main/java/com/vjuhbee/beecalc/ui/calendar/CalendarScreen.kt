package com.vjuhbee.beecalc.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory

/**
 * Сезонный календарь работ (SPEC.md §5.2).
 * Работы можно отмечать выполненными, редактировать, удалять
 * и добавлять свои — база в Room, стартовый список для средней полосы.
 */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthNames = stringArrayResource(R.array.month_names)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "region_note") {
            Text(
                text = stringResource(R.string.calendar_region_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
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
                    TaskCard(
                        task = task,
                        onDoneChange = { done -> viewModel.setDone(task, done) },
                        onEdit = { viewModel.startEdit(task) }
                    )
                }
                item(key = "add_$month") {
                    OutlinedButton(
                        onClick = { viewModel.startAdd(month) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_add_task),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        TaskEditorDialog(
            editor = editor,
            monthNames = monthNames,
            onSave = { task -> viewModel.saveTask(task, editor.isNew) },
            onDelete = { viewModel.deleteTask(it) },
            onDismiss = { viewModel.closeEditor() }
        )
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
private fun TaskCard(
    task: CalendarTask,
    onDoneChange: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    // Нажатие раскрывает карточку: полное описание и кнопка «Изменить».
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onDoneChange
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 8.dp),
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
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = task.shortDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (expanded) {
                    if (task.fullDescription != null) {
                        Text(
                            text = task.fullDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.calendar_edit_task))
                    }
                }
            }
        }
    }
}

/** Диалог добавления/редактирования работы. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    editor: TaskEditor,
    monthNames: Array<String>,
    onSave: (CalendarTask) -> Unit,
    onDelete: (CalendarTask) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(editor.task.title) }
    var shortDescription by remember { mutableStateOf(editor.task.shortDescription) }
    var fullDescription by remember { mutableStateOf(editor.task.fullDescription.orEmpty()) }
    var month by remember { mutableStateOf(editor.task.month) }
    var category by remember { mutableStateOf(editor.task.category) }
    var isImportant by remember { mutableStateOf(editor.task.importance == Importance.HIGH) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(
                        if (editor.isNew) R.string.editor_title_new else R.string.editor_title_edit
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.editor_task_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortDescription,
                    onValueChange = { shortDescription = it },
                    label = { Text(stringResource(R.string.editor_short_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullDescription,
                    onValueChange = { fullDescription = it },
                    label = { Text(stringResource(R.string.editor_full_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Месяц: стрелки вместо выпадающего списка — крупно и наглядно.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { month = if (month == 1) 12 else month - 1 },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("◀") }
                    Text(
                        text = monthNames[month - 1],
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { month = if (month == 12) 1 else month + 1 },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("▶") }
                }

                Text(
                    text = stringResource(R.string.editor_category),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskCategory.entries.forEach { entry ->
                        FilterChip(
                            selected = category == entry,
                            onClick = { category = entry },
                            label = { Text(stringResource(categoryNameRes(entry))) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.calendar_important),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                }

                Button(
                    onClick = {
                        onSave(
                            editor.task.copy(
                                title = title.trim(),
                                shortDescription = shortDescription.trim(),
                                fullDescription = fullDescription.trim().ifBlank { null },
                                month = month,
                                category = category,
                                importance = if (isImportant) Importance.HIGH else Importance.NORMAL
                            )
                        )
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.titleMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.editor_cancel))
                    }
                    if (!editor.isNew) {
                        TextButton(
                            onClick = {
                                if (confirmDelete) onDelete(editor.task) else confirmDelete = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (confirmDelete) R.string.editor_delete_confirm else R.string.editor_delete
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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
