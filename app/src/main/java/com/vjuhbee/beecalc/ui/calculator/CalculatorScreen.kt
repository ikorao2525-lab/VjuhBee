package com.vjuhbee.beecalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.SyrupPreset
import java.util.Locale

/**
 * Калькулятор сахарного сиропа (SPEC.md §5.1).
 * Всё крупное: тач-цели от 48dp, цифры результата читаются с метра (SPEC.md §8).
 */
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.calc_volume_label, state.volumeLiters),
            style = MaterialTheme.typography.titleLarge
        )

        Slider(
            value = state.volumeLiters.toFloat(),
            onValueChange = { viewModel.setVolume(it.toInt()) },
            valueRange = 1f..50f,
            steps = 48
        )

        // Быстрые кнопки дополняют слайдер: нажатие ставит его значение.
        // По две кнопки в ряду (последняя — во всю ширину): крупнее, удобнее в перчатках.
        viewModel.quickVolumes.chunked(2).forEach { rowVolumes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowVolumes.forEach { liters ->
                    VolumeButton(
                        liters = liters,
                        selected = liters == state.volumeLiters,
                        onClick = { viewModel.setVolume(liters) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.calc_concentration_label),
            style = MaterialTheme.typography.titleLarge
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.presets.forEach { preset ->
                PresetButton(
                    preset = preset,
                    selected = preset.id == state.preset.id,
                    onClick = { viewModel.selectPreset(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = stringResource(R.string.calc_ratio_note),
            style = MaterialTheme.typography.bodyMedium
        )

        ResultCard(
            sugarKg = state.result.sugarKg,
            waterLiters = state.result.waterLiters
        )
    }
}

@Composable
private fun VolumeButton(
    liters: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) {
            Text(text = stringResource(R.string.calc_liters_button, liters), style = MaterialTheme.typography.titleLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) {
            Text(text = stringResource(R.string.calc_liters_button, liters), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PresetButton(
    preset: SyrupPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    Button(
        onClick = onClick,
        colors = colors,
        modifier = modifier.heightIn(min = 64.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = preset.ratioLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(text = preset.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultCard(sugarKg: Double, waterLiters: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.calc_sugar_result, formatAmount(sugarKg)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.calc_water_result, formatAmount(waterLiters)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.calc_approximate_note),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Одна цифра после запятой, разделитель — по локали устройства. */
private fun formatAmount(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)
