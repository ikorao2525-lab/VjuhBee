package com.vjuhbee.beecalc.domain

data class CandiResult(val sugarKg: Double, val honeyKg: Double) {
    val totalKg: Double get() = sugarKg + honeyKg
}

/** Ориентировочная модель состава канди; фактический рецепт зависит от технологии пасечника. */
object CandiCalculator {
    fun calculate(sugarKg: Double, honeyKg: Double): CandiResult =
        CandiResult(sugarKg.coerceAtLeast(0.0), honeyKg.coerceAtLeast(0.0))
}