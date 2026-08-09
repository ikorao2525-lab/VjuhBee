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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.vjuhbee.beecalc.domain.TreatmentCalculator
import com.vjuhbee.beecalc.domain.TreatmentResult
import java.util.Locale

@Composable
fun TreatmentScreen(onBack: () -> Unit) {
    var hivesText by remember { mutableStateOf("1") }
    var doseText by remember { mutableStateOf("1.0") }
    var reserveText by remember { mutableStateOf("10") }
    var unit by remember { mutableStateOf("мл") }
    var unitExpanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TreatmentResult?>(null) }
    var inputError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.calculator_treatment), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.treatment_formula_note), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(hivesText, { hivesText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.treatment_hives)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(doseText, { doseText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.treatment_dose)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedButton(onClick = { unitExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.treatment_unit_value, unit)) }
        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
            listOf("мл", "г", "л", "кг").forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { unit = option; unitExpanded = false })
            }
        }
        OutlinedTextField(reserveText, { reserveText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.treatment_reserve)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        Button(onClick = {
            val hives = hivesText.replace(',', '.').toDoubleOrNull()
            val dose = doseText.replace(',', '.').toDoubleOrNull()
            val reserve = reserveText.replace(',', '.').toDoubleOrNull()
            inputError = hives == null || dose == null || reserve == null
            result = if (inputError) null else TreatmentCalculator.calculate(hives!!, dose!!, reserve!!)
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.treatment_calculate)) }
        if (inputError) {
            Text(stringResource(R.string.treatment_invalid_input), color = MaterialTheme.colorScheme.error)
        }
        result?.let { calculated ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.treatment_result, formatTreatment(calculated.amountWithReserve), unit), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.treatment_without_reserve, formatTreatment(calculated.totalAmount), unit))
                    Text(stringResource(R.string.treatment_warning), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_back)) }
    }
}

private fun formatTreatment(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
