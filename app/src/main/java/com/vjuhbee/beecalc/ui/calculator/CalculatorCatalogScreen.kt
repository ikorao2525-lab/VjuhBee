package com.vjuhbee.beecalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.R

@Composable
fun CalculatorCatalogScreen(
    onSyrupClick: () -> Unit,
    onCandiClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.calculators_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        CalculatorCard(R.string.calculator_syrup, R.string.calculator_syrup_description, onSyrupClick, true)
        CalculatorCard(R.string.calculator_candi, R.string.calculator_candi_description, onCandiClick, true)
        CalculatorCard(R.string.calculator_winter, R.string.calculator_winter_description, {}, false)
        CalculatorCard(R.string.calculator_honey_jars, R.string.calculator_honey_jars_description, {}, false)
    }
}

@Composable
private fun CalculatorCard(titleRes: Int, descriptionRes: Int, onClick: () -> Unit, enabled: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodyMedium)
            if (enabled) Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_open)) }
            else OutlinedButton(onClick = onClick, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_planned)) }
        }
    }
}