package com.vjuhbee.beecalc.domain

data class HoneyJarsResult(val fullJars: Int, val remainderKilograms: Double)

object HoneyJarsCalculator {
    fun calculate(honeyKilograms: Double, jarKilograms: Double): HoneyJarsResult {
        val honey = honeyKilograms.coerceAtLeast(0.0)
        val jar = jarKilograms.coerceAtLeast(0.0)
        if (jar == 0.0) return HoneyJarsResult(0, honey)
        val full = kotlin.math.floor(honey / jar).toInt()
        return HoneyJarsResult(full, honey - full * jar)
    }
}
