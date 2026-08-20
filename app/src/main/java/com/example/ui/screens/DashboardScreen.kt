package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyMarketSummary
import com.example.data.DateRangeMarketSummary
import com.example.data.LiveIndexQuote
import com.example.data.MarketInsight
import com.example.data.NewListedCompanyNewsItem
import com.example.data.NseStockRecord
import com.example.data.WatchlistEntity
import com.example.data.WatchlistItemEntity
import com.example.ui.components.DateRangePickerComponent
import com.example.ui.components.MarketBreadthBar
import com.example.ui.components.MarketSentimentIndicator
import com.example.ui.components.NseMarketStatusBadge
import com.example.ui.components.NseMarketStatusCard
import com.example.ui.components.StockTrendLineChart
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.NeutralBlue
import com.example.ui.theme.NeutralBlueBg
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class DashboardSortMode(val label: String) {
    PERFORMANCE("Performance"),
    VOLUME("Volume"),
    DATE("Date"),
    SYMBOL("Symbol")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    selectedDate: String,
    startDateRange: String,
    endDateRange: String,
    isDateRangeActive: Boolean,
    availableDates: List<String>,
    dailySummary: DailyMarketSummary?,
    dateRangeSummary: DateRangeMarketSummary?,
    allStockRecords: List<NseStockRecord>,
    insights: List<MarketInsight>,
    watchlists: List<WatchlistEntity>,
    watchlistItems: List<WatchlistItemEntity> = emptyList(),
    selectedWatchlistId: Long?,
    // Live NSE Streaming parameters
    isAutoRefreshEnabled: Boolean = true,
    refreshIntervalSeconds: Int = 15,
    isLiveUpdating: Boolean = false,
    lastLiveUpdateTime: String = "",
    liveIndices: List<LiveIndexQuote> = emptyList(),
    liveMarketStatus: String = "",
    secondsUntilNextRefresh: Int = 15,
    onToggleAutoRefresh: (Boolean) -> Unit = {},
    onSetRefreshInterval: (Int) -> Unit = {},
    onRefreshLiveNow: () -> Unit = {},
    onDateSelected: (String) -> Unit,
    onDateRangeSelected: (start: String, end: String) -> Unit,
    onClearDateRange: () -> Unit,
    onSelectWatchlist: (Long?) -> Unit,
    onSymbolClick: (String) -> Unit,
    onBookmarkToggle: (Long, Boolean) -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToWatchlists: () -> Unit,
    onNavigateToGainersLosers: () -> Unit = {},
    newListingsNews: List<NewListedCompanyNewsItem> = emptyList(),
    onNavigateToNews: () -> Unit = {}
) {
    var gainersTabSelected by remember { mutableIntStateOf(0) } // 0: Gainers, 1: Losers, 2: High Volume
    var searchQuery by remember { mutableStateOf("") }
    var activeSortMode by remember { mutableStateOf(DashboardSortMode.PERFORMANCE) }
    var isSortDescending by remember { mutableStateOf(true) }
    var showAllSearchResults by remember { mutableStateOf(false) }

    // Live Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val filteredAndSortedStockList = remember(
        allStockRecords,
        selectedDate,
        startDateRange,
        endDateRange,
        isDateRangeActive,
        searchQuery,
        activeSortMode,
        isSortDescending,
        selectedWatchlistId,
        watchlists,
        watchlistItems
    ) {
        var baseList = if (isDateRangeActive && startDateRange.isNotEmpty() && endDateRange.isNotEmpty()) {
            allStockRecords.filter { it.date in startDateRange..endDateRange }
        } else if (selectedDate.isNotEmpty()) {
            val dateRecords = allStockRecords.filter { it.date == selectedDate }
            if (dateRecords.isNotEmpty()) dateRecords else allStockRecords
        } else {
            allStockRecords
        }

        if (selectedWatchlistId != null) {
            val symbolsForWatchlist = watchlistItems
                .filter { it.watchlistId == selectedWatchlistId }
                .map { it.symbol }
            if (symbolsForWatchlist.isNotEmpty()) {
                baseList = baseList.filter { rec -> symbolsForWatchlist.any { sym -> sym.equals(rec.symbol, ignoreCase = true) } }
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            baseList = baseList.filter { rec ->
                rec.symbol.contains(q, ignoreCase = true) || rec.series.contains(q, ignoreCase = true)
            }
        }

        when (activeSortMode) {
            DashboardSortMode.PERFORMANCE -> {
                if (isSortDescending) baseList.sortedByDescending { it.priceChangePct }
                else baseList.sortedBy { it.priceChangePct }
            }
            DashboardSortMode.VOLUME -> {
                if (isSortDescending) baseList.sortedByDescending { it.totalTradedQty }
                else baseList.sortedBy { it.totalTradedQty }
            }
            DashboardSortMode.DATE -> {
                if (isSortDescending) baseList.sortedByDescending { it.date }
                else baseList.sortedBy { it.date }
            }
            DashboardSortMode.SYMBOL -> {
                if (isSortDescending) baseList.sortedByDescending { it.symbol }
                else baseList.sortedBy { it.symbol }
            }
        }
    }

    val availableSymbols = remember(allStockRecords) {
        val list = allStockRecords.map { it.symbol }.distinct().filter { it.isNotBlank() }.sorted()
        if (list.contains("NIFTY 50")) listOf("NIFTY 50") + (list - "NIFTY 50") else list
    }

    var selectedChartSymbol by remember(availableSymbols) {
        mutableStateOf(availableSymbols.firstOrNull() ?: "NIFTY 50")
    }

    val selectedSymbolRecords = remember(allStockRecords, selectedChartSymbol, isDateRangeActive, startDateRange, endDateRange) {
        val filteredBySymbol = allStockRecords.filter { it.symbol == selectedChartSymbol }.sortedBy { it.date }
        if (isDateRangeActive && startDateRange.isNotBlank() && endDateRange.isNotBlank()) {
            filteredBySymbol.filter { it.date >= startDateRange && it.date <= endDateRange }
        } else {
            filteredBySymbol
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // --- Live NSE Streaming Control Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_nse_control_card"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isAutoRefreshEnabled) PrimaryCyan.copy(alpha = 0.5f) else NavyCardBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing Live Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isAutoRefreshEnabled) BullishGreen.copy(alpha = pulseAlpha) else AccentGold,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LIVE NSE MARKET",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isAutoRefreshEnabled) BullishGreenBg else NeutralBlueBg)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAutoRefreshEnabled) "STREAMING" else "PAUSED",
                                            color = if (isAutoRefreshEnabled) BullishGreen else NeutralBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (lastLiveUpdateTime.isNotEmpty()) "Last synced: $lastLiveUpdateTime" else "Auto-fetching live NSE quotes",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Auto-Refresh Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Auto",
                                color = if (isAutoRefreshEnabled) PrimaryCyan else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isAutoRefreshEnabled,
                                onCheckedChange = { onToggleAutoRefresh(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = NavySurfaceVariant
                                ),
                                modifier = Modifier.testTag("auto_refresh_toggle")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Live Controls Row: Countdown, Interval Selector, and Refresh Now Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Countdown / Interval Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAutoRefreshEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryCyan.copy(alpha = 0.15f))
                                        .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⚡ In ${secondsUntilNextRefresh}s",
                                        color = PrimaryCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Interval pickers
                            listOf(30, 60, 120).forEach { sec ->
                                val isSelected = refreshIntervalSeconds == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                                        .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(8.dp))
                                        .clickable { onSetRefreshInterval(sec) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Manual Refresh Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySurfaceVariant)
                                .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = !isLiveUpdating) { onRefreshLiveNow() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("refresh_live_now_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Live Feed",
                                    tint = PrimaryCyan,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .rotate(if (isLiveUpdating) spinAngle else 0f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLiveUpdating) "Updating..." else "Refresh Now",
                                    color = PrimaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (liveMarketStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• $liveMarketStatus",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // --- Major NSE Live Indices Ticker Row ---
        if (liveIndices.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Major NSE Indices Live",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(liveIndices) { indexQuote ->
                            LiveIndexCardItem(
                                quote = indexQuote,
                                onClick = { onSymbolClick(indexQuote.name) }
                            )
                        }
                    }
                }
            }
        }

        // Date Range Picker Component
        item {
            DateRangePickerComponent(
                selectedDate = selectedDate,
                startDateRange = startDateRange,
                endDateRange = endDateRange,
                isDateRangeActive = isDateRangeActive,
                availableDates = availableDates,
                dateRangeSummary = dateRangeSummary,
                onDateSelected = onDateSelected,
                onDateRangeSelected = onDateRangeSelected,
                onClearDateRange = onClearDateRange
            )
        }

        // Watchlists Quick Strip
        if (watchlists.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Watchlist Filter",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Manage Watchlists →",
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToWatchlists() }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isAllSelected = selectedWatchlistId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isAllSelected) PrimaryCyan else NavySurfaceVariant)
                                    .border(1.dp, if (isAllSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(20.dp))
                                    .clickable { onSelectWatchlist(null) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All Stocks",
                                    color = if (isAllSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        items(watchlists) { wl ->
                            val isSelected = wl.id == selectedWatchlistId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                                    .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(20.dp))
                                    .clickable { onSelectWatchlist(wl.id) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⭐ ${wl.name}",
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Executive Summary Card
        dailySummary?.let { summary ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryCyan.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = PrimaryCyan
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("NSE Daily Market Pulse", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Date: ${summary.date}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            // Sentiment Badge
                            val sentimentBg = if (summary.sentimentScore >= 0) BullishGreenBg else BearishRedBg
                            val sentimentColor = if (summary.sentimentScore >= 0) BullishGreen else BearishRed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(sentimentBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = summary.sentimentLabel,
                                    color = sentimentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Key Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(
                                label = "Total Turnover",
                                value = "₹${summary.totalTurnoverCrores} Cr",
                                color = AccentGold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MetricBox(
                                label = "Advances",
                                value = "${summary.advanceCount}",
                                color = BullishGreen,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MetricBox(
                                label = "Declines",
                                value = "${summary.declineCount}",
                                color = BearishRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Market Sentiment & Bias Indicator (Advancing vs. Declining Algorithm)
            item {
                MarketSentimentIndicator(
                    advances = summary.advanceCount,
                    declines = summary.declineCount,
                    unchanged = summary.unchangedCount,
                    dateLabel = summary.date
                )
            }
        }

        // NSE Market Status Card providing context for trend analysis
        item {
            NseMarketStatusCard(
                lastFetchedTimestamp = lastLiveUpdateTime,
                onRefreshRequested = onRefreshLiveNow
            )
        }

        // Stock Performance Trend Line Graph Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Historical Performance Graph",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDateRangeActive && startDateRange.isNotEmpty() && endDateRange.isNotEmpty())
                                    "Filter: $startDateRange to $endDateRange"
                                else
                                    "All Historical Dates",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NseMarketStatusBadge(
                            timestamp = lastLiveUpdateTime,
                            showTime = false
                        )

                        if (isDateRangeActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📅 Range Active",
                                    color = PrimaryCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symbol Selector Chips
                if (availableSymbols.isNotEmpty()) {
                    Text(
                        text = "Select Symbol:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSymbols) { sym ->
                            val isSelected = sym == selectedChartSymbol
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                                    .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(16.dp))
                                    .clickable { selectedChartSymbol = sym }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = sym,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                val rangeSubtitle = if (isDateRangeActive && startDateRange.isNotEmpty() && endDateRange.isNotEmpty()) {
                    "Selected Range: $startDateRange to $endDateRange (${selectedSymbolRecords.size} Days)"
                } else {
                    "${selectedSymbolRecords.size} Trading Days History (${selectedSymbolRecords.firstOrNull()?.date ?: ""} - ${selectedSymbolRecords.lastOrNull()?.date ?: ""})"
                }

                StockTrendLineChart(
                    records = selectedSymbolRecords,
                    lineColor = PrimaryCyan,
                    showVolume = true,
                    title = "$selectedChartSymbol Performance",
                    subtitle = rangeSubtitle
                )
            }
        }

        // Search & Filtered Stock List Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Stock Search & Directory",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredAndSortedStockList.size} stocks found",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "Clear Filter",
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter stock symbol (e.g. RELIANCE, INFY, TCS)...", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PrimaryCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NavySurfaceVariant,
                        unfocusedContainerColor = NavySurfaceVariant,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sorting Label & Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Organize By:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySurfaceVariant)
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                            .clickable { isSortDescending = !isSortDescending }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("sort_direction_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSortDescending) "Desc ⬇️" else "Asc ⬆️",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sorting Buttons
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DashboardSortMode.entries.toList()) { mode ->
                        val isSelected = activeSortMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                                .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    if (activeSortMode == mode) {
                                        isSortDescending = !isSortDescending
                                    } else {
                                        activeSortMode = mode
                                        isSortDescending = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("sort_button_${mode.name.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isSortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stock Items List
                val displayList = if (showAllSearchResults) filteredAndSortedStockList else filteredAndSortedStockList.take(10)

                if (filteredAndSortedStockList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No stocks matching '$searchQuery'" else "No stock records found for this filter.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayList.forEach { stock ->
                            StockRowItem(
                                stock = stock,
                                onClick = { onSymbolClick(stock.symbol) }
                            )
                        }
                    }

                    if (filteredAndSortedStockList.size > 10) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavySurfaceVariant)
                                .clickable { showAllSearchResults = !showAllSearchResults }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showAllSearchResults) "Show Fewer Stocks" else "Show All ${filteredAndSortedStockList.size} Stocks →",
                                color = PrimaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top Movers Tabs (Gainers, Losers, Volume Surges)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = gainersTabSelected,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryCyan
                ) {
                    Tab(
                        selected = gainersTabSelected == 0,
                        onClick = { gainersTabSelected = 0 },
                        text = { Text("Top Gainers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = gainersTabSelected == 1,
                        onClick = { gainersTabSelected = 1 },
                        text = { Text("Top Losers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = gainersTabSelected == 2,
                        onClick = { gainersTabSelected = 2 },
                        text = { Text("High Delivery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val currentList = when (gainersTabSelected) {
                    0 -> dailySummary?.topGainers ?: emptyList()
                    1 -> dailySummary?.topLosers ?: emptyList()
                    else -> dailySummary?.highDeliveryStocks ?: emptyList()
                }

                if (currentList.isEmpty()) {
                    Text("No stocks available for this selection", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentList.forEach { stock ->
                            StockRowItem(
                                stock = stock,
                                onClick = { onSymbolClick(stock.symbol) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavySurfaceVariant)
                            .clickable { onNavigateToGainersLosers() }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Leaderboard, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Full Gainers & Losers Page (Date & Company Filters) →",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // New Listed Companies & IPO Buzz Section
        if (newListingsNews.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_new_listings_news_card"),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("🚀 NEW LISTINGS", color = PrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IPO & Debut News",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "All News (${newListingsNews.size}) →",
                                color = PrimaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToNews() }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Cards for Recent Listing News
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(newListingsNews.take(4)) { newsItem ->
                                Box(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NavySurfaceVariant)
                                        .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToNews() }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = newsItem.symbol,
                                                color = PrimaryCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (newsItem.listingGainPct != 0.0) {
                                                Text(
                                                    text = "${if (newsItem.listingGainPct >= 0) "+" else ""}${String.format("%.1f", newsItem.listingGainPct)}%",
                                                    color = if (newsItem.listingGainPct >= 0) BullishGreen else BearishRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = newsItem.headline,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = newsItem.source,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = newsItem.publishDate,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Auto Insights Stream Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Date Trend Insights",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "View All (${insights.size})",
                    color = PrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToInsights() }
                )
            }
        }

        // Recent Insights Items
        val recentInsights = insights.take(4)
        if (recentInsights.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(NavySurface, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trend insights generated for this date.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(recentInsights) { insight ->
                InsightCardItem(
                    insight = insight,
                    onBookmarkToggle = { onBookmarkToggle(insight.id, insight.isBookmarked) },
                    onSymbolClick = { symbol -> symbol?.let { onSymbolClick(it) } }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(NavySurfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StockRowItem(
    stock: NseStockRecord,
    onClick: () -> Unit
) {
    val isUp = stock.priceChangePct >= 0
    val changeColor = if (isUp) BullishGreen else BearishRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NavySurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stock.symbol,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(NavySurface, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(stock.series, color = TextMuted, fontSize = 9.sp)
                }
            }
            Text(
                text = "Vol: ${formatQty(stock.totalTradedQty)} | Deliv: ${String.format("%.1f", stock.pctDlyQtToTrd)}%",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${String.format("%.2f", stock.close)}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = changeColor,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${if (isUp) "+" else ""}${String.format("%.2f", stock.priceChangePct)}%",
                    color = changeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InsightCardItem(
    insight: MarketInsight,
    onBookmarkToggle: () -> Unit,
    onSymbolClick: (String?) -> Unit
) {
    val catColor = when (insight.category) {
        "BULLISH" -> BullishGreen
        "BEARISH" -> BearishRed
        "VOLUME_SURGE" -> AccentGold
        "BREAKOUT" -> PrimaryCyan
        else -> NeutralBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = insight.category,
                            color = catColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    insight.metricValue?.let { metric ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = metric,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(insight.date, color = TextMuted, fontSize = 10.sp)
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (insight.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (insight.isBookmarked) AccentGold else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = insight.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = insight.summary,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            insight.impactSymbol?.let { sym ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Symbol: $sym →",
                    color = PrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSymbolClick(sym) }
                )
            }
        }
    }
}

@Composable
fun LiveIndexCardItem(
    quote: LiveIndexQuote,
    onClick: () -> Unit
) {
    val isUp = quote.changePct >= 0
    val changeColor = if (isUp) BullishGreen else BearishRed
    val changeBg = if (isUp) BullishGreenBg else BearishRedBg

    Card(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("live_index_${quote.name.replace(" ", "_").lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(changeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", quote.changePct)}%",
                        color = changeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format("%,.2f", quote.currentPrice),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", quote.change)}",
                        color = changeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "H:${String.format("%.0f", quote.high)}",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun formatQty(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.2f Cr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.2f L", qty / 1_00_000.0)
        qty >= 1_000 -> String.format("%.1f K", qty / 1000.0)
        else -> qty.toString()
    }
}
