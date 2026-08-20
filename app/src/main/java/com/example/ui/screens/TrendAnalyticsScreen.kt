package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.StockTrendLineChart
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendAnalyticsScreen(
    selectedSymbol: String,
    availableSymbols: List<String>,
    availableDates: List<String>,
    recordsForSymbol: List<NseStockRecord>,
    allStockRecords: List<NseStockRecord>,
    compareDate1: String,
    compareDate2: String,
    onSymbolSelected: (String) -> Unit,
    onCompareDatesChanged: (String, String) -> Unit
) {
    var symbolDropdownExpanded by remember { mutableStateOf(false) }
    var symbolSearchText by remember { mutableStateOf("") }

    val filteredSymbols = remember(availableSymbols, symbolSearchText) {
        if (symbolSearchText.isBlank()) availableSymbols else availableSymbols.filter {
            it.contains(symbolSearchText, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Symbol Selector Dropdown & Search Bar
        item {
            Column {
                Text(
                    text = "Select Stock or Index for Trend Analysis",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = symbolDropdownExpanded,
                    onExpandedChange = { symbolDropdownExpanded = !symbolDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSymbol,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = symbolDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface,
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("symbol_selector_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = symbolDropdownExpanded,
                        onDismissRequest = { symbolDropdownExpanded = false },
                        modifier = Modifier.background(NavySurface)
                    ) {
                        OutlinedTextField(
                            value = symbolSearchText,
                            onValueChange = { symbolSearchText = it },
                            placeholder = { Text("Search Symbol...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NavySurfaceVariant,
                                unfocusedContainerColor = NavySurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )

                        filteredSymbols.forEach { sym ->
                            DropdownMenuItem(
                                text = { Text(sym, color = TextPrimary, fontWeight = if (sym == selectedSymbol) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onSymbolSelected(sym)
                                    symbolDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Symbol Chips Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("NIFTY 50", "NIFTY BANK", "RELIANCE", "TCS", "HDFCBANK", "INFY", "SBIN")) { sym ->
                    val isSelected = sym == selectedSymbol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else NavySurfaceVariant)
                            .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(16.dp))
                            .clickable { onSymbolSelected(sym) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sym,
                            color = if (isSelected) PrimaryCyan else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Interactive Price & Volume Chart
        item {
            StockTrendLineChart(
                records = recordsForSymbol,
                lineColor = PrimaryCyan,
                showVolume = true
            )
        }

        // Key Stats Summary Card for Selected Symbol
        val latestRecord = recordsForSymbol.lastOrNull()
        latestRecord?.let { rec ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Latest Market Metrics (${rec.date})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill(label = "Open", value = "₹${rec.open}")
                            StatPill(label = "Day High", value = "₹${rec.high}")
                            StatPill(label = "Day Low", value = "₹${rec.low}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill(label = "Close Price", value = "₹${rec.close}")
                            StatPill(label = "Prev Close", value = "₹${rec.prevClose}")
                            StatPill(label = "Delivery %", value = "${String.format("%.1f", rec.pctDlyQtToTrd)}%")
                        }
                    }
                }
            }
        }

        // Date-over-Date Comparison Tool
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
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Date-over-Date Performance Comparison", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Selectors
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Base Date T1", color = TextMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            DateChipSelector(
                                currentDate = compareDate1,
                                availableDates = availableDates,
                                onSelect = { onCompareDatesChanged(it, compareDate2) }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Comparison Date T2", color = TextMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            DateChipSelector(
                                currentDate = compareDate2,
                                availableDates = availableDates,
                                onSelect = { onCompareDatesChanged(compareDate1, it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Perform comparison for current symbol
                    val recordT1 = allStockRecords.find { it.symbol == selectedSymbol && it.date == compareDate1 }
                    val recordT2 = allStockRecords.find { it.symbol == selectedSymbol && it.date == compareDate2 }

                    if (recordT1 != null && recordT2 != null) {
                        val priceDiff = recordT2.close - recordT1.close
                        val priceDiffPct = if (recordT1.close > 0) (priceDiff / recordT1.close) * 100.0 else 0.0
                        val isUp = priceDiff >= 0
                        val diffColor = if (isUp) BullishGreen else BearishRed

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavySurfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "$selectedSymbol Performance ($compareDate1 → $compareDate2)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Price on $compareDate1: ₹${recordT1.close}", color = TextPrimary, fontSize = 12.sp)
                                        Text("Price on $compareDate2: ₹${recordT2.close}", color = TextPrimary, fontSize = 12.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isUp) "+" else ""}₹${String.format("%.2f", priceDiff)}",
                                            color = diffColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(${if (isUp) "+" else ""}${String.format("%.2f", priceDiffPct)}%)",
                                            color = diffColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Select valid dates to compare performance", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // Historical Dates Table for Selected Stock
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Historical Date Records for $selectedSymbol",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Close (₹)", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Change %", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Volume", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                recordsForSymbol.reversed().forEach { rec ->
                    val isUp = rec.priceChangePct >= 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(rec.date, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("₹${String.format("%.1f", rec.close)}", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = "${if (isUp) "+" else ""}${String.format("%.2f", rec.priceChangePct)}%",
                            color = if (isUp) BullishGreen else BearishRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatShortVol(rec.totalTradedQty), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column {
        Text(label, color = TextMuted, fontSize = 10.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DateChipSelector(
    currentDate: String,
    availableDates: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurfaceVariant)
            .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(currentDate.ifEmpty { "Select Date" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NavySurface)
        ) {
            availableDates.forEach { d ->
                DropdownMenuItem(
                    text = { Text(d, color = TextPrimary) },
                    onClick = {
                        onSelect(d)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatShortVol(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.1fCr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.1fL", qty / 1_00_000.0)
        else -> "${qty / 1000}K"
    }
}
