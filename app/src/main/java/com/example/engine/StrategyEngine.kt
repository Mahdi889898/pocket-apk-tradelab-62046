package com.example.engine

import com.example.model.BacktestResult
import com.example.model.Candle
import com.example.model.MarketInstrument
import com.example.model.MarketRegime
import com.example.model.StrategyConfig
import com.example.model.StrategyType
import com.example.model.TradeSignal
import com.example.model.TradeSignalAction
import kotlin.math.max
import kotlin.math.sqrt

object StrategyEngine {

    private const val SIMULATED_FEE_RATE = 0.0008 // 0.08% simulated execution slippage & fee

    fun evaluateSignal(
        candles: List<Candle>,
        config: StrategyConfig
    ): TradeSignal {
        if (candles.size < 30) {
            return TradeSignal(TradeSignalAction.HOLD, "در حال جمع‌آوری داده کافی…")
        }
        val closes = candles.map { it.close }
        val lastIdx = closes.lastIndex
        val prevIdx = lastIdx - 1

        return when (config.strategyType) {
            StrategyType.EMA_CROSSOVER -> evaluateEmaCrossover(closes, prevIdx, lastIdx, config)
            StrategyType.RSI_MEAN_REVERSION -> evaluateRsiReversion(closes, prevIdx, lastIdx, config)
            StrategyType.MACD_MOMENTUM -> evaluateMacdMomentum(closes, prevIdx, lastIdx)
            StrategyType.BOLLINGER_BREAKOUT -> evaluateBollinger(closes, prevIdx, lastIdx)
        }
    }

    private fun evaluateEmaCrossover(
        closes: List<Double>,
        prevIdx: Int,
        lastIdx: Int,
        config: StrategyConfig
    ): TradeSignal {
        val fastPeriod = config.emaFastPeriod.coerceIn(3, 30)
        val slowPeriod = config.emaSlowPeriod.coerceIn(fastPeriod + 2, 60)
        val fast = TechnicalIndicators.calculateEma(closes, fastPeriod)
        val slow = TechnicalIndicators.calculateEma(closes, slowPeriod)

        val prevFast = fast[prevIdx]
        val prevSlow = slow[prevIdx]
        val curFast = fast[lastIdx]
        val curSlow = slow[lastIdx]

        if (prevFast == null || prevSlow == null || curFast == null || curSlow == null) {
            return TradeSignal(TradeSignalAction.HOLD, "تکمیل دوره میانگین متحرک")
        }

        val spreadPct = ((curFast - curSlow) / curSlow) * 100.0
        return when {
            prevFast <= prevSlow && curFast > curSlow -> {
                TradeSignal(
                    TradeSignalAction.BUY,
                    "تقاطع صعودی EMA($fastPeriod) بالای EMA($slowPeriod)"
                )
            }
            prevFast >= prevSlow && curFast < curSlow -> {
                TradeSignal(
                    TradeSignalAction.SELL,
                    "تقاطع نزولی EMA($fastPeriod) زیر EMA($slowPeriod)"
                )
            }
            spreadPct > 0.12 && closes[lastIdx] > closes[prevIdx] && closes[prevIdx] > curFast -> {
                TradeSignal(
                    TradeSignalAction.BUY,
                    "تأیید روند صعودی EMA (فاصله مثبت)"
                )
            }
            else -> TradeSignal(TradeSignalAction.HOLD, "روند بدون سیگنال تقاطع")
        }
    }

    private fun evaluateRsiReversion(
        closes: List<Double>,
        prevIdx: Int,
        lastIdx: Int,
        config: StrategyConfig
    ): TradeSignal {
        val rsi = TechnicalIndicators.calculateRsi(closes, 14)
        val prevRsi = rsi[prevIdx]
        val curRsi = rsi[lastIdx]
        if (prevRsi == null || curRsi == null) {
            return TradeSignal(TradeSignalAction.HOLD, "محاسبه شاخص RSI")
        }

        val oversold = config.rsiOversold.toDouble()
        val overbought = config.rsiOverbought.toDouble()

        return when {
            (prevRsi <= oversold && curRsi > prevRsi) || curRsi < (oversold - 4.0) -> {
                TradeSignal(
                    TradeSignalAction.BUY,
                    "بازگشت RSI از اشباع فروش (${String.format("%.1f", curRsi)})"
                )
            }
            curRsi >= overbought -> {
                TradeSignal(
                    TradeSignalAction.SELL,
                    "رسیدن RSI به اشباع خرید (${String.format("%.1f", curRsi)})"
                )
            }
            else -> TradeSignal(
                TradeSignalAction.HOLD,
                "RSI در محدوده خنثی (${String.format("%.1f", curRsi)})"
            )
        }
    }

