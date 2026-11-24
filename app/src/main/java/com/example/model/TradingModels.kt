package com.example.model

enum class MarketInstrument(
    val id: String,
    val ticker: String,
    val persianName: String,
    val sector: String,
    val basePrice: Double,
    val baseVolatility: Double,
    val baseDrift: Double
) {
    IDX_ALPHA(
        id = "IDX_ALPHA",
        ticker = "IDX-ALPHA",
        persianName = "شاخص فناوری آلفا",
        sector = "فناوری",
        basePrice = 420.0,
        baseVolatility = 0.011,
        baseDrift = 0.0005
    ),
    IDX_GOLD(
        id = "IDX_GOLD",
        ticker = "IDX-GOLD",
        persianName = "شاخص طلای مجازی",
        sector = "فلزات",
        basePrice = 2350.0,
        baseVolatility = 0.0065,
        baseDrift = 0.0003
    ),
    IDX_ENERGY(
        id = "IDX_ENERGY",
        ticker = "IDX-ENERGY",
        persianName = "شاخص انرژی پاک",
        sector = "انرژی",
        basePrice = 148.0,
        baseVolatility = 0.0135,
        baseDrift = 0.0004
    ),
    IDX_GLOBAL(
        id = "IDX_GLOBAL",
        ticker = "IDX-GLOBAL",
        persianName = "شاخص سبد جهانی",
        sector = "عمومی",
        basePrice = 5120.0,
        baseVolatility = 0.0075,
        baseDrift = 0.00035
    ),
    IDX_TECH(
        id = "IDX_TECH",
        ticker = "IDX-TECH",
        persianName = "شاخص هوش مصنوعی",
        sector = "نوآوری",
        basePrice = 860.0,
        baseVolatility = 0.015,
        baseDrift = 0.0006
    );

    companion object {
        fun fromId(id: String): MarketInstrument =
            entries.firstOrNull { it.id == id } ?: IDX_ALPHA
    }
}

enum class StrategyType(
    val id: String,
    val persianName: String,
    val shortBadge: String,
    val description: String,
    val formulaHint: String
) {
    EMA_CROSSOVER(
        id = "EMA_CROSSOVER",
        persianName = "تقاطع میانگین متحرک (EMA)",
        shortBadge = "EMA Cross",
        description = "ورود خودکار هنگام عبور EMA سریع به بالای EMA کند همراه با تأیید مومنتوم و خروج در تقاطع معکوس.",
        formulaHint = "Buy: EMA(Fast) ↑ EMA(Slow) • Exit: Cross ↓ / TP / SL"
    ),
    RSI_MEAN_REVERSION(
        id = "RSI_MEAN_REVERSION",
        persianName = "بازگشت به میانگین RSI",
        shortBadge = "RSI Reversal",
        description = "شناسایی نقاط اشباع فروش برای خرید در کف‌های کوتاه‌مدت و ذخیره سود در محدوده اشباع خرید.",
        formulaHint = "Buy: RSI < Oversold & Hook ↑ • Exit: RSI > Overbought"
    ),
    MACD_MOMENTUM(
        id = "MACD_MOMENTUM",
        persianName = "مومنتوم هوشمند MACD",
        shortBadge = "MACD Trend",
        description = "تعقیب روندهای پرقدرت بر اساس تقاطع خط MACD و خط سیگنال با فیلتر هیستوگرام صعودی.",
        formulaHint = "Buy: MACD ↑ Signal (Hist > 0) • Exit: Hist < 0 / TP / SL"
    ),
    BOLLINGER_BREAKOUT(
        id = "BOLLINGER_BREAKOUT",
        persianName = "شکست باند بولینگر",
        shortBadge = "BB Breakout",
        description = "شکار بازگشت از کف باند پایین بولینگر و خروج در برخورد به باند میانی یا بالایی با کنترل نوسان.",
        formulaHint = "Buy: Close Rebounds Lower BB • Exit: Upper BB / TP / SL"
    );

    companion object {
        fun fromId(id: String): StrategyType =
            entries.firstOrNull { it.id == id } ?: EMA_CROSSOVER
    }
}

enum class MarketRegime(
    val id: String,
    val persianName: String,
    val driftPerBar: Double,
    val volatilityMultiplier: Double,
    val meanReversionStrength: Double
) {
    BULLISH_TREND(
        id = "BULLISH",
        persianName = "روند صعودی",
        driftPerBar = 0.0014,
        volatilityMultiplier = 0.95,
        meanReversionStrength = 0.04
    ),
    SIDEWAYS_RANGE(
        id = "SIDEWAYS",
        persianName = "بازار رنج (نوسانی)",
        driftPerBar = 0.0,
        volatilityMultiplier = 0.9,
        meanReversionStrength = 0.16
    ),
    HIGH_VOLATILITY(
        id = "HIGH_VOL",
        persianName = "نوسان شدید",
        driftPerBar = 0.0003,
        volatilityMultiplier = 1.75,
        meanReversionStrength = 0.06
    ),
    CRASH_RECOVERY(
        id = "CRASH_REC",
        persianName = "سقوط و بازیابی",
        driftPerBar = 0.0002,
        volatilityMultiplier = 1.35,
        meanReversionStrength = 0.05
    )
}

enum class SignalMarker {
    NONE,
    BUY,
    SELL_PROFIT,
    SELL_LOSS
}

data class Candle(
    val index: Int,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val timestampMillis: Long,
    val signalMarker: SignalMarker = SignalMarker.NONE
)

data class StrategyConfig(
    val strategyType: StrategyType = StrategyType.EMA_CROSSOVER,
    val stopLossPercent: Double = 2.0,
    val takeProfitPercent: Double = 4.0,
    val allocationPercent: Double = 25.0,
    val maxDrawdownPercent: Double = 12.0,
    val emaFastPeriod: Int = 9,
    val emaSlowPeriod: Int = 21,
    val rsiOversold: Int = 32,
    val rsiOverbought: Int = 68
)

data class OpenPosition(
    val instrument: MarketInstrument,
    val strategyType: StrategyType,
    val entryPrice: Double,
    val quantity: Double,
    val investedCapital: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val entryCandleIndex: Int,
    val entryTimestampMillis: Long,
    val entryReason: String
) {
    fun unrealizedPnl(currentPrice: Double): Double = (currentPrice - entryPrice) * quantity
    fun unrealizedPnlPercent(currentPrice: Double): Double =
        if (entryPrice > 0.0) ((currentPrice - entryPrice) / entryPrice) * 100.0 else 0.0
}

data class ClosedTradeSummary(
    val id: Long,
    val instrument: MarketInstrument,
    val strategyType: StrategyType,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnlAmount: Double,
    val pnlPercent: Double,
    val exitReason: String,
    val timestampMillis: Long
)

data class BacktestResult(
    val instrument: MarketInstrument,
    val strategyConfig: StrategyConfig,
    val regime: MarketRegime,
    val candleCount: Int,
    val initialCapital: Double,
    val finalEquity: Double,
    val equityCurve: List<Double>,
    val benchmarkCurve: List<Double>,
    val netReturnPercent: Double,
    val benchmarkReturnPercent: Double,
    val winRatePercent: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double,
    val profitFactor: Double,
    val bestTradePercent: Double,
    val worstTradePercent: Double
)

enum class TradeSignalAction {
    BUY,
    SELL,
    HOLD
}

data class TradeSignal(
    val action: TradeSignalAction,
    val reason: String
)
