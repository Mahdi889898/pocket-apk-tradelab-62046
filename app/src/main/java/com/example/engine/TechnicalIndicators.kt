package com.example.engine

import kotlin.math.sqrt

data class BollingerBands(
    val upper: List<Double?>,
    val middle: List<Double?>,
    val lower: List<Double?>
)

data class MacdResult(
    val macdLine: List<Double?>,
    val signalLine: List<Double?>,
    val histogram: List<Double?>
)

object TechnicalIndicators {

    fun calculateEma(values: List<Double>, period: Int): List<Double?> {
        if (values.isEmpty() || period <= 0) return emptyList()
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result

        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        var ema = sum / period
        result[period - 1] = ema

        val multiplier = 2.0 / (period + 1.0)
        for (i in period until values.size) {
            ema = (values[i] - ema) * multiplier + ema
            result[i] = ema
        }
        return result
    }

    fun calculateRsi(values: List<Double>, period: Int = 14): List<Double?> {
        if (values.isEmpty() || period <= 0) return emptyList()
        val result = MutableList<Double?>(values.size) { null }
        if (values.size <= period) return result

        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val change = values[i] - values[i - 1]
            if (change > 0) gainSum += change else lossSum -= change
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        result[period] = computeRsiValue(avgGain, avgLoss)

        for (i in (period + 1) until values.size) {
            val change = values[i] - values[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
            result[i] = computeRsiValue(avgGain, avgLoss)
        }
        return result
    }

    private fun computeRsiValue(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun calculateMacd(
        values: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdResult {
        val fastEma = calculateEma(values, fastPeriod)
        val slowEma = calculateEma(values, slowPeriod)
        val macdLine = MutableList<Double?>(values.size) { null }
        val nonNullMacd = mutableListOf<Double>()
        val nonNullIndices = mutableListOf<Int>()

        for (i in values.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                val m = f - s
                macdLine[i] = m
                nonNullMacd.add(m)
                nonNullIndices.add(i)
            }
        }

        val rawSignal = calculateEma(nonNullMacd, signalPeriod)
        val signalLine = MutableList<Double?>(values.size) { null }
        val histogram = MutableList<Double?>(values.size) { null }

        for (j in nonNullIndices.indices) {
            val idx = nonNullIndices[j]
            val sig = rawSignal[j]
            signalLine[idx] = sig
            val m = macdLine[idx]
            if (m != null && sig != null) {
                histogram[idx] = m - sig
            }
        }

        return MacdResult(macdLine = macdLine, signalLine = signalLine, histogram = histogram)
    }

    fun calculateBollingerBands(
        values: List<Double>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): BollingerBands {
        val upper = MutableList<Double?>(values.size) { null }
        val middle = MutableList<Double?>(values.size) { null }
        val lower = MutableList<Double?>(values.size) { null }

        if (values.size < period || period <= 1) {
            return BollingerBands(upper, middle, lower)
        }

        for (i in (period - 1) until values.size) {
            var sum = 0.0
            for (j in (i - period + 1)..i) {
                sum += values[j]
            }
            val mean = sum / period
            var varianceSum = 0.0
            for (j in (i - period + 1)..i) {
                val diff = values[j] - mean
                varianceSum += diff * diff
            }
            val stdDev = sqrt(varianceSum / period)
            middle[i] = mean
            upper[i] = mean + stdDevMultiplier * stdDev
            lower[i] = mean - stdDevMultiplier * stdDev
        }

        return BollingerBands(upper = upper, middle = middle, lower = lower)
    }
}
