package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ALL_WORLDS
import com.example.model.AppLanguage
import com.example.model.GameProgress
import com.example.model.LocalizedStrings
import com.example.ui.components.RainbowProgressBar
import com.example.ui.components.TopResourceBar
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyGreenDark
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.LockedGray
import com.example.ui.theme.SurfaceDarkPurple

@Composable
fun CollectionsScreen(
    progress: GameProgress,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val totalStars = progress.starsByLevel.values.sum()

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
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = LocalizedStrings.collectionsTitle(lang),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )

            // Summary Stats Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TrophyStat(
                    value = totalStars.toString(),
                    label = LocalizedStrings.statsStars(lang),
                    color = CandyGold
                )
                TrophyStat(
                    value = progress.totalWordsFound.toString(),
                    label = LocalizedStrings.statsWords(lang),
                    color = CandyPinkRed
                )
                TrophyStat(
                    value = "%,d".format(progress.totalPoints),
                    label = LocalizedStrings.statsPoints(lang),
                    color = CandyPurple
                )
            }

            // World Trophies (10 Worlds)
            ALL_WORLDS.forEach { world ->
                val completedInWorld = progress.completedLevels.count {
                    it in world.startLevel..world.endLevel
                }
                val isCompletedWorld = completedInWorld >= 100
                val isUnlockedWorld = progress.unlockedLevel >= world.startLevel

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = if (isUnlockedWorld) 0.96f else 0.72f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    when {
                                        isCompletedWorld -> listOf(CandyGold, CandyOrange)
                                        isUnlockedWorld -> listOf(CandyGreenDark, CandyBlue)
                                        else -> listOf(LockedGray, Color(0xFF938BA8))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlockedWorld) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.EN) world.nameEn else world.nameFr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SurfaceDarkPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RainbowProgressBar(
                            progress = completedInWorld / 100f,
                            height = 8.dp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$completedInWorld / 100",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF6B5A8E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrophyStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = SurfaceDarkPurple,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6B5A8E),
            textAlign = TextAlign.Center
        )
    }
}
