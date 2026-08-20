package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.NeutralBlueBg
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class NseMarketSession(
    val title: String,
    val shortCode: String,
    val timing: String,
    val trendContext: String
) {
    PRE_OPEN(
        title = "NSE Pre-Open Session",
        shortCode = "PRE-OPEN",
        timing = "09:00 - 09:15 IST",
        trendContext = "Price discovery phase; order matching in progress. Trend signals may adjust rapidly upon normal open."
    ),
    OPEN(
        title = "NSE Regular Trading Open",
        shortCode = "OPEN",
        timing = "09:15 - 15:30 IST",
        trendContext = "Continuous live market auction active. Intraday price action, volume surges, and breakout trends are live."
    ),
    POST_CLOSE(
        title = "NSE Post-Market Session",
        shortCode = "POST-CLOSE",
        timing = "15:30 - 16:00 IST",
        trendContext = "Closing session active. Final weighted average settlement prices being finalized."
    ),
    CLOSED(
        title = "NSE Market Closed",
        shortCode = "CLOSED",
        timing = "16:00 - 09:00 IST",
        trendContext = "Market is closed. Trend charts display finalized historical & end-of-day closing session data."
    )
}

data class NseMarketStatusInfo(
    val session: NseMarketSession,
    val istTimeFormatted: String,
    val sessionDetail: String,
    val nextEventCountdown: String,
    val isLiveAuctionActive: Boolean,
    val isWeekend: Boolean,
    val primaryColor: Color,
    val backgroundColor: Color
)

object NseMarketTimeUtils {

    fun determineMarketStatus(timestampStr: String? = null): NseMarketStatusInfo {
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istTimeZone)

