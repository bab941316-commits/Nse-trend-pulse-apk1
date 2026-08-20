package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NseDao {
    // --- Stock Records Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockRecords(records: List<NseStockRecord>)

    @Query("SELECT * FROM stock_records ORDER BY date DESC, symbol ASC")
    fun getAllStockRecords(): Flow<List<NseStockRecord>>

    @Query("SELECT DISTINCT date FROM stock_records ORDER BY date DESC")
    fun getDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM stock_records WHERE date = :date ORDER BY (close - prevClose) / NULLIF(prevClose, 0) DESC")
    fun getRecordsByDate(date: String): Flow<List<NseStockRecord>>

    @Query("SELECT * FROM stock_records WHERE symbol = :symbol ORDER BY date ASC")
    fun getRecordsForSymbol(symbol: String): Flow<List<NseStockRecord>>

    @Query("SELECT * FROM stock_records WHERE date = :date AND series = 'EQ' AND prevClose > 0 ORDER BY ((close - prevClose) / prevClose) DESC LIMIT :limit")
    fun getTopGainersByDate(date: String, limit: Int = 5): Flow<List<NseStockRecord>>

    @Query("SELECT * FROM stock_records WHERE date = :date AND series = 'EQ' AND prevClose > 0 ORDER BY ((close - prevClose) / prevClose) ASC LIMIT :limit")
    fun getTopLosersByDate(date: String, limit: Int = 5): Flow<List<NseStockRecord>>

    @Query("SELECT * FROM stock_records WHERE symbol = :symbol ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecordForSymbol(symbol: String): NseStockRecord?

    @Query("SELECT DISTINCT symbol FROM stock_records ORDER BY symbol ASC")
    fun getDistinctSymbols(): Flow<List<String>>

    @Query("DELETE FROM stock_records")
    suspend fun clearStockRecords()

    // --- Market Insights Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: MarketInsight)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<MarketInsight>)

    @Query("SELECT * FROM market_insights ORDER BY date DESC, id DESC")
    fun getAllInsights(): Flow<List<MarketInsight>>

    @Query("SELECT * FROM market_insights WHERE isBookmarked = 1 ORDER BY date DESC")
    fun getBookmarkedInsights(): Flow<List<MarketInsight>>

    @Query("SELECT * FROM market_insights WHERE category = :category ORDER BY date DESC")
    fun getInsightsByCategory(category: String): Flow<List<MarketInsight>>

    @Query("SELECT * FROM market_insights WHERE date = :date ORDER BY id DESC")
    fun getInsightsForDate(date: String): Flow<List<MarketInsight>>

    @Query("UPDATE market_insights SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM market_insights WHERE id = :id")
    suspend fun deleteInsightById(id: Long)

    @Query("DELETE FROM market_insights")
    suspend fun clearMarketInsights()

    // --- Watchlist Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: WatchlistEntity): Long

    @Query("SELECT * FROM watchlists ORDER BY name ASC")
    fun getAllWatchlists(): Flow<List<WatchlistEntity>>

    @Query("DELETE FROM watchlists WHERE id = :id")
    suspend fun deleteWatchlistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist_items WHERE watchlistId = :watchlistId AND symbol = :symbol")
    suspend fun deleteWatchlistItem(watchlistId: Long, symbol: String)

    @Query("SELECT * FROM watchlist_items WHERE watchlistId = :watchlistId ORDER BY addedAt DESC")
    fun getWatchlistItems(watchlistId: Long): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getAllWatchlistItems(): Flow<List<WatchlistItemEntity>>
}

