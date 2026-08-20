package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NseStockRecord::class, MarketInsight::class, WatchlistEntity::class, WatchlistItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NseDatabase : RoomDatabase() {
    abstract fun nseDao(): NseDao

    companion object {
        @Volatile
        private var INSTANCE: NseDatabase? = null

        fun getDatabase(context: Context): NseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NseDatabase::class.java,
                    "nse_analytics_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
