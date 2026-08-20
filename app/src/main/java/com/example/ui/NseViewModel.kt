package com.example.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader

sealed interface ImportStatus {
    object Idle : ImportStatus
    object Loading : ImportStatus
    data class Success(val count: Int, val message: String) : ImportStatus
    data class Error(val errorMsg: String) : ImportStatus
}

class NseViewModel(private val repository: NseRepository) : ViewModel() {

    // --- Live Market & Auto-Refresh State (60s Coroutine Polling Ticker) ---
    private val _isAutoRefreshEnabled = MutableStateFlow(true)
    val isAutoRefreshEnabled: StateFlow<Boolean> = _isAutoRefreshEnabled.asStateFlow()

    private val _refreshIntervalSeconds = MutableStateFlow(60) // Default: 60s background polling ticker
    val refreshIntervalSeconds: StateFlow<Int> = _refreshIntervalSeconds.asStateFlow()

    private val _isLiveUpdating = MutableStateFlow(false)
    val isLiveUpdating: StateFlow<Boolean> = _isLiveUpdating.asStateFlow()

    private val _lastLiveUpdateTime = MutableStateFlow("")
    val lastLiveUpdateTime: StateFlow<String> = _lastLiveUpdateTime.asStateFlow()

    private val _liveIndices = MutableStateFlow<List<LiveIndexQuote>>(emptyList())
    val liveIndices: StateFlow<List<LiveIndexQuote>> = _liveIndices.asStateFlow()

    private val _liveMarketStatus = MutableStateFlow("Initializing Live Feed...")
    val liveMarketStatus: StateFlow<String> = _liveMarketStatus.asStateFlow()

    private val _secondsUntilNextRefresh = MutableStateFlow(60)
    val secondsUntilNextRefresh: StateFlow<Int> = _secondsUntilNextRefresh.asStateFlow()

    // --- New Listed Company News & IPO Trackers ---
    private val _newListingsNews = MutableStateFlow<List<NewListedCompanyNewsItem>>(NseNewListingsService.getInitialCuratedNews())
    val newListingsNews: StateFlow<List<NewListedCompanyNewsItem>> = _newListingsNews.asStateFlow()

    private val _ipoDebutTrackers = MutableStateFlow<List<IpoDebutTrackRecord>>(NseNewListingsService.curatedIpoDebutRecords)
    val ipoDebutTrackers: StateFlow<List<IpoDebutTrackRecord>> = _ipoDebutTrackers.asStateFlow()

