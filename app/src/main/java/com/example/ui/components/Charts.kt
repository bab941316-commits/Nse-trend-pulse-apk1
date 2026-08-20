package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NseStockRecord
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
import kotlin.math.roundToInt

@Composable
fun StockTrendLineChart(
    records: List<NseStockRecord>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryCyan,
    showVolume: Boolean = true,
    title: String? = null,
    subtitle: String? = null
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(NavySurface, RoundedCornerShape(12.dp))
                .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No historical stock data available for selected date range", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    var showSmaOverlay by remember { mutableStateOf(true) }
    var showVolumeBars by remember { mutableStateOf(showVolume) }
    var touchXPosition by remember { mutableStateOf<Float?>(null) }

    val prices = records.map { it.close }
    val minPrice = (prices.minOrNull() ?: 0.0) * 0.992
    val maxPrice = (prices.maxOrNull() ?: 100.0) * 1.008
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

    val volumes = records.map { it.totalTradedQty }
    val maxVolume = (volumes.maxOrNull() ?: 1L).coerceAtLeast(1L)

    val firstRecord = records.first()
    val lastRecord = records.last()
    val isOverallBullish = lastRecord.close >= firstRecord.close
    val chartColor = if (isOverallBullish) BullishGreen else BearishRed

    val periodHigh = records.maxOfOrNull { it.high.coerceAtLeast(it.close) } ?: lastRecord.close
    val periodLow = records.minOfOrNull { it.low.let { l -> if (l > 0) l else it.close } } ?: lastRecord.close
    val avgVolume = if (records.isNotEmpty()) volumes.average() else 0.0
    val totalChgAmount = lastRecord.close - firstRecord.close
    val totalChgPct = if (firstRecord.close > 0) (totalChgAmount / firstRecord.close) * 100.0 else 0.0

    // 5-Period Simple Moving Average
    val sma5List = remember(records) {
        records.indices.map { i ->
            val startIdx = (i - 4).coerceAtLeast(0)
            records.subList(startIdx, i + 1).map { it.close }.average()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NavySurface, RoundedCornerShape(16.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: (lastRecord.symbol.ifEmpty { "Historical Stock Trend" }),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle ?: "${records.size} Trading Days (${firstRecord.date} to ${lastRecord.date})",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.2f", lastRecord.close)}",
                    color = chartColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (totalChgAmount >= 0) "+" else ""}₹${String.format("%.2f", totalChgAmount)} (${if (totalChgPct >= 0) "+" else ""}${String.format("%.2f", totalChgPct)}%)",
                        color = if (totalChgPct >= 0) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Controls & Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // SMA Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showSmaOverlay) AccentGold.copy(alpha = 0.2f) else NavySurfaceVariant)
                        .border(1.dp, if (showSmaOverlay) AccentGold else NavyCardBorder, RoundedCornerShape(16.dp))
                        .clickable { showSmaOverlay = !showSmaOverlay }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📈 5-SMA",
                        color = if (showSmaOverlay) AccentGold else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Volume Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showVolumeBars) PrimaryCyan.copy(alpha = 0.2f) else NavySurfaceVariant)
                        .border(1.dp, if (showVolumeBars) PrimaryCyan else NavyCardBorder, RoundedCornerShape(16.dp))
                        .clickable { showVolumeBars = !showVolumeBars }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📊 Volume",
                        color = if (showVolumeBars) PrimaryCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (touchXPosition != null) "Hold & Drag to Scrub" else "Tap/Drag to inspect",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas Line Graph with Gesture Scrubbing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(records.size) {
                        detectTapGestures(
                            onPress = { offset ->
                                touchXPosition = offset.x
                                tryAwaitRelease()
                                touchXPosition = null
                            },
                            onTap = { offset ->
                                touchXPosition = offset.x
                            }
                        )
                    }
                    .pointerInput(records.size) {
                        detectDragGestures(
                            onDragStart = { offset -> touchXPosition = offset.x },
                            onDragEnd = { touchXPosition = null },
                            onDragCancel = { touchXPosition = null },
                            onDrag = { change, _ ->
                                change.consume()
                                touchXPosition = change.position.x
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val chartHeight = if (showVolumeBars) height * 0.72f else height * 0.90f
                val volumeHeight = height * 0.22f

                val stepX = width / (records.size - 1).coerceAtLeast(1)

                // Draw Horizontal Grid Lines & Price Axis Labels
                val gridColor = NavyCardBorder.copy(alpha = 0.4f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..3) {
                    val y = chartHeight * (i / 3.0f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Build Price Trend Line Path & Fill Path
                val path = Path()
                val fillPath = Path()

                records.forEachIndexed { index, record ->
                    val x = index * stepX
                    val normalizedY = ((maxPrice - record.close) / priceRange).toFloat()
                    val y = normalizedY * chartHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    if (index == records.size - 1) {
                        fillPath.lineTo(x, chartHeight)
                        fillPath.close()
                    }
                }

                // Draw Area Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            chartColor.copy(alpha = 0.28f),
                            chartColor.copy(alpha = 0.01f)
                        )
                    )
                )

                // Draw Main Price Trend Line
                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw 5-SMA Overlay Line if enabled
                if (showSmaOverlay && sma5List.size == records.size) {
                    val smaPath = Path()
                    sma5List.forEachIndexed { index, smaVal ->
                        val x = index * stepX
                        val normalizedY = ((maxPrice - smaVal) / priceRange).toFloat()
                        val y = normalizedY * chartHeight
                        if (index == 0) smaPath.moveTo(x, y) else smaPath.lineTo(x, y)
                    }
                    drawPath(
                        path = smaPath,
                        color = AccentGold.copy(alpha = 0.85f),
                        style = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f))
                    )
                }

                // Draw Volume Histogram Bars
                if (showVolumeBars) {
                    records.forEachIndexed { index, record ->
                        val x = index * stepX
                        val volRatio = (record.totalTradedQty.toFloat() / maxVolume).coerceIn(0f, 1f)
                        val barH = (volRatio * volumeHeight).coerceAtLeast(2f)
                        val barY = height - barH

                        val isUp = record.close >= record.prevClose
                        val barColor = if (isUp) BullishGreen.copy(alpha = 0.55f) else BearishRed.copy(alpha = 0.55f)

                        drawRect(
                            color = barColor,
                            topLeft = Offset(x - (stepX * 0.35f), barY),
                            size = Size((stepX * 0.70f).coerceAtLeast(3f), barH)
                        )
                    }
                }

                // Draw Touch Scrubbing Crosshair & Highlighting
                touchXPosition?.let { touchX ->
                    val clampedTouchX = touchX.coerceIn(0f, width)
                    val selectedIdx = ((clampedTouchX / width) * (records.size - 1)).roundToInt().coerceIn(0, records.size - 1)
                    val targetRecord = records[selectedIdx]

                    val targetX = selectedIdx * stepX
                    val targetNormalizedY = ((maxPrice - targetRecord.close) / priceRange).toFloat()
                    val targetY = targetNormalizedY * chartHeight

                    // Vertical Crosshair
                    drawLine(
                        color = PrimaryCyan.copy(alpha = 0.8f),
                        start = Offset(targetX, 0f),
                        end = Offset(targetX, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Horizontal Price Line
                    drawLine(
                        color = PrimaryCyan.copy(alpha = 0.4f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // Outer & Inner Indicator Dots
                    drawCircle(color = PrimaryCyan, radius = 7.dp.toPx(), center = Offset(targetX, targetY))
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(targetX, targetY))
                } ?: run {
                    // Default Last Point Pulse Dot
                    val lastX = (records.size - 1) * stepX
                    val lastY = (((maxPrice - lastRecord.close) / priceRange).toFloat()) * chartHeight
                    drawCircle(color = chartColor, radius = 6.dp.toPx(), center = Offset(lastX, lastY))
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Tooltip Card or X-Axis Date Labels
        val currentTouchX = touchXPosition
        if (currentTouchX != null) {
            val approxWidth = 300f // approximate pixel scale fallback
            val selectedIdx = ((currentTouchX.coerceAtLeast(0f) / approxWidth) * (records.size - 1)).roundToInt().coerceIn(0, records.size - 1)
            val selectedRec = records.getOrNull(selectedIdx) ?: lastRecord

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceVariant)
                    .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📅 ${selectedRec.date} (${selectedRec.symbol})",
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Open: ₹${selectedRec.open} | High: ₹${selectedRec.high} | Low: ₹${selectedRec.low}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Close: ₹${String.format("%.2f", selectedRec.close)}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val dayChg = selectedRec.priceChangePct
                        Text(
                            text = "Day Chg: ${if (dayChg >= 0) "+" else ""}${String.format("%.2f", dayChg)}%",
                            color = if (dayChg >= 0) BullishGreen else BearishRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // X-Axis Date Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val datesToDisplay = if (records.size <= 5) {
                    records.map { it.date.takeLast(5) }
                } else {
                    listOf(
                        records.first().date.takeLast(5),
                        records[records.size / 3].date.takeLast(5),
                        records[(records.size * 2) / 3].date.takeLast(5),
                        records.last().date.takeLast(5)
                    )
                }

                datesToDisplay.forEach { dateStr ->
                    Text(dateStr, color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Period Summary Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavySurfaceVariant, RoundedCornerShape(10.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Period High", color = TextMuted, fontSize = 9.sp)
                Text("₹${String.format("%.2f", periodHigh)}", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Period Low", color = TextMuted, fontSize = 9.sp)
                Text("₹${String.format("%.2f", periodLow)}", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avg Volume", color = TextMuted, fontSize = 9.sp)
                Text(formatQtyVal(avgVolume.toLong()), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Range Return", color = TextMuted, fontSize = 9.sp)
                Text(
                    text = "${if (totalChgPct >= 0) "+" else ""}${String.format("%.2f", totalChgPct)}%",
                    color = if (totalChgPct >= 0) BullishGreen else BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatQtyVal(qty: Long): String {
    return when {
        qty >= 1_00_00_000 -> String.format("%.1f Cr", qty / 1_00_00_000.0)
        qty >= 1_00_000 -> String.format("%.1f L", qty / 1_00_000.0)
        qty >= 1_000 -> String.format("%.1f K", qty / 1000.0)
        else -> qty.toString()
    }
}

@Composable
fun MarketBreadthBar(
    advances: Int,
    declines: Int,
    unchanged: Int,
    modifier: Modifier = Modifier
) {
    val total = (advances + declines + unchanged).coerceAtLeast(1)
    val advWeight = advances.toFloat() / total
    val decWeight = declines.toFloat() / total
    val uncWeight = unchanged.toFloat() / total

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NavySurface, RoundedCornerShape(12.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Market Breadth (Advances vs Declines)",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "A:D Ratio ${String.format("%.2f", advances.toDouble() / declines.coerceAtLeast(1))}",
                color = PrimaryCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stacked Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(NavySurfaceVariant, RoundedCornerShape(6.dp))
        ) {
            if (advWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(advWeight)
                        .background(BullishGreen, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                )
            }
            if (uncWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(uncWeight)
                        .background(Color.Gray)
                )
            }
            if (decWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(decWeight)
                        .background(BearishRed, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Breadth Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(BullishGreen, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Advances: $advances (${(advWeight * 100).toInt()}%)", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(Color.Gray, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Unchanged: $unchanged", color = TextMuted, fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(BearishRed, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Declines: $declines (${(decWeight * 100).toInt()}%)", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
