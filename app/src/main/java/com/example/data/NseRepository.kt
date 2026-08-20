package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NseRepository(private val dao: NseDao) {

    val allStockRecords: Flow<List<NseStockRecord>> = dao.getAllStockRecords()
    val availableDates: Flow<List<String>> = dao.getDistinctDates()
    val availableSymbols: Flow<List<String>> = dao.getDistinctSymbols()
    val allInsights: Flow<List<MarketInsight>> = dao.getAllInsights()
    val bookmarkedInsights: Flow<List<MarketInsight>> = dao.getBookmarkedInsights()
    val allWatchlists: Flow<List<WatchlistEntity>> = dao.getAllWatchlists()
    val allWatchlistItems: Flow<List<WatchlistItemEntity>> = dao.getAllWatchlistItems()

    fun getWatchlistItems(watchlistId: Long): Flow<List<WatchlistItemEntity>> {
        return dao.getWatchlistItems(watchlistId)
    }

    suspend fun createWatchlist(name: String, description: String = "", initialSymbols: List<String> = emptyList()): Long = withContext(Dispatchers.IO) {
        val id = dao.insertWatchlist(WatchlistEntity(name = name, description = description))
        for (sym in initialSymbols) {
            dao.insertWatchlistItem(WatchlistItemEntity(watchlistId = id, symbol = sym))
        }
        return@withContext id
    }

    suspend fun addSymbolToWatchlist(watchlistId: Long, symbol: String) = withContext(Dispatchers.IO) {
        dao.insertWatchlistItem(WatchlistItemEntity(watchlistId = watchlistId, symbol = symbol))
    }

    suspend fun removeSymbolFromWatchlist(watchlistId: Long, symbol: String) = withContext(Dispatchers.IO) {
        dao.deleteWatchlistItem(watchlistId, symbol)
    }

    suspend fun deleteWatchlist(watchlistId: Long) = withContext(Dispatchers.IO) {
        dao.deleteWatchlistById(watchlistId)
    }

    suspend fun loadSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val currentDates = availableDates.first()
        if (currentDates.isEmpty()) {
            val sampleRecords = NseCsvParser.generateSampleNseData()
            dao.insertStockRecords(sampleRecords)

            // Generate auto-insights for each date
            val allDistinctDates = sampleRecords.map { it.date }.distinct()
            val allAutoInsights = mutableListOf<MarketInsight>()
            for (date in allDistinctDates) {
                allAutoInsights.addAll(NseAnalyticsEngine.generateAutoInsights(date, sampleRecords))
            }
            dao.insertInsights(allAutoInsights)

            // Seed sample Watchlists
            val wl1Id = dao.insertWatchlist(WatchlistEntity(name = "Core Favorites", description = "Key portfolio benchmark stocks"))
            listOf("RELIANCE", "TCS", "HDFCBANK", "INFY").forEach { sym ->
                dao.insertWatchlistItem(WatchlistItemEntity(watchlistId = wl1Id, symbol = sym))
            }

            val wl2Id = dao.insertWatchlist(WatchlistEntity(name = "Banking & Finance", description = "Banking sector movers"))
            listOf("HDFCBANK", "ICICIBANK", "SBIN", "KOTAKBANK").forEach { sym ->
                dao.insertWatchlistItem(WatchlistItemEntity(watchlistId = wl2Id, symbol = sym))
            }
        }
    }

    suspend fun importCsvContent(csvText: String, defaultDate: String? = null, sourceFileName: String = "Uploaded_CSV"): Int = withContext(Dispatchers.IO) {
        val parsedRecords = NseCsvParser.parseCsv(csvText, defaultDate, sourceFileName)
        if (parsedRecords.isNotEmpty()) {
            dao.insertStockRecords(parsedRecords)

            // Auto-generate insights for dates in imported batch
            val importedDates = parsedRecords.map { it.date }.distinct()
            val currentAll = dao.getAllStockRecords().first()
            val newInsights = mutableListOf<MarketInsight>()
            for (date in importedDates) {
                newInsights.addAll(NseAnalyticsEngine.generateAutoInsights(date, currentAll))
            }
            if (newInsights.isNotEmpty()) {
                dao.insertInsights(newInsights)
            }
        }
        return@withContext parsedRecords.size
    }

    suspend fun addCustomInsight(insight: MarketInsight) = withContext(Dispatchers.IO) {
        dao.insertInsight(insight)
    }

    suspend fun toggleBookmark(insightId: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBookmarkStatus(insightId, isBookmarked)
    }

    suspend fun deleteInsight(insightId: Long) = withContext(Dispatchers.IO) {
        dao.deleteInsightById(insightId)
    }

    suspend fun resetDataToSample() = withContext(Dispatchers.IO) {
        dao.clearStockRecords()
        dao.clearMarketInsights()
        loadSampleDataIfNeeded()
    }
}