    private val _isFetchingNews = MutableStateFlow(false)
    val isFetchingNews: StateFlow<Boolean> = _isFetchingNews.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadSampleDataIfNeeded()
            // Initial live sync on startup
            fetchLiveNseData()
            fetchNewListingsNews()
        }

        // Background Polling Mechanism: 1-second interval ticker managing 60s cycle
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_isAutoRefreshEnabled.value && !_isLiveUpdating.value) {
                    val remaining = _secondsUntilNextRefresh.value - 1
                    if (remaining <= 0) {
                        _secondsUntilNextRefresh.value = _refreshIntervalSeconds.value
                        fetchLiveNseData()
                    } else {
                        _secondsUntilNextRefresh.value = remaining
                    }
                }
            }
        }
    }

    fun toggleAutoRefresh(enabled: Boolean? = null) {
        val next = enabled ?: !_isAutoRefreshEnabled.value
        _isAutoRefreshEnabled.value = next
        if (next) {
            _secondsUntilNextRefresh.value = _refreshIntervalSeconds.value
        }
    }

    fun setRefreshInterval(seconds: Int) {
        _refreshIntervalSeconds.value = seconds
        _secondsUntilNextRefresh.value = seconds
    }

    fun fetchLiveNseData() {
        viewModelScope.launch {
            _isLiveUpdating.value = true
            _liveMarketStatus.value = "Fetching live NSE quotes..."
            try {
                // Collect any watchlist symbols to also fetch live
                val watchlistSymbols = repository.allWatchlistItems.first().map { it.symbol }
                val result = repository.fetchAndSyncLiveNseData(watchlistSymbols)
                _liveIndices.value = result.indices
                _lastLiveUpdateTime.value = result.timestamp
                _liveMarketStatus.value = result.message
                _secondsUntilNextRefresh.value = _refreshIntervalSeconds.value

                // If today's date was just synced, select today
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val dates = repository.availableDates.first()
                if (dates.contains(todayStr) && _selectedDate.value.isEmpty()) {
                    _selectedDate.value = todayStr
                }
            } catch (e: Exception) {
                _liveMarketStatus.value = "Live feed fallback active"
            } finally {
                _isLiveUpdating.value = false
            }
        }
    }

    fun fetchNewListingsNews() {
        viewModelScope.launch {
            _isFetchingNews.value = true
            try {
                val liveNews = NseNewListingsService.fetchLiveIpoNews()
                if (liveNews.isNotEmpty()) {
                    _newListingsNews.value = liveNews
                }
            } catch (e: Exception) {
                // Keep existing curated list
            } finally {
                _isFetchingNews.value = false
            }
        }
    }

    val availableDates: StateFlow<List<String>> = repository.availableDates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableSymbols: StateFlow<List<String>> = repository.availableSymbols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("NIFTY 50")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedInsightCategory = MutableStateFlow("ALL")
    val selectedInsightCategory: StateFlow<String> = _selectedInsightCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    // Date Range Filter State
    private val _startDateRange = MutableStateFlow("")
    val startDateRange: StateFlow<String> = _startDateRange.asStateFlow()

    private val _endDateRange = MutableStateFlow("")
    val endDateRange: StateFlow<String> = _endDateRange.asStateFlow()

    private val _isDateRangeActive = MutableStateFlow(false)
    val isDateRangeActive: StateFlow<Boolean> = _isDateRangeActive.asStateFlow()

    // Watchlists State
    val allWatchlists: StateFlow<List<WatchlistEntity>> = repository.allWatchlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWatchlistItems: StateFlow<List<WatchlistItemEntity>> = repository.allWatchlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWatchlistId = MutableStateFlow<Long?>(null)
    val selectedWatchlistId: StateFlow<Long?> = _selectedWatchlistId.asStateFlow()

    // Auto-select initial date & date range when available
    init {
        viewModelScope.launch {
            availableDates.collect { dates ->
                if (dates.isNotEmpty()) {
                    val sortedDates = dates.sorted()
                    if (_selectedDate.value.isEmpty()) {
                        _selectedDate.value = sortedDates.last()
                        _startDateRange.value = sortedDates.first()
                        _endDateRange.value = sortedDates.last()
                    }
                }
            }
        }
    }

    // Date Range Market Summary
    val dateRangeSummary: StateFlow<DateRangeMarketSummary?> = combine(
        startDateRange,
        endDateRange,
        isDateRangeActive,
        repository.allStockRecords
    ) { start, end, active, records ->
        if (!active || start.isEmpty() || end.isEmpty()) null
        else NseAnalyticsEngine.analyzeDateRange(start, end, records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All Records
    val allStockRecords: StateFlow<List<NseStockRecord>> = repository.allStockRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Records for selected date or selected date range
    val recordsForSelectedDate: StateFlow<List<NseStockRecord>> = combine(
        combine(repository.allStockRecords, selectedDate, startDateRange) { records, date, start ->
            Triple(records, date, start)
        },
        combine(endDateRange, isDateRangeActive, searchQuery) { end, rangeActive, query ->
            Triple(end, rangeActive, query)
        }
    ) { (records, date, start), (end, rangeActive, query) ->
        val dateFiltered = if (rangeActive && start.isNotEmpty() && end.isNotEmpty()) {
            records.filter { it.date in start..end }
        } else {
            records.filter { it.date == date }
        }
        if (query.isBlank()) {
            dateFiltered
        } else {
            dateFiltered.filter {
                it.symbol.contains(query, ignoreCase = true) || it.series.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Market Summary
    val dailySummary: StateFlow<DailyMarketSummary?> = combine(
        selectedDate,
        repository.allStockRecords
    ) { date, records ->
        if (date.isEmpty()) null else NseAnalyticsEngine.analyzeDate(date, records)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Symbol Historical Records
    val recordsForSelectedSymbol: StateFlow<List<NseStockRecord>> = combine(
        repository.allStockRecords,
        selectedSymbol
    ) { records, symbol ->
        records.filter { it.symbol.equals(symbol, ignoreCase = true) }.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Insights
    val filteredInsights: StateFlow<List<MarketInsight>> = combine(
        repository.allInsights,
        selectedInsightCategory,
        selectedDate
    ) { insights, category, date ->
        when (category) {
            "ALL" -> insights
            "BOOKMARKED" -> insights.filter { it.isBookmarked }
            "SELECTED_DATE" -> insights.filter { it.date == date }
            else -> insights.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Actions
    fun selectDate(date: String) {
        _selectedDate.value = date
        _isDateRangeActive.value = false
    }

    fun setDateRange(start: String, end: String) {
        if (start <= end) {
            _startDateRange.value = start
            _endDateRange.value = end
            _isDateRangeActive.value = true
        } else {
            _startDateRange.value = end
            _endDateRange.value = start
            _isDateRangeActive.value = true
        }
    }

    fun toggleDateRangeActive(active: Boolean) {
        _isDateRangeActive.value = active
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
    }

    fun selectWatchlist(watchlistId: Long?) {
        _selectedWatchlistId.value = watchlistId
    }

    fun createWatchlist(name: String, description: String = "", initialSymbols: List<String> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repository.createWatchlist(name, description, initialSymbols)
            _selectedWatchlistId.value = newId
        }
    }

    fun toggleSymbolInWatchlist(watchlistId: Long, symbol: String) {
        viewModelScope.launch {
            val currentItems = repository.getWatchlistItems(watchlistId).first()
            val exists = currentItems.any { it.symbol.equals(symbol, ignoreCase = true) }
            if (exists) {
                repository.removeSymbolFromWatchlist(watchlistId, symbol)
            } else {
                repository.addSymbolToWatchlist(watchlistId, symbol)
            }
        }
    }

    fun deleteWatchlist(watchlistId: Long) {
        viewModelScope.launch {
            repository.deleteWatchlist(watchlistId)
            if (_selectedWatchlistId.value == watchlistId) {
                _selectedWatchlistId.value = null
            }
        }
    }

    fun setInsightCategory(category: String) {
        _selectedInsightCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(id: Long, current: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(id, !current)
        }
    }

    fun deleteInsight(id: Long) {
        viewModelScope.launch {
            repository.deleteInsight(id)
        }
    }

    fun addCustomInsightNote(title: String, summary: String, category: String, symbol: String? = null) {
        viewModelScope.launch {
            val date = _selectedDate.value.ifEmpty { "2026-08-09" }
            val insight = MarketInsight(
                date = date,
                title = title,
                summary = summary,
                category = category,
                impactSymbol = symbol,
                isBookmarked = true
            )
            repository.addCustomInsight(insight)
        }
    }

    fun importCsvText(csvText: String, sourceName: String = "Pasted CSV") {
        if (csvText.isBlank()) {
            _importStatus.value = ImportStatus.Error("CSV text is empty.")
            return
        }
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            try {
                val count = repository.importCsvContent(csvText, sourceFileName = sourceName)
                if (count > 0) {
                    _importStatus.value = ImportStatus.Success(count, "Successfully imported $count stock records!")
                    // Update date selection to imported data if available
                    val dates = repository.availableDates.first()
                    if (dates.isNotEmpty()) {
                        _selectedDate.value = dates.first()
                    }
                } else {
                    _importStatus.value = ImportStatus.Error("No valid stock records found in CSV.")
                }
            } catch (e: Exception) {
                _importStatus.value = ImportStatus.Error("Failed to parse CSV: ${e.localizedMessage}")
            }
        }
    }

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            try {
                val contentResolver = context.contentResolver
                val stringBuilder = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }
                val fileName = uri.lastPathSegment ?: "Uploaded_NSE.csv"
                val count = repository.importCsvContent(stringBuilder.toString(), sourceFileName = fileName)
                if (count > 0) {
                    _importStatus.value = ImportStatus.Success(count, "Successfully imported $count records from $fileName!")
                    val dates = repository.availableDates.first()
                    if (dates.isNotEmpty()) {
                        _selectedDate.value = dates.first()
                    }
                } else {
                    _importStatus.value = ImportStatus.Error("No records parsed from file.")
                }
            } catch (e: Exception) {
                _importStatus.value = ImportStatus.Error("Error reading file: ${e.localizedMessage}")
            }
        }
    }

    fun resetImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            repository.resetDataToSample()
            val dates = repository.availableDates.first()
            if (dates.isNotEmpty()) {
                _selectedDate.value = dates.first()
            }
            _importStatus.value = ImportStatus.Success(0, "Reset data to authentic NSE sample dataset!")
        }
    }
}

class NseViewModelFactory(private val repository: NseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
