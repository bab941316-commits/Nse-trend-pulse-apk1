package com.example.data

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NseCsvParser {

    /**
     * Parses CSV lines into NseStockRecord items.
     * Supports various NSE CSV layouts (Equity Bhavcopy, Index Bhavcopy, Custom CSV).
     */
    fun parseCsv(csvText: String, defaultDate: String? = null, sourceFileName: String = "Uploaded_CSV"): List<NseStockRecord> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Extract header
        val header = parseCsvLine(lines[0]).map { it.uppercase().trim().replace("\"", "").replace(" ", "_") }
        
        val symbolCol = findColumnIndex(header, listOf("SYMBOL", "TICKER", "STOCK", "INDEX_NAME", "SECURITY", "NAME"))
        val seriesCol = findColumnIndex(header, listOf("SERIES", "SERIES_TYPE", "GROUP"))
        val dateCol = findColumnIndex(header, listOf("DATE", "TIMESTAMP", "TRADING_DATE", "RECORD_DATE"))
        val openCol = findColumnIndex(header, listOf("OPEN", "OPEN_PRICE", "INDEX_OPEN"))
        val highCol = findColumnIndex(header, listOf("HIGH", "HIGH_PRICE", "INDEX_HIGH"))
        val lowCol = findColumnIndex(header, listOf("LOW", "LOW_PRICE", "INDEX_LOW"))
        val closeCol = findColumnIndex(header, listOf("CLOSE", "CLOSE_PRICE", "LAST_PRICE", "INDEX_CLOSE"))
        val prevCloseCol = findColumnIndex(header, listOf("PREVCLOSE", "PREV_CLOSE", "PREVIOUS_CLOSE"))
        val qtyCol = findColumnIndex(header, listOf("TOTTRDQTY", "TOTALTRADEDQTY", "VOLUME", "TRADED_QTY", "QUANTITY"))
        val valCol = findColumnIndex(header, listOf("TOTTRDVAL", "TOTALTRADEDVAL", "TURNOVER", "TRADED_VAL", "VALUE"))
        val tradesCol = findColumnIndex(header, listOf("TOTALTRADES", "TOTAL_TRADES", "NO_OF_TRADES", "TRADES"))
        val delivQtyCol = findColumnIndex(header, listOf("DELIV_QTY", "DELIVERABLE_QTY", "DELIVERY_QTY"))
        val delivPctCol = findColumnIndex(header, listOf("PCT_DELIV_TO_TRADED", "%_DLY_QT_TO_TRD", "DELIVERY_PCT", "PCT_DELIVERY"))

        val records = mutableListOf<NseStockRecord>()

        for (i in 1 until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.size <= 2) continue

            val rawSymbol = getColValue(cols, symbolCol)
            if (rawSymbol.isEmpty()) continue

            val rawSeries = getColValue(cols, seriesCol, default = "EQ")
            val rawDate = getColValue(cols, dateCol, default = defaultDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            val normalizedDate = normalizeDate(rawDate)

            val open = parseNumber(getColValue(cols, openCol))
            val high = parseNumber(getColValue(cols, highCol))
            val low = parseNumber(getColValue(cols, lowCol))
            val close = parseNumber(getColValue(cols, closeCol))
            val prevClose = parseNumber(getColValue(cols, prevCloseCol, default = close.toString()))
            val qty = parseLongNumber(getColValue(cols, qtyCol))
            val value = parseNumber(getColValue(cols, valCol))
            val trades = parseLongNumber(getColValue(cols, tradesCol))
            val delivQty = parseLongNumber(getColValue(cols, delivQtyCol))
            val delivPct = parseNumber(getColValue(cols, delivPctCol))

            records.add(
                NseStockRecord(
                    date = normalizedDate,
                    symbol = rawSymbol.uppercase().replace("\"", "").trim(),
                    series = rawSeries.uppercase().replace("\"", "").trim(),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    last = close,
                    prevClose = if (prevClose > 0) prevClose else close,
                    totalTradedQty = qty,
                    totalTradedVal = value,
                    totalTrades = trades,
                    deliverableQty = delivQty,
                    pctDlyQtToTrd = delivPct,
                    sourceFileName = sourceFileName
                )
            )
        }

        return records
    }

    private fun findColumnIndex(header: List<String>, candidates: List<String>): Int {
        for (candidate in candidates) {
            val idx = header.indexOfFirst { it == candidate || it.contains(candidate) }
            if (idx != -1) return idx
        }
        return -1
    }

    private fun getColValue(cols: List<String>, index: Int, default: String = ""): String {
        return if (index >= 0 && index < cols.size) cols[index].trim().replace("\"", "") else default
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        cur.append(ch)
                    } else {
                        result.add(cur.toString().trim())
                        cur = StringBuilder()
                    }
                }
                else -> cur.append(ch)
            }
        }
        result.add(cur.toString().trim())
        return result
    }

    private fun parseNumber(str: String): Double {
        if (str.isEmpty()) return 0.0
        val cleanStr = str.replace(",", "").replace("%", "").replace("₹", "").trim()
        return cleanStr.toDoubleOrNull() ?: 0.0
    }

    private fun parseLongNumber(str: String): Long {
        if (str.isEmpty()) return 0L
        val cleanStr = str.replace(",", "").trim()
        return cleanStr.toLongOrNull() ?: cleanStr.toDoubleOrNull()?.toLong() ?: 0L
    }

    /**
     * Normalizes various date inputs into YYYY-MM-DD
     */
    fun normalizeDate(rawDate: String): String {
        val clean = rawDate.trim().replace("\"", "")
        if (clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return clean

        val formats = listOf(
            SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
            SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH),
            SimpleDateFormat("d-MMM-yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
        )

        for (fmt in formats) {
            try {
                val d = fmt.parse(clean)
                if (d != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(d)
                }
            } catch (_: Exception) {}
        }
        return clean
    }

    /**
     * Generates an authentic multi-date NSE market sample dataset covering recent trading dates
     * (e.g. 2026-08-01 through 2026-08-09) with real stock symbols, realistic prices, turnover & volume.
     */
    fun generateSampleNseData(): List<NseStockRecord> {
        val dates = listOf(
            "2026-08-01", "2026-08-04", "2026-08-05", "2026-08-06", "2026-08-07", "2026-08-08", "2026-08-09"
        )

        val stockBaseData = listOf(
            // Symbol, BasePrice, VolatilityFactor, VolumeBase
            StockConfig("NIFTY 50", 24500.0, 0.008, 15000000L, "IN"),
            StockConfig("NIFTY BANK", 51800.0, 0.011, 8500000L, "IN"),
            StockConfig("RELIANCE", 2980.0, 0.012, 6200000L, "EQ"),
            StockConfig("TCS", 4250.0, 0.010, 2800000L, "EQ"),
            StockConfig("HDFCBANK", 1680.0, 0.009, 14500000L, "EQ"),
            StockConfig("INFY", 1820.0, 0.015, 8200000L, "EQ"),
            StockConfig("ICICIBANK", 1210.0, 0.011, 9800000L, "EQ"),
            StockConfig("SBIN", 840.0, 0.014, 18200000L, "EQ"),
            StockConfig("BHARTIARTL", 1460.0, 0.010, 5400000L, "EQ"),
            StockConfig("ITC", 495.0, 0.008, 12000000L, "EQ"),
            StockConfig("LTIM", 5600.0, 0.018, 1200000L, "EQ"),
            StockConfig("LT", 3720.0, 0.013, 3100000L, "EQ"),
            StockConfig("TATAMOTORS", 1080.0, 0.016, 11500000L, "EQ"),
            StockConfig("SUNPHARMA", 1720.0, 0.009, 2900000L, "EQ"),
            StockConfig("AXISBANK", 1180.0, 0.012, 7800000L, "EQ")
        )

        val records = mutableListOf<NseStockRecord>()

        // Price simulation across dates
        val lastCloseMap = mutableMapOf<String, Double>()
        stockBaseData.forEach { lastCloseMap[it.symbol] = it.basePrice }

        // Day specific market trend multipliers (Bullish, Neutral, Bearish days)
        val dayFactors = mapOf(
            "2026-08-01" to 0.004,  // Mild bullish start
            "2026-08-04" to 0.012,  // Strong Rally
            "2026-08-05" to -0.008, // Pullback
            "2026-08-06" to 0.003,  // Consolidation
            "2026-08-07" to 0.015,  // Huge Breakout (IT & Banking)
            "2026-08-08" to -0.002, // Profit booking
            "2026-08-09" to 0.009   // Strong closing momentum
        )

        for (date in dates) {
            val marketBias = dayFactors[date] ?: 0.0

            for (cfg in stockBaseData) {
                val prevClose = lastCloseMap[cfg.symbol] ?: cfg.basePrice
                
                // Specific stock variation
                val stockRand = when (cfg.symbol) {
                    "INFY", "TCS", "LTIM" -> if (date == "2026-08-07") 0.028 else marketBias + (cfg.volatility * 0.4)
                    "HDFCBANK", "ICICIBANK", "SBIN" -> if (date == "2026-08-04") 0.022 else marketBias - (cfg.volatility * 0.2)
                    "RELIANCE" -> if (date == "2026-08-09") 0.018 else marketBias
                    else -> marketBias + (Math.sin((date.hashCode() + cfg.symbol.hashCode()).toDouble()) * cfg.volatility)
                }

                val close = (prevClose * (1.0 + stockRand)).let { Math.round(it * 100.0) / 100.0 }
                val open = (prevClose * (1.0 + (stockRand * 0.3))).let { Math.round(it * 100.0) / 100.0 }
                val high = Math.max(open, close) * (1.0 + Math.abs(cfg.volatility * 0.6))
                val low = Math.min(open, close) * (1.0 - Math.abs(cfg.volatility * 0.5))

                val volumeMult = if (Math.abs(stockRand) > 0.015) 1.8 else 1.0
                val totalQty = (cfg.baseVolume * volumeMult * (0.85 + Math.random() * 0.3)).toLong()
                val totalVal = (totalQty * close).let { Math.round(it * 100.0) / 100.0 }
                val delivQty = (totalQty * (0.45 + (Math.random() * 0.25))).toLong()
                val delivPct = (delivQty.toDouble() / totalQty) * 100.0

                records.add(
                    NseStockRecord(
                        date = date,
                        symbol = cfg.symbol,
                        series = cfg.series,
                        open = Math.round(open * 100.0) / 100.0,
                        high = Math.round(high * 100.0) / 100.0,
                        low = Math.round(low * 100.0) / 100.0,
                        close = close,
                        last = close,
                        prevClose = Math.round(prevClose * 100.0) / 100.0,
                        totalTradedQty = totalQty,
                        totalTradedVal = totalVal,
                        totalTrades = totalQty / 80,
                        deliverableQty = delivQty,
                        pctDlyQtToTrd = Math.round(delivPct * 100.0) / 100.0,
                        sourceFileName = "NSE_Bhavcopy_Sample.csv"
                    )
                )

                lastCloseMap[cfg.symbol] = close
            }
        }

        return records
    }

    private data class StockConfig(
        val symbol: String,
        val basePrice: Double,
        val volatility: Double,
        val baseVolume: Long,
        val series: String
    )
}
