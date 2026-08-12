package com.vjuhbee.beecalc.ui.hives

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.YearHarvestTotals
import com.vjuhbee.beecalc.utils.createHiveQrPoster
import com.vjuhbee.beecalc.utils.formatDate
import com.vjuhbee.beecalc.utils.generateHiveQr
import com.vjuhbee.beecalc.utils.saveQrToCache

/**
 * Экран одного улья (SPEC.md §7): данные улья и история —
 * осмотры и обработки вперемешку, свежие сверху.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun HiveDetailScreen(
    onQuickReport: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiveDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var historyFiltersExpanded by remember { mutableStateOf(false) }
    val historyFiltersActive = state.historyQuery.isNotBlank() || state.historyTypeFilter != HistoryTypeFilter.ALL || state.historyYear != null

    // После удаления улья возвращаемся к списку.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    val hive = state.hive ?: return
    var confirmDeleteHive by remember { mutableStateOf(false) }
    // Выбранная привязанная работа — показываем её read-only поверх экрана.
    var selectedTask by remember { mutableStateOf<CalendarTask?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.hive_back))
                    }
                    Text(
                        text = hive.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hive.note.isNotBlank()) {
                    Text(text = hive.note, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onQuickReport(hive.uuid) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.hive_quick_report))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.openDialog(HiveDialog.AddInspection) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hive_add_inspection),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
                        onClick = { viewModel.openDialog(HiveDialog.AddTreatment) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hive_add_treatment),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.openDialog(HiveDialog.EditHive) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.calendar_edit_task))
                    }
                    OutlinedButton(
                        onClick = {
                            if (confirmDeleteHive) viewModel.deleteHive(hive)
                            else confirmDeleteHive = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (confirmDeleteHive) R.string.hive_delete_confirm
                                else R.string.hive_delete
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.openDialog(HiveDialog.ShowQr) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.hive_show_qr))
                    }
                }
            }
        }

        item(key = "quick_summary") {
            QuickHiveSummary(
                inspections = state.inspections.size,
                treatments = state.treatments.size,
                tasks = state.linkedTasks.size,
                harvestItems = state.linkedTasks.flatMap { it.harvestItems }
            )
        }

        item(key = "linked_tasks_title") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.hive_linked_tasks_title, state.linkedTasks.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (state.linkedTasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.hive_linked_tasks_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // Короткий список привязанных работ; по нажатию — подробности
                    // работы поверх экрана улья (SPEC.md §5.2, v0.5).
                    state.linkedTasks.forEach { task ->
                        LinkedTaskRow(
                            task = task,
                            onClick = { selectedTask = task }
                        )
                    }
                }
            }
        }

        item(key = "history_title") {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.hive_history_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { historyFiltersExpanded = true },
                    modifier = Modifier.clip(CircleShape).background(if (historyFiltersActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.hive_history_filters), tint = if (historyFiltersActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val history = buildHistory(state.inspections, state.treatments)
        if (history.isEmpty()) {
            item(key = "history_empty") { Text(stringResource(R.string.hive_history_empty), style = MaterialTheme.typography.bodyLarge) }
        }
        items(history, key = { it.key }) { entry ->
            when (entry) {
                is HistoryEntry.InspectionEntry -> InspectionCard(entry.inspection) { viewModel.deleteInspection(entry.inspection) }
                is HistoryEntry.TreatmentEntry -> TreatmentCard(entry.treatment) { viewModel.deleteTreatment(entry.treatment) }
            }
        }
    }

    if (historyFiltersExpanded) {
        Dialog(onDismissRequest = { historyFiltersExpanded = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.hive_history_filters), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = state.historyQuery, onValueChange = viewModel::setHistoryQuery, label = { Text(stringResource(R.string.hive_history_search)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text(stringResource(R.string.hive_history_filter_type), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.historyTypeFilter == HistoryTypeFilter.ALL, onClick = { viewModel.setHistoryTypeFilter(HistoryTypeFilter.ALL) }, label = { Text(stringResource(R.string.calendar_filter_all)) })
                        FilterChip(selected = state.historyTypeFilter == HistoryTypeFilter.INSPECTIONS, onClick = { viewModel.setHistoryTypeFilter(HistoryTypeFilter.INSPECTIONS) }, label = { Text(stringResource(R.string.inspection_label)) })
                        FilterChip(selected = state.historyTypeFilter == HistoryTypeFilter.TREATMENTS, onClick = { viewModel.setHistoryTypeFilter(HistoryTypeFilter.TREATMENTS) }, label = { Text(stringResource(R.string.treatment_label)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = viewModel::clearHistoryFilters) { Text(stringResource(R.string.calendar_filter_clear)) }
                        Button(onClick = { historyFiltersExpanded = false }) { Text(stringResource(R.string.calendar_filter_apply)) }
                    }
                }
            }
        }
    }

    when (state.dialog) {
        HiveDialog.EditHive -> HiveEditorDialog(
            editor = HiveEditor(hive = hive, isNew = false),
            onSave = { viewModel.saveHive(it) },
            onDismiss = { viewModel.closeDialog() }
        )
        HiveDialog.AddInspection -> InspectionDialog(
            onSave = { viewModel.addInspection(it) },
            onDismiss = { viewModel.closeDialog() }
        )
        HiveDialog.AddTreatment -> TreatmentDialog(
            onSave = { viewModel.addTreatment(it) },
            onDismiss = { viewModel.closeDialog() }
        )
        HiveDialog.ShowQr -> HiveQrDialog(
            hive = hive,
            onDismiss = { viewModel.closeDialog() }
        )
        null -> Unit
    }

    // Подробности привязанной работы поверх экрана улья (read-only).
    selectedTask?.let { task ->
        LinkedTaskDialog(
            task = task,
            hiveUuid = hive.uuid,
            onUnlink = {
                viewModel.unlinkTask(task, hive.uuid)
                selectedTask = null
            },
            onDismiss = { selectedTask = null }
        )
    }
}

@Composable
private fun QuickHiveSummary(
    inspections: Int,
    treatments: Int,
    tasks: Int,
    harvestItems: List<com.vjuhbee.beecalc.model.HarvestItem>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Сводка улья", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Работ: $tasks · осмотров: $inspections · обработок: $treatments")
            if (harvestItems.isEmpty()) {
                Text("Урожай: пока нет данных")
            } else {
                harvestItems.groupBy { it.product to it.unit }.forEach { (key, items) ->
                    Text("${key.first.code}: ${items.sumOf { it.amount }} ${key.second.label}")
                }
            }
        }
    }
}

/** История: осмотры и обработки одним списком, свежие сверху. */
private sealed interface HistoryEntry {
    val date: Long
    val key: String

