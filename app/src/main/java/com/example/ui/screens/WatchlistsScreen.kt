package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NseStockRecord
import com.example.data.WatchlistEntity
import com.example.data.WatchlistItemEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WatchlistsScreen(
    watchlists: List<WatchlistEntity>,
    watchlistItems: List<WatchlistItemEntity>,
    availableSymbols: List<String>,
    allStockRecords: List<NseStockRecord>,
    selectedWatchlistId: Long?,
    onSelectWatchlist: (Long?) -> Unit,
    onCreateWatchlist: (name: String, description: String, symbols: List<String>) -> Unit,
    onToggleSymbol: (watchlistId: Long, symbol: String) -> Unit,
    onDeleteWatchlist: (watchlistId: Long) -> Unit,
    onSymbolClick: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddSymbolDialog by remember { mutableStateOf(false) }

    // Active Watchlist
    val activeWatchlist = remember(watchlists, selectedWatchlistId) {
        if (selectedWatchlistId != null) {
            watchlists.find { it.id == selectedWatchlistId } ?: watchlists.firstOrNull()
        } else {
            watchlists.firstOrNull()
        }
    }

    val activeWatchlistId = activeWatchlist?.id

    // Symbols in current watchlist
    val currentWatchlistSymbols = remember(watchlistItems, activeWatchlistId) {
        if (activeWatchlistId == null) emptySet()
        else watchlistItems.filter { it.watchlistId == activeWatchlistId }.map { it.symbol }.toSet()
    }

    // Latest Stock Records per symbol
    val latestRecordsMap = remember(allStockRecords) {
        allStockRecords.groupBy { it.symbol }
            .mapValues { entry -> entry.value.maxByOrNull { it.date } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Watchlist Header & Create Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Custom Stock Watchlists",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track and monitor your target NSE stocks",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_watchlist_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Watchlist Selector Chips
        if (watchlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No watchlists created yet. Click 'Create New' to build one!", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlists) { wl ->
                    val isSelected = wl.id == activeWatchlistId
                    val itemCount = watchlistItems.count { it.watchlistId == wl.id }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                            .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(20.dp))
                            .clickable { onSelectWatchlist(wl.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("watchlist_chip_${wl.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${wl.name} ($itemCount)",
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Watchlist Info & Actions Header
        activeWatchlist?.let { wl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(wl.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (wl.description.isNotEmpty()) {
                            Text(wl.description, color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("${currentWatchlistSymbols.size} Stock Symbols monitored", color = PrimaryCyan, fontSize = 11.sp)
                    }

                    Row {
                        IconButton(
                            onClick = { showAddSymbolDialog = true },
                            modifier = Modifier.testTag("add_symbol_to_watchlist_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Symbol", tint = PrimaryCyan)
                        }
                        IconButton(
                            onClick = { onDeleteWatchlist(wl.id) },
                            modifier = Modifier.testTag("delete_watchlist_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Watchlist", tint = BearishRed)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stock Items List
        if (currentWatchlistSymbols.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(NavySurface, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This watchlist is empty.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click '+' above to add stock symbols to '${activeWatchlist?.name ?: "Watchlist"}'.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentWatchlistSymbols.toList()) { symbol ->
                    val record = latestRecordsMap[symbol]
                    WatchlistStockRow(
                        symbol = symbol,
                        record = record,
                        onSymbolClick = { onSymbolClick(symbol) },
                        onRemoveClick = {
                            activeWatchlistId?.let { wlId -> onToggleSymbol(wlId, symbol) }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialog: Create Watchlist
    if (showCreateDialog) {
        CreateWatchlistDialog(
            availableSymbols = availableSymbols,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, symbols ->
                onCreateWatchlist(name, desc, symbols)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Add Symbols to Active Watchlist
    if (showAddSymbolDialog && activeWatchlistId != null) {
        AddSymbolsDialog(
            availableSymbols = availableSymbols,
            existingSymbols = currentWatchlistSymbols,
            onDismiss = { showAddSymbolDialog = false },
            onAddSymbol = { sym ->
                onToggleSymbol(activeWatchlistId, sym)
            }
        )
    }
}

@Composable
private fun WatchlistStockRow(
    symbol: String,
    record: NseStockRecord?,
    onSymbolClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val isUp = (record?.priceChangePct ?: 0.0) >= 0
    val changeColor = if (isUp) BullishGreen else BearishRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSymbolClick() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = symbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    record?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(NavySurfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(it.series, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                if (record != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Date: ${record.date} | Vol: ${formatQtyShort(record.totalTradedQty)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                } else {
                    Text("No recent market data available", color = TextMuted, fontSize = 11.sp)
                }
            }

            if (record != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", record.close)}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", record.priceChangePct)}%",
                        color = changeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(28.dp).testTag("remove_from_watchlist_$symbol")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkRemove,
                    contentDescription = "Remove",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateWatchlistDialog(
    availableSymbols: List<String>,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, symbols: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedSymbols = remember { mutableStateListOf<String>() }

    val filteredSymbols = remember(availableSymbols, searchQuery) {
        if (searchQuery.isBlank()) availableSymbols
        else availableSymbols.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Text("Create Custom Watchlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Watchlist Name") },
                    placeholder = { Text("e.g., Banking Stocks") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedLabelColor = PrimaryCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("watchlist_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g., High yield dividend stocks") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedLabelColor = PrimaryCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Initial Stock Symbols (${selectedSymbols.size}):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search symbol...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(160.dp)) {
                    items(filteredSymbols) { sym ->
                        val isChecked = selectedSymbols.contains(sym)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedSymbols.remove(sym) else selectedSymbols.add(sym)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedSymbols.add(sym) else selectedSymbols.remove(sym)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan, checkmarkColor = Color.Black)
                            )
                            Text(sym, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description, selectedSymbols.toList()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_create_watchlist")
            ) {
                Text("Create Watchlist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun AddSymbolsDialog(
    availableSymbols: List<String>,
    existingSymbols: Set<String>,
    onDismiss: () -> Unit,
    onAddSymbol: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(availableSymbols, searchQuery) {
        if (searchQuery.isBlank()) availableSymbols
        else availableSymbols.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Text("Add Symbol to Watchlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search stock symbol...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = NavyCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(filtered) { sym ->
                        val isAdded = existingSymbols.contains(sym)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddSymbol(sym)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sym, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAdded) BearishRed.copy(alpha = 0.2f) else PrimaryCyan)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAdded) "Remove" else "+ Add",
                                    color = if (isAdded) BearishRed else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant, contentColor = TextPrimary)
            ) {
                Text("Done")
            }
        }
    )
}

private fun formatQtyShort(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.2f Cr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.2f L", qty / 1_00_00_000.0)
        qty >= 1_000 -> String.format("%.1f K", qty / 1000.0)
        else -> qty.toString()
    }
}
