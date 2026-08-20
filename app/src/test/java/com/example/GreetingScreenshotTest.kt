package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.LiveIndexQuote
import com.example.ui.screens.LiveIndexCardItem
import com.example.ui.theme.NseAnalyticsTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      NseAnalyticsTheme {
        LiveIndexCardItem(
          quote = LiveIndexQuote(
            symbol = "^NSEI",
            name = "NIFTY 50",
            currentPrice = 24350.20,
            change = 112.40,
            changePct = 0.46,
            high = 24410.0,
            low = 24290.0,
            open = 24250.0,
            prevClose = 24237.80,
            lastUpdated = "15:30"
          ),
          onClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
