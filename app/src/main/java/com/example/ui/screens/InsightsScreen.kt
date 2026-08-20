package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.IpoDebutTrackRecord
import com.example.data.MarketInsight
import com.example.data.NewListedCompanyNewsItem
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.NeutralBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class NewsInsightTab(val title: String) {
    NEW_LISTINGS_NEWS("New Listed News 🚀"),
    IPO_DEBUT_TRACKER("IPO Tracker 📈"),
    MARKET_INSIGHTS("Market Intelligence 💡")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    insights: List<MarketInsight>,
    selectedCategory: String,
    selectedDate: String,
    availableSymbols: List<String>,
    newListingsNews: List<NewListedCompanyNewsItem> = emptyList(),
    ipoDebutTrackers: List<IpoDebutTrackRecord> = emptyList(),
    isFetchingNews: Boolean = false,
    onRefreshNews: () -> Unit = {},
    onCategorySelected: (String) -> Unit,
    onBookmarkToggle: (Long, Boolean) -> Unit,
    onDeleteInsight: (Long) -> Unit,
    onAddCustomNote: (title: String, summary: String, category: String, symbol: String?) -> Unit,
    onSymbolClick: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(NewsInsightTab.NEW_LISTINGS_NEWS) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedNewsFilter by remember { mutableStateOf("ALL") }

    val categories = listOf(
        "ALL" to "All Insights",
        "BOOKMARKED" to "Bookmarked",
        "SELECTED_DATE" to "Date: $selectedDate",
        "BULLISH" to "Bullish",
        "BEARISH" to "Bearish",
        "VOLUME_SURGE" to "Volume Surges",
        "BREAKOUT" to "Breakouts"
    )

    val newsFilters = listOf(
        "ALL" to "All New Listings",
        "IPO DEBUT" to "Debut Listings",
        "LISTING GAINS" to "High Listing Gains",
        "EARNINGS" to "Debut Earnings",
        "ANCHOR EXPIRY" to "Anchor Lock-in"
    )

    val filteredInsightsList = remember(insights, searchQuery) {
        if (searchQuery.isBlank()) insights else insights.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.summary.contains(searchQuery, ignoreCase = true) ||
                    (it.impactSymbol?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val filteredNewsList = remember(newListingsNews, searchQuery, selectedNewsFilter) {
        var list = newListingsNews
        if (selectedNewsFilter != "ALL") {
            list = list.filter { it.newsType.equals(selectedNewsFilter, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter {
                it.companyName.contains(q, ignoreCase = true) ||
                        it.symbol.contains(q, ignoreCase = true) ||
                        it.headline.contains(q, ignoreCase = true) ||
                        it.summary.contains(q, ignoreCase = true)
            }
        }
        list
    }

    val filteredIpoTrackers = remember(ipoDebutTrackers, searchQuery) {
        if (searchQuery.isBlank()) ipoDebutTrackers else ipoDebutTrackers.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
                    it.symbol.contains(searchQuery, ignoreCase = true) ||
                    it.sector.contains(searchQuery, ignoreCase = true)
        }
    }

    // Refresh Spin Animation
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (activeTab == NewsInsightTab.MARKET_INSIGHTS) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryCyan,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_custom_insight_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Observation Note")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Tab Selector: New Listed News / IPO Tracker / Market Intelligence
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = activeTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = PrimaryCyan,
                        divider = {}
                    ) {
                        NewsInsightTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    activeTab = tab
                                    searchQuery = ""
                                },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryCyan else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Search Bar & Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                when (activeTab) {
                                    NewsInsightTab.NEW_LISTINGS_NEWS -> "Search new company news, IPOs..."
                                    NewsInsightTab.IPO_DEBUT_TRACKER -> "Search newly listed companies..."
                                    NewsInsightTab.MARKET_INSIGHTS -> "Search insights or symbols..."
                                },
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface,
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    if (activeTab == NewsInsightTab.NEW_LISTINGS_NEWS) {
                        IconButton(
                            onClick = onRefreshNews,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavySurface)
                                .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                                .size(48.dp)
                                .testTag("refresh_ipo_news_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh IPO News",
                                tint = PrimaryCyan,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(if (isFetchingNews) spinAngle else 0f)
                            )
                        }
                    }
                }
            }

            // Sub-filter Chips
            item {
                when (activeTab) {
                    NewsInsightTab.NEW_LISTINGS_NEWS -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(newsFilters) { (filterKey, filterLabel) ->
                                val isSelected = filterKey == selectedNewsFilter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) PrimaryCyan else NavySurface)
                                        .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(18.dp))
                                        .clickable { selectedNewsFilter = filterKey }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("news_filter_$filterKey")
                                ) {
                                    Text(
                                        text = filterLabel,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    NewsInsightTab.MARKET_INSIGHTS -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { (catKey, catLabel) ->
                                val isSelected = catKey == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) PrimaryCyan else NavySurface)
                                        .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(18.dp))
                                        .clickable { onCategorySelected(catKey) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("insight_cat_$catKey")
                                ) {
                                    Text(
                                        text = catLabel,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    NewsInsightTab.IPO_DEBUT_TRACKER -> {
                        // Header Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent IPO Debuts & Track Record", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${filteredIpoTrackers.size} Listed Companies", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // CONTENT SECTION
            when (activeTab) {
                NewsInsightTab.NEW_LISTINGS_NEWS -> {
                    if (filteredNewsList.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No new listing news found",
                                subtitle = "Try changing search or refresh live news feed."
                            )
                        }
                    } else {
                        items(filteredNewsList, key = { it.id }) { newsItem ->
                            NewListedCompanyNewsCard(
                                newsItem = newsItem,
                                onSymbolClick = { onSymbolClick(newsItem.symbol) }
                            )
                        }
                    }
                }

                NewsInsightTab.IPO_DEBUT_TRACKER -> {
                    if (filteredIpoTrackers.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No IPO trackers match search",
                                subtitle = "Search by symbol or company name."
                            )
                        }
                    } else {
                        items(filteredIpoTrackers, key = { it.symbol }) { ipo ->
                            IpoDebutTrackRecordCard(
                                ipo = ipo,
                                onSymbolClick = { onSymbolClick(ipo.symbol) }
                            )
                        }
                    }
                }

                NewsInsightTab.MARKET_INSIGHTS -> {
                    if (filteredInsightsList.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No insights found for selected filter",
                                subtitle = "Add a custom observation note or import trading data."
                            )
                        }
                    } else {
                        items(filteredInsightsList, key = { it.id }) { insight ->
                            InsightCardItem(
                                insight = insight,
                                onBookmarkToggle = { onBookmarkToggle(insight.id, insight.isBookmarked) },
                                onDelete = { onDeleteInsight(insight.id) },
                                onSymbolClick = { sym -> sym?.let { onSymbolClick(it) } }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Dialog for Adding Custom Observation / Note
    if (showAddDialog) {
        AddObservationDialog(
            availableSymbols = availableSymbols,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, summary, category, symbol ->
                onAddCustomNote(title, summary, category, symbol)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NewListedCompanyNewsCard(
    newsItem: NewListedCompanyNewsItem,
    onSymbolClick: () -> Unit
) {
    val sentimentColor = when (newsItem.sentiment) {
        "BULLISH" -> BullishGreen
        "BEARISH" -> BearishRed
        else -> PrimaryCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_card_${newsItem.symbol}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Company Symbol Badge + News Type + Sentiment Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryCyan.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = newsItem.symbol,
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavySurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = newsItem.newsType,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (newsItem.isHot) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🔥 HOT", color = AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sentiment pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(sentimentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(sentimentColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = newsItem.sentiment,
                        color = sentimentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Company Full Name
            Text(
                text = newsItem.companyName,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Main Headline
            Text(
                text = newsItem.headline,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Detailed Summary
            Text(
                text = newsItem.summary,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Pricing & Listing Return Metrics (if available)
            if (newsItem.issuePrice > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavySurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Issue Price", color = TextMuted, fontSize = 9.sp)
                        Text("₹${String.format("%.1f", newsItem.issuePrice)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Listing Price", color = TextMuted, fontSize = 9.sp)
                        Text("₹${String.format("%.1f", newsItem.listingPrice)}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (newsItem.currentPrice > 0) {
                        Column {
                            Text("Current LTP", color = TextMuted, fontSize = 9.sp)
                            Text("₹${String.format("%.1f", newsItem.currentPrice)}", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Listing Gain", color = TextMuted, fontSize = 9.sp)
                        Text(
                            text = "${if (newsItem.listingGainPct >= 0) "+" else ""}${String.format("%.1f", newsItem.listingGainPct)}%",
                            color = if (newsItem.listingGainPct >= 0) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tags Row
            if (newsItem.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(newsItem.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NavySurfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("#$tag", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Source, Date and Direct Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${newsItem.source} • ${newsItem.publishDate}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSymbolClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View Chart",
                        color = PrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "View Chart",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IpoDebutTrackRecordCard(
    ipo: IpoDebutTrackRecord,
    onSymbolClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ipo_tracker_${ipo.symbol}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ipo.symbol,
                            color = PrimaryCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(ipo.sector, color = TextMuted, fontSize = 9.sp)
                        }
                    }
                    Text(
                        text = ipo.companyName,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", ipo.currentPrice)}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Listing: ${ipo.listingDate}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavySurfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Issue Px", color = TextMuted, fontSize = 9.sp)
                    Text("₹${ipo.issuePrice.toInt()}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Listing Px", color = TextMuted, fontSize = 9.sp)
                    Text("₹${ipo.listingPrice.toInt()}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Debut Gain", color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = "+${String.format("%.1f", ipo.listingGainPct)}%",
                        color = BullishGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Total Return", color = TextMuted, fontSize = 9.sp)
                    Text(
                        text = "${if (ipo.totalReturnFromIssuePct >= 0) "+" else ""}${String.format("%.1f", ipo.totalReturnFromIssuePct)}%",
                        color = if (ipo.totalReturnFromIssuePct >= 0) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (ipo.subscriptionTimes > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Subscribed", color = TextMuted, fontSize = 9.sp)
                        Text("${ipo.subscriptionTimes}x", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSymbolClick,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Historical Trend Chart →", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .background(NavySurface, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextMuted, modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InsightCardItem(
    insight: MarketInsight,
    onBookmarkToggle: () -> Unit,
    onDelete: () -> Unit,
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
        shape = RoundedCornerShape(14.dp)
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
                        Text(insight.category, color = catColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    insight.metricValue?.let { valStr ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(valStr, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(insight.date, color = TextMuted, fontSize = 10.sp)
                    IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (insight.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (insight.isBookmarked) AccentGold else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(insight.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(insight.summary, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)

            insight.impactSymbol?.let { sym ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Symbol: $sym →",
                    color = PrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSymbolClick(sym) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddObservationDialog(
    availableSymbols: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, summary: String, category: String, symbol: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("BULLISH") }
    var selectedSymbol by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text("Add Market Observation / Note", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Observation Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary Details / Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips Selection
                Text("Category", color = TextMuted, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("BULLISH", "BEARISH", "VOLUME_SURGE", "BREAKOUT")) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) PrimaryCyan else NavySurfaceVariant)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(cat, color = if (isSel) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, summary, selectedCategory, selectedSymbol.ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black)
            ) {
                Text("Save Insight", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
