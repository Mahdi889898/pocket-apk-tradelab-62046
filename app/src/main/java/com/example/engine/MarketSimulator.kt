package com.example.engine

import com.example.model.Candle
import com.example.model.MarketInstrument
import com.example.model.MarketRegime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MarketSimulator(seed: Long = System.currentTimeMillis()) {
    private val random = Random(seed)

    private fun nextGaussian(): Double {
        val u1 = max(1e-9, random.nextDouble())
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }

    fun generateInitialSeries(
        instrument: MarketInstrument,
        count: Int = 64,
        regime: MarketRegime? = null,
        startTimestampMillis: Long = System.currentTimeMillis() - (count * 60_000L)
    ): List<Candle> {
        val candles = ArrayList<Candle>(count)
        var currentPrice = instrument.basePrice
        var trendPhase = random.nextDouble() * 2.0 * Math.PI
        var momentum = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / max(1, count)
            val effectiveDrift = when (regime) {
                MarketRegime.CRASH_RECOVERY -> {
                    when {
                        progress < 0.25 -> 0.0016
                        progress < 0.48 -> -0.0042
                        else -> 0.0024
                    }
                }
                null -> instrument.baseDrift + sin(trendPhase + i * 0.11) * instrument.baseVolatility * 0.45
                else -> regime.driftPerBar + sin(trendPhase + i * 0.14) * instrument.baseVolatility * 0.38
            }

            val volMult = regime?.volatilityMultiplier ?: 1.0
            val meanRev = regime?.meanReversionStrength ?: 0.03
            val meanPull = ((instrument.basePrice - currentPrice) / instrument.basePrice) * meanRev

            val candle = generateNextCandleInternal(
                index = i,
                previousClose = currentPrice,
                baseVolatility = instrument.baseVolatility * volMult,
                drift = effectiveDrift + meanPull + momentum * 0.22,
                timestampMillis = startTimestampMillis + i * 60_000L
            )
            momentum = (candle.close - candle.open) / max(1e-6, candle.open)
            currentPrice = candle.close
            candles.add(candle)
        }
        return candles
    }

    fun generateNextCandle(
        instrument: MarketInstrument,
        lastCandle: Candle
    ): Candle {
        val cycleWave = sin(lastCandle.index * 0.13 + instrument.ordinal * 1.1)
        val drift = instrument.baseDrift + cycleWave * instrument.baseVolatility * 0.55
        val prevReturn = (lastCandle.close - lastCandle.open) / max(1e-6, lastCandle.open)
        val anchorPull = ((instrument.basePrice - lastCandle.close) / instrument.basePrice) * 0.012
        val effectiveDrift = drift + prevReturn * 0.18 + anchorPull

        return generateNextCandleInternal(
            index = lastCandle.index + 1,
            previousClose = lastCandle.close,
            baseVolatility = instrument.baseVolatility,
            drift = effectiveDrift,
            timestampMillis = lastCandle.timestampMillis + 60_000L
        )
    }

    private fun generateNextCandleInternal(
        index: Int,
        previousClose: Double,
        baseVolatility: Double,
        drift: Double,
        timestampMillis: Long
    ): Candle {
        val shock = nextGaussian() * baseVolatility
        val pctMove = (drift + shock).coerceIn(-0.07, 0.07)
        val open = previousClose
        val close = max(open * 0.2, open * (1.0 + pctMove))

        val bodyHigh = max(open, close)
        val bodyLow = min(open, close)
        val upperWick = abs(nextGaussian()) * baseVolatility * 0.45 * open
        val lowerWick = abs(nextGaussian()) * baseVolatility * 0.45 * open

        val high = bodyHigh + upperWick
        val low = max(open * 0.15, bodyLow - lowerWick)
        val volume = (1200.0 + abs(pctMove) * 180_000.0 + random.nextDouble() * 900.0)

        return Candle(
            index = index,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
            timestampMillis = timestampMillis
        )
    }
}
