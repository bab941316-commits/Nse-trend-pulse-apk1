package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryCyan,
  onPrimary = Color.Black,
  primaryContainer = NavySurfaceVariant,
  onPrimaryContainer = TextPrimary,
  secondary = AccentGold,
  onSecondary = Color.Black,
  tertiary = BullishGreen,
  background = NavyBackground,
  onBackground = TextPrimary,
  surface = NavySurface,
  onSurface = TextPrimary,
  surfaceVariant = NavySurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = NavyCardBorder
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF006689),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC3E8FF),
  onPrimaryContainer = Color(0xFF001E2C),
  secondary = Color(0xFF765800),
  onSecondary = Color.White,
  tertiary = Color(0xFF006E38),
  background = Color(0xFFF8F9FE),
  onBackground = Color(0xFF191C1E),
  surface = Color.White,
  onSurface = Color(0xFF191C1E),
  surfaceVariant = Color(0xFFE1E2EC),
  onSurfaceVariant = Color(0xFF44474F),
  outline = Color(0xFF74777F)
)

@Composable
fun NseAnalyticsTheme(
  darkTheme: Boolean = true, // Default to sleek financial dark mode
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
