package com.vjuhbee.beecalc.ui.calculator

import androidx.lifecycle.ViewModel
import com.vjuhbee.beecalc.domain.SyrupCalculator
import com.vjuhbee.beecalc.model.SyrupPreset
import com.vjuhbee.beecalc.model.SyrupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Состояние экрана калькулятора: входные данные и готовый результат. */
data class CalculatorUiState(
    val volumeLiters: Int,
    val preset: SyrupPreset,
    val result: SyrupResult
)

class CalculatorViewModel : ViewModel() {

    /** Быстрые кнопки объёма (SPEC.md §5.1). */
    val quickVolumes = listOf(5, 10, 20, 30, 50)

    val presets = SyrupCalculator.presets

    private val _uiState = MutableStateFlow(buildState(volumeLiters = 10, preset = presets.first()))
    val uiState: StateFlow<CalculatorUiState> = _uiState

    fun setVolume(liters: Int) {
        _uiState.update { buildState(liters.coerceIn(1, 50), it.preset) }
    }

    fun selectPreset(preset: SyrupPreset) {
        _uiState.update { buildState(it.volumeLiters, preset) }
    }

    private fun buildState(volumeLiters: Int, preset: SyrupPreset) = CalculatorUiState(
        volumeLiters = volumeLiters,
        preset = preset,
        result = SyrupCalculator.calculate(volumeLiters, preset)
    )
}
