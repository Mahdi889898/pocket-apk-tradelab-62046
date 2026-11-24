package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TradeRecordEntity::class, StrategyConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TradeLabDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile
        private var INSTANCE: TradeLabDatabase? = null

        fun getInstance(context: Context): TradeLabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TradeLabDatabase::class.java,
                    "tradelab_simulator.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
