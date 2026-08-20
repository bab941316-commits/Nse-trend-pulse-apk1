package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyMarketSummary
import com.example.data.NseStockRecord
import com.example.ui.components.NseMarketStatusBadge
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.NeutralBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat

enum class MoversTab(val label: String, val badgeColor: Color) {
    GAINERS("Top Gainers", BullishGreen),
    LOSERS("Top Losers", BearishRed),
    ALL_MOVERS("All Active", PrimaryCyan),
    VOLUME_SURGE("Volume Shockers", AccentGold),
    HIGH_DELIVERY("High Delivery", NeutralBlue)
}

enum class MoversSortCriteria(val label: String) {
    PCT_CHANGE("% Change"),
    PRICE_CHANGE("₹ Change"),
    VOLUME("Volume"),
    TURNOVER("Turnover"),
    LTP("Price (LTP)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GainersLosersScreen(
    selectedDate: String,
    availableSymbols: List<String>,
    allStockRecords: List<NseStockRecord>,
    dailySummary: DailyMarketSummary?,
    availableDates: List<String> = emptyList(),
    onDateSelected: (String) -> Unit = {},
    onSymbolSelected: (String) -> Unit,
    onNavigateToTrends: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMoversTab by remember { mutableStateOf(MoversTab.GAINERS) }
    var selectedCompanySymbol by remember { mutableStateOf<String?>(null) }
    var companySearchQuery by remember { mutableStateOf("") }
    var sortCriteria by remember { mutableStateOf(MoversSortCriteria.PCT_CHANGE) }
    var isDescending by remember { mutableStateOf(true) }
    var isCompanyDropdownExpanded by remember { mutableStateOf(false) }

    // Filter records for currently selected date
    val dateRecords = remember(selectedDate, allStockRecords) {
        allStockRecords.filter { it.date == selectedDate && it.series == "EQ" }
    }

    // Top Gainers: positive priceChangePct sorted desc
    val gainersList = remember(dateRecords) {
        dateRecords.filter { it.priceChangePct > 0 }.sortedByDescending { it.priceChangePct }
    }

    // Top Losers: negative priceChangePct sorted asc (biggest drop first)
    val losersList = remember(dateRecords) {
        dateRecords.filter { it.priceChangePct < 0 }.sortedBy { it.priceChangePct }
    }

    // High Delivery
    val highDeliveryList = remember(dateRecords) {
        dateRecords.filter { it.pctDlyQtToTrd > 0 }.sortedByDescending { it.pctDlyQtToTrd }
    }

    // Volume Shockers
    val volumeSurgeList = remember(dateRecords) {
        dateRecords.sortedByDescending { it.totalTradedQty }
    }

    // Base list according to selected tab
    val currentTabList = remember(selectedMoversTab, gainersList, losersList, dateRecords, volumeSurgeList, highDeliveryList) {
        when (selectedMoversTab) {
            MoversTab.GAINERS -> gainersList
            MoversTab.LOSERS -> losersList
            MoversTab.ALL_MOVERS -> dateRecords.sortedByDescending { kotlin.math.abs(it.priceChangePct) }
            MoversTab.VOLUME_SURGE -> volumeSurgeList
            MoversTab.HIGH_DELIVERY -> highDeliveryList
        }
    }

    // Apply company search & company filter
    val filteredList = remember(currentTabList, companySearchQuery, selectedCompanySymbol, sortCriteria, isDescending) {
        var list = currentTabList

        // Filter by selected company if specified
        if (!selectedCompanySymbol.isNullOrBlank()) {
            list = list.filter { it.symbol.equals(selectedCompanySymbol, ignoreCase = true) }
        }

        // Filter by search text
        if (companySearchQuery.isNotBlank()) {
            list = list.filter {
                it.symbol.contains(companySearchQuery, ignoreCase = true) ||
                        it.series.contains(companySearchQuery, ignoreCase = true)
            }
        }

        // Sort based on criteria
        when (sortCriteria) {
            MoversSortCriteria.PCT_CHANGE -> {
                if (isDescending) list.sortedByDescending { it.priceChangePct }
                else list.sortedBy { it.priceChangePct }
            }
            MoversSortCriteria.PRICE_CHANGE -> {
                if (isDescending) list.sortedByDescending { it.priceChange }
                else list.sortedBy { it.priceChange }
            }
            MoversSortCriteria.VOLUME -> {
                if (isDescending) list.sortedByDescending { it.totalTradedQty }
                else list.sortedBy { it.totalTradedQty }
            }
            MoversSortCriteria.TURNOVER -> {
                if (isDescending) list.sortedByDescending { it.totalTradedVal }
                else list.sortedBy { it.totalTradedVal }
            }
            MoversSortCriteria.LTP -> {
                if (isDescending) list.sortedByDescending { it.close }
                else list.sortedBy { it.close }
            }
        }
    }

    // Selected company detail record (if any)
    val selectedCompanyRecord = remember(selectedCompanySymbol, dateRecords) {
        if (selectedCompanySymbol != null) {
            dateRecords.find { it.symbol.equals(selectedCompanySymbol, ignoreCase = true) }
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("gainers_losers_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(listOf(BullishGreen.copy(alpha = 0.2f), BearishRed.copy(alpha = 0.2f))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Top Gainers & Losers",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Daily Momentum, Breakouts & Drawdowns",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                NseMarketStatusBadge(showTime = false)
            }
        }

        // Section: Company Selection Option
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("company_selection_card"),
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
                            Icon(
                                imageVector = Icons.Default.CorporateFare,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Company Selection & Filter",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedCompanySymbol != null) {
                            Text(
                                text = "Clear Company",
                                color = PrimaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    selectedCompanySymbol = null
                                    companySearchQuery = ""
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search and Dropdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = companySearchQuery,
                            onValueChange = { companySearchQuery = it },
                            placeholder = { Text("Search company symbol...", color = TextMuted, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (companySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { companySearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        // Company Dropdown Select Button
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedCompanySymbol != null) AccentGold else NavySurfaceVariant)
                                    .border(1.dp, if (selectedCompanySymbol != null) AccentGold else NavyCardBorder, RoundedCornerShape(10.dp))
                                    .clickable { isCompanyDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 13.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCompanySymbol ?: "Select",
                                        color = if (selectedCompanySymbol != null) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (selectedCompanySymbol != null) Color.Black else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isCompanyDropdownExpanded,
                                onDismissRequest = { isCompanyDropdownExpanded = false },
                                modifier = Modifier.background(NavySurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Companies (Reset)", color = PrimaryCyan, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCompanySymbol = null
                                        isCompanyDropdownExpanded = false
                                    }
                                )
                                availableSymbols.forEach { sym ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sym,
                                                color = if (sym == selectedCompanySymbol) PrimaryCyan else TextPrimary,
                                                fontWeight = if (sym == selectedCompanySymbol) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedCompanySymbol = sym
                                            isCompanyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Popular Company Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isAll = selectedCompanySymbol == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isAll) PrimaryCyan else NavySurfaceVariant)
                                    .border(1.dp, if (isAll) PrimaryCyan else NavyCardBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedCompanySymbol = null }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("All Companies", color = if (isAll) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        items(availableSymbols.take(8)) { sym ->
                            val isSelected = sym == selectedCompanySymbol
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) AccentGold else NavySurfaceVariant)
                                    .border(1.dp, if (isSelected) AccentGold else NavyCardBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedCompanySymbol = sym }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(sym, color = if (isSelected) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Highlight Selected Company (if selected)
        if (selectedCompanyRecord != null) {
            item {
                SelectedCompanyHighlightCard(
                    record = selectedCompanyRecord,
                    gainersList = gainersList,
                    losersList = losersList,
                    onViewTrendChart = { onNavigateToTrends(selectedCompanyRecord.symbol) }
                )
            }
        }

        // Section 4: Movers Category Tabs & Sorting Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // Category Tabs
                TabRow(
                    selectedTabIndex = selectedMoversTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryCyan
                ) {
                    MoversTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedMoversTab == tab,
                            onClick = { selectedMoversTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedMoversTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sorting Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sort By:", color = TextMuted, fontSize = 11.sp)
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MoversSortCriteria.entries.toList()) { criteria ->
                            val isSelected = sortCriteria == criteria
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                                    .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (sortCriteria == criteria) {
                                            isDescending = !isDescending
                                        } else {
                                            sortCriteria = criteria
                                            isDescending = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = criteria.label,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Movers Stock List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (companySearchQuery.isNotBlank() || selectedCompanySymbol != null)
                                "No stocks match the company filter on $selectedDate."
                            else
                                "No stock records found for this category on $selectedDate.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        if (selectedCompanySymbol != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    selectedCompanySymbol = null
                                    companySearchQuery = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                            ) {
                                Text("View All Companies", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(filteredList) { index, stock ->
                val rank = index + 1
                MoversStockRowCard(
                    stock = stock,
                    rank = rank,
                    tab = selectedMoversTab,
                    onSelectCompany = {
                        selectedCompanySymbol = stock.symbol
                        onSymbolSelected(stock.symbol)
                    },
                    onViewTrendChart = { onNavigateToTrends(stock.symbol) }
                )
            }
        }
    }
}

/**
 * Dedicated Card Highlighting a Selected Company's specific Gain / Loss performance on the chosen date.
 */
@Composable
private fun SelectedCompanyHighlightCard(
    record: NseStockRecord,
    gainersList: List<NseStockRecord>,
    losersList: List<NseStockRecord>,
    onViewTrendChart: () -> Unit
) {
    val isUp = record.priceChange >= 0
    val changeColor = if (isUp) BullishGreen else BearishRed
    val changeBg = if (isUp) BullishGreenBg else BearishRedBg

    // Determine rank in Gainers or Losers list
    val gainerRank = gainersList.indexOfFirst { it.symbol == record.symbol }.let { if (it >= 0) it + 1 else null }
    val loserRank = losersList.indexOfFirst { it.symbol == record.symbol }.let { if (it >= 0) it + 1 else null }

    val statusBadgeText = when {
        gainerRank != null -> "🟢 Rank #$gainerRank in Top Gainers"
        loserRank != null -> "🔴 Rank #$loserRank in Top Losers"
        else -> "⚪ Balanced / Unchanged Equity"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_company_highlight_card"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(changeColor.copy(alpha = 0.5f))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Symbol, Series & Rank Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(changeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.symbol.take(2),
                            color = changeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = record.symbol,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NavySurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(record.series, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Date: ${record.date}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Status Rank Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(changeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        color = changeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Price & Change Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Close / LTP", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "₹${String.format("%,.2f", record.close)}",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Day Change", color = TextMuted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isUp) "+" else ""}₹${String.format("%.2f", record.priceChange)} (${if (isUp) "+" else ""}${String.format("%.2f", record.priceChangePct)}%)",
                            color = changeColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key OHLC & Volume Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(label = "Open", value = "₹${String.format("%.2f", record.open)}", modifier = Modifier.weight(1f))
                MetricBox(label = "Day High", value = "₹${String.format("%.2f", record.high)}", modifier = Modifier.weight(1f))
                MetricBox(label = "Day Low", value = "₹${String.format("%.2f", record.low)}", modifier = Modifier.weight(1f))
                MetricBox(label = "Prev Close", value = "₹${String.format("%.2f", record.prevClose)}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox(label = "Volume", value = formatShortVol(record.totalTradedQty), modifier = Modifier.weight(1f))
                MetricBox(label = "Turnover", value = formatShortTurnover(record.totalTradedVal), modifier = Modifier.weight(1f))
                MetricBox(label = "Delivery %", value = "${String.format("%.1f", record.pctDlyQtToTrd)}%", modifier = Modifier.weight(1f))
                MetricBox(label = "Day Range", value = "${String.format("%.1f", record.dayRangePct)}%", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trend Chart Navigation Action Button
            Button(
                onClick = onViewTrendChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View ${record.symbol} Trend Chart & History →",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Individual Stock Row in Gainers / Losers List
 */
@Composable
private fun MoversStockRowCard(
    stock: NseStockRecord,
    rank: Int,
    tab: MoversTab,
    onSelectCompany: () -> Unit,
    onViewTrendChart: () -> Unit
) {
    val isUp = stock.priceChange >= 0
    val color = if (isUp) BullishGreen else BearishRed
    val bg = if (isUp) BullishGreenBg else BearishRedBg

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCompany() }
            .testTag("movers_row_${stock.symbol}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge & Stock Symbol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                // Rank Pill
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            when (rank) {
                                1 -> AccentGold.copy(alpha = 0.25f)
                                2 -> Color(0xFFC0C0C0).copy(alpha = 0.25f)
                                3 -> Color(0xFFCD7F32).copy(alpha = 0.25f)
                                else -> NavySurfaceVariant
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = when (rank) {
                            1 -> AccentGold
                            2 -> Color(0xFFE0E0E0)
                            3 -> Color(0xFFE59866)
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(stock.series, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Vol: ${formatShortVol(stock.totalTradedQty)} • Dly: ${String.format("%.0f", stock.pctDlyQtToTrd)}%",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // High/Low Range Indicator Bar
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val span = (stock.high - stock.low).coerceAtLeast(0.01)
                val currentPos = ((stock.close - stock.low) / span).coerceIn(0.0, 1.0).toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("L: ₹${stock.low.toInt()}", color = TextMuted, fontSize = 9.sp)
                    Text("H: ₹${stock.high.toInt()}", color = TextMuted, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NavySurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(currentPos)
                            .height(4.dp)
                            .background(color)
                    )
                }
            }

            // Price & % Change Badge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.9f)
            ) {
                Text(
                    text = "₹${String.format("%,.2f", stock.close)}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", stock.priceChangePct)}%",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurfaceVariant)
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(1.dp))
            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun formatShortVol(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.1fCr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.1fL", qty / 1_00_000.0)
        qty >= 1_000 -> String.format("%.1fK", qty / 1_000.0)
        else -> qty.toString()
    }
}

private fun formatShortTurnover(valRupees: Double): String {
    return when {
        valRupees >= 1_00_00_000 -> "₹${String.format("%.1f", valRupees / 1_00_00_000.0)}Cr"
        valRupees >= 1_00_000 -> "₹${String.format("%.1f", valRupees / 1_00_000.0)}L"
        else -> "₹${String.format("%.0f", valRupees)}"
    }
}
