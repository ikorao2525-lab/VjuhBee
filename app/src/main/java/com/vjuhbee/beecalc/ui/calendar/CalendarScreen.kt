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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.YearHarvestTotals

/**
 * Сезонный календарь работ (SPEC.md §5.2).
 * Структура: годы → месяцы → работы. Текущий год и месяц раскрыты.
 * Прошлые годы — архив с сохранёнными отметками.
 * Удалённые работы лежат в корзине внизу, их можно вернуть.
 */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthNames = stringArrayResource(R.array.month_names)
    var trashExpanded by remember { mutableStateOf(false) }

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
                Text(
                    text = task.shortDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
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
    var honeyKgText by remember { mutableStateOf(editor.task.honeyKg.toAmountText()) }
    var honeyLitersText by remember { mutableStateOf(editor.task.honeyLiters.toAmountText()) }
    var pollenText by remember { mutableStateOf(editor.task.pollenKg.toAmountText()) }
    var beeBreadText by remember { mutableStateOf(editor.task.beeBreadKg.toAmountText()) }
    var propolisText by remember { mutableStateOf(editor.task.propolisGrams.toAmountText()) }
    var waxText by remember { mutableStateOf(editor.task.waxKg.toAmountText()) }
    var royalJellyText by remember { mutableStateOf(editor.task.royalJellyGrams.toAmountText()) }
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

                // Сбор продукции — необязательные поля, доступны для любой работы
                // (пыльцу или прополис можно собрать и в день «Ухода»).
                Text(
                    text = stringResource(R.string.editor_harvest_section),
                    style = MaterialTheme.typography.titleMedium
                )
                HarvestField(honeyKgText, { honeyKgText = it }, R.string.editor_honey_kg)
                HarvestField(honeyLitersText, { honeyLitersText = it }, R.string.editor_honey_l)
                HarvestField(pollenText, { pollenText = it }, R.string.editor_pollen)
                HarvestField(beeBreadText, { beeBreadText = it }, R.string.editor_bee_bread)
                HarvestField(propolisText, { propolisText = it }, R.string.editor_propolis)
                HarvestField(waxText, { waxText = it }, R.string.editor_wax)
                HarvestField(royalJellyText, { royalJellyText = it }, R.string.editor_royal_jelly)

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
                                importance = if (isImportant) Importance.HIGH else Importance.NORMAL,
                                honeyKg = parseAmount(honeyKgText),
                                honeyLiters = parseAmount(honeyLitersText),
                                pollenKg = parseAmount(pollenText),
                                beeBreadKg = parseAmount(beeBreadText),
                                propolisGrams = parseAmount(propolisText),
                                waxKg = parseAmount(waxText),
                                royalJellyGrams = parseAmount(royalJellyText)
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
