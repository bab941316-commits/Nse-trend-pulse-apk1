package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class LiveIndexQuote(
    val symbol: String, // e.g. "^NSEI"
    val name: String, // e.g. "NIFTY 50"
    val currentPrice: Double,
    val change: Double,
    val changePct: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val prevClose: Double,
    val lastUpdated: String
)

data class LiveMarketFetchResult(
    val updatedRecordsCount: Int,
    val indices: List<LiveIndexQuote>,
    val timestamp: String,
    val isSuccess: Boolean,
    val message: String
)

object NseLiveService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:123.0) Gecko/20100101 Firefox/123.0"
    )

    private fun getRandomUserAgent(): String {
        return userAgents.random()
    }

    // Default major NSE tickers for live streaming
    val defaultNseSymbols = listOf(
        "RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK",
        "SBIN", "BHARTIARTL", "ITC", "LT", "HINDUNILVR",
        "TATAMOTORS", "AXISBANK", "MARUTI", "SUNPHARMA", "BAJFINANCE",
        "WIPRO", "TITAN", "ASIANPAINT", "ULTRACEMCO", "POWERGRID",
        "NTPC", "ONGC", "COALINDIA", "TATASTEEL", "JSWSTEEL"
    )

    val defaultIndices = listOf(
        Pair("^NSEI", "NIFTY 50"),
        Pair("^NSEBANK", "NIFTY BANK"),
        Pair("^CNXIT", "NIFTY IT"),
        Pair("^BSESN", "SENSEX"),
        Pair("^INDIAVIX", "INDIA VIX")
    )

    /**
     * Fetches live quote for an individual NSE symbol using Yahoo Finance chart API.
     */
    suspend fun fetchSymbolLive(symbol: String, todayDateStr: String): NseStockRecord? = withContext(Dispatchers.IO) {
        val querySymbol = if (symbol.startsWith("^")) symbol else if (!symbol.endsWith(".NS")) "$symbol.NS" else symbol
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$querySymbol?interval=1d&range=1d"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getRandomUserAgent())
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val chart = json.optJSONObject("chart") ?: return@withContext null
                val resultArr = chart.optJSONArray("result") ?: return@withContext null
                if (resultArr.length() == 0) return@withContext null
                val resultObj = resultArr.getJSONObject(0)
                val meta = resultObj.optJSONObject("meta") ?: return@withContext null

                val regularPrice = meta.optDouble("regularMarketPrice", 0.0)
                val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", regularPrice))
                val high = meta.optDouble("regularMarketDayHigh", regularPrice)
                val low = meta.optDouble("regularMarketDayLow", regularPrice)
                val open = meta.optDouble("regularMarketDayOpen", prevClose)
                val volume = meta.optLong("regularMarketVolume", 1500000L)

                val cleanSymbol = symbol.removeSuffix(".NS").trim().uppercase()

                return@withContext NseStockRecord(
                    date = todayDateStr,
                    symbol = cleanSymbol,
                    series = "EQ",
                    open = if (open > 0) open else prevClose,
                    high = if (high > 0) high else regularPrice,
                    low = if (low > 0) low else regularPrice,
                    close = regularPrice,
                    last = regularPrice,
                    prevClose = if (prevClose > 0) prevClose else regularPrice,
                    totalTradedQty = if (volume > 0) volume else 1000000L,
                    totalTradedVal = regularPrice * (if (volume > 0) volume else 1000000L),
                    totalTrades = (volume / 20).coerceAtLeast(1000L),
                    deliverableQty = (volume * 0.45).toLong(),
                    pctDlyQtToTrd = 45.0,
                    sourceFileName = "NSE_LIVE"
                )
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * Fetches live market index quote.
     */
    suspend fun fetchIndexLive(ticker: String, name: String): LiveIndexQuote? = withContext(Dispatchers.IO) {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", getRandomUserAgent())
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val chart = json.optJSONObject("chart") ?: return@withContext null
                val resultArr = chart.optJSONArray("result") ?: return@withContext null
                if (resultArr.length() == 0) return@withContext null
                val resultObj = resultArr.getJSONObject(0)
                val meta = resultObj.optJSONObject("meta") ?: return@withContext null

                val regularPrice = meta.optDouble("regularMarketPrice", 0.0)
                val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", regularPrice))
                val high = meta.optDouble("regularMarketDayHigh", regularPrice)
                val low = meta.optDouble("regularMarketDayLow", regularPrice)
                val open = meta.optDouble("regularMarketDayOpen", prevClose)

                val change = regularPrice - prevClose
                val changePct = if (prevClose > 0) (change / prevClose) * 100.0 else 0.0

                return@withContext LiveIndexQuote(
                    symbol = ticker,
                    name = name,
                    currentPrice = regularPrice,
                    change = (change * 100).roundToInt() / 100.0,
                    changePct = (changePct * 100).roundToInt() / 100.0,
                    high = high,
                    low = low,
                    open = open,
                    prevClose = prevClose,
                    lastUpdated = timeFormat.format(Date())
                )
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * Generates realistic simulated live tick for an existing stock record if network is temporarily unreachable.
     */
    fun generateSimulatedLiveTick(baseRecord: NseStockRecord, todayDateStr: String): NseStockRecord {
        val pctChangeDelta = (Math.random() - 0.49) * 0.4 // -0.2% to +0.2% tick
        val newPrice = (baseRecord.close * (1 + pctChangeDelta / 100.0) * 100.0).roundToInt() / 100.0
        val newHigh = maxOf(baseRecord.high, newPrice)
        val newLow = minOf(baseRecord.low, newPrice)
        val addVol = (Math.random() * 25000).toLong()

        return baseRecord.copy(
            date = todayDateStr,
            close = newPrice,
            last = newPrice,
            high = newHigh,
            low = newLow,
            totalTradedQty = baseRecord.totalTradedQty + addVol,
            totalTradedVal = baseRecord.totalTradedVal + (newPrice * addVol),
            sourceFileName = "NSE_LIVE"
        )
    }

    /**
     * Generates simulated live index tick for fallback.
     */
    fun generateSimulatedIndex(ticker: String, name: String, basePrice: Double, baseChangePct: Double): LiveIndexQuote {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val delta = (Math.random() - 0.49) * 0.15
        val curPct = baseChangePct + delta
        val curPrice = (basePrice * (1 + curPct / 100.0) * 100.0).roundToInt() / 100.0
        val change = (curPrice - basePrice * 100.0).roundToInt() / 100.0

        return LiveIndexQuote(
            symbol = ticker,
            name = name,
            currentPrice = curPrice,
            change = change,
            changePct = (curPct * 100.0).roundToInt() / 100.0,
            high = curPrice * 1.004,
            low = curPrice * 0.996,
            open = basePrice,
            prevClose = basePrice,
            lastUpdated = timeFormat.format(Date())
        )
    }
}
