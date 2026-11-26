package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.LocalizedStrings
import com.example.model.LossSummary
import com.example.ui.components.GlossyCandyButton
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.SurfaceDarkPurple

@Composable
fun LossScreen(
    summary: LossSummary,
    coins: Int,
    language: AppLanguage,
    isRewardedAdReady: Boolean,
    onWatchAdExtraTime: () -> Unit,
    onExtraTime: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDarkPurple.copy(alpha = 0.82f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CandyPinkRed, CandyOrange)))
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassEmpty,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = LocalizedStrings.timesUp(language),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 40.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        offset = Offset(0f, 5f),
                        blurRadius = 10f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(3.dp, CandyPinkRed, RoundedCornerShape(28.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalizedStrings.levelFailed(language, summary.level),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SurfaceDarkPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = LocalizedStrings.foundOutOf(language, summary.wordsFound, summary.totalWords),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CandyPurple,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isRewardedAdReady) {
                    GlossyCandyButton(
                        text = LocalizedStrings.watchAdExtraTime(language),
                        gradientColors = listOf(Color(0xFF18B858), CandyBlue),
                        leadingIcon = Icons.Rounded.PlayCircle,
                        height = 56.dp,
                        onClick = onWatchAdExtraTime,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "loss_watch_ad_extra_time_button"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                GlossyCandyButton(
                    text = LocalizedStrings.extraTime(language),
                    gradientColors = listOf(CandyBlue, CandyPurple),
                    leadingIcon = Icons.Rounded.MoreTime,
                    height = 56.dp,
                    enabled = coins >= 30,
                    onClick = onExtraTime,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "loss_extra_time_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlossyCandyButton(
                    text = LocalizedStrings.tryAgain(language),
                    gradientColors = listOf(CandyOrange, CandyPinkRed),
                    leadingIcon = Icons.Rounded.Refresh,
                    height = 56.dp,
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "loss_retry_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(2.dp, CandyPurple, RoundedCornerShape(25.dp))
                        .clickable(onClick = onHome)
                        .testTag("loss_home_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = CandyPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LocalizedStrings.navHome(language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CandyPurple
                    )
                }
            }
        }
    }
}
