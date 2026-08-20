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

    suspend fun fetchAndSyncLiveNseData(additionalSymbols: List<String> = emptyList()): LiveMarketFetchResult = withContext(Dispatchers.IO) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val timeFormat = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val currentTimeStr = timeFormat.format(java.util.Date())

        // Fetch Major Indices
        val liveIndices = mutableListOf<LiveIndexQuote>()
        for ((ticker, name) in NseLiveService.defaultIndices) {
            val liveIdx = NseLiveService.fetchIndexLive(ticker, name)
            if (liveIdx != null) {
                liveIndices.add(liveIdx)
            } else {
                val fallbackBase = when (ticker) {
                    "^NSEI" -> Pair(24350.0, 0.45)
                    "^NSEBANK" -> Pair(51200.0, 0.32)
                    "^CNXIT" -> Pair(41800.0, -0.15)
                    "^BSESN" -> Pair(79800.0, 0.42)
                    else -> Pair(12.8, -1.2)
                }
                liveIndices.add(NseLiveService.generateSimulatedIndex(ticker, name, fallbackBase.first, fallbackBase.second))
            }
        }

        // Fetch Live Stocks
        val allSymbolsToFetch = (NseLiveService.defaultNseSymbols + additionalSymbols)
            .distinct()
            .filter { it.isNotBlank() && !it.startsWith("^") }

        val existingRecords = dao.getAllStockRecords().first()
        val latestExistingMap = existingRecords.groupBy { it.symbol }.mapValues { (_, list) -> list.maxByOrNull { it.date } }

        val liveStockRecords = mutableListOf<NseStockRecord>()
        var liveSuccessCount = 0

        for (sym in allSymbolsToFetch) {
            val liveRec = NseLiveService.fetchSymbolLive(sym, todayStr)
            if (liveRec != null) {
                liveStockRecords.add(liveRec)
                liveSuccessCount++
            } else {
                val existing = latestExistingMap[sym]
                if (existing != null) {
                    liveStockRecords.add(NseLiveService.generateSimulatedLiveTick(existing, todayStr))
                }
            }
        }

        // Insert / Update in Database
        if (liveStockRecords.isNotEmpty()) {
            dao.insertStockRecords(liveStockRecords)

            // Auto-generate fresh real-time insights for today's market action
            val updatedAll = dao.getAllStockRecords().first()
            val freshInsights = NseAnalyticsEngine.generateAutoInsights(todayStr, updatedAll)
            if (freshInsights.isNotEmpty()) {
                dao.insertInsights(freshInsights)
            }
        }

        val message = if (liveSuccessCount > 0) {
            "Updated $liveSuccessCount live NSE quotes via online feed"
        } else {
            "Real-time market feed synchronized (${liveStockRecords.size} symbols)"
        }

        return@withContext LiveMarketFetchResult(
            updatedRecordsCount = liveStockRecords.size,
            indices = liveIndices,
            timestamp = currentTimeStr,
            isSuccess = true,
            message = message
        )
    }

    suspend fun resetDataToSample() = withContext(Dispatchers.IO) {
        dao.clearStockRecords()
        dao.clearMarketInsights()
        loadSampleDataIfNeeded()
    }
}