    private fun evaluateMacdMomentum(
        closes: List<Double>,
        prevIdx: Int,
        lastIdx: Int
    ): TradeSignal {
        val macd = TechnicalIndicators.calculateMacd(closes)
        val prevHist = macd.histogram[prevIdx]
        val curHist = macd.histogram[lastIdx]
        if (prevHist == null || curHist == null) {
            return TradeSignal(TradeSignalAction.HOLD, "در انتظار محاسبه MACD")
        }

        return when {
            prevHist <= 0.0 && curHist > 0.0 -> {
                TradeSignal(TradeSignalAction.BUY, "عبور مثبت هیستوگرام MACD")
            }
            prevHist >= 0.0 && curHist < 0.0 -> {
                TradeSignal(TradeSignalAction.SELL, "عبور منفی هیستوگرام MACD")
            }
            else -> TradeSignal(TradeSignalAction.HOLD, "مومنتوم MACD بدون تغییر جهت")
        }
    }

    private fun evaluateBollinger(
        closes: List<Double>,
        prevIdx: Int,
        lastIdx: Int
    ): TradeSignal {
        val bb = TechnicalIndicators.calculateBollingerBands(closes, 20, 2.0)
        val lowerPrev = bb.lower[prevIdx]
        val lowerCur = bb.lower[lastIdx]
        val upperCur = bb.upper[lastIdx]
        if (lowerPrev == null || lowerCur == null || upperCur == null) {
            return TradeSignal(TradeSignalAction.HOLD, "در انتظار تشکیل باند بولینگر")
        }

        val prevClose = closes[prevIdx]
        val curClose = closes[lastIdx]

        return when {
            (prevClose <= lowerPrev && curClose > lowerCur) || curClose < lowerCur * 0.998 -> {
                TradeSignal(TradeSignalAction.BUY, "واکنش خرید به کف باند بولینگر")
            }
            curClose >= upperCur -> {
                TradeSignal(TradeSignalAction.SELL, "برخورد قیمت به سقف باند بولینگر")
            }
            else -> TradeSignal(TradeSignalAction.HOLD, "قیمت درون کانال بولینگر")
        }
    }

