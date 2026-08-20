package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.DateRangeMarketSummary
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
fun DateRangePickerComponent(
    selectedDate: String,
    startDateRange: String,
    endDateRange: String,
    isDateRangeActive: Boolean,
    availableDates: List<String>,
    dateRangeSummary: DateRangeMarketSummary?,
    onDateSelected: (String) -> Unit,
    onDateRangeSelected: (start: String, end: String) -> Unit,
    onClearDateRange: () -> Unit
) {
    val sortedDates = remember(availableDates) { availableDates.sorted() }
    var expandedMode by remember { mutableStateOf(false) }
    var startDropdownOpen by remember { mutableStateOf(false) }
    var endDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavySurface, RoundedCornerShape(16.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Mode Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDateRangeActive) PrimaryCyan else NavySurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDateRangeActive) Icons.Default.DateRange else Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDateRangeActive) Color.Black else PrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isDateRangeActive) "Analyzed Market Range" else "Trading Date Selection",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isDateRangeActive) "$startDateRange to $endDateRange" else "Filter market data by single date or range",
                        color = if (isDateRangeActive) PrimaryCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isDateRangeActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Toggle Filter Mode Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDateRangeActive) PrimaryCyan.copy(alpha = 0.2f) else NavySurfaceVariant)
                        .border(1.dp, if (isDateRangeActive) PrimaryCyan else NavyCardBorder, RoundedCornerShape(12.dp))
                        .clickable { expandedMode = !expandedMode }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("date_range_picker_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Mode",
                            tint = if (isDateRangeActive) PrimaryCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDateRangeActive) "Range Active" else "Range Mode",
                            color = if (isDateRangeActive) PrimaryCyan else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isDateRangeActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onClearDateRange,
                        modifier = Modifier.size(28.dp).testTag("clear_date_range_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Range",
                            tint = BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Single Date Row (When not in custom expanded range builder mode)
        if (!expandedMode && !isDateRangeActive) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedDates.reversed()) { date ->
                    val isSelected = date == selectedDate
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryCyan else NavySurfaceVariant)
                            .border(1.dp, if (isSelected) PrimaryCyan else NavyCardBorder, RoundedCornerShape(20.dp))
                            .clickable { onDateSelected(date) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("single_date_chip_$date")
                    ) {
                        Text(
                            text = date,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Expanded Custom Date Range Controls
        if (expandedMode || isDateRangeActive) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date Selector
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavySurfaceVariant)
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { startDropdownOpen = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("start_date_dropdown")
                        ) {
                            Column {
                                Text("FROM DATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(startDateRange.ifEmpty { "Select Start" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(
                            expanded = startDropdownOpen,
                            onDismissRequest = { startDropdownOpen = false },
                            modifier = Modifier.background(NavySurface)
                        ) {
                            sortedDates.forEach { date ->
                                DropdownMenuItem(
                                    text = { Text(date, color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        startDropdownOpen = false
                                        val currentEnd = endDateRange.ifEmpty { sortedDates.last() }
                                        onDateRangeSelected(date, currentEnd)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TO", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))

                    // End Date Selector
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavySurfaceVariant)
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { endDropdownOpen = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("end_date_dropdown")
                        ) {
                            Column {
                                Text("TO DATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(endDateRange.ifEmpty { "Select End" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(
                            expanded = endDropdownOpen,
                            onDismissRequest = { endDropdownOpen = false },
                            modifier = Modifier.background(NavySurface)
                        ) {
                            sortedDates.forEach { date ->
                                DropdownMenuItem(
                                    text = { Text(date, color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        endDropdownOpen = false
                                        val currentStart = startDateRange.ifEmpty { sortedDates.first() }
                                        onDateRangeSelected(currentStart, date)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Range Preset Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("All Dates", modifier = Modifier.weight(1f)) {
                        if (sortedDates.isNotEmpty()) {
                            onDateRangeSelected(sortedDates.first(), sortedDates.last())
                        }
                    }
                    PresetChip("Last 3 Days", modifier = Modifier.weight(1f)) {
                        if (sortedDates.size >= 3) {
                            val start = sortedDates[sortedDates.size - 3]
                            val end = sortedDates.last()
                            onDateRangeSelected(start, end)
                        } else if (sortedDates.isNotEmpty()) {
                            onDateRangeSelected(sortedDates.first(), sortedDates.last())
                        }
                    }
                    PresetChip("Single Date", modifier = Modifier.weight(1f)) {
                        onClearDateRange()
                    }
                }
            }
        }

        // Range Analytics Summary Card
        if (isDateRangeActive && dateRangeSummary != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryCyan.copy(alpha = 0.4f))),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Period Performance (${dateRangeSummary.totalDays} Trading Days)",
                            color = PrimaryCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (dateRangeSummary.indexReturnPct != 0.0) {
                            val isUp = dateRangeSummary.indexReturnPct >= 0
                            val retColor = if (isUp) BullishGreen else BearishRed
                            Text(
                                text = "NIFTY: ${if (isUp) "+" else ""}${String.format("%.2f", dateRangeSummary.indexReturnPct)}%",
                                color = retColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Turnover", color = TextMuted, fontSize = 10.sp)
                            Text("₹${dateRangeSummary.totalTurnoverCrores} Cr", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Top Period Gainer", color = TextMuted, fontSize = 10.sp)
                            val topGainer = dateRangeSummary.overallTopGainers.firstOrNull()
                            if (topGainer != null) {
                                Text("${topGainer.symbol} (+${topGainer.returnPct}%)", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("-", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                        Column {
                            Text("Top Period Drag", color = TextMuted, fontSize = 10.sp)
                            val topLoser = dateRangeSummary.overallTopLosers.firstOrNull()
                            if (topLoser != null) {
                                Text("${topLoser.symbol} (${topLoser.returnPct}%)", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("-", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurfaceVariant)
            .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