    data class InspectionEntry(val inspection: Inspection) : HistoryEntry {
        override val date: Long get() = inspection.date
        override val key: String get() = "inspection_${inspection.id}"
    }

    data class TreatmentEntry(val treatment: Treatment) : HistoryEntry {
        override val date: Long get() = treatment.date
        override val key: String get() = "treatment_${treatment.id}"
    }
}

private fun buildHistory(
    inspections: List<Inspection>,
    treatments: List<Treatment>
): List<HistoryEntry> =
    (inspections.map { HistoryEntry.InspectionEntry(it) } +
        treatments.map { HistoryEntry.TreatmentEntry(it) })
        .sortedByDescending { it.date }

@Composable
private fun InspectionCard(inspection: Inspection, onDelete: () -> Unit) {
    RecordCard(
        label = stringResource(R.string.inspection_label),
        date = inspection.date,
        onDelete = onDelete
    ) {
        Text(
            text = stringResource(R.string.inspection_summary, inspection.frames, inspection.brood),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(
                if (inspection.queenSeen) R.string.queen_seen_yes else R.string.queen_seen_no
            ),
            style = MaterialTheme.typography.bodyLarge
        )
        if (inspection.note.isNotBlank()) {
            Text(text = inspection.note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TreatmentCard(treatment: Treatment, onDelete: () -> Unit) {
    RecordCard(
        label = stringResource(R.string.treatment_label),
        date = treatment.date,
        onDelete = onDelete
    ) {
        Text(
            text = treatment.medicine +
                if (treatment.dose.isNotBlank()) " — ${treatment.dose}" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        if (treatment.note.isNotBlank()) {
            Text(text = treatment.note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Общая карточка записи истории: метка типа, дата, удаление с подтверждением. */
@Composable
private fun RecordCard(
    label: String,
    date: Long,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = formatDate(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (confirmDelete) R.string.editor_delete_confirm else R.string.editor_delete
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            content()
        }
    }
}

/** Диалог нового осмотра: дата, рамки, расплод, матка, заметка. */
@Composable
private fun InspectionDialog(
    onSave: (Inspection) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var frames by remember { mutableIntStateOf(0) }
    var brood by remember { mutableIntStateOf(0) }
    var queenSeen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    RecordDialog(
        title = stringResource(R.string.inspection_label),
        date = date,
        onDateChange = { date = it },
        onSave = {
            onSave(
                Inspection(
                    id = 0, hiveId = 0, date = date,
                    frames = frames, brood = brood,
                    queenSeen = queenSeen, note = note.trim()
                )
            )
        },
        saveEnabled = true,
        onDismiss = onDismiss
    ) {
        NumberStepper(
            label = stringResource(R.string.inspection_frames),
            value = frames,
            onValueChange = { frames = it }
        )
        NumberStepper(
            label = stringResource(R.string.inspection_brood),
            value = brood,
            onValueChange = { brood = it }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.inspection_queen),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = queenSeen, onCheckedChange = { queenSeen = it })
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.note_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Диалог новой обработки: дата, препарат, доза, заметка. */
@Composable
private fun TreatmentDialog(
    onSave: (Treatment) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var medicine by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    RecordDialog(
        title = stringResource(R.string.treatment_label),
        date = date,
        onDateChange = { date = it },
        onSave = {
            onSave(
                Treatment(
                    id = 0, hiveId = 0, date = date,
                    medicine = medicine.trim(), dose = dose.trim(), note = note.trim()
                )
            )
        },
        saveEnabled = medicine.isNotBlank(),
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = medicine,
            onValueChange = { medicine = it },
            label = { Text(stringResource(R.string.treatment_medicine)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dose,
            onValueChange = { dose = it },
            label = { Text(stringResource(R.string.treatment_dose)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.note_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Каркас диалога записи: заголовок, кнопка даты, поля, сохранить/отмена. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDialog(
    title: String,
    date: Long,
    onDateChange: (Long) -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.date_label, formatDate(date)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                content()

                Button(
                    onClick = onSave,
                    enabled = saveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(onDateChange)
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Крупный степпер «− значение +»: удобнее цифровой клавиатуры в перчатках. */
@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = { if (value > 0) onValueChange(value - 1) },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp)
        )
        OutlinedButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Диалог QR-кода улья (SPEC.md §9, v0.4): показ QR, кнопки
 * «Поделиться» (Intent.ACTION_SEND через FileProvider) и «Печать»
 * (встроенный android.print.PrintHelper).
 */
@Composable
private fun HiveQrDialog(
    hive: Hive,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.hive_qr_share_title)
    val printTitle = stringResource(R.string.hive_qr_title)
    val qrBitmap: Bitmap? = remember(hive.uuid, hive.name, hive.note) {
        if (hive.uuid.isBlank()) null else generateHiveQr(hive.uuid, hive.name, hive.note)
    }

    fun shareQr() {
        // Постер (QR + название + заметка), а не голый QR.
        val poster = createHiveQrPoster(hive.uuid, hive.name, hive.note)
        val uri = saveQrToCache(context, poster, "hive_${hive.uuid}.png")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, shareTitle)
        )
    }

    fun printQr() {
        // Печатаем постер с подписью, а не голый QR.
        val poster = createHiveQrPoster(hive.uuid, hive.name, hive.note)
        val printHelper = androidx.print.PrintHelper(context)
        printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap(printTitle, poster)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.hive_qr_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                val image: ImageBitmap? = qrBitmap?.asImageBitmap()
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = stringResource(R.string.hive_qr_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else {
                    Text(
                        text = hive.uuid,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Подпись под QR: название и (обрезанная) заметка (SPEC.md §9).
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hive.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (hive.note.isNotBlank()) {
                        Text(
                            text = hive.note,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.hive_qr_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { shareQr() },
                        enabled = qrBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.hive_qr_share))
                    }
                    OutlinedButton(
                        onClick = { printQr() },
                        enabled = qrBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.hive_qr_print))
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.hive_qr_close), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * Короткая строка привязанной работы на экране улья (SPEC.md §5.2, v0.5).
 * Название + месяц/год; выполненные работы зачёркиваются и приглушаются.
 * По нажатию — подробности работы поверх экрана улья.
 */
@Composable
private fun LinkedTaskRow(
    task: CalendarTask,
    onClick: () -> Unit
) {
    val monthNames = stringArrayResource(R.array.month_names)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (task.isDone) {
                    "${monthNames[task.month - 1]} ${task.year} ✓"
                } else {
                    "${monthNames[task.month - 1]} ${task.year}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Подробности привязанной работы поверх экрана улья (SPEC.md §5.2, v0.5).
 * Полное описание как в календаре, но без редактирования. Кнопка
 * «Отвязать» убирает связь работы с ульем; сама работа в календаре
 * остаётся (для отчётов по годам).
 */
@Composable
private fun LinkedTaskDialog(
    task: CalendarTask,
    hiveUuid: String,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmUnlink by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                val monthNames = stringArrayResource(R.array.month_names)
                val categoryName = categoryNameRes(task.category)
                Text(
                    text = "${monthNames[task.month - 1]} ${task.year} · ${stringResource(categoryName)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (task.shortDescription.isNotBlank()) {
                    Text(
                        text = task.shortDescription,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (task.fullDescription != null) {
                    Text(
                        text = task.fullDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Статус выполнения, как в календаре (пункт 3).
                Text(
                    text = if (task.isDone) {
                        stringResource(R.string.hive_linked_task_done)
                    } else {
                        stringResource(R.string.hive_linked_task_pending)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                LinkedHarvestSummary(
                    harvest = task.toHarvestTotals(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    bold = true
                )

                TextButton(
                    onClick = {
                        if (confirmUnlink) onUnlink() else confirmUnlink = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (confirmUnlink) R.string.hive_linked_unlink_confirm
                            else R.string.hive_linked_unlink
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        }
    }
}

/** Сводка по сбору: только ненулевые продукты, через « · ». */
@Composable
private fun LinkedHarvestSummary(
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

private fun categoryNameRes(category: TaskCategory): Int = when (category) {
    TaskCategory.INSPECTION -> R.string.category_inspection
    TaskCategory.FEEDING -> R.string.category_feeding
    TaskCategory.TREATMENT -> R.string.category_treatment
    TaskCategory.MAINTENANCE -> R.string.category_maintenance
    TaskCategory.HARVEST -> R.string.category_harvest
    TaskCategory.SEASONAL -> R.string.category_seasonal
    TaskCategory.OTHER -> R.string.category_other
}
