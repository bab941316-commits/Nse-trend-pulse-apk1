package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.NeutralBlue
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MarketSentimentBias(
    val title: String,
    val shortLabel: String,
    val description: String,
    val color: Color,
    val bgColor: Color
) {
    EXTREME_BULLISH(
        title = "Strong Bullish Bias",
        shortLabel = "STRONG BULLISH",
        description = "Broad market rally with overwhelming buying pressure. Advances heavily dominate trading volume.",
        color = BullishGreen,
        bgColor = BullishGreenBg
    ),
    MODERATE_BULLISH(
        title = "Moderate Bullish Bias",
        shortLabel = "BULLISH",
        description = "Advances comfortably outnumber declines. Upward momentum across major active stocks.",
        color = Color(0xFF69F0AE),
        bgColor = BullishGreenBg
    ),
    NEUTRAL(
        title = "Neutral / Balanced",
        shortLabel = "NEUTRAL",
        description = "Advances and declines are evenly balanced. Indecisive consolidation with mixed sectoral bias.",
        color = AccentGold,
        bgColor = AccentGold.copy(alpha = 0.15f)
    ),
    MODERATE_BEARISH(
        title = "Moderate Bearish Bias",
        shortLabel = "BEARISH",
        description = "Declines outpace advances. Selling pressure and profit taking noted across broader market.",
        color = Color(0xFFFF6E40),
        bgColor = BearishRedBg
    ),
    EXTREME_BEARISH(
        title = "Strong Bearish Bias",
        shortLabel = "STRONG BEARISH",
        description = "Widespread market decline. Sellers dominate across large-caps and mid-caps.",
        color = BearishRed,
        bgColor = BearishRedBg
    )
}

data class SentimentAnalysis(
    val score: Int, // 0 to 100 (50 is neutral)
    val rawNetScore: Double, // -100 to +100
    val bias: MarketSentimentBias,
    val advanceDeclineRatio: Double,
    val advances: Int,
    val declines: Int,
    val unchanged: Int,
    val advancePct: Double,
    val declinePct: Double,
    val unchangedPct: Double
)

object SentimentAlgorithm {
    fun evaluate(advances: Int, declines: Int, unchanged: Int): SentimentAnalysis {
        val total = (advances + declines + unchanged).coerceAtLeast(1)
        val activeTotal = (advances + declines).coerceAtLeast(1)

        val advancePct = (advances.toDouble() / total) * 100.0
        val declinePct = (declines.toDouble() / total) * 100.0
        val unchangedPct = (unchanged.toDouble() / total) * 100.0

        // Ratio of advances to total non-flat stocks mapped to 0..100
        val score = ((advances.toDouble() / activeTotal) * 100.0).roundToInt().coerceIn(0, 100)
        val rawNetScore = (((advances - declines).toDouble() / total) * 100.0 * 10).roundToInt() / 10.0
        val adRatio = if (declines > 0) advances.toDouble() / declines else advances.toDouble()

        val bias = when {
            score >= 75 -> MarketSentimentBias.EXTREME_BULLISH
            score >= 58 -> MarketSentimentBias.MODERATE_BULLISH
            score <= 25 -> MarketSentimentBias.EXTREME_BEARISH
            score <= 42 -> MarketSentimentBias.MODERATE_BEARISH
            else -> MarketSentimentBias.NEUTRAL
        }

        return SentimentAnalysis(
            score = score,
            rawNetScore = rawNetScore,
            bias = bias,
            advanceDeclineRatio = (adRatio * 100).roundToInt() / 100.0,
            advances = advances,
            declines = declines,
            unchanged = unchanged,
            advancePct = (advancePct * 10).roundToInt() / 10.0,
            declinePct = (declinePct * 10).roundToInt() / 10.0,
            unchangedPct = (unchangedPct * 10).roundToInt() / 10.0
        )
    }
}

/**
 * Dedicated Market Sentiment Gauge & Indicator Component.
 * Visualizes advancing vs declining distribution, A/D ratio, sentiment score, and bias context.
 */
