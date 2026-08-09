package com.vjuhbee.beecalc.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiaryExpansionCalculatorTest {
    @Test fun calculatesResourcesAndOwnQueenShortage() {
        val result = ApiaryExpansionCalculator.calculate(3.0, 2.0, 2.0, 1.0, 4.0, QueenSource.OWN)
        assertEquals(6, result.splits)
        assertEquals(12, result.broodFrames)
        assertEquals(6, result.feedFrames)
        assertEquals(6, result.queenUnitsNeeded)
        assertEquals(2, result.queenUnitsToAcquire)
    }
}
