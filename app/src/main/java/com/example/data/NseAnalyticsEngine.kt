package com.example.data

import kotlin.math.roundToInt

data class DailyMarketSummary(
    val date: String,
    val advanceCount: Int = 0,
    val declineCount: Int = 0,
    val unchangedCount: Int = 0,
    val totalTurnoverCrores: Double = 0.0,
    val totalVolume: Long = 0L,
    val topGainers: List<NseStockRecord> = emptyList(),
    val topLosers: List<NseStockRecord> = emptyList(),
    val volumeSurgeStocks: List<NseStockRecord> = emptyList(),
    val highDeliveryStocks: List<NseStockRecord> = emptyList(),
    val indexRecord: NseStockRecord? = null,
    val sentimentScore: Double = 0.0, // -100 to +100
    val sentimentLabel: String = "Neutral"
)

data class SymbolPerformanceSummary(
    val symbol: String,
    val startClose: Double,
    val endClose: Double,
    val returnPct: Double,
    val maxHigh: Double,
    val minLow: Double,
    val totalVolume: Long,
    val avgDeliveryPct: Double
)

data class DateRangeMarketSummary(
    val startDate: String,
    val endDate: String,
    val totalDays: Int,
    val totalTurnoverCrores: Double = 0.0,
    val totalVolume: Long = 0L,
    val avgAdvances: Double = 0.0,
    val avgDeclines: Double = 0.0,
    val overallTopGainers: List<SymbolPerformanceSummary> = emptyList(),
    val overallTopLosers: List<SymbolPerformanceSummary> = emptyList(),
    val indexStartClose: Double = 0.0,
    val indexEndClose: Double = 0.0,
    val indexReturnPct: Double = 0.0
)

object NseAnalyticsEngine {

    fun analyzeDateRange(startDate: String, endDate: String, records: List<NseStockRecord>): DateRangeMarketSummary {
        val filtered = records.filter { it.date in startDate..endDate }
        if (filtered.isEmpty()) {
            return DateRangeMarketSummary(startDate = startDate, endDate = endDate, totalDays = 0)
        }

        val distinctDates = filtered.map { it.date }.distinct().sorted()
        val totalDays = distinctDates.size

        var totalTurnover = 0.0
        var totalVol = 0L

        val eqRecords = filtered.filter { it.series == "EQ" }
        for (rec in eqRecords) {
            totalTurnover += rec.totalTradedVal
            totalVol += rec.totalTradedQty
        }

        val turnoverCr = if (totalTurnover > 1_00_00_000) totalTurnover / 1_00_00_000.0 else totalTurnover / 100.0

        // Per symbol aggregates over range
        val groupedBySymbol = eqRecords.groupBy { it.symbol }
        val symbolSummaries = mutableListOf<SymbolPerformanceSummary>()

        for ((symbol, symRecords) in groupedBySymbol) {
            val sortedByDate = symRecords.sortedBy { it.date }
            val firstRec = sortedByDate.first()
            val lastRec = sortedByDate.last()

            val startPx = if (firstRec.prevClose > 0) firstRec.prevClose else firstRec.open
            val endPx = lastRec.close

            val retPct = if (startPx > 0) ((endPx - startPx) / startPx) * 100.0 else 0.0
            val maxH = sortedByDate.maxOfOrNull { it.high } ?: 0.0
            val minL = sortedByDate.minOfOrNull { it.low } ?: 0.0
            val symVol = sortedByDate.sumOf { it.totalTradedQty }
            val avgDeliv = sortedByDate.map { it.pctDlyQtToTrd }.average()

            symbolSummaries.add(
                SymbolPerformanceSummary(
                    symbol = symbol,
                    startClose = startPx,
                    endClose = endPx,
                    returnPct = (retPct * 100).roundToInt() / 100.0,
                    maxHigh = maxH,
                    minLow = minL,
                    totalVolume = symVol,
                    avgDeliveryPct = (avgDeliv * 10).roundToInt() / 10.0
                )
            )
        }

        val topGainersOverRange = symbolSummaries.sortedByDescending { it.returnPct }.take(5)
        val topLosersOverRange = symbolSummaries.sortedBy { it.returnPct }.take(5)

        // Index stats over range
        val indexRecords = filtered.filter { it.symbol == "NIFTY 50" || it.series == "IN" }.sortedBy { it.date }
        val idxStartPx = indexRecords.firstOrNull()?.close ?: 0.0
        val idxEndPx = indexRecords.lastOrNull()?.close ?: 0.0
        val idxRet = if (idxStartPx > 0) ((idxEndPx - idxStartPx) / idxStartPx) * 100.0 else 0.0

        return DateRangeMarketSummary(
            startDate = startDate,
            endDate = endDate,
            totalDays = totalDays,
            totalTurnoverCrores = (turnoverCr * 100).roundToInt() / 100.0,
            totalVolume = totalVol,
            overallTopGainers = topGainersOverRange,
            overallTopLosers = topLosersOverRange,
            indexStartClose = idxStartPx,
            indexEndClose = idxEndPx,
            indexReturnPct = (idxRet * 100).roundToInt() / 100.0
        )
    }

