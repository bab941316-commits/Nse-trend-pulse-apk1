package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NseDatabase
import com.example.data.NseRepository
import com.example.data.NseViewModel
import com.example.data.NseViewModelFactory
import com.example.ui.screens.CsvImportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.TrendAnalyticsScreen
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.NseAnalyticsTheme
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.screens.WatchlistsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: NseViewModel by viewModels {
        val database = NseDatabase.getDatabase(applicationContext)
        val repository = NseRepository(database.nseDao())
        NseViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            NseAnalyticsTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel, isDarkTheme = isDarkTheme)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: NseViewModel, isDarkTheme: Boolean) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Observe ViewModel state reactively
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val startDateRange by viewModel.startDateRange.collectAsStateWithLifecycle()
    val endDateRange by viewModel.endDateRange.collectAsStateWithLifecycle()
    val isDateRangeActive by viewModel.isDateRangeActive.collectAsStateWithLifecycle()
    val dateRangeSummary by viewModel.dateRangeSummary.collectAsStateWithLifecycle()

    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()
    val availableSymbols by viewModel.availableSymbols.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val allStockRecords by viewModel.allStockRecords.collectAsStateWithLifecycle()
    val recordsForSelectedDate by viewModel.recordsForSelectedDate.collectAsStateWithLifecycle()
    val recordsForSelectedSymbol by viewModel.recordsForSelectedSymbol.collectAsStateWithLifecycle()
    val filteredInsights by viewModel.filteredInsights.collectAsStateWithLifecycle()
    val selectedInsightCategory by viewModel.selectedInsightCategory.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val compareDate1 by viewModel.compareDate1.collectAsStateWithLifecycle()
    val compareDate2 by viewModel.compareDate2.collectAsStateWithLifecycle()

    val allWatchlists by viewModel.allWatchlists.collectAsStateWithLifecycle()
    val allWatchlistItems by viewModel.allWatchlistItems.collectAsStateWithLifecycle()
    val selectedWatchlistId by viewModel.selectedWatchlistId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NSE Market Analytics",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Date Trends, Watchlists & Insights",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDarkTheme) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Trends") },
                    label = { Text("Trends", fontSize = 10.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_trends")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Watchlists") },
                    label = { Text("Watchlist", fontSize = 10.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_watchlists")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Insights") },
                    label = { Text("Insights", fontSize = 10.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_insights")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "CSV Data") },
                    label = { Text("CSV Import", fontSize = 10.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_csv")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    selectedDate = selectedDate,
                    startDateRange = startDateRange,
                    endDateRange = endDateRange,
                    isDateRangeActive = isDateRangeActive,
                    availableDates = availableDates,
                    dailySummary = dailySummary,
                    dateRangeSummary = dateRangeSummary,
                    allStockRecords = allStockRecords,
                    insights = filteredInsights,
                    watchlists = allWatchlists,
                    watchlistItems = allWatchlistItems,
                    selectedWatchlistId = selectedWatchlistId,
                    onDateSelected = { viewModel.selectDate(it) },
                    onDateRangeSelected = { start, end -> viewModel.setDateRange(start, end) },
                    onClearDateRange = { viewModel.toggleDateRangeActive(false) },
                    onSelectWatchlist = { viewModel.selectWatchlist(it) },
                    onSymbolClick = {
                        viewModel.selectSymbol(it)
                        selectedTab = 1
                    },
                    onBookmarkToggle = { id, cur -> viewModel.toggleBookmark(id, cur) },
                    onNavigateToInsights = { selectedTab = 3 },
                    onNavigateToWatchlists = { selectedTab = 2 }
                )

                1 -> TrendAnalyticsScreen(
                    selectedSymbol = selectedSymbol,
                    availableSymbols = availableSymbols,
                    availableDates = availableDates,
                    recordsForSymbol = recordsForSelectedSymbol,
                    allStockRecords = allStockRecords,
                    compareDate1 = compareDate1,
                    compareDate2 = compareDate2,
                    onSymbolSelected = { viewModel.selectSymbol(it) },
                    onCompareDatesChanged = { d1, d2 -> viewModel.setCompareDates(d1, d2) }
                )

                2 -> WatchlistsScreen(
                    watchlists = allWatchlists,
                    watchlistItems = allWatchlistItems,
                    availableSymbols = availableSymbols,
                    allStockRecords = allStockRecords,
                    selectedWatchlistId = selectedWatchlistId,
                    onSelectWatchlist = { viewModel.selectWatchlist(it) },
                    onCreateWatchlist = { name, desc, syms -> viewModel.createWatchlist(name, desc, syms) },
                    onToggleSymbol = { wlId, sym -> viewModel.toggleSymbolInWatchlist(wlId, sym) },
                    onDeleteWatchlist = { viewModel.deleteWatchlist(it) },
                    onSymbolClick = {
                        viewModel.selectSymbol(it)
                        selectedTab = 1
                    }
                )

                3 -> InsightsScreen(
                    insights = filteredInsights,
                    selectedCategory = selectedInsightCategory,
                    selectedDate = selectedDate,
                    availableSymbols = availableSymbols,
                    onCategorySelected = { viewModel.setInsightCategory(it) },
                    onBookmarkToggle = { id, cur -> viewModel.toggleBookmark(id, cur) },
                    onDeleteInsight = { viewModel.deleteInsight(it) },
                    onAddCustomNote = { title, summary, category, symbol ->
                        viewModel.addCustomInsightNote(title, summary, category, symbol)
                    },
                    onSymbolClick = {
                        viewModel.selectSymbol(it)
                        selectedTab = 1
                    }
                )

                4 -> CsvImportScreen(
                    importStatus = importStatus,
                    allStockRecords = allStockRecords,
                    recordsForSelectedDate = recordsForSelectedDate,
                    selectedDate = selectedDate,
                    onImportCsvFromUri = { uri -> viewModel.importCsvFromUri(context, uri) },
                    onImportCsvText = { text -> viewModel.importCsvText(text) },
                    onResetToSampleData = { viewModel.resetToSampleData() },
                    onResetStatus = { viewModel.resetImportStatus() }
                )
            }
        }
    }
}
