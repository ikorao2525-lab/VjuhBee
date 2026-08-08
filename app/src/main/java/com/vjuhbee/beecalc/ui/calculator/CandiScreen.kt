package com.vjuhbee.beecalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.domain.CandiCalculator
import java.util.Locale

@Composable
fun CandiScreen(onBack: () -> Unit) {
    var sugarText by remember { mutableStateOf("1.0") }
    var honeyText by remember { mutableStateOf("0.2") }
    val sugar = sugarText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val honey = honeyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val result = CandiCalculator.calculate(sugar, honey)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.calculator_candi), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.candi_formula_note), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(sugarText, { sugarText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.candi_sugar)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(honeyText, { honeyText = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.candi_honey)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.candi_result, formatCandi(result.totalKg)), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.candi_sugar_result, formatCandi(result.sugarKg)))
                Text(stringResource(R.string.candi_honey_result, formatCandi(result.honeyKg)))
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_back)) }
    }
}

private fun formatCandi(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)