package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ClosedTradeSummary
import com.example.model.MarketInstrument
import com.example.model.StrategyConfig
import com.example.model.StrategyType

@Entity(tableName = "trade_records")
data class TradeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val instrumentId: String,
    val strategyId: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnlAmount: Double,
    val pnlPercent: Double,
    val exitReason: String,
    val timestampMillis: Long
) {
    fun toDomain(): ClosedTradeSummary = ClosedTradeSummary(
        id = id,
        instrument = MarketInstrument.fromId(instrumentId),
        strategyType = StrategyType.fromId(strategyId),
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        quantity = quantity,
        pnlAmount = pnlAmount,
        pnlPercent = pnlPercent,
        exitReason = exitReason,
        timestampMillis = timestampMillis
    )
}

@Entity(tableName = "strategy_config")
data class StrategyConfigEntity(
    @PrimaryKey val id: Int = 1,
    val strategyId: String,
    val stopLossPercent: Double,
    val takeProfitPercent: Double,
    val allocationPercent: Double,
    val maxDrawdownPercent: Double,
    val emaFastPeriod: Int,
    val emaSlowPeriod: Int,
    val rsiOversold: Int,
    val rsiOverbought: Int
) {
    fun toDomain(): StrategyConfig = StrategyConfig(
        strategyType = StrategyType.fromId(strategyId),
        stopLossPercent = stopLossPercent,
        takeProfitPercent = takeProfitPercent,
        allocationPercent = allocationPercent,
        maxDrawdownPercent = maxDrawdownPercent,
        emaFastPeriod = emaFastPeriod,
        emaSlowPeriod = emaSlowPeriod,
        rsiOversold = rsiOversold,
        rsiOverbought = rsiOverbought
    )

    companion object {
        fun fromDomain(config: StrategyConfig): StrategyConfigEntity = StrategyConfigEntity(
            id = 1,
            strategyId = config.strategyType.id,
            stopLossPercent = config.stopLossPercent,
            takeProfitPercent = config.takeProfitPercent,
            allocationPercent = config.allocationPercent,
            maxDrawdownPercent = config.maxDrawdownPercent,
            emaFastPeriod = config.emaFastPeriod,
            emaSlowPeriod = config.emaSlowPeriod,
            rsiOversold = config.rsiOversold,
            rsiOverbought = config.rsiOverbought
        )
    }
}