@Composable
fun MarketSentimentIndicator(
    advances: Int,
    declines: Int,
    unchanged: Int,
    modifier: Modifier = Modifier,
    dateLabel: String? = null
) {
    val analysis = remember(advances, declines, unchanged) {
        SentimentAlgorithm.evaluate(advances, declines, unchanged)
    }

    val animatedScore by animateFloatAsState(
        targetValue = analysis.score.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "sentimentGaugeScore"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("market_sentiment_indicator"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NavyCardBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title and Bias Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(analysis.bias.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Market Sentiment",
                            tint = analysis.bias.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Market Sentiment & Bias",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!dateLabel.isNullOrBlank()) "Based on $dateLabel Advance/Decline Data" else "Advancing vs. Declining Equities",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Current Bias Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(analysis.bias.bgColor)
                        .border(1.dp, analysis.bias.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (analysis.bias) {
                                MarketSentimentBias.EXTREME_BULLISH, MarketSentimentBias.MODERATE_BULLISH -> Icons.Default.TrendingUp
                                MarketSentimentBias.EXTREME_BEARISH, MarketSentimentBias.MODERATE_BEARISH -> Icons.Default.TrendingDown
                                MarketSentimentBias.NEUTRAL -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = analysis.bias.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = analysis.bias.shortLabel,
                            color = analysis.bias.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Arc Gauge with Pointer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                contentAlignment = Alignment.Center
            ) {
                SentimentArcGauge(
                    score = animatedScore,
                    biasColor = analysis.bias.color
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = "${analysis.score}",
                        color = analysis.bias.color,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Sentiment Score (0-100)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sentiment Scale Legend Bar (Bearish -> Neutral -> Bullish)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Bearish", color = BearishRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("50 Neutral", color = AccentGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("100 Bullish", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advance / Decline Ratio & Metrics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("A:D Ratio", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "${analysis.advanceDeclineRatio}x",
                        color = PrimaryCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Net Breadth Bias", color = TextMuted, fontSize = 10.sp)
                    val sign = if (analysis.rawNetScore > 0) "+" else ""
                    Text(
                        text = "$sign${analysis.rawNetScore}%",
                        color = if (analysis.rawNetScore >= 0) BullishGreen else BearishRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Market Mode", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = analysis.bias.title,
                        color = analysis.bias.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Breadth Distribution Bar
            val advWeight = (analysis.advances.toFloat() / (advances + declines + unchanged).coerceAtLeast(1))
            val decWeight = (analysis.declines.toFloat() / (advances + declines + unchanged).coerceAtLeast(1))
            val uncWeight = (analysis.unchanged.toFloat() / (advances + declines + unchanged).coerceAtLeast(1))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NavySurfaceVariant)
            ) {
                if (advWeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(advWeight)
                            .background(BullishGreen)
                    )
                }
                if (uncWeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(uncWeight)
                            .background(Color.Gray.copy(alpha = 0.6f))
                    )
                }
                if (decWeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(decWeight)
                            .background(BearishRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Advances vs Declines Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🟢 ${analysis.advances} Adv (${analysis.advancePct.toInt()}%)",
                    color = BullishGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "⚪ ${analysis.unchanged} Unc",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "🔴 ${analysis.declines} Dec (${analysis.declinePct.toInt()}%)",
                    color = BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation & Interpretation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(analysis.bias.bgColor.copy(alpha = 0.5f))
                    .border(1.dp, analysis.bias.color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = analysis.bias.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SentimentArcGauge(
    score: Float, // 0..100
    biasColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 240.dp, height = 115.dp)) {
        val strokeWidth = 14.dp.toPx()
        val arcPadding = strokeWidth / 2 + 4.dp.toPx()
        val arcWidth = size.width - arcPadding * 2
        val arcHeight = (size.height * 2) - arcPadding * 2

        val topLeft = Offset(arcPadding, arcPadding)
        val arcSize = Size(arcWidth, arcHeight)

        // Draw Background Gauge Arc
        drawArc(
            color = Color(0xFF1E2D4A),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw Multi-color Gradient Segmented Arc
        // Map 0 -> 180 deg, 100 -> 360 deg
        val sweep = (score / 100f) * 180f

        val gradientBrush = Brush.sweepGradient(
            colors = listOf(
                BearishRed,
                Color(0xFFFF7043),
                AccentGold,
                Color(0xFF69F0AE),
                BullishGreen
            ),
            center = Offset(size.width / 2, size.height)
        )

        drawArc(
            brush = gradientBrush,
            startAngle = 180f,
            sweepAngle = sweep.coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw Pointer Needle Indicator
        val angleRad = (180f + sweep) * (PI / 180.0)
        val centerX = size.width / 2
        val centerY = size.height - 4.dp.toPx()
        val needleLength = (arcWidth / 2) - 10.dp.toPx()

        val needleEndX = (centerX + needleLength * cos(angleRad)).toFloat()
        val needleEndY = (centerY + needleLength * sin(angleRad)).toFloat()

        // Pointer Line
        drawLine(
            color = Color.White,
            start = Offset(centerX, centerY),
            end = Offset(needleEndX, needleEndY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Pointer Hub Circles
        drawCircle(
            color = biasColor,
            radius = 7.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}
