package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEffects
import com.example.data.WordGridGenerator
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.model.GameProgress
import com.example.model.GeneratedBoard
import com.example.model.GridCoord
import com.example.model.LocalizedStrings
import com.example.model.WordMatchBurst
import com.example.ui.GameplayUiState
import com.example.ui.WordSplashViewModel
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.GlossyCandyButton
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.RainbowProgressBar
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyCyan
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyGreenDark
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.RainbowColors
import com.example.ui.theme.SurfaceDarkPurple
import com.example.ui.theme.TilePalette
import com.example.ui.theme.style
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameplayScreen(
    progress: GameProgress,
    gameplay: GameplayUiState,
    onDragStart: (GridCoord) -> Unit,
    onDragMove: (GridCoord) -> Unit,
    onDragEnd: () -> Unit,
    onUseHint: () -> Unit,
    onUseWordHint: () -> Unit,
    onShuffle: () -> Unit,
    onToggleLanguage: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateLevels: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val sfx = LocalSoundEffects.current
    val levelData = gameplay.levelData
    val board = gameplay.board
    var showHintChooser by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (gameplay.isLoading || levelData == null || board == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
            return@Box
        }

        val totalWords = levelData.words.size.coerceAtLeast(1)
        val foundCount = gameplay.foundWords.size
        val pct = ((foundCount * 100f) / totalWords).toInt()
        val mins = gameplay.remainingSeconds / 60
        val secs = gameplay.remainingSeconds % 60
        val timerText = "%02d:%02d".format(mins, secs)
        val isTimeLow = gameplay.remainingSeconds <= 15

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Game Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkPurple.copy(alpha = 0.75f))
                            .testTag("gameplay_back_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = LocalizedStrings.findTheWords(lang, levelData.level),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${levelData.theme} • ${LocalizedStrings.wordsFoundRatio(lang, foundCount, totalWords, pct)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CandyYellow
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isTimeLow) CandyPinkRed.copy(alpha = 0.92f)
                                else SurfaceDarkPurple.copy(alpha = 0.8f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("gameplay_timer"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = if (isTimeLow) Color.White else CandyCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timerText,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            color = Color.White
                        )
                    }
                    LanguageTogglePill(language = lang, onToggle = onToggleLanguage)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            RainbowProgressBar(progress = foundCount.toFloat() / totalWords.toFloat(), height = 12.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable middle section for chips + grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Words to Find Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    levelData.words.forEach { word ->
                        val isFound = word in gameplay.foundWords
                        val isHighlighted = word in gameplay.highlightedWords && !isFound
                        WordChip(word = word, isFound = isFound, isHighlighted = isHighlighted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Candy Letter Grid
                WordSearchGrid(
                    board = board,
                    boardTheme = progress.activeBoardTheme,
                    selectedCoords = gameplay.selectedCoords,
                    foundCellMap = gameplay.foundCellMap,
                    hintedCoords = gameplay.hintedCoords,
                    highlightedWordCoords = gameplay.highlightedWordCoords,
                    lastMatchedBurst = gameplay.lastMatchedBurst,
                    hapticsEnabled = progress.hapticsEnabled,
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hint & Shuffle Action Row
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlossyCandyButton(
                    text = if (progress.hints > 0) {
                        LocalizedStrings.hintLeft(lang, progress.hints)
                    } else {
                        "${LocalizedStrings.hintLeft(lang, 0).substringBefore("(").trim()} • ${WordSplashViewModel.LETTER_HINT_COIN_COST}+"
                    },
                    gradientColors = listOf(CandyGold, CandyOrange),
                    leadingIcon = Icons.Rounded.Lightbulb,
                    height = 50.dp,
                    onClick = {
                        sfx?.playClick()
                        showHintChooser = true
                    },
                    modifier = Modifier.weight(1.35f),
                    testTag = "hint_button"
                )
                GlossyCandyButton(
                    text = LocalizedStrings.shuffle(lang),
                    gradientColors = listOf(CandyBlue, CandyPurple),
                    leadingIcon = Icons.Rounded.Shuffle,
                    height = 50.dp,
                    onClick = onShuffle,
                    modifier = Modifier.weight(1f),
                    testTag = "shuffle_button"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Gameplay Footer Nav (Levels / Settings)
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickFooterChip(
                    icon = Icons.Rounded.Map,
                    label = LocalizedStrings.navLevels(lang),
                    onClick = onNavigateLevels,
                    testTag = "gameplay_levels_button"
                )
                QuickFooterChip(
                    icon = Icons.Rounded.Settings,
                    label = LocalizedStrings.navSettings(lang),
                    onClick = onNavigateSettings,
                    testTag = "gameplay_settings_button"
                )
            }
        }

        // Confetti burst whenever a word is found
        ConfettiOverlay(burstTrigger = gameplay.confettiBurstId, continuous = false)

        if (showHintChooser) {
            HintChooserDialog(
                lang = lang,
                coins = progress.coins,
                hints = progress.hints,
                onDismiss = {
                    sfx?.playClick()
                    showHintChooser = false
                },
                onRevealLetter = {
                    showHintChooser = false
                    onUseHint()
                },
                onHighlightWord = {
                    showHintChooser = false
                    onUseWordHint()
                }
            )
        }
    }
}

@Composable
private fun HintChooserDialog(
    lang: AppLanguage,
    coins: Int,
    hints: Int,
    onDismiss: () -> Unit,
    onRevealLetter: () -> Unit,
    onHighlightWord: () -> Unit
) {
    val letterCostBadge = if (hints > 0) {
        LocalizedStrings.hintTokenCost(lang, hints)
    } else {
        LocalizedStrings.coinsCost(lang, WordSplashViewModel.LETTER_HINT_COIN_COST)
    }
    val canAffordLetter = hints > 0 || coins >= WordSplashViewModel.LETTER_HINT_COIN_COST
    val canAffordWord = coins >= WordSplashViewModel.WORD_HINT_COIN_COST

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("hint_chooser_dialog"),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LocalizedStrings.hintDialogTitle(lang),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDarkPurple)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = CandyGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = coins.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HintOptionCard(
                    icon = Icons.Rounded.Visibility,
                    title = LocalizedStrings.revealLetterTitle(lang),
                    subtitle = LocalizedStrings.revealLetterDesc(lang),
                    costLabel = letterCostBadge,
                    gradient = listOf(CandyGold, CandyOrange),
                    enabled = canAffordLetter,
                    onClick = onRevealLetter,
                    testTag = "hint_option_reveal_letter"
                )
                HintOptionCard(
                    icon = Icons.Rounded.AutoFixHigh,
                    title = LocalizedStrings.highlightWordTitle(lang),
                    subtitle = LocalizedStrings.highlightWordDesc(lang),
                    costLabel = LocalizedStrings.coinsCost(lang, WordSplashViewModel.WORD_HINT_COIN_COST),
                    gradient = listOf(CandyPurple, CandyPinkRed),
                    enabled = canAffordWord,
                    onClick = onHighlightWord,
                    testTag = "hint_option_highlight_word"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("hint_dialog_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(LocalizedStrings.cancel(lang))
            }
        }
    )
}