    fun analyzeDate(date: String, records: List<NseStockRecord>): DailyMarketSummary {
        val dateRecords = records.filter { it.date == date }
        if (dateRecords.isEmpty()) {
            return DailyMarketSummary(date = date)
        }

        var advances = 0
        var declines = 0
        var unchanged = 0
        var turnover = 0.0
        var totalVol = 0L

        val eqRecords = dateRecords.filter { it.series == "EQ" }
        val indexRecord = dateRecords.find { it.symbol == "NIFTY 50" || it.series == "IN" }

        for (rec in eqRecords) {
            val change = rec.priceChangePct
            when {
                change > 0.05 -> advances++
                change < -0.05 -> declines++
                else -> unchanged++
            }
            turnover += rec.totalTradedVal
            totalVol += rec.totalTradedQty
        }

        val sortedGainers = eqRecords.sortedByDescending { it.priceChangePct }.take(5)
        val sortedLosers = eqRecords.sortedBy { it.priceChangePct }.take(5)
        val highDelivery = eqRecords.filter { it.pctDlyQtToTrd >= 50.0 }.sortedByDescending { it.pctDlyQtToTrd }.take(5)
        val volumeSurges = eqRecords.sortedByDescending { it.totalTradedQty }.take(5)

        val totalEq = (advances + declines + unchanged).coerceAtLeast(1)
        val rawSentiment = ((advances - declines).toDouble() / totalEq) * 100.0
        val roundedSentiment = (rawSentiment * 10).roundToInt() / 10.0

        val label = when {
            roundedSentiment >= 40.0 -> "Strongly Bullish"
            roundedSentiment >= 10.0 -> "Mildly Bullish"
            roundedSentiment <= -40.0 -> "Strongly Bearish"
            roundedSentiment <= -10.0 -> "Mildly Bearish"
            else -> "Consolidating"
        }

        // Turnover in Crores (assuming totalTradedVal in INR or Lakhs)
        val turnoverCr = if (turnover > 1_00_00_000) turnover / 1_00_00_000.0 else turnover / 100.0

        return DailyMarketSummary(
            date = date,
            advanceCount = advances,
            declineCount = declines,
            unchangedCount = unchanged,
            totalTurnoverCrores = (turnoverCr * 100).roundToInt() / 100.0,
            totalVolume = totalVol,
            topGainers = sortedGainers,
            topLosers = sortedLosers,
            volumeSurgeStocks = volumeSurges,
            highDeliveryStocks = highDelivery,
            indexRecord = indexRecord,
            sentimentScore = roundedSentiment,
            sentimentLabel = label
        )
    }

