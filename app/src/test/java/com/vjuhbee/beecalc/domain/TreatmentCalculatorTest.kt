package com.vjuhbee.beecalc.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TreatmentCalculatorTest {
    @Test fun multipliesHivesByDoseAndAddsReserve() {
        val result = TreatmentCalculator.calculate(10.0, 5.0, 10.0)
        assertEquals(50.0, result.totalAmount, 0.0001)
        assertEquals(55.0, result.amountWithReserve, 0.0001)
    }

    @Test fun negativeValuesBecomeZero() {
        val result = TreatmentCalculator.calculate(-1.0, -2.0, -10.0)
        assertEquals(0.0, result.amountWithReserve, 0.0001)
    }
}