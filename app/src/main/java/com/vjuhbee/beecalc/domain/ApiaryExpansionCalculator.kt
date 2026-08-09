package com.vjuhbee.beecalc.domain

data class ApiaryExpansionResult(
    val splits: Int,
    val donorFamilies: Int,
    val broodFrames: Int,
    val feedFrames: Int,
    val queenUnitsNeeded: Int,
    val queenUnitsToAcquire: Int
)

enum class QueenSource { OWN, PURCHASED, QUEEN_CELLS }

object ApiaryExpansionCalculator {
    fun calculate(
        donorFamilies: Double,
        splitsPerDonor: Double,
        broodFramesPerSplit: Double,
        feedFramesPerSplit: Double,
        availableQueens: Double,
        queenSource: QueenSource
    ): ApiaryExpansionResult {
        val donors = donorFamilies.coerceAtLeast(0.0)
        val splitsPerFamily = splitsPerDonor.coerceAtLeast(0.0)
        val splits = kotlin.math.floor(donors * splitsPerFamily).toInt()
        val brood = kotlin.math.ceil(splits * broodFramesPerSplit.coerceAtLeast(0.0)).toInt()
        val feed = kotlin.math.ceil(splits * feedFramesPerSplit.coerceAtLeast(0.0)).toInt()
        val queens = splits
        val available = availableQueens.coerceAtLeast(0.0).toInt()
        val acquire = when (queenSource) {
            QueenSource.OWN -> (queens - available).coerceAtLeast(0)
            QueenSource.PURCHASED, QueenSource.QUEEN_CELLS -> queens
        }
        return ApiaryExpansionResult(splits, donors.toInt(), brood, feed, queens, acquire)
    }
}