    fun runBacktest(
        instrument: MarketInstrument,
        config: StrategyConfig,
        regime: MarketRegime,
        candleCount: Int,
        initialCapital: Double = 10_000.0
    ): BacktestResult {
        val deterministicSeed = (instrument.ordinal + 1) * 31_337L +
            (regime.ordinal + 1) * 7_919L +
            candleCount * 131L
        val simulator = MarketSimulator(seed = deterministicSeed)
        val candles = simulator.generateInitialSeries(
            instrument = instrument,
            count = candleCount,
            regime = regime
        )

        var cash = initialCapital
        var openQty = 0.0
        var entryPrice = 0.0
        var stopLossPrice = 0.0
        var takeProfitPrice = 0.0
        var peakEquity = initialCapital
        var maxDrawdownPct = 0.0
        var halted = false

        val equityCurve = ArrayList<Double>(candleCount)
        val benchmarkCurve = ArrayList<Double>(candleCount)
        val tradeReturns = mutableListOf<Double>()
        var grossProfit = 0.0
        var grossLoss = 0.0

        val startPrice = candles.first().close

        for (i in candles.indices) {
            val candle = candles[i]
            val benchmarkEquity = initialCapital * (candle.close / startPrice)
            benchmarkCurve.add(benchmarkEquity)

            if (openQty > 0.0) {
                var exitPrice: Double? = null
                if (candle.low <= stopLossPrice) {
                    exitPrice = stopLossPrice
                } else if (candle.high >= takeProfitPrice) {
                    exitPrice = takeProfitPrice
                } else if (i >= 30) {
                    val signal = evaluateSignal(candles.subList(0, i + 1), config)
                    if (signal.action == TradeSignalAction.SELL) {
                        exitPrice = candle.close
                    }
                }

                if (exitPrice != null) {
                    val proceeds = openQty * exitPrice * (1.0 - SIMULATED_FEE_RATE)
                    val cost = openQty * entryPrice
                    val pnl = proceeds - cost
                    val returnPct = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
                    tradeReturns.add(returnPct)
                    if (pnl >= 0.0) grossProfit += pnl else grossLoss += -pnl
                    cash += proceeds
                    openQty = 0.0
                    entryPrice = 0.0
                }
            } else if (!halted && i >= 30 && i < candles.lastIndex) {
                val signal = evaluateSignal(candles.subList(0, i + 1), config)
                if (signal.action == TradeSignalAction.BUY) {
                    val invest = cash * (config.allocationPercent / 100.0)
                    if (invest > 10.0) {
                        val effectiveCost = invest * (1.0 - SIMULATED_FEE_RATE)
                        openQty = effectiveCost / candle.close
                        entryPrice = candle.close
                        stopLossPrice = entryPrice * (1.0 - config.stopLossPercent / 100.0)
                        takeProfitPrice = entryPrice * (1.0 + config.takeProfitPercent / 100.0)
                        cash -= invest
                    }
                }
            }

            val currentEquity = cash + openQty * candle.close
            equityCurve.add(currentEquity)

            if (currentEquity > peakEquity) {
                peakEquity = currentEquity
            }
            val drawdown = if (peakEquity > 0.0) ((peakEquity - currentEquity) / peakEquity) * 100.0 else 0.0
            if (drawdown > maxDrawdownPct) {
                maxDrawdownPct = drawdown
            }
            if (drawdown >= config.maxDrawdownPercent) {
                halted = true
            }
        }

        // Liquidate any open position at final candle for clean reporting
        if (openQty > 0.0) {
            val lastClose = candles.last().close
            val proceeds = openQty * lastClose * (1.0 - SIMULATED_FEE_RATE)
            val cost = openQty * entryPrice
            val pnl = proceeds - cost
            val returnPct = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
            tradeReturns.add(returnPct)
            if (pnl >= 0.0) grossProfit += pnl else grossLoss += -pnl
            cash += proceeds
            openQty = 0.0
            if (equityCurve.isNotEmpty()) {
                equityCurve[equityCurve.lastIndex] = cash
            }
        }

        val finalEquity = equityCurve.lastOrNull() ?: initialCapital
        val netReturnPct = ((finalEquity - initialCapital) / initialCapital) * 100.0
        val benchmarkReturnPct = (((benchmarkCurve.lastOrNull() ?: initialCapital) - initialCapital) / initialCapital) * 100.0
        val wins = tradeReturns.count { it > 0.0 }
        val losses = tradeReturns.count { it <= 0.0 }
        val winRate = if (tradeReturns.isNotEmpty()) (wins.toDouble() / tradeReturns.size) * 100.0 else 0.0
        val profitFactor = when {
            grossLoss > 0.0 -> (grossProfit / grossLoss).coerceAtMost(99.9)
            grossProfit > 0.0 -> 99.9
            else -> 0.0
        }

        val sharpe = computeSharpeEstimate(equityCurve)

        return BacktestResult(
            instrument = instrument,
            strategyConfig = config,
            regime = regime,
            candleCount = candleCount,
            initialCapital = initialCapital,
            finalEquity = finalEquity,
            equityCurve = equityCurve,
            benchmarkCurve = benchmarkCurve,
            netReturnPercent = netReturnPct,
            benchmarkReturnPercent = benchmarkReturnPct,
            winRatePercent = winRate,
            totalTrades = tradeReturns.size,
            winningTrades = wins,
            losingTrades = losses,
            maxDrawdownPercent = maxDrawdownPct,
            sharpeRatio = sharpe,
            profitFactor = profitFactor,
            bestTradePercent = tradeReturns.maxOrNull() ?: 0.0,
            worstTradePercent = tradeReturns.minOrNull() ?: 0.0
        )
    }

    private fun computeSharpeEstimate(equityCurve: List<Double>): Double {
        if (equityCurve.size < 3) return 0.0
        val returns = ArrayList<Double>(equityCurve.size - 1)
        for (i in 1 until equityCurve.size) {
            val prev = equityCurve[i - 1]
            if (prev > 0.0) {
                returns.add((equityCurve[i] - prev) / prev)
            }
        }
        if (returns.isEmpty()) return 0.0
        val mean = returns.average()
        val variance = returns.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        if (stdDev < 1e-9) return 0.0
        return (mean / stdDev) * sqrt(max(1.0, returns.size.toDouble()))
    }
}
