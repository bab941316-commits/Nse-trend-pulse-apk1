package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_records",
    indices = [
        Index(value = ["date"]),
        Index(value = ["symbol"]),
        Index(value = ["date", "symbol"], unique = true)
    ]
)
data class NseStockRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Normalized format: YYYY-MM-DD
    val symbol: String, // e.g., RELIANCE, NIFTY 50, TCS
    val series: String = "EQ",
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val last: Double = 0.0,
    val prevClose: Double = 0.0,
    val totalTradedQty: Long = 0,
    val totalTradedVal: Double = 0.0,
    val totalTrades: Long = 0,
    val deliverableQty: Long = 0,
    val pctDlyQtToTrd: Double = 0.0,
    val sourceFileName: String = "NSE_CSV"
) {
    val priceChange: Double
        get() = close - prevClose

    val priceChangePct: Double
        get() = if (prevClose > 0) ((close - prevClose) / prevClose) * 100.0 else 0.0

    val dayRangePct: Double
        get() = if (low > 0) ((high - low) / low) * 100.0 else 0.0
}

@Entity(
    tableName = "market_insights",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class MarketInsight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val title: String,
    val summary: String,
    val category: String, // BULLISH, BEARISH, VOLUME_SURGE, BREAKOUT, MARKET_BREADTH, SECTOR_TREND
    val impactSymbol: String? = null,
    val metricValue: String? = null, // e.g. "+2.45%", "3.1x Vol"
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watchlist_items",
    primaryKeys = ["watchlistId", "symbol"],
    indices = [Index(value = ["symbol"])]
)
data class WatchlistItemEntity(
    val watchlistId: Long,
    val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)

