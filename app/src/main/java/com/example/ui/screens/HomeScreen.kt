package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameProgress
import com.example.model.LocalizedStrings
import com.example.ui.components.GlossyCandyButton
import com.example.ui.components.TopResourceBar
import com.example.ui.components.WordSplashLogoTitle
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SurfaceDarkPurple

@Composable
fun HomeScreen(
    progress: GameProgress,
    onPlayClick: () -> Unit,
    onDailyRewardClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val infiniteTransition = rememberInfiniteTransition(label = "home_bob")
    val bobY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bob"
    )
    val playPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_pulse"
    )

    val cooldownMs = 24L * 60L * 60L * 1000L
    val elapsedSinceDaily = System.currentTimeMillis() - progress.lastDailyRewardMs
    val canClaimDaily = elapsedSinceDaily >= cooldownMs
    val hoursRemaining = (((cooldownMs - elapsedSinceDaily).coerceAtLeast(0L)) / 3600000L).toInt() + 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopResourceBar(
            coins = progress.coins,
            hints = progress.hints,
            language = lang,
            onToggleLanguage = onToggleLanguage
        )

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            WordSplashLogoTitle()
            Spacer(modifier = Modifier.height(14.dp))

            // Mascot Hero Card: Cute boy holding yellow star
            Box(
                modifier = Modifier
                    .offset(y = bobY.dp)
                    .size(210.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CandyYellow.copy(alpha = 0.7f), CandyPinkRed.copy(alpha = 0.4f))
                        )
                    )
                    .border(5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mascot_boy),
                    contentDescription = "Word Candy mascot boy holding a golden star",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1000+ Levels Ready Pill
            Row(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceDarkPurple.copy(alpha = 0.85f))
                    .border(1.5.dp, CandyYellow, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = CandyGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = LocalizedStrings.levelsReady(lang),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Big Glowing PLAY Button
            GlossyCandyButton(
                text = "${LocalizedStrings.play(lang)}  •  ${LocalizedStrings.levelBadge(lang, progress.unlockedLevel)}",
                gradientColors = listOf(CandyOrange, CandyPinkRed),
                leadingIcon = Icons.Rounded.PlayArrow,
                height = 64.dp,
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(playPulse),
                testTag = "home_play_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Reward Button
            GlossyCandyButton(
                text = if (canClaimDaily) {
                    LocalizedStrings.dailyReward(lang)
                } else {
                    LocalizedStrings.dailyClaimed(lang, hoursRemaining)
                },
                gradientColors = listOf(CandyPurple, Color(0xFF7B2FF7)),
                leadingIcon = Icons.Rounded.CardGiftcard,
                height = 54.dp,
                enabled = canClaimDaily,
                onClick = onDailyRewardClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "daily_reward_button"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Progress Snapshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnapshotStat(
                    label = LocalizedStrings.statsStars(lang),
                    value = progress.starsByLevel.values.sum().toString()
                )
                SnapshotStat(
                    label = LocalizedStrings.statsWords(lang),
                    value = progress.totalWordsFound.toString()
                )
                SnapshotStat(
                    label = LocalizedStrings.statsPoints(lang),
                    value = "%,d".format(progress.totalPoints)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SnapshotStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
