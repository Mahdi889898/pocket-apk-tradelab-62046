package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_records ORDER BY timestampMillis DESC, id DESC")
    fun observeAllTrades(): Flow<List<TradeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(record: TradeRecordEntity)

    @Query("DELETE FROM trade_records")
    suspend fun clearAllTrades()

    @Query("SELECT * FROM strategy_config WHERE id = 1 LIMIT 1")
    fun observeStrategyConfig(): Flow<StrategyConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStrategyConfig(config: StrategyConfigEntity)
}
