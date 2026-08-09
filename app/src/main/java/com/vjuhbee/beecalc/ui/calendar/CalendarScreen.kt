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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.YearHarvestTotals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Сезонный календарь работ (SPEC.md §5.2).
 * Структура: годы → месяцы → работы. Текущий год и месяц раскрыты.
 * Прошлые годы — архив с сохранёнными отметками.
 * Удалённые работы лежат в корзине внизу, их можно вернуть.
 */
@Composable
fun CalendarScreen(
    year: Int? = null,
    month: Int? = null,
    taskUuid: String? = null,
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthNames = stringArrayResource(R.array.month_names)
    var trashExpanded by remember { mutableStateOf(false) }
    // uuid улья -> имя: для показа привязанных ульев в карточке работы.
    val nameByUuid = state.hives.associate { it.uuid to it.name }
    var filtersExpanded by remember { mutableStateOf(false) }
    val filtersActive = state.searchQuery.isNotBlank() || state.statusFilter != TaskStatusFilter.ALL || state.categoryFilter != null || state.importantOnly

    // Переход из экрана улья (маршрут calendar/{year}/{month}): раскрыть
    // нужный год+месяц, где работа видна (SPEC.md §5.2, v0.5).
    if (year != null && month != null) {
        LaunchedEffect(year, month) {
            viewModel.expandTo(year, month)
            taskUuid?.let { uuid ->
                state.tasksByYearMonth[year]?.get(month)?.firstOrNull { it.uuid == uuid }?.let(viewModel::startEdit)
            }
        }
    }

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

        item(key = "calendar_filters_button") {
            val activeCount = listOf(
                state.searchQuery.isNotBlank(),
                state.statusFilter != TaskStatusFilter.ALL,
                state.categoryFilter != null,
                state.importantOnly
            ).count { it }
            Surface(
                color = if (filtersActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.calendar_filters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (filtersActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (filtersActive) {
                        Text(
                            text = stringResource(R.string.calendar_filter_active_count, activeCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { filtersExpanded = true }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.calendar_filters), tint = if (filtersActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        state.years.forEach { year ->
            val isCurrentYear = year == state.currentYear
            val monthsOfYear = state.tasksByYearMonth[year].orEmpty()

            item(key = "year_$year") {
                YearHeader(
                    year = year,
                    harvest = state.harvestByYear[year] ?: YearHarvestTotals(),
                    isCurrent = isCurrentYear,
                    isExpanded = year in state.expandedYears,
                    onClick = { viewModel.toggleYear(year) }
                )
            }

            if (year in state.expandedYears) {
                (1..12).forEach { month ->
                    val tasks = monthsOfYear[month].orEmpty()
                    val isCurrentMonth = isCurrentYear && month == state.currentMonth
                    val isExpanded = MonthKey(year, month) in state.expandedMonths

                    item(key = "month_${year}_$month") {
                        MonthHeader(
                            name = monthNames[month - 1],
                            taskCount = tasks.size,
                            isCurrent = isCurrentMonth,
                            isExpanded = isExpanded,
                            onClick = { viewModel.toggleMonth(year, month) }
                        )
                    }
                    if (isExpanded) {
                        items(tasks, key = { "task_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                hiveNames = task.linkedHiveUuids.mapNotNull { nameByUuid[it] },
                                onDoneChange = { done -> viewModel.setDone(task, done) },
                                onEdit = { viewModel.startEdit(task) },
                                onDelete = { viewModel.deleteTask(task) }
                            )
                        }
                        item(key = "add_${year}_$month") {
                            OutlinedButton(
                                onClick = { viewModel.startAdd(year, month) },
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
        }

        if (state.deletedTasks.isNotEmpty()) {
            item(key = "trash_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { trashExpanded = !trashExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_trash_header, state.deletedTasks.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (trashExpanded) "−" else "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            if (trashExpanded) {
                items(state.deletedTasks, key = { "trash_${it.id}" }) { task ->
                    TrashCard(
                        task = task,
                        monthNames = monthNames,
                        onRestore = { viewModel.restoreTask(task) },
                        onDeleteForever = { viewModel.deleteForever(task) }
                    )
                }
            }
        }
    }

    if (filtersExpanded) {
        AlertDialog(
            onDismissRequest = { filtersExpanded = false },
            title = { Text(stringResource(R.string.calendar_filters)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        label = { Text(stringResource(R.string.calendar_search)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(stringResource(R.string.calendar_filter_status), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.statusFilter == TaskStatusFilter.ALL, onClick = { viewModel.setStatusFilter(TaskStatusFilter.ALL) }, label = { Text(stringResource(R.string.calendar_filter_all)) })
                        FilterChip(selected = state.statusFilter == TaskStatusFilter.ACTIVE, onClick = { viewModel.setStatusFilter(TaskStatusFilter.ACTIVE) }, label = { Text(stringResource(R.string.calendar_filter_active)) })
                        FilterChip(selected = state.statusFilter == TaskStatusFilter.DONE, onClick = { viewModel.setStatusFilter(TaskStatusFilter.DONE) }, label = { Text(stringResource(R.string.calendar_filter_done)) })
                    }
                    Text(stringResource(R.string.calendar_filter_category), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.categoryFilter == null, onClick = { viewModel.setCategoryFilter(null) }, label = { Text(stringResource(R.string.calendar_filter_category_all)) })
                        TaskCategory.entries.forEach { category ->
                            FilterChip(selected = state.categoryFilter == category, onClick = { viewModel.setCategoryFilter(category) }, label = { Text(stringResource(categoryNameRes(category))) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.calendar_filter_important), modifier = Modifier.weight(1f))
                        Switch(checked = state.importantOnly, onCheckedChange = viewModel::setImportantOnly)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { filtersExpanded = false }) {
                    Text(stringResource(R.string.calendar_filter_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearFilters) {
                    Text(stringResource(R.string.calendar_filter_clear))
                }
            }
        )
    }
    state.editor?.let { editor ->
        TaskEditorDialog(
            editor = editor,
            monthNames = monthNames,
            hives = state.hives,
            onSave = { task -> viewModel.saveTask(task, editor.isNew) },
            onDelete = { viewModel.deleteTask(it) },
            onDismiss = { viewModel.closeEditor() }
        )
    }
}

@Composable
private fun YearHeader(
    year: Int,
    harvest: YearHarvestTotals,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCurrent) {
                        stringResource(R.string.calendar_current_year, year)
                    } else {
                        stringResource(R.string.calendar_year, year)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                HarvestSummary(harvest = harvest, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = if (isExpanded) "−" else "+",
                style = MaterialTheme.typography.headlineSmall
            )
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
private fun TaskCard(
    task: CalendarTask,
    hiveNames: List<String>,
    onDoneChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Нажатие раскрывает карточку: полное описание и кнопки действий.
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
                task.dueDateMillis?.let { dueDate ->
                    Text(stringResource(R.string.calendar_due_date, formatDueDate(dueDate)) + dueStatus(dueDate).label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = task.shortDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (hiveNames.isNotEmpty()) {
                    Text(
                        text = hiveNames.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                HarvestSummary(
                    harvest = task.toHarvestTotals(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    bold = true
                )
                if (expanded) {
                    if (task.fullDescription != null) {
                        Text(
                            text = task.fullDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(text = stringResource(R.string.calendar_edit_task))
                        }
                        // Удаление обратимо: работа уезжает в корзину внизу списка.
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.editor_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashCard(
    task: CalendarTask,
    monthNames: Array<String>,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    var confirmForever by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${monthNames[task.month - 1]} ${task.year}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.calendar_restore))
                }
                OutlinedButton(
                    onClick = {
                        if (confirmForever) onDeleteForever() else confirmForever = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (confirmForever) R.string.calendar_delete_forever_confirm
                            else R.string.calendar_delete_forever
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** Диалог добавления/редактирования работы. */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    editor: TaskEditor,
    monthNames: Array<String>,
    hives: List<Hive>,
    onSave: (CalendarTask) -> Unit,
    onDelete: (CalendarTask) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(editor.task.title) }
    var shortDescription by remember { mutableStateOf(editor.task.shortDescription) }
    var fullDescription by remember { mutableStateOf(editor.task.fullDescription.orEmpty()) }
    var month by remember { mutableStateOf(editor.task.month) }
    var dueDateMillis by remember { mutableStateOf(editor.task.dueDateMillis) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(editor.task.category) }
    var isImportant by remember { mutableStateOf(editor.task.importance == Importance.HIGH) }
    var linkedUuids by remember {
        mutableStateOf(editor.task.linkedHiveUuids.toSet())
    }
    var harvestDrafts by remember { mutableStateOf(editor.task.harvestItems.map { HarvestDraft(it.product, formatAmount(it.amount), it.unit) }) }
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

                OutlinedButton(onClick = { showDueDatePicker = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (dueDateMillis == null) stringResource(R.string.editor_due_date_add) else stringResource(R.string.editor_due_date_value, formatDueDate(dueDateMillis!!)))
                }
                if (dueDateMillis != null) {
                    TextButton(onClick = { dueDateMillis = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.editor_due_date_clear))
                    }
                }
                if (showDueDatePicker) {
                    val pickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
                    DatePickerDialog(onDismissRequest = { showDueDatePicker = false }, confirmButton = {
                        TextButton(onClick = { dueDateMillis = pickerState.selectedDateMillis; showDueDatePicker = false }) { Text(stringResource(R.string.editor_due_date_confirm)) }
                    }, dismissButton = {
                        TextButton(onClick = { showDueDatePicker = false }) { Text(stringResource(R.string.editor_cancel)) }
                    }) { DatePicker(state = pickerState) }
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

                // Привязка работы к ульям (SPEC.md §5.2, v0.5). Показываем
                // только если есть хотя бы один улей.
                if (hives.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_hives_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hives.forEach { hive ->
                            FilterChip(
                                selected = hive.uuid in linkedUuids,
                                onClick = {
                                    linkedUuids = if (hive.uuid in linkedUuids) {
                                        linkedUuids - hive.uuid
                                    } else {
                                        linkedUuids + hive.uuid
                                    }
                                },
                                label = { Text(hive.name) }
                            )
                        }
                    }
                }

Text(
                    text = stringResource(R.string.editor_harvest_section),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.editor_harvest_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                harvestDrafts.forEachIndexed { index, draft ->
                    HarvestDraftRow(
                        draft = draft,
                        onChange = { harvestDrafts = harvestDrafts.toMutableList().also { it[index] = draft.copy(product = it[index].product, amountText = it[index].amountText, unit = it[index].unit) } },
                        onProductChange = { product -> harvestDrafts = harvestDrafts.toMutableList().also { it[index] = draft.copy(product = product, unit = if (product == HarvestProduct.HONEY) draft.unit else product.defaultUnit) } },
                        onAmountChange = { value -> harvestDrafts = harvestDrafts.toMutableList().also { it[index] = draft.copy(amountText = value) } },
                        onUnitChange = { unit -> harvestDrafts = harvestDrafts.toMutableList().also { it[index] = draft.copy(unit = unit) } },
                        onRemove = { harvestDrafts = harvestDrafts.toMutableList().also { it.removeAt(index) } }
                    )
                }
                OutlinedButton(
                    onClick = { harvestDrafts = harvestDrafts + HarvestDraft(HarvestProduct.HONEY, "", HarvestUnit.KG) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.editor_add_harvest)) }
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
                                dueDateMillis = dueDateMillis,
                                category = category,
                                importance = if (isImportant) Importance.HIGH else Importance.NORMAL,
                                harvestItems = harvestDrafts.mapNotNull { draft -> parseAmount(draft.amountText)?.let { HarvestItem(draft.product, it, draft.unit) } },
                                linkedHiveUuids = linkedUuids.toList()
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
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Text(stringResource(R.string.editor_cancel), fontWeight = FontWeight.Bold)
                    }
                    if (!editor.isNew) {
                        OutlinedButton(
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

private data class HarvestDraft(
    val product: HarvestProduct,
    val amountText: String,
    val unit: HarvestUnit
)

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HarvestDraftRow(
    draft: HarvestDraft,
    onChange: () -> Unit,
    onProductChange: (HarvestProduct) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (HarvestUnit) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var productExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = productExpanded,
                onExpandedChange = { productExpanded = !productExpanded }
            ) {
                OutlinedTextField(
                    value = harvestProductName(draft.product),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.editor_harvest_product)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = productExpanded,
                    onDismissRequest = { productExpanded = false }
                ) {
                    HarvestProduct.entries.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(harvestProductName(product)) },
                            onClick = { onProductChange(product); productExpanded = false }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.editor_harvest_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.product == HarvestProduct.HONEY) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(HarvestUnit.KG, HarvestUnit.LITER).forEach { unit ->
                        FilterChip(
                            selected = draft.unit == unit,
                            onClick = { onUnitChange(unit) },
                            label = { Text(stringResource(R.string.editor_harvest_unit_choice, unit.label)) }
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.editor_harvest_unit, draft.unit.label))
            }
            OutlinedButton(
                onClick = onRemove,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.editor_remove_harvest), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun harvestProductName(product: HarvestProduct): String = when (product) {
    HarvestProduct.HONEY -> "Мёд"
    HarvestProduct.POLLEN -> "Пыльца"
    HarvestProduct.BEE_BREAD -> "Перга"
    HarvestProduct.PROPOLIS -> "Прополис"
    HarvestProduct.WAX -> "Воск"
    HarvestProduct.ROYAL_JELLY -> "Маточное молочко"
    HarvestProduct.CAPPINGS -> "Забрус"
    HarvestProduct.WAX_MERVA -> "Мерва"
    HarvestProduct.BEE_VENOM -> "Пчелиный яд"
    HarvestProduct.WINTER_BEES -> "Подмор"
    HarvestProduct.QUEENS -> "Матки"
    HarvestProduct.NUCLEUS_COLONIES -> "Отводки"
    HarvestProduct.PACKAGE_BEES -> "Пчелопакеты"
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

@Composable
private fun HarvestField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Сводка по сбору: только ненулевые продукты, через « · ». */
@Composable
private fun HarvestSummary(
    harvest: YearHarvestTotals,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    bold: Boolean = false
) {
    if (!harvest.hasAny) return
    val parts = buildList {
        if (harvest.honeyKg > 0) {
            add(stringResource(R.string.harvest_honey_kg, formatAmount(harvest.honeyKg)))
        }
        if (harvest.honeyLiters > 0) {
            add(stringResource(R.string.harvest_honey_l, formatAmount(harvest.honeyLiters)))
        }
        if (harvest.pollenKg > 0) {
            add(stringResource(R.string.harvest_pollen, formatAmount(harvest.pollenKg)))
        }
        if (harvest.beeBreadKg > 0) {
            add(stringResource(R.string.harvest_bee_bread, formatAmount(harvest.beeBreadKg)))
        }
        if (harvest.propolisGrams > 0) {
            add(stringResource(R.string.harvest_propolis, formatAmount(harvest.propolisGrams)))
        }
        if (harvest.waxKg > 0) {
            add(stringResource(R.string.harvest_wax, formatAmount(harvest.waxKg)))
        }
        if (harvest.royalJellyGrams > 0) {
            add(stringResource(R.string.harvest_royal_jelly, formatAmount(harvest.royalJellyGrams)))
        }
    }
    Text(
        text = parts.joinToString(" · "),
        style = style,
        fontWeight = if (bold) FontWeight.Bold else null,
        color = color
    )
}

private fun CalendarTask.toHarvestTotals() = YearHarvestTotals(
    honeyKg = honeyKg ?: 0.0,
    honeyLiters = honeyLiters ?: 0.0,
    pollenKg = pollenKg ?: 0.0,
    beeBreadKg = beeBreadKg ?: 0.0,
    propolisGrams = propolisGrams ?: 0.0,
    waxKg = waxKg ?: 0.0,
    royalJellyGrams = royalJellyGrams ?: 0.0
)

/** «120» или «37.5» — без хвоста нулей. */
private fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", value)

private fun Double?.toAmountText(): String =
    this?.takeIf { it > 0 }?.let { formatAmount(it) } ?: ""

/** Понимает и запятую (русская клавиатура), и точку. */
private fun parseAmount(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

private fun categoryNameRes(category: TaskCategory): Int = when (category) {
    TaskCategory.INSPECTION -> R.string.category_inspection
    TaskCategory.FEEDING -> R.string.category_feeding
    TaskCategory.TREATMENT -> R.string.category_treatment
    TaskCategory.MAINTENANCE -> R.string.category_maintenance
    TaskCategory.HARVEST -> R.string.category_harvest
    TaskCategory.SEASONAL -> R.string.category_seasonal
    TaskCategory.OTHER -> R.string.category_other
}

private fun formatDueDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
