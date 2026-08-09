package com.vjuhbee.beecalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.domain.HoneyJarsCalculator
import com.vjuhbee.beecalc.domain.HoneyJarsResult
import com.vjuhbee.beecalc.domain.WinterFeedCalculator
import com.vjuhbee.beecalc.domain.WinterFeedResult
import java.util.Locale

@Composable
fun WinterFeedScreen(onBack: () -> Unit) {
    var families by remember { mutableStateOf("10") }
    var months by remember { mutableStateOf("5") }
    var consumption by remember { mutableStateOf("2") }
    var reserve by remember { mutableStateOf("10") }
    var result by remember { mutableStateOf<WinterFeedResult?>(null) }
    var error by remember { mutableStateOf(false) }
    SimpleCalculatorScreen(
        title = R.string.calculator_winter,
        note = R.string.winter_formula_note,
        fields = listOf(
            Field(R.string.winter_families, families) { families = it },
            Field(R.string.winter_months, months) { months = it },
            Field(R.string.winter_consumption, consumption) { consumption = it },
            Field(R.string.winter_reserve, reserve) { reserve = it }
        ),
        error = error,
        onCalculate = {
            val values = listOf(families, months, consumption, reserve).map { it.decimalOrNull() }
            error = values.any { it == null }
            result = if (error) null else WinterFeedCalculator.calculate(values[0]!!, values[1]!!, values[2]!!, values[3]!!)
        },
        result = result?.let { stringResource(R.string.winter_result, formatValue(it.baseKilograms), formatValue(it.totalKilograms)) },
        onBack = onBack
    )
}

@Composable
fun HoneyJarsScreen(onBack: () -> Unit) {
    var honey by remember { mutableStateOf("37") }
    var jar by remember { mutableStateOf("0.5") }
    var result by remember { mutableStateOf<HoneyJarsResult?>(null) }
    var error by remember { mutableStateOf(false) }
    SimpleCalculatorScreen(
        title = R.string.calculator_honey_jars,
        note = R.string.honey_jars_formula_note,
        fields = listOf(
            Field(R.string.honey_total, honey) { honey = it },
            Field(R.string.honey_jar_size, jar) { jar = it }
        ),
        error = error,
        onCalculate = {
            val values = listOf(honey, jar).map { it.decimalOrNull() }
            error = values.any { it == null } || values[1] == 0.0
            result = if (error) null else HoneyJarsCalculator.calculate(values[0]!!, values[1]!!)
        },
        result = result?.let { stringResource(R.string.honey_jars_result, it.fullJars, formatValue(it.remainderKilograms)) },
        onBack = onBack
    )
}

private data class Field(val label: Int, val value: String, val onValueChange: (String) -> Unit)

@Composable
private fun SimpleCalculatorScreen(title: Int, note: Int, fields: List<Field>, error: Boolean, onCalculate: () -> Unit, result: String?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(note), style = MaterialTheme.typography.bodyMedium)
        fields.forEach { field -> OutlinedTextField(field.value, field.onValueChange, Modifier.fillMaxWidth(), label = { Text(stringResource(field.label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
        Button(onClick = onCalculate, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_calculate)) }
        if (error) Text(stringResource(R.string.calculator_invalid_input), color = MaterialTheme.colorScheme.error)
        result?.let { Card(modifier = Modifier.fillMaxWidth()) { Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall) } }
        Text(stringResource(R.string.calc_approximate_note), style = MaterialTheme.typography.bodySmall)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_back)) }
    }
}

private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
private fun formatValue(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
