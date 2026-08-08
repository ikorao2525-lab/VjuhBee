package com.vjuhbee.beecalc.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CandiCalculatorTest {
    @Test fun sumsSugarAndHoney() {
        val result = CandiCalculator.calculate(1.0, 0.2)
        assertEquals(1.2, result.totalKg, 0.0001)
    }

    @Test fun negativeInputsBecomeZero() {
        val result = CandiCalculator.calculate(-1.0, -0.5)
        assertEquals(0.0, result.totalKg, 0.0001)
    }
}