@Composable
private fun HintOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    costLabel: String,
    gradient: List<Color>,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (enabled) 5.dp else 1.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Color(0xFFFDF8FF) else Color(0xFFF2EFF7))
            .border(
                width = 2.dp,
                color = if (enabled) gradient.last().copy(alpha = 0.7f) else Color.LightGray,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B5A8E)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(gradient)
                    else Brush.horizontalGradient(listOf(Color(0xFF9E98B3), Color(0xFF7D7694)))
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = costLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun WordChip(word: String, isFound: Boolean, isHighlighted: Boolean) {
    val bgColors = when {
        isFound -> listOf(CandyGreenDark, Color(0xFF0E9F4A))
        isHighlighted -> listOf(CandyGold, CandyOrange)
        else -> listOf(CandyOrange, CandyPinkRed)
    }
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(bgColors))
            .border(
                width = if (isHighlighted) 2.5.dp else 1.5.dp,
                color = if (isHighlighted) CandyYellow else Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("word_chip_$word"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = when {
                isFound -> Icons.Rounded.CheckCircle
                isHighlighted -> Icons.Rounded.AutoFixHigh
                else -> Icons.Rounded.Search
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = word,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = Color.White
        )
    }
}

@Composable
private fun WordSearchGrid(
    board: GeneratedBoard,
    boardTheme: BoardTheme,
    selectedCoords: List<GridCoord>,
    foundCellMap: Map<GridCoord, Int>,
    hintedCoords: Set<GridCoord>,
    highlightedWordCoords: Set<GridCoord>,
    lastMatchedBurst: WordMatchBurst?,
    hapticsEnabled: Boolean,
    onDragStart: (GridCoord) -> Unit,
    onDragMove: (GridCoord) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val gridSize = board.gridSize
    val themeStyle = remember(boardTheme) { boardTheme.style() }
    val themePalette = themeStyle.tilePalette
    val tileRadius = if (gridSize <= 8) themeStyle.tileCornerRadiusDp.dp else (themeStyle.tileCornerRadiusDp - 3).coerceAtLeast(4).dp

    val infiniteTransition = rememberInfiniteTransition(label = "hint_glow")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_pulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(themeStyle.boardFrameColor)
            .border(3.dp, themeStyle.boardBorderColor, RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("word_search_grid")
    ) {
        fun offsetToCoord(pos: Offset, widthPx: Float, heightPx: Float): GridCoord? {
            if (widthPx <= 0f || heightPx <= 0f) return null
            val cellW = widthPx / gridSize.toFloat()
            val cellH = heightPx / gridSize.toFloat()
            val col = (pos.x / cellW).toInt().coerceIn(0, gridSize - 1)
            val row = (pos.y / cellH).toInt().coerceIn(0, gridSize - 1)
            return GridCoord(row, col)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val start = offsetToCoord(offset, size.width.toFloat(), size.height.toFloat())
                            if (start != null) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onDragStart(start)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val current = offsetToCoord(change.position, size.width.toFloat(), size.height.toFloat())
                                ?: return@detectDragGestures
                            onDragMove(current)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        }
                    )
                }
        ) {
            val cellSpacing = if (gridSize <= 6) 5.dp else if (gridSize <= 8) 4.dp else 2.5.dp
            val fontSize = when {
                gridSize <= 6 -> 22.sp
                gridSize <= 8 -> 17.sp
                gridSize <= 10 -> 14.sp
                else -> 11.5.sp
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                for (r in 0 until gridSize) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                    ) {
                        for (c in 0 until gridSize) {
                            val coord = GridCoord(r, c)
                            val isSelected = coord in selectedCoords
                            val foundColorIdx = foundCellMap[coord]
                            val isFound = foundColorIdx != null
                            val isWordHighlighted = coord in highlightedWordCoords && !isFound
                            val isHinted = (coord in hintedCoords && !isFound) || isWordHighlighted

                            val baseColor = when {
                                isSelected -> CandyBlue
                                isFound -> CandyGreenDark
                                isWordHighlighted -> CandyGold
                                else -> themePalette[board.colorIndices[r][c] % themePalette.size]
                            }

                            val borderColor = when {
                                isSelected -> CandyYellow
                                isWordHighlighted -> Color.White.copy(alpha = hintAlpha)
                                isHinted -> CandyYellow.copy(alpha = hintAlpha)
                                isFound -> Color.White
                                else -> Color.White.copy(alpha = 0.45f)
                            }

                            val borderWidth = if (isSelected || isHinted) 2.5.dp else 1.2.dp
                            val isJustMatched = lastMatchedBurst != null && coord in lastMatchedBurst.coords
                            val popScale = remember(coord) { Animatable(1f) }
                            LaunchedEffect(lastMatchedBurst?.id, isJustMatched) {
                                if (isJustMatched) {
                                    popScale.snapTo(1f)
                                    popScale.animateTo(
                                        targetValue = 1.18f,
                                        animationSpec = tween(durationMillis = 120)
                                    )
                                    popScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .scale(if (isSelected) 1.05f else popScale.value)
                                    .clip(RoundedCornerShape(tileRadius))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(baseColor.copy(alpha = 0.92f), baseColor)
                                        )
                                    )
                                    .border(
                                        width = borderWidth,
                                        color = borderColor,
                                        shape = RoundedCornerShape(tileRadius)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = board.letters[r][c].toString(),
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = fontSize,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Connecting drag line overlay
            if (selectedCoords.size >= 2) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepW = size.width / gridSize.toFloat()
                    val stepH = size.height / gridSize.toFloat()
                    val first = selectedCoords.first()
                    val last = selectedCoords.last()
                    val startPt = Offset((first.col + 0.5f) * stepW, (first.row + 0.5f) * stepH)
                    val endPt = Offset((last.col + 0.5f) * stepW, (last.row + 0.5f) * stepH)
                    drawLine(
                        color = CandyYellow.copy(alpha = 0.48f),
                        start = startPt,
                        end = endPt,
                        strokeWidth = stepW * 0.42f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Per-cell radial particle burst + shockwave flash over matched word tiles
            GridWordParticleOverlay(
                gridSize = gridSize,
                burst = lastMatchedBurst,
                themePalette = themePalette,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private enum class GridSparkShape {
    STAR,
    CIRCLE,
    DIAMOND
}

private class GridSparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationDeg: Float,
    var angularVel: Float,
    val radius: Float,
    val color: Color,
    val shape: GridSparkShape,
    var age: Float = 0f,
    val lifetime: Float = 0.9f
)

private class GridWaveRing(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val color: Color,
    var age: Float = 0f,
    val lifetime: Float = 0.55f
)

private class GridSweepBeam(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    var age: Float = 0f,
    val lifetime: Float = 0.45f
)

@Composable
private fun GridWordParticleOverlay(
    gridSize: Int,
    burst: WordMatchBurst?,
    themePalette: List<Color>,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastSpawnedBurstId by remember { mutableStateOf(0) }
    var hasActiveEffects by remember { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }

    val particles = remember { mutableListOf<GridSparkParticle>() }
    val rings = remember { mutableListOf<GridWaveRing>() }
    val beams = remember { mutableListOf<GridSweepBeam>() }

    LaunchedEffect(burst?.id, canvasSize) {
        val currentBurst = burst ?: return@LaunchedEffect
        if (currentBurst.id <= 0 || currentBurst.id == lastSpawnedBurstId) return@LaunchedEffect
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect

        lastSpawnedBurstId = currentBurst.id
        val stepW = canvasSize.width.toFloat() / gridSize.toFloat()
        val stepH = canvasSize.height.toFloat() / gridSize.toFloat()
        val cellMin = minOf(stepW, stepH)
        val rnd = Random(currentBurst.id * 977 + currentBurst.word.hashCode())
        val sparkColors = (themePalette + RainbowColors + listOf(Color.White, CandyGold)).distinct()

        if (currentBurst.coords.size >= 2) {
            val first = currentBurst.coords.first()
            val last = currentBurst.coords.last()
            beams.add(
                GridSweepBeam(
                    start = Offset((first.col + 0.5f) * stepW, (first.row + 0.5f) * stepH),
                    end = Offset((last.col + 0.5f) * stepW, (last.row + 0.5f) * stepH),
                    strokeWidth = cellMin * 0.58f
                )
            )
        }

        val sparksPerCell = if (currentBurst.coords.size <= 5) 14 else 10
        currentBurst.coords.forEachIndexed { idx, coord ->
            val cx = (coord.col + 0.5f) * stepW
            val cy = (coord.row + 0.5f) * stepH

            rings.add(
                GridWaveRing(
                    x = cx,
                    y = cy,
                    maxRadius = cellMin * 0.92f,
                    color = if (idx % 2 == 0) CandyYellow else Color.White,
                    age = -idx * 0.03f
                )
            )

            repeat(sparksPerCell) { pIdx ->
                val baseAngle = (pIdx.toFloat() / sparksPerCell) * 6.283185f + (rnd.nextFloat() - 0.5f) * 0.45f
                val speed = cellMin * (1.7f + rnd.nextFloat() * 2.6f)
                particles.add(
                    GridSparkParticle(
                        x = cx + (rnd.nextFloat() - 0.5f) * stepW * 0.25f,
                        y = cy + (rnd.nextFloat() - 0.5f) * stepH * 0.25f,
                        vx = cos(baseAngle) * speed,
                        vy = sin(baseAngle) * speed - cellMin * 0.4f,
                        rotationDeg = rnd.nextFloat() * 360f,
                        angularVel = (rnd.nextFloat() - 0.5f) * 520f,
                        radius = (cellMin * 0.11f + rnd.nextFloat() * cellMin * 0.13f).coerceIn(5f, 16f),
                        color = sparkColors[rnd.nextInt(sparkColors.size)],
                        shape = GridSparkShape.entries[rnd.nextInt(GridSparkShape.entries.size)],
                        age = -idx * 0.025f,
                        lifetime = 0.68f + rnd.nextFloat() * 0.34f
                    )
                )
            }
        }
        hasActiveEffects = true
    }

    LaunchedEffect(hasActiveEffects) {
        if (!hasActiveEffects) return@LaunchedEffect
        var lastNanos = 0L
        val gravity = 280f
        val drag = 0.955f

        while (particles.isNotEmpty() || rings.isNotEmpty() || beams.isNotEmpty()) {
            val now = withFrameNanos { it }
            if (lastNanos == 0L) {
                lastNanos = now
                continue
            }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
            lastNanos = now

            val pIter = particles.listIterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.age += dt
                if (p.age >= 0f) {
                    p.vx *= drag
                    p.vy = p.vy * drag + gravity * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.rotationDeg += p.angularVel * dt
                }
                if (p.age >= p.lifetime) {
                    pIter.remove()
                }
            }

            val rIter = rings.listIterator()
            while (rIter.hasNext()) {
                val r = rIter.next()
                r.age += dt
                if (r.age >= r.lifetime) {
                    rIter.remove()
                }
            }

            val bIter = beams.listIterator()
            while (bIter.hasNext()) {
                val b = bIter.next()
                b.age += dt
                if (b.age >= b.lifetime) {
                    bIter.remove()
                }
            }

            frameTick = now
        }
        hasActiveEffects = false
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .then(
                if (hasActiveEffects || (burst != null && burst.id > 0)) {
                    Modifier.testTag("grid_particle_canvas")
                } else {
                    Modifier
                }
            )
    ) {
        val tickRead = frameTick
        if (tickRead >= 0L) {
            beams.forEach { b ->
                if (b.age >= 0f) {
                    val t = (b.age / b.lifetime).coerceIn(0f, 1f)
                    val alpha = (1f - t) * 0.55f
                    drawLine(
                        color = CandyYellow.copy(alpha = alpha),
                        start = b.start,
                        end = b.end,
                        strokeWidth = b.strokeWidth * (1f + t * 0.35f),
                        cap = StrokeCap.Round
                    )
                }
            }

            rings.forEach { r ->
                if (r.age >= 0f) {
                    val t = (r.age / r.lifetime).coerceIn(0f, 1f)
                    val eased = 1f - (1f - t) * (1f - t)
                    val radius = r.maxRadius * (0.25f + 0.75f * eased)
                    val alpha = (1f - t).coerceIn(0f, 1f) * 0.85f
                    drawCircle(
                        color = r.color.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(r.x, r.y),
                        style = Stroke(width = (5f * (1f - t * 0.6f)).coerceAtLeast(1.5f))
                    )
                }
            }

            particles.forEach { p ->
                if (p.age >= 0f) {
                    val t = (p.age / p.lifetime).coerceIn(0f, 1f)
                    val alpha = if (t < 0.25f) 1f else (1f - (t - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    val scaleFactor = if (t < 0.15f) 0.5f + (t / 0.15f) * 0.65f else (1.15f - t * 0.55f).coerceAtLeast(0.25f)
                    val drawColor = p.color.copy(alpha = alpha)
                    val r = p.radius * scaleFactor

                    rotate(degrees = p.rotationDeg, pivot = Offset(p.x, p.y)) {
                        when (p.shape) {
                            GridSparkShape.CIRCLE -> {
                                drawCircle(
                                    color = drawColor,
                                    radius = r * 0.72f,
                                    center = Offset(p.x, p.y)
                                )
                            }
                            GridSparkShape.STAR -> {
                                val rInner = r * 0.42f
                                val starPath = Path().apply {
                                    moveTo(p.x, p.y - r)
                                    lineTo(p.x + rInner, p.y - rInner)
                                    lineTo(p.x + r, p.y)
                                    lineTo(p.x + rInner, p.y + rInner)
                                    lineTo(p.x, p.y + r)
                                    lineTo(p.x - rInner, p.y + rInner)
                                    lineTo(p.x - r, p.y)
                                    lineTo(p.x - rInner, p.y - rInner)
                                    close()
                                }
                                drawPath(path = starPath, color = drawColor)
                            }
                            GridSparkShape.DIAMOND -> {
                                val diamondPath = Path().apply {
                                    moveTo(p.x, p.y - r)
                                    lineTo(p.x + r * 0.65f, p.y)
                                    lineTo(p.x, p.y + r)
                                    lineTo(p.x - r * 0.65f, p.y)
                                    close()
                                }
                                drawPath(path = diamondPath, color = drawColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFooterChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDarkPurple.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}
