package com.vjuhbee.beecalc.ui.hives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import com.vjuhbee.beecalc.utils.formatDate

/**
 * Экран одного улья (SPEC.md §7): данные улья и история —
 * осмотры и обработки вперемешку, свежие сверху.
 */
@Composable
fun HiveDetailScreen(
    onBack: () -> Unit,
    viewModel: HiveDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // После удаления улья возвращаемся к списку.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    val hive = state.hive ?: return
    var confirmDeleteHive by remember { mutableStateOf(false) }

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
            }
        }

        item(key = "history_title") {
            Text(
                text = stringResource(R.string.hive_history_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val history = buildHistory(state.inspections, state.treatments)
        if (history.isEmpty()) {
            item(key = "history_empty") {
                Text(
                    text = stringResource(R.string.hive_history_empty),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        items(history, key = { it.key }) { entry ->
            when (entry) {
                is HistoryEntry.InspectionEntry -> InspectionCard(
                    inspection = entry.inspection,
                    onDelete = { viewModel.deleteInspection(entry.inspection) }
                )
                is HistoryEntry.TreatmentEntry -> TreatmentCard(
                    treatment = entry.treatment,
                    onDelete = { viewModel.deleteTreatment(entry.treatment) }
                )
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
        null -> Unit
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
