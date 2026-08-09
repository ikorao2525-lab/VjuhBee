package com.vjuhbee.beecalc.domain

data class TreatmentResult(
    val totalAmount: Double,
    val amountWithReserve: Double
)

object TreatmentCalculator {
    fun calculate(hives: Double, dosePerHive: Double, reservePercent: Double): TreatmentResult {
        val safeHives = hives.coerceAtLeast(0.0)
        val safeDose = dosePerHive.coerceAtLeast(0.0)
        val reserve = reservePercent.coerceIn(0.0, 100.0)
        val total = safeHives * safeDose
        return TreatmentResult(total, total * (1.0 + reserve / 100.0))
    }
}