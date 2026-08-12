package com.vjuhbee.beecalc.domain

data class WinterFeedResult(val baseKilograms: Double, val totalKilograms: Double)

object WinterFeedCalculator {
    fun calculate(families: Double, months: Double, kilogramsPerFamilyMonth: Double, reservePercent: Double): WinterFeedResult {
        val base = families.coerceAtLeast(0.0) * months.coerceAtLeast(0.0) * kilogramsPerFamilyMonth.coerceAtLeast(0.0)
        return WinterFeedResult(base, base * (1.0 + reservePercent.coerceAtLeast(0.0) / 100.0))
    }
}
