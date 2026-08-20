package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.NseMarketSession
import com.example.ui.components.NseMarketTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NSE Analytics", appName)
  }

  @Test
  fun `test nse market status calculation`() {
    val statusNow = NseMarketTimeUtils.determineMarketStatus()
    assertNotNull(statusNow)
    assertNotNull(statusNow.session)
    assertNotNull(statusNow.session.trendContext)
  }

  @Test
  fun `test sentiment algorithm advancing vs declining bias`() {
    val bullishResult = com.example.ui.components.SentimentAlgorithm.evaluate(advances = 40, declines = 10, unchanged = 5)
    assertEquals(com.example.ui.components.MarketSentimentBias.EXTREME_BULLISH, bullishResult.bias)
    assertEquals(80, bullishResult.score)

    val bearishResult = com.example.ui.components.SentimentAlgorithm.evaluate(advances = 8, declines = 32, unchanged = 5)
    assertEquals(com.example.ui.components.MarketSentimentBias.EXTREME_BEARISH, bearishResult.bias)
    assertEquals(20, bearishResult.score)

    val neutralResult = com.example.ui.components.SentimentAlgorithm.evaluate(advances = 25, declines = 25, unchanged = 10)
    assertEquals(com.example.ui.components.MarketSentimentBias.NEUTRAL, neutralResult.bias)
    assertEquals(50, neutralResult.score)
  }

  @Test
  fun `test top gainer and loser ranking logic`() {
    val sampleRecords = listOf(
      com.example.data.NseStockRecord(date = "2024-03-28", symbol = "RELIANCE", close = 3000.0, prevClose = 2900.0, series = "EQ"),
      com.example.data.NseStockRecord(date = "2024-03-28", symbol = "TCS", close = 3800.0, prevClose = 3900.0, series = "EQ"),
      com.example.data.NseStockRecord(date = "2024-03-28", symbol = "INFY", close = 1500.0, prevClose = 1400.0, series = "EQ")
    )

    val gainers = sampleRecords.filter { it.priceChangePct > 0 }.sortedByDescending { it.priceChangePct }
    val losers = sampleRecords.filter { it.priceChangePct < 0 }.sortedBy { it.priceChangePct }

    assertEquals("INFY", gainers[0].symbol) // +7.14%
    assertEquals("RELIANCE", gainers[1].symbol) // +3.45%
    assertEquals(1, losers.size)
    assertEquals("TCS", losers[0].symbol) // -2.56%
  }

  @Test
  fun `test new listed company news curated records and fallback`() {
    val initialNews = com.example.data.NseNewListingsService.getInitialCuratedNews()
    assertNotNull(initialNews)
    assert(initialNews.isNotEmpty())
    assert(initialNews.any { it.symbol == "BAJAJHOUS" })
    assert(initialNews.any { it.isHot })

    val ipoTrackers = com.example.data.NseNewListingsService.curatedIpoDebutRecords
    assertNotNull(ipoTrackers)
    assert(ipoTrackers.isNotEmpty())
    assert(ipoTrackers.any { it.symbol == "PREMIERENE" })
  }
}
