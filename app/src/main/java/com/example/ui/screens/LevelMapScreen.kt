package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.LocalSoundEffects
import com.example.model.ALL_WORLDS
import com.example.model.AppLanguage
import com.example.model.GameProgress
import com.example.model.LocalizedStrings
import com.example.ui.components.GlossyCandyButton
import com.example.ui.components.RainbowProgressBar
import com.example.ui.components.TopResourceBar
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyGreenDark
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.LockedGray
import com.example.ui.theme.LockedGrayDark
import com.example.ui.theme.SurfaceDarkPurple

@Composable
fun LevelMapScreen(
    progress: GameProgress,
    onSelectLevel: (Int) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val sfx = LocalSoundEffects.current
    val currentWorldIndex = ((progress.unlockedLevel - 1) / 100).coerceIn(0, 9)
    var selectedWorldIndex by remember(currentWorldIndex) { mutableIntStateOf(currentWorldIndex) }
    val world = ALL_WORLDS[selectedWorldIndex]
    val levelsInWorld = remember(world) { (world.startLevel..world.endLevel).toList() }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedWorldIndex, progress.unlockedLevel) {
        if (progress.unlockedLevel in world.startLevel..world.endLevel) {
            val offsetIdx = (progress.unlockedLevel - world.startLevel - 2).coerceAtLeast(0)
            listState.scrollToItem(offsetIdx)
        } else {
            listState.scrollToItem(0)
        }
    }

    val completedCount = progress.completedLevels.size
    val overallPercent = ((completedCount.coerceAtLeast(progress.unlockedLevel - 1) * 100f) / 1000f)
        .toInt()
        .coerceIn(0, 100)

    Column(
        modifier = modifier.fillMaxSize(),
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
                .widthIn(max = 560.dp)
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // World Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_candy_meadow),
                    contentDescription = world.nameEn,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SurfaceDarkPurple.copy(alpha = 0.55f),
                                    SurfaceDarkPurple.copy(alpha = 0.78f)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.EN) world.nameEn else world.nameFr,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = Color.White
                        )
                        Text(
                            text = LocalizedStrings.levelProgressSummary(
                                lang = lang,
                                current = progress.unlockedLevel,
                                completed = completedCount,
                                percent = overallPercent
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        RainbowProgressBar(
                            progress = (completedCount.coerceAtLeast(progress.unlockedLevel - 1)) / 1000f,
                            height = 10.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // World Tabs (Worlds 1..10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALL_WORLDS.forEach { w ->
                    val isSelected = w.worldIndex == selectedWorldIndex
                    val isWorldUnlocked = progress.unlockedLevel >= w.startLevel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isSelected -> Brush.horizontalGradient(listOf(CandyOrange, CandyPinkRed))
                                    isWorldUnlocked -> Brush.horizontalGradient(
                                        listOf(SurfaceDarkPurple.copy(alpha = 0.75f), SurfaceDarkPurple.copy(alpha = 0.75f))
                                    )
                                    else -> Brush.horizontalGradient(
                                        listOf(LockedGrayDark.copy(alpha = 0.7f), LockedGrayDark.copy(alpha = 0.7f))
                                    )
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                sfx?.playClick()
                                selectedWorldIndex = w.worldIndex
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${if (lang == AppLanguage.EN) "W" else "M"}${w.worldIndex + 1} (${w.startLevel}-${w.endLevel})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Winding Candy Path of Level Nodes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(levelsInWorld, key = { _, lvl -> lvl }) { index, levelNum ->
                    val isCompleted = levelNum in progress.completedLevels
                    val isCurrent = levelNum == progress.unlockedLevel
                    val isUnlocked = levelNum <= progress.unlockedLevel
                    val stars = progress.starsByLevel[levelNum] ?: 0
                    val nextLevelExists = index < levelsInWorld.lastIndex

                    WindingLevelRow(
                        index = index,
                        levelNumber = levelNum,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        isUnlocked = isUnlocked,
                        stars = stars,
                        lang = lang,
                        drawConnectorBelow = nextLevelExists,
                        onClick = {
                            if (isUnlocked) onSelectLevel(levelNum) else sfx?.playWrongGuess()
                        }
                    )
                }
            }

            // Bottom Play Active Level Button
            GlossyCandyButton(
                text = LocalizedStrings.playLevel(lang, progress.unlockedLevel),
                gradientColors = listOf(CandyPinkRed, CandyOrange),
                leadingIcon = Icons.Rounded.PlayArrow,
                height = 54.dp,
                onClick = { onSelectLevel(progress.unlockedLevel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                testTag = "map_play_current_button"
            )
        }
    }
}

@Composable
private fun WindingLevelRow(
    index: Int,
    levelNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    stars: Int,
    lang: AppLanguage,
    drawConnectorBelow: Boolean,
    onClick: () -> Unit
) {
    val xFractions = listOf(0.5f, 0.76f, 0.5f, 0.24f)
    val currX = xFractions[index % xFractions.size]
    val nextX = xFractions[(index + 1) % xFractions.size]

    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "current_node_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
    ) {
        if (drawConnectorBelow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = size.width * currX
                val endX = size.width * nextX
                val startY = size.height * 0.45f
                val endY = size.height * 1.45f
                val path = Path().apply {
                    moveTo(startX, startY)
                    cubicTo(
                        startX, (startY + endY) / 2f,
                        endX, (startY + endY) / 2f,
                        endX, endY
                    )
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.55f),
                    style = Stroke(
                        width = 12f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f), 0f)
                    )
                )
            }
        }

        // Position node horizontally along the winding wave
        val alignment = when {
            currX < 0.35f -> Alignment.TopStart
            currX > 0.65f -> Alignment.TopEnd
            else -> Alignment.TopCenter
        }
        val horizontalPadding = if (alignment == Alignment.TopCenter) 0.dp else 44.dp

        Column(
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = horizontalPadding, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val nodeColors = when {
                isCurrent -> listOf(CandyOrange, CandyPinkRed)
                isCompleted -> listOf(CandyGreen, CandyGreenDark)
                isUnlocked -> listOf(CandyBlue, CandyPurple)
                else -> listOf(LockedGray, LockedGrayDark)
            }

            Box(
                modifier = Modifier
                    .scale(if (isCurrent) pulseScale else 1f)
                    .size(if (isCurrent) 70.dp else 62.dp)
                    .shadow(if (isUnlocked) 8.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(nodeColors))
                    .border(
                        width = if (isCurrent) 3.5.dp else 2.5.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable(enabled = isUnlocked, onClick = onClick)
                    .testTag("level_node_$levelNumber"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isCompleted && !isCurrent) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = levelNumber.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (levelNumber >= 1000) 14.sp else 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isCurrent) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkPurple.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = LocalizedStrings.levelBadge(lang, levelNumber),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = CandyGold
                    )
                }
            } else if (stars > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { s ->
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (s < stars) CandyGold else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
