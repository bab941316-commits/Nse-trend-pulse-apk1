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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketInsight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    insights: List<MarketInsight>,
    selectedCategory: String,
    selectedDate: String,
    availableSymbols: List<String>,
    onCategorySelected: (String) -> Unit,
    onBookmarkToggle: (Long, Boolean) -> Unit,
    onDeleteInsight: (Long) -> Unit,
    onAddCustomNote: (title: String, summary: String, category: String, symbol: String?) -> Unit,
    onSymbolClick: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf(
        "ALL" to "All Insights",
        "BOOKMARKED" to "Bookmarked",
        "SELECTED_DATE" to "Date: $selectedDate",
        "BULLISH" to "Bullish",
        "BEARISH" to "Bearish",
        "VOLUME_SURGE" to "Volume Surges",
        "BREAKOUT" to "Breakouts"
    )

    val filteredList = remember(insights, searchQuery) {
        if (searchQuery.isBlank()) insights else insights.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.summary.contains(searchQuery, ignoreCase = true) ||
            (it.impactSymbol?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_custom_insight_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Observation Note")
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

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search insights or symbols...", color = TextMuted) },
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (catKey, catLabel) ->
                        val isSelected = catKey == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) PrimaryCyan else NavySurface)
                                .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(18.dp))
                                .clickable { onCategorySelected(catKey) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("insight_cat_$catKey")
                        ) {
                            Text(
                                text = catLabel,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Insights Feed Items
            if (filteredList.isEmpty()) {
                item {
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
                            Text("No insights found for selected filter", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredList) { insight ->
                    InsightCardItem(
                        insight = insight,
                        onBookmarkToggle = { onBookmarkToggle(insight.id, insight.isBookmarked) },
                        onDelete = { onDeleteInsight(insight.id) },
                        onSymbolClick = { sym -> sym?.let { onSymbolClick(it) } }
                    )
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
                            imageVector = if (insight.isBookmarked) Icons.Default.AutoAwesome else Icons.Default.FilterList,
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
