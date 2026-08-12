package com.vjuhbee.beecalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.domain.ApiaryExpansionCalculator
import com.vjuhbee.beecalc.domain.ApiaryExpansionResult
import com.vjuhbee.beecalc.domain.QueenSource
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun ApiaryExpansionScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as BeeCalcApp
    val scope = rememberCoroutineScope()
    var donors by remember { mutableStateOf("3") }
    var splitsPerDonor by remember { mutableStateOf("2") }
    var broodFrames by remember { mutableStateOf("2") }
    var feedFrames by remember { mutableStateOf("1") }
    var availableQueens by remember { mutableStateOf("0") }
    var queenSource by remember { mutableStateOf(QueenSource.OWN) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ApiaryExpansionResult?>(null) }
    var error by remember { mutableStateOf(false) }
    var confirmCreate by remember { mutableStateOf(false) }
    var created by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var formationDate by remember { mutableStateOf(startOfToday()) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.calculator_expansion), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.expansion_formula_note), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.expansion_formation_date, formatPlanDate(formationDate)))
        }
        NumberField(R.string.expansion_donors, donors) { donors = it }
        NumberField(R.string.expansion_splits_per_donor, splitsPerDonor) { splitsPerDonor = it }
        NumberField(R.string.expansion_brood_frames, broodFrames) { broodFrames = it }
        NumberField(R.string.expansion_feed_frames, feedFrames) { feedFrames = it }
        NumberField(R.string.expansion_available_queens, availableQueens) { availableQueens = it }
        OutlinedButton(onClick = { sourceExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(queenSourceLabel(queenSource))) }
        DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
            QueenSource.entries.forEach { source -> DropdownMenuItem(text = { Text(stringResource(queenSourceLabel(source))) }, onClick = { queenSource = source; sourceExpanded = false }) }
        }
        Button(onClick = {
            val values = listOf(donors, splitsPerDonor, broodFrames, feedFrames, availableQueens).map { it.decimalOrNull() }
            error = values.any { it == null }
            result = if (error) null else ApiaryExpansionCalculator.calculate(values[0]!!, values[1]!!, values[2]!!, values[3]!!, values[4]!!, queenSource)
            created = false
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_calculate)) }
        if (error) Text(stringResource(R.string.calculator_invalid_input), color = MaterialTheme.colorScheme.error)
        result?.let { calculated ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.expansion_result_splits, calculated.splits))
                    Text(stringResource(R.string.expansion_result_donors, calculated.donorFamilies))
                    Text(stringResource(R.string.expansion_result_brood, calculated.broodFrames))
                    Text(stringResource(R.string.expansion_result_feed, calculated.feedFrames))
                    Text(stringResource(R.string.expansion_result_queens, calculated.queenUnitsNeeded))
                    Text(stringResource(R.string.expansion_result_acquire, calculated.queenUnitsToAcquire), style = MaterialTheme.typography.titleMedium)
                }
            }
            Button(onClick = { confirmCreate = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.expansion_create_tasks)) }
        }
        if (created) Text(stringResource(R.string.expansion_tasks_created), color = MaterialTheme.colorScheme.primary)
        if (created) Text(stringResource(R.string.expansion_tasks_unlinked), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.calc_approximate_note), style = MaterialTheme.typography.bodySmall)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_back)) }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = formationDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { pickerState.selectedDateMillis?.let { formationDate = it }; showDatePicker = false }) { Text(stringResource(R.string.editor_due_date_confirm)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.editor_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
    if (confirmCreate) {
        AlertDialog(
            onDismissRequest = { confirmCreate = false },
            title = { Text(stringResource(R.string.expansion_confirm_title)) },
            text = { Text(stringResource(R.string.expansion_confirm_text, result?.splits ?: 0)) },
            confirmButton = {
                Button(onClick = {
                    val calculated = result ?: return@Button
                    confirmCreate = false
                    scope.launch { createExpansionTasks(app.calendarRepository, calculated, queenSource, formationDate); created = true }
                }) { Text(stringResource(R.string.expansion_confirm_create)) }
            },
            dismissButton = { Button(onClick = { confirmCreate = false }) { Text(stringResource(R.string.expansion_confirm_cancel)) } }
        )
    }
}

@Composable
private fun NumberField(label: Int, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
}

private fun queenSourceLabel(source: QueenSource): Int = when (source) {
    QueenSource.OWN -> R.string.expansion_queen_source_own
    QueenSource.PURCHASED -> R.string.expansion_queen_source_purchased
    QueenSource.QUEEN_CELLS -> R.string.expansion_queen_source_cells
}

private suspend fun createExpansionTasks(repository: CalendarRepository, result: ApiaryExpansionResult, source: QueenSource, formationDate: Long) {
    val date = Calendar.getInstance().apply { timeInMillis = formationDate }
    val year = date.get(Calendar.YEAR)
    val month = date.get(Calendar.MONTH) + 1
    val preparationDate = formationDate - 86400000L
    val firstCheckDate = formationDate + 604800000L
    val queenCheckDate = formationDate + 1814400000L
    val now = System.currentTimeMillis()
    val queenTitle = when (source) {
        QueenSource.OWN -> "Подготовить свои матки или маточники"
        QueenSource.PURCHASED -> "Купить маток для отводков"
        QueenSource.QUEEN_CELLS -> "Подготовить маточники для отводков"
    }
    val tasks = listOf(
        CalendarTask(0, year, month, dueDateMillis = preparationDate, title = "Подготовить рамки с расплодом", shortDescription = "Подготовить ${result.broodFrames} рамок для ${result.splits} отводков", category = TaskCategory.MAINTENANCE, importance = Importance.HIGH),
        CalendarTask(0, year, month, dueDateMillis = preparationDate, title = "Подготовить кормовые рамки", shortDescription = "Подготовить ${result.feedFrames} кормовых рамок", category = TaskCategory.MAINTENANCE),
        CalendarTask(0, year, month, dueDateMillis = preparationDate, title = queenTitle, shortDescription = "Нужно подготовить ${result.queenUnitsNeeded} маток или маточников", category = TaskCategory.OTHER, importance = Importance.HIGH),
        CalendarTask(0, year, month, dueDateMillis = formationDate, title = "Сформировать отводки", shortDescription = "Сформировать ${result.splits} новых отводков из ${result.donorFamilies} семей-доноров", category = TaskCategory.OTHER, importance = Importance.HIGH),
        CalendarTask(0, year, month, dueDateMillis = firstCheckDate, title = "Проверить сформированные отводки", shortDescription = "Первый контроль состояния ${result.splits} отводков", category = TaskCategory.INSPECTION),
        CalendarTask(0, year, month, dueDateMillis = queenCheckDate, title = "Проверить наличие матки и яиц", shortDescription = "Контроль матки и яйцекладки в ${result.splits} отводках", category = TaskCategory.INSPECTION, importance = Importance.HIGH)
    ).map { it.copy(uuid = UUID.randomUUID().toString(), updatedAt = now) }
    tasks.forEach { repository.addTask(it) }
}

private fun startOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun formatPlanDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
