package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.model.WinSummary
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.GlossyCandyButton
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.SurfaceDarkPurple
import kotlinx.coroutines.delay

@Composable
fun WinScreen(
    summary: WinSummary,
    language: AppLanguage,
    onNextLevel: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starScales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(summary.level) {
        starScales.forEachIndexed { idx, anim ->
            delay(idx * 160L)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDarkPurple.copy(alpha = 0.78f))
    ) {
        ConfettiOverlay(burstTrigger = summary.level, continuous = true)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = LocalizedStrings.youWin(language),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 46.sp,
                    brush = Brush.linearGradient(listOf(CandyYellow, CandyOrange, CandyPinkRed)),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        offset = Offset(0f, 5f),
                        blurRadius = 10f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Animated Golden Stars
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 3) {
                    val earned = i < summary.starsEarned
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (earned) CandyGold else Color.White.copy(alpha = 0.28f),
                        modifier = Modifier
                            .size(if (i == 1) 68.dp else 56.dp)
                            .scale(starScales[i].value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Celebration Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(3.dp, CandyYellow, RoundedCornerShape(28.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalizedStrings.levelComplete(language, summary.level),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SurfaceDarkPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = LocalizedStrings.amazingSummary(
                        language,
                        summary.wordsFound,
                        summary.totalWords,
                        summary.elapsedSeconds
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CandyPurple,
                    textAlign = TextAlign.Center
                )
                if (summary.newlyUnlockedLevel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = LocalizedStrings.levelUnlocked(language, summary.newlyUnlockedLevel),
                        style = MaterialTheme.typography.labelLarge,
                        color = CandyPinkRed,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Rewards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RewardBadge(
                        icon = Icons.Rounded.MonetizationOn,
                        iconTint = CandyGold,
                        text = LocalizedStrings.rewardCoins(language, summary.coinsReward),
                        bg = Color(0xFFFFF5D6),
                        modifier = Modifier.weight(1f)
                    )
                    RewardBadge(
                        icon = Icons.Rounded.Star,
                        iconTint = CandyOrange,
                        text = LocalizedStrings.bonusPoints(language, summary.pointsBonus),
                        bg = Color(0xFFF3E8FF),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GlossyCandyButton(
                    text = LocalizedStrings.nextLevel(language),
                    gradientColors = listOf(CandyOrange, CandyPinkRed),
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    height = 58.dp,
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "win_next_level_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(2.dp, CandyPurple, RoundedCornerShape(25.dp))
                        .clickable(onClick = onHome)
                        .testTag("win_home_button"),
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

@Composable
private fun RewardBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = SurfaceDarkPurple,
            textAlign = TextAlign.Center
        )
    }
}
