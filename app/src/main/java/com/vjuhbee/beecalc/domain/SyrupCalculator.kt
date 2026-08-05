package com.vjuhbee.beecalc.domain

import com.vjuhbee.beecalc.model.SyrupPreset
import com.vjuhbee.beecalc.model.SyrupResult

/**
 * Чистая логика расчёта сахарного сиропа, без UI (SPEC.md §6).
 *
 * Модель: 1 кг растворённого сахара добавляет ~0.6 л объёма.
 *   объём сиропа ≈ вода_л + 0.6 × сахар_кг
 *   вода  = V / (1 + 0.6 × k)
 *   сахар = k × вода
 * где k — кг сахара на 1 л воды, V — желаемый объём сиропа в литрах.
 * Точность ±5%, в UI обязательна пометка «Расчёт приблизительный».
 */
object SyrupCalculator {

    private const val VOLUME_PER_KG_SUGAR = 0.6

    /** Пресеты концентрации (SPEC.md §5.1). Свои пропорции появятся в v0.1.x/v0.2. */
    val presets = listOf(
        SyrupPreset(id = "1_1", ratioLabel = "1:1", name = "жидкий", sugarPerLiterWater = 1.0),
        SyrupPreset(id = "1_5_1", ratioLabel = "1.5:1", name = "средний", sugarPerLiterWater = 1.5),
        SyrupPreset(id = "2_1", ratioLabel = "2:1", name = "густой", sugarPerLiterWater = 2.0)
    )

    fun calculate(volumeLiters: Int, preset: SyrupPreset): SyrupResult {
        val k = preset.sugarPerLiterWater
        val water = volumeLiters / (1 + VOLUME_PER_KG_SUGAR * k)
        val sugar = k * water
        return SyrupResult(waterLiters = water, sugarKg = sugar)
    }
}
