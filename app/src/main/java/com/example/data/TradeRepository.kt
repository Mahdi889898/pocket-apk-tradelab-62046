package com.example.data

import com.example.model.ClosedTradeSummary
import com.example.model.StrategyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TradeRepository(private val tradeDao: TradeDao) {
    val trades: Flow<List<ClosedTradeSummary>> =
        tradeDao.observeAllTrades().map { list -> list.map { it.toDomain() } }

    val strategyConfig: Flow<StrategyConfig> =
        tradeDao.observeStrategyConfig().map { entity -> entity?.toDomain() ?: StrategyConfig() }

    suspend fun recordTrade(
        instrumentId: String,
        strategyId: String,
        entryPrice: Double,
        exitPrice: Double,
        quantity: Double,
        pnlAmount: Double,
        pnlPercent: Double,
        exitReason: String,
        timestampMillis: Long
    ) {
        tradeDao.insertTrade(
            TradeRecordEntity(
                instrumentId = instrumentId,
                strategyId = strategyId,
                entryPrice = entryPrice,
                exitPrice = exitPrice,
                quantity = quantity,
                pnlAmount = pnlAmount,
                pnlPercent = pnlPercent,
                exitReason = exitReason,
                timestampMillis = timestampMillis
            )
        )
    }

    suspend fun clearTrades() {
        tradeDao.clearAllTrades()
    }

    suspend fun saveStrategyConfig(config: StrategyConfig) {
        tradeDao.upsertStrategyConfig(StrategyConfigEntity.fromDomain(config))
    }
}
