package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.StrategyEngine
import com.example.engine.TechnicalIndicators
import com.example.model.MarketInstrument
import com.example.model.MarketRegime
import com.example.model.StrategyConfig
import com.example.model.StrategyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تریدلب", appName)
    }

    @Test
    fun `technical indicators and backtester produce valid deterministic output`() {
        val prices = List(60) { i -> 100.0 + i * 0.8 + kotlin.math.sin(i * 0.4) * 3.0 }
        val ema = TechnicalIndicators.calculateEma(prices, 9)
        val rsi = TechnicalIndicators.calculateRsi(prices, 14)
        val bb = TechnicalIndicators.calculateBollingerBands(prices, 20)

        assertTrue(ema.last() != null && ema.last()!! > 100.0)
        assertTrue(rsi.last() != null && rsi.last()!! in 0.0..100.0)
        assertTrue(bb.upper.last()!! > bb.lower.last()!!)

        val result = StrategyEngine.runBacktest(
            instrument = MarketInstrument.IDX_ALPHA,
            config = StrategyConfig(strategyType = StrategyType.EMA_CROSSOVER),
            regime = MarketRegime.BULLISH_TREND,
            candleCount = 200
        )
        assertEquals(200, result.equityCurve.size)
        assertEquals(200, result.benchmarkCurve.size)
        assertTrue(result.finalEquity > 0.0)
    }
}