    /**
     * Auto-generates smart market insights for a specific date given current & historical dataset
     */
    fun generateAutoInsights(date: String, allRecords: List<NseStockRecord>): List<MarketInsight> {
        val dateRecords = allRecords.filter { it.date == date }
        if (dateRecords.isEmpty()) return emptyList()

        val summary = analyzeDate(date, dateRecords)
        val insights = mutableListOf<MarketInsight>()

        // 1. Market Breadth Insight
        val total = (summary.advanceCount + summary.declineCount + summary.unchangedCount).coerceAtLeast(1)
        val advancePct = ((summary.advanceCount.toDouble() / total) * 100).roundToInt()
        insights.add(
            MarketInsight(
                date = date,
                title = "Market Breadth: ${summary.sentimentLabel}",
                summary = "${summary.advanceCount} stocks advanced while ${summary.declineCount} declined (${advancePct}% advancing ratio). Total turnover recorded at ₹${summary.totalTurnoverCrores} Cr.",
                category = if (summary.sentimentScore >= 0) "BULLISH" else "BEARISH",
                impactSymbol = "NIFTY 50",
                metricValue = "${summary.advanceCount} A / ${summary.declineCount} D"
            )
        )

        // 2. Top Gainer Outperformance
        val topGainer = summary.topGainers.firstOrNull()
        if (topGainer != null && topGainer.priceChangePct > 1.2) {
            insights.add(
                MarketInsight(
                    date = date,
                    title = "${topGainer.symbol} Leads Top Gainers",
                    summary = "${topGainer.symbol} rallied +${String.format("%.2f", topGainer.priceChangePct)}% to close at ₹${topGainer.close} with total volume of ${formatVolume(topGainer.totalTradedQty)} shares.",
                    category = "BREAKOUT",
                    impactSymbol = topGainer.symbol,
                    metricValue = "+${String.format("%.2f", topGainer.priceChangePct)}%"
                )
            )
        }

        // 3. Top Loser Drag
        val topLoser = summary.topLosers.firstOrNull()
        if (topLoser != null && topLoser.priceChangePct < -1.0) {
            insights.add(
                MarketInsight(
                    date = date,
                    title = "${topLoser.symbol} Faces Selling Pressure",
                    summary = "${topLoser.symbol} declined ${String.format("%.2f", topLoser.priceChangePct)}% closing at ₹${topLoser.close} from previous close of ₹${topLoser.prevClose}.",
                    category = "BEARISH",
                    impactSymbol = topLoser.symbol,
                    metricValue = "${String.format("%.2f", topLoser.priceChangePct)}%"
                )
            )
        }

        // 4. Institutional Delivery Accumulation
        val highDeliv = summary.highDeliveryStocks.firstOrNull()
        if (highDeliv != null && highDeliv.pctDlyQtToTrd >= 55.0) {
            insights.add(
                MarketInsight(
                    date = date,
                    title = "High Institutional Delivery in ${highDeliv.symbol}",
                    summary = "${highDeliv.symbol} logged an impressive ${String.format("%.1f", highDeliv.pctDlyQtToTrd)}% delivery ratio, indicating strong institutional holding build-up.",
                    category = "VOLUME_SURGE",
                    impactSymbol = highDeliv.symbol,
                    metricValue = "${String.format("%.1f", highDeliv.pctDlyQtToTrd)}% Deliv"
                )
            )
        }

        // 5. Index Movement
        summary.indexRecord?.let { idx ->
            val idxChg = idx.priceChangePct
            val direction = if (idxChg >= 0) "rose +${String.format("%.2f", idxChg)}%" else "dropped ${String.format("%.2f", idxChg)}%"
            insights.add(
                MarketInsight(
                    date = date,
                    title = "Benchmark Index Update",
                    summary = "${idx.symbol} $direction closing at ${String.format("%.2f", idx.close)} (Day Range: ${String.format("%.2f", idx.low)} - ${String.format("%.2f", idx.high)}).",
                    category = if (idxChg >= 0) "BULLISH" else "BEARISH",
                    impactSymbol = idx.symbol,
                    metricValue = "${if (idxChg >= 0) "+" else ""}${String.format("%.2f", idxChg)}%"
                )
            )
        }

        return insights
    }

    private fun formatVolume(qty: Long): String {
        return when {
            qty >= 1_00_00_000 -> String.format("%.2f Cr", qty / 1_00_00_000.0)
            qty >= 1_00_000 -> String.format("%.2f L", qty / 1_00_000.0)
            qty >= 1_000 -> String.format("%.1f K", qty / 1000.0)
            else -> qty.toString()
        }
    }
}