        // If a valid date/time string is passed, try parsing it
        if (!timestampStr.isNullOrBlank()) {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()),
                SimpleDateFormat("hh:mm:ss a", Locale.getDefault()),
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            )
            for (fmt in formats) {
                fmt.timeZone = istTimeZone
                try {
                    val parsed = fmt.parse(timestampStr)
                    if (parsed != null) {
                        val parsedCal = Calendar.getInstance(istTimeZone)
                        parsedCal.time = parsed
                        if (parsedCal.get(Calendar.YEAR) > 1970) {
                            calendar.time = parsed
                        } else {
                            // Only time was supplied
                            calendar.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                            calendar.set(Calendar.SECOND, parsedCal.get(Calendar.SECOND))
                        }
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = hour * 60 + minute

        val timeFormat = SimpleDateFormat("hh:mm a 'IST'", Locale.getDefault())
        timeFormat.timeZone = istTimeZone
        val istFormatted = timeFormat.format(calendar.time)

        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        if (isWeekend) {
            val daysUntilMon = if (dayOfWeek == Calendar.SATURDAY) 2 else 1
            return NseMarketStatusInfo(
                session = NseMarketSession.CLOSED,
                istTimeFormatted = istFormatted,
                sessionDetail = "Weekend Market Holiday",
                nextEventCountdown = "Opens Monday at 09:00 AM IST",
                isLiveAuctionActive = false,
                isWeekend = true,
                primaryColor = NeutralBlue,
                backgroundColor = NeutralBlueBg
            )
        }

        // Weekday timings in minutes:
        // 09:00 = 540 min
        // 09:15 = 555 min
        // 15:30 = 930 min
        // 16:00 = 960 min
        return when {
            currentMinutesOfDay in 540 until 555 -> {
                val minsLeft = 555 - currentMinutesOfDay
                NseMarketStatusInfo(
                    session = NseMarketSession.PRE_OPEN,
                    istTimeFormatted = istFormatted,
                    sessionDetail = "Order matching & discovery",
                    nextEventCountdown = "Regular trading starts in ${minsLeft}m",
                    isLiveAuctionActive = true,
                    isWeekend = false,
                    primaryColor = AccentGold,
                    backgroundColor = AccentGold.copy(alpha = 0.15f)
                )
            }
            currentMinutesOfDay in 555 until 930 -> {
                val minsLeft = 930 - currentMinutesOfDay
                val hoursLeft = minsLeft / 60
                val remMins = minsLeft % 60
                val timeLeftStr = if (hoursLeft > 0) "${hoursLeft}h ${remMins}m" else "${remMins}m"
                NseMarketStatusInfo(
                    session = NseMarketSession.OPEN,
                    istTimeFormatted = istFormatted,
                    sessionDetail = "Live continuous auction",
                    nextEventCountdown = "Closes in $timeLeftStr",
                    isLiveAuctionActive = true,
                    isWeekend = false,
                    primaryColor = BullishGreen,
                    backgroundColor = BullishGreenBg
                )
            }
            currentMinutesOfDay in 930 until 960 -> {
                val minsLeft = 960 - currentMinutesOfDay
                NseMarketStatusInfo(
                    session = NseMarketSession.POST_CLOSE,
                    istTimeFormatted = istFormatted,
                    sessionDetail = "Closing price settlement",
                    nextEventCountdown = "Session ends in ${minsLeft}m",
                    isLiveAuctionActive = false,
                    isWeekend = false,
                    primaryColor = PrimaryCyan,
                    backgroundColor = PrimaryCyan.copy(alpha = 0.15f)
                )
            }
            else -> {
                val isNextDay = currentMinutesOfDay >= 960
                val nextOpenStr = if (dayOfWeek == Calendar.FRIDAY && isNextDay) "Opens Monday 09:00 AM IST" else "Opens today/tomorrow 09:00 AM IST"
                NseMarketStatusInfo(
                    session = NseMarketSession.CLOSED,
                    istTimeFormatted = istFormatted,
                    sessionDetail = "Session ended",
                    nextEventCountdown = nextOpenStr,
                    isLiveAuctionActive = false,
                    isWeekend = false,
                    primaryColor = BearishRed,
                    backgroundColor = BearishRedBg
                )
            }
        }
    }
}

/**
 * Compact market status badge, suitable for placing next to titles, header bars, or chart legends.
 */
@Composable
fun NseMarketStatusBadge(
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    showTime: Boolean = true
) {
    val statusInfo = remember(timestamp) {
        NseMarketTimeUtils.determineMarketStatus(timestamp)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusInfo.backgroundColor)
            .border(1.dp, statusInfo.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("nse_market_status_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (statusInfo.isLiveAuctionActive) statusInfo.primaryColor.copy(alpha = pulseAlpha) else statusInfo.primaryColor,
                        CircleShape
                    )
            )
            Text(
                text = "NSE: ${statusInfo.session.shortCode}",
                color = statusInfo.primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (showTime) {
                Text(
                    text = "• ${statusInfo.istTimeFormatted}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Rich contextual market status card tailored to give clear context for stock trend analysis and data interpretations.
 */
@Composable
fun NseMarketStatusCard(
    modifier: Modifier = Modifier,
    lastFetchedTimestamp: String? = null,
    onRefreshRequested: (() -> Unit)? = null
) {
    val statusInfo = remember(lastFetchedTimestamp) {
        NseMarketTimeUtils.determineMarketStatus(lastFetchedTimestamp)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseCard")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseCardAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nse_market_status_card"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (statusInfo.isLiveAuctionActive) statusInfo.primaryColor.copy(alpha = 0.5f) else NavyCardBorder
            )
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status Badge & IST Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (statusInfo.isLiveAuctionActive) statusInfo.primaryColor.copy(alpha = pulseAlpha) else statusInfo.primaryColor,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusInfo.session.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Session Status Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusInfo.backgroundColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusInfo.session.shortCode,
                        color = statusInfo.primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Context Bar / Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Session Timing",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusInfo.session.timing,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Event Countdown",
                        tint = statusInfo.primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusInfo.nextEventCountdown,
                        color = statusInfo.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trend Analysis Context Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusInfo.backgroundColor.copy(alpha = 0.5f))
                    .border(1.dp, statusInfo.primaryColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Trend Analysis Context",
                    tint = statusInfo.primaryColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Trend Analysis Context",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusInfo.session.trendContext,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (!lastFetchedTimestamp.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last data synchronization: $lastFetchedTimestamp",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
