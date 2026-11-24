package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TradeLabDatabase
import com.example.data.TradeRepository
import com.example.engine.BollingerBands
import com.example.engine.MarketSimulator
import com.example.engine.StrategyEngine
import com.example.engine.TechnicalIndicators
import com.example.model.BacktestResult
import com.example.model.Candle
import com.example.model.ClosedTradeSummary
import com.example.model.MarketInstrument
import com.example.model.MarketRegime
import com.example.model.OpenPosition
import com.example.model.SignalMarker
import com.example.model.StrategyConfig
import com.example.model.StrategyType
import com.example.model.TradeSignalAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SimulationSpeed(val label: String, val tickDelayMs: Long) {
    NORMAL("1x", 1350L),
    FAST("2x", 700L),
    TURBO("5x", 280L)
}

enum class TradeHistoryFilter {
    ALL,
    WINS,
    LOSSES
}

data class InstrumentSnapshot(
    val instrument: MarketInstrument,
    val lastPrice: Double,
    val changePercent: Double
)

data class TradeLabUiState(
    val isDarkTheme: Boolean = true,
    val selectedInstrument: MarketInstrument = MarketInstrument.IDX_ALPHA,
    val instrumentSnapshots: List<InstrumentSnapshot> = emptyList(),
    val visibleCandles: List<Candle> = emptyList(),
    val emaFastSeries: List<Double?> = emptyList(),
    val emaSlowSeries: List<Double?> = emptyList(),
    val bollingerBands: BollingerBands = BollingerBands(emptyList(), emptyList(), emptyList()),
    val latestRsi: Double? = null,
    val activeConfig: StrategyConfig = StrategyConfig(),
    val isBotRunning: Boolean = false,
    val isDrawdownHalted: Boolean = false,
    val simulationSpeed: SimulationSpeed = SimulationSpeed.NORMAL,
    val lastSignalNote: String = "آماده تحلیل بازار مجازی",
    val initialCapital: Double = 10_000.0,
    val availableCash: Double = 10_000.0,
    val peakEquity: Double = 10_000.0,
    val maxObservedDrawdownPct: Double = 0.0,
    val openPosition: OpenPosition? = null,
    val closedTrades: List<ClosedTradeSummary> = emptyList(),
    val historyFilter: TradeHistoryFilter = TradeHistoryFilter.ALL,
    // Backtest state
    val backtestInstrument: MarketInstrument = MarketInstrument.IDX_ALPHA,
    val backtestStrategyType: StrategyType = StrategyType.EMA_CROSSOVER,
    val backtestRegime: MarketRegime = MarketRegime.BULLISH_TREND,
    val backtestCandleCount: Int = 250,
    val isBacktestRunning: Boolean = false,
    val backtestResult: BacktestResult? = null,
    val snackbarMessage: String? = null
) {
    val currentPrice: Double
        get() = visibleCandles.lastOrNull()?.close ?: selectedInstrument.basePrice

    val totalEquity: Double
        get() {
            val pos = openPosition
            return if (pos != null) {
                val posPrice = instrumentSnapshots
                    .firstOrNull { it.instrument == pos.instrument }
                    ?.lastPrice ?: pos.entryPrice
                availableCash + (pos.quantity * posPrice)
            } else {
                availableCash
            }
        }

    val totalPnlAmount: Double
        get() = totalEquity - initialCapital

    val totalPnlPercent: Double
        get() = if (initialCapital > 0.0) (totalPnlAmount / initialCapital) * 100.0 else 0.0

    val winRatePercent: Double
        get() {
            if (closedTrades.isEmpty()) return 0.0
            val wins = closedTrades.count { it.pnlAmount > 0.0 }
            return (wins.toDouble() / closedTrades.size) * 100.0
        }

    val profitFactor: Double
        get() {
            val grossProfit = closedTrades.filter { it.pnlAmount > 0.0 }.sumOf { it.pnlAmount }
            val grossLoss = closedTrades.filter { it.pnlAmount < 0.0 }.sumOf { -it.pnlAmount }
            return when {
                grossLoss > 0.0 -> (grossProfit / grossLoss).coerceAtMost(99.9)
                grossProfit > 0.0 -> 99.9
                else -> 0.0
            }
        }

    val filteredTrades: List<ClosedTradeSummary>
        get() = when (historyFilter) {
            TradeHistoryFilter.ALL -> closedTrades
            TradeHistoryFilter.WINS -> closedTrades.filter { it.pnlAmount > 0.0 }
            TradeHistoryFilter.LOSSES -> closedTrades.filter { it.pnlAmount <= 0.0 }
        }
}

class TradeLabViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val INITIAL_CAPITAL = 10_000.0
        private const val SIMULATED_FEE_RATE = 0.0008
        private const val MAX_BUFFER_CANDLES = 120
        private const val VISIBLE_WINDOW = 48
    }

    private val repository = TradeRepository(
        TradeLabDatabase.getInstance(application.applicationContext).tradeDao()
    )

    private val simulator = MarketSimulator()
    private val candleBuffers = mutableMapOf<MarketInstrument, MutableList<Candle>>()
    private var botLoopJob: Job? = null

    private val _uiState = MutableStateFlow(TradeLabUiState(initialCapital = INITIAL_CAPITAL))
    val uiState: StateFlow<TradeLabUiState> = _uiState.asStateFlow()

    init {
        initializeMarketSeries()
        observePersistentData()
        runBacktest()
    }

    private fun initializeMarketSeries() {
        MarketInstrument.entries.forEach { instrument ->
            candleBuffers[instrument] = simulator
                .generateInitialSeries(instrument = instrument, count = 64)
                .toMutableList()
        }
        publishMarketState()
    }

    private fun observePersistentData() {
        viewModelScope.launch {
            repository.trades.collect { trades ->
                _uiState.update { it.copy(closedTrades = trades) }
            }
        }
        viewModelScope.launch {
            repository.strategyConfig.collect { savedConfig ->
                _uiState.update { state ->
                    state.copy(
                        activeConfig = savedConfig,
                        backtestStrategyType = savedConfig.strategyType
                    )
                }
                publishMarketState()
            }
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun selectInstrument(instrument: MarketInstrument) {
        if (_uiState.value.selectedInstrument == instrument) return
        _uiState.update { it.copy(selectedInstrument = instrument) }
        publishMarketState()
    }

    fun setSimulationSpeed(speed: SimulationSpeed) {
        _uiState.update { it.copy(simulationSpeed = speed) }
    }

    fun toggleBotRunning() {
        val current = _uiState.value
        if (current.isBotRunning) {
            pauseBot()
        } else {
            startBot()
        }
    }

    private fun startBot() {
        if (_uiState.value.isDrawdownHalted) {
            _uiState.update {
                it.copy(
                    isDrawdownHalted = false,
                    peakEquity = it.totalEquity,
                    snackbarMessage = "محافظ افت سرمایه بازنشانی شد و ربات دوباره فعال گردید"
                )
            }
        }
        _uiState.update { it.copy(isBotRunning = true) }
        botLoopJob?.cancel()
        botLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.isBotRunning) {
                stepSimulationTick()
                delay(_uiState.value.simulationSpeed.tickDelayMs)
            }
        }
    }

    private fun pauseBot() {
        botLoopJob?.cancel()
        botLoopJob = null
        _uiState.update { it.copy(isBotRunning = false) }
    }

    private suspend fun stepSimulationTick() {
        // Advance every market instrument by 1 realistic candle
        MarketInstrument.entries.forEach { inst ->
            val list = candleBuffers[inst] ?: return@forEach
            val next = simulator.generateNextCandle(inst, list.last())
            list.add(next)
            if (list.size > MAX_BUFFER_CANDLES) {
                list.removeAt(0)
            }
        }

        val state = _uiState.value
        val activeInstrument = state.selectedInstrument
        val activeBuffer = candleBuffers[activeInstrument] ?: return
        val latestCandle = activeBuffer.last()
        val config = state.activeConfig

        var cash = state.availableCash
        var position = state.openPosition
        var signalNote = state.lastSignalNote
        var peakEquity = state.peakEquity
        var maxDd = state.maxObservedDrawdownPct
        var drawdownHalted = state.isDrawdownHalted

        // 1. Check open position exits (SL, TP, or algorithmic exit signal)
        if (position != null) {
            val posBuffer = candleBuffers[position.instrument]
            val posCandle = posBuffer?.lastOrNull()
            if (posCandle != null) {
                var exitPrice: Double? = null
                var exitReason = ""
                if (posCandle.low <= position.stopLossPrice) {
                    exitPrice = position.stopLossPrice
                    exitReason = "حد ضرر خودکار (SL)"
                } else if (posCandle.high >= position.takeProfitPrice) {
                    exitPrice = position.takeProfitPrice
                    exitReason = "حد سود خودکار (TP)"
                } else {
                    val eval = StrategyEngine.evaluateSignal(posBuffer, config)
                    signalNote = eval.reason
                    if (eval.action == TradeSignalAction.SELL) {
                        exitPrice = posCandle.close
                        exitReason = "سیگنال خروج الگوریتمی"
                    }
                }

                if (exitPrice != null) {
                    val proceeds = position.quantity * exitPrice * (1.0 - SIMULATED_FEE_RATE)
                    val pnlAmount = proceeds - position.investedCapital
                    val pnlPercent = if (position.investedCapital > 0.0) {
                        (pnlAmount / position.investedCapital) * 100.0
                    } else 0.0

                    cash += proceeds
                    val marker = if (pnlAmount >= 0.0) SignalMarker.SELL_PROFIT else SignalMarker.SELL_LOSS
                    posBuffer[posBuffer.lastIndex] = posCandle.copy(signalMarker = marker)

                    val closedPos = position
                    position = null
                    signalNote = "$exitReason • ${if (pnlAmount >= 0) "+" else ""}${String.format("%.2f", pnlPercent)}%"

                    repository.recordTrade(
                        instrumentId = closedPos.instrument.id,
                        strategyId = closedPos.strategyType.id,
                        entryPrice = closedPos.entryPrice,
                        exitPrice = exitPrice,
                        quantity = closedPos.quantity,
                        pnlAmount = pnlAmount,
                        pnlPercent = pnlPercent,
                        exitReason = exitReason,
                        timestampMillis = System.currentTimeMillis()
                    )
                }
            }
        } else {
            // 2. Evaluate automated entry on selected instrument
            val signal = StrategyEngine.evaluateSignal(activeBuffer, config)
            signalNote = signal.reason
            if (signal.action == TradeSignalAction.BUY) {
                val invest = cash * (config.allocationPercent / 100.0)
                if (invest >= 25.0) {
                    val entryPrice = latestCandle.close
                    val effectiveCapital = invest * (1.0 - SIMULATED_FEE_RATE)
                    val qty = effectiveCapital / entryPrice
                    val slPrice = entryPrice * (1.0 - config.stopLossPercent / 100.0)
                    val tpPrice = entryPrice * (1.0 + config.takeProfitPercent / 100.0)
                    cash -= invest
                    position = OpenPosition(
                        instrument = activeInstrument,
                        strategyType = config.strategyType,
                        entryPrice = entryPrice,
                        quantity = qty,
                        investedCapital = invest,
                        stopLossPrice = slPrice,
                        takeProfitPrice = tpPrice,
                        entryCandleIndex = latestCandle.index,
                        entryTimestampMillis = System.currentTimeMillis(),
                        entryReason = signal.reason
                    )
                    activeBuffer[activeBuffer.lastIndex] = latestCandle.copy(signalMarker = SignalMarker.BUY)
                }
            }
        }

        // 3. Drawdown risk guard check
        val currentPriceOfPos = position?.let { candleBuffers[it.instrument]?.lastOrNull()?.close } ?: 0.0
        val currentEquity = cash + (position?.quantity ?: 0.0) * currentPriceOfPos
        if (currentEquity > peakEquity) {
            peakEquity = currentEquity
        }
        val currentDrawdownPct = if (peakEquity > 0.0) {
            ((peakEquity - currentEquity) / peakEquity) * 100.0
        } else 0.0
        if (currentDrawdownPct > maxDd) {
            maxDd = currentDrawdownPct
        }

        var shouldKeepRunning = _uiState.value.isBotRunning
        if (currentDrawdownPct >= config.maxDrawdownPercent) {
            drawdownHalted = true
            shouldKeepRunning = false
            signalNote = "محافظ افت سرمایه فعال شد (${String.format("%.1f", currentDrawdownPct)}%)"
        }

        _uiState.update {
            it.copy(
                availableCash = cash,
                openPosition = position,
                lastSignalNote = signalNote,
                peakEquity = peakEquity,
                maxObservedDrawdownPct = maxDd,
                isDrawdownHalted = drawdownHalted,
                isBotRunning = shouldKeepRunning
            )
        }
        publishMarketState()
        if (!shouldKeepRunning) {
            botLoopJob?.cancel()
            botLoopJob = null
        }
    }

    fun closeOpenPositionManually() {
        val state = _uiState.value
        val pos = state.openPosition ?: return
        val buffer = candleBuffers[pos.instrument] ?: return
        val lastCandle = buffer.lastOrNull() ?: return
        val exitPrice = lastCandle.close

        val proceeds = pos.quantity * exitPrice * (1.0 - SIMULATED_FEE_RATE)
        val pnlAmount = proceeds - pos.investedCapital
        val pnlPercent = if (pos.investedCapital > 0.0) (pnlAmount / pos.investedCapital) * 100.0 else 0.0

        val marker = if (pnlAmount >= 0.0) SignalMarker.SELL_PROFIT else SignalMarker.SELL_LOSS
        buffer[buffer.lastIndex] = lastCandle.copy(signalMarker = marker)

        _uiState.update {
            it.copy(
                availableCash = it.availableCash + proceeds,
                openPosition = null,
                lastSignalNote = "پوزیشن به صورت دستی بسته شد",
                snackbarMessage = "پوزیشن با ${if (pnlAmount >= 0) "سود" else "زیان"} ${String.format("%.2f", pnlPercent)}% بسته شد"
            )
        }
        publishMarketState()

        viewModelScope.launch {
            repository.recordTrade(
                instrumentId = pos.instrument.id,
                strategyId = pos.strategyType.id,
                entryPrice = pos.entryPrice,
                exitPrice = exitPrice,
                quantity = pos.quantity,
                pnlAmount = pnlAmount,
                pnlPercent = pnlPercent,
                exitReason = "بستن دستی",
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    fun resetVirtualPortfolio() {
        pauseBot()
        _uiState.update {
            it.copy(
                availableCash = INITIAL_CAPITAL,
                peakEquity = INITIAL_CAPITAL,
                maxObservedDrawdownPct = 0.0,
                openPosition = null,
                isDrawdownHalted = false,
                lastSignalNote = "سرمایه مجازی به 10,000 واحد بازنشانی شد",
                snackbarMessage = "سرمایه مجازی با موفقیت بازنشانی شد"
            )
        }
        initializeMarketSeries()
    }

    fun updateStrategyConfig(newConfig: StrategyConfig, showToast: Boolean = true) {
        val sanitized = newConfig.copy(
            emaFastPeriod = newConfig.emaFastPeriod.coerceIn(4, 24),
            emaSlowPeriod = newConfig.emaSlowPeriod.coerceIn(
                (newConfig.emaFastPeriod + 3).coerceAtLeast(8),
                50
            ),
            stopLossPercent = newConfig.stopLossPercent.coerceIn(0.5, 15.0),
            takeProfitPercent = newConfig.takeProfitPercent.coerceIn(1.0, 25.0),
            allocationPercent = newConfig.allocationPercent.coerceIn(5.0, 60.0),
            maxDrawdownPercent = newConfig.maxDrawdownPercent.coerceIn(3.0, 30.0),
            rsiOversold = newConfig.rsiOversold.coerceIn(15, 45),
            rsiOverbought = newConfig.rsiOverbought.coerceIn(55, 85)
        )
        _uiState.update {
            it.copy(
                activeConfig = sanitized,
                snackbarMessage = if (showToast) "تنظیمات استراتژی ذخیره و به ربات اعمال شد" else it.snackbarMessage
            )
        }
        publishMarketState()
        viewModelScope.launch {
            repository.saveStrategyConfig(sanitized)
        }
    }

    fun setHistoryFilter(filter: TradeHistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearTrades()
            _uiState.update { it.copy(snackbarMessage = "تاریخچه معاملات پاک شد") }
        }
    }

    // Backtest controls
    fun setBacktestInstrument(instrument: MarketInstrument) {
        _uiState.update { it.copy(backtestInstrument = instrument) }
    }

    fun setBacktestStrategy(strategyType: StrategyType) {
        _uiState.update { it.copy(backtestStrategyType = strategyType) }
    }

    fun setBacktestRegime(regime: MarketRegime) {
        _uiState.update { it.copy(backtestRegime = regime) }
    }

    fun setBacktestCandleCount(count: Int) {
        _uiState.update { it.copy(backtestCandleCount = count) }
    }

    fun runBacktest() {
        val state = _uiState.value
        val configForBacktest = state.activeConfig.copy(strategyType = state.backtestStrategyType)
        _uiState.update { it.copy(isBacktestRunning = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                StrategyEngine.runBacktest(
                    instrument = state.backtestInstrument,
                    config = configForBacktest,
                    regime = state.backtestRegime,
                    candleCount = state.backtestCandleCount,
                    initialCapital = INITIAL_CAPITAL
                )
            }
            _uiState.update {
                it.copy(
                    isBacktestRunning = false,
                    backtestResult = result
                )
            }
        }
    }

    fun applyBacktestToLiveBot() {
        val result = _uiState.value.backtestResult ?: return
        _uiState.update {
            it.copy(
                selectedInstrument = result.instrument,
                snackbarMessage = "استراتژی ${result.strategyConfig.strategyType.persianName} روی ${result.instrument.ticker} فعال شد"
            )
        }
        updateStrategyConfig(result.strategyConfig, showToast = false)
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun publishMarketState() {
        val state = _uiState.value
        val snapshots = MarketInstrument.entries.map { inst ->
            val list = candleBuffers[inst].orEmpty()
            val last = list.lastOrNull()?.close ?: inst.basePrice
            val refIdx = (list.size - 24).coerceAtLeast(0)
            val ref = list.getOrNull(refIdx)?.close ?: inst.basePrice
            val change = if (ref > 0.0) ((last - ref) / ref) * 100.0 else 0.0
            InstrumentSnapshot(instrument = inst, lastPrice = last, changePercent = change)
        }

        val fullBuffer = candleBuffers[state.selectedInstrument].orEmpty()
        val closes = fullBuffer.map { it.close }
        val config = state.activeConfig

        val emaFastAll = TechnicalIndicators.calculateEma(closes, config.emaFastPeriod)
        val emaSlowAll = TechnicalIndicators.calculateEma(closes, config.emaSlowPeriod)
        val bbAll = TechnicalIndicators.calculateBollingerBands(closes, 20, 2.0)
        val rsiAll = TechnicalIndicators.calculateRsi(closes, 14)

        val dropCount = (fullBuffer.size - VISIBLE_WINDOW).coerceAtLeast(0)
        val visibleCandles = fullBuffer.drop(dropCount)
        val visibleFast = emaFastAll.drop(dropCount)
        val visibleSlow = emaSlowAll.drop(dropCount)
        val visibleBb = BollingerBands(
            upper = bbAll.upper.drop(dropCount),
            middle = bbAll.middle.drop(dropCount),
            lower = bbAll.lower.drop(dropCount)
        )

        _uiState.update {
            it.copy(
                instrumentSnapshots = snapshots,
                visibleCandles = visibleCandles,
                emaFastSeries = visibleFast,
                emaSlowSeries = visibleSlow,
                bollingerBands = visibleBb,
                latestRsi = rsiAll.lastOrNull()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        botLoopJob?.cancel()
    }
}
