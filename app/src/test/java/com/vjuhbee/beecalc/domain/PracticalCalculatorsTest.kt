package com.vjuhbee.beecalc.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticalCalculatorsTest {
    @Test fun calculatesWinterFeedWithReserve() {
        val result = WinterFeedCalculator.calculate(10.0, 5.0, 2.0, 10.0)
        assertEquals(100.0, result.baseKilograms, 0.0001)
        assertEquals(110.0, result.totalKilograms, 0.0001)
    }

    @Test fun calculatesFullHoneyJarsAndRemainder() {
        val result = HoneyJarsCalculator.calculate(37.0, 0.5)
        assertEquals(74, result.fullJars)
        assertEquals(0.0, result.remainderKilograms, 0.0001)
    }
}
