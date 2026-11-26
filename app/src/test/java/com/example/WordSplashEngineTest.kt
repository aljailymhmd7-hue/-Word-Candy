package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.ads.UnityAdsManager
import com.example.ads.UnityAdsStatus
import com.example.audio.SoundEffect
import com.example.audio.SoundEffects
import com.example.data.LevelRepository
import com.example.data.WordGridGenerator
import com.example.engine.WordSplashGameEngine
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.model.GameProgress
import com.example.model.GameSessionStatus
import com.example.model.GridCoord
import com.example.model.LossReason
import com.example.model.SelectionOutcome
import com.example.model.WinSummary
import com.example.model.WordMatchBurst
import com.example.ui.GameplayUiState
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.WinScreen
import com.example.ui.theme.WordSplashTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WordSplashEngineTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `win screen renders particle confetti canvas when level completes`() {
        val summary = WinSummary(
            level = 24,
            nextLevel = 25,
            wordsFound = 5,
            totalWords = 5,
            elapsedSeconds = 32,
            starsEarned = 3,
            coinsReward = 70,
            pointsBonus = 276,
            newlyUnlockedLevel = 25
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WordSplashTheme {
                WinScreen(
                    summary = summary,
                    language = AppLanguage.EN,
                    onNextLevel = {},
                    onHome = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(600)
        composeRule.onNodeWithText("YOU WIN!").assertIsDisplayed()
        composeRule.onNodeWithTag("confetti_canvas").assertExists()
    }

    @Test
    fun `engine loads JSON level, snaps drag selection, and transitions to Won when all words found`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level24 = repo.getLevel(AppLanguage.EN, 24)

        val engine = WordSplashGameEngine()
        val initial = engine.startLevel(level24, previouslyUnlockedLevel = 24)
        val board = initial.board
        assertNotNull(board)
        assertTrue(initial.status is GameSessionStatus.Playing)

        // Find all words via simulated drag gestures (including reverse drags)
        val placements = board!!.placements
        placements.forEachIndexed { idx, placement ->
            val start = if (idx % 2 == 0) placement.coords.first() else placement.coords.last()
            val end = if (idx % 2 == 0) placement.coords.last() else placement.coords.first()

            engine.onDragStart(start)
            engine.onDragMove(end)
            val outcome = engine.onDragEnd(previouslyUnlockedLevel = 24)

            assertTrue(outcome is SelectionOutcome.WordMatched)
            val matched = outcome as SelectionOutcome.WordMatched
            assertEquals(placement.word, matched.word)
            assertEquals(idx == placements.lastIndex, matched.isLevelComplete)
        }

        val won = engine.state.status as? GameSessionStatus.Won
        assertNotNull(won)
        assertEquals(24, won!!.summary.level)
        assertEquals(25, won.summary.nextLevel)
        assertEquals(25, won.summary.newlyUnlockedLevel)
        assertEquals(3, won.summary.starsEarned)
    }

    @Test
    fun `engine rejects wrong drag, preserves words on shuffle, and triggers Loss on timeout`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level1Fr = repo.getLevel(AppLanguage.FR, 1)

        val engine = WordSplashGameEngine()
        engine.startLevel(level1Fr, previouslyUnlockedLevel = 1)

        // Single-cell drag is ignored
        engine.onDragStart(GridCoord(0, 0))
        val singleOutcome = engine.onDragEnd(previouslyUnlockedLevel = 1)
        assertTrue(singleOutcome is SelectionOutcome.Ignored)

        // Filler-only shuffle keeps every placed word intact
        engine.shuffleFillerLetters()
        val shuffledBoard = engine.state.board!!
        shuffledBoard.placements.forEach { placement ->
            val word = placement.coords.joinToString("") {
                shuffledBoard.letters[it.row][it.col].toString()
            }
            assertEquals(placement.word, word)
        }

        // Countdown to zero triggers Lost state
        val total = (engine.state.status as GameSessionStatus.Playing).totalTimeSeconds
        repeat(total) {
            engine.tickSecond()
        }
        val lost = engine.state.status as? GameSessionStatus.Lost
        assertNotNull(lost)
        assertEquals(LossReason.TIME_EXPIRED, lost!!.summary.reason)

        // Granting +30s extra time resumes Playing state
        engine.grantExtraTime(30)
        val resumed = engine.state.status as? GameSessionStatus.Playing
        assertNotNull(resumed)
        assertEquals(30, resumed!!.remainingSeconds)
    }

    @Test
    fun `computeLineCoords snaps cleanly to 8 directions`() {
        val diag = WordGridGenerator.computeLineCoords(GridCoord(1, 1), GridCoord(4, 3), 6)
        assertEquals(
            listOf(GridCoord(1, 1), GridCoord(2, 2), GridCoord(3, 3), GridCoord(4, 4)),
            diag
        )
        val horiz = WordGridGenerator.computeLineCoords(GridCoord(2, 5), GridCoord(2, 2), 6)
        assertEquals(
            listOf(GridCoord(2, 5), GridCoord(2, 4), GridCoord(2, 3), GridCoord(2, 2)),
            horiz
        )
    }

    @Test
    fun `sound effects controller triggers safely for clicks, guesses, wins, and mute state`() {
        val controller = SoundEffects()
        controller.enabled = false
        controller.playClick()
        controller.playDing()
        controller.playWinFanfare()

        controller.enabled = true
        SoundEffect.entries.forEach { effect ->
            controller.play(effect)
        }
        controller.playClick()
        controller.playPop()
        controller.playDing()
        controller.playWinFanfare()
        controller.playWrongGuess()
        controller.playTimeUp()
        controller.release()
    }

    @Test
    fun `engine reveals single letter or highlights full word and clears highlights on match`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level1 = repo.getLevel(AppLanguage.EN, 1)

        val engine = WordSplashGameEngine()
        engine.startLevel(level1, previouslyUnlockedLevel = 1)

        // Reveal a single letter hint
        val letterCoord = engine.revealLetterHint()
        assertNotNull(letterCoord)
        assertEquals(1, engine.state.hintedCoords.size)
        assertEquals(1, engine.state.hintsUsed)

        // Highlight an entire unfound word
        val highlightedWord = engine.revealWordHint()
        assertNotNull(highlightedWord)
        assertTrue(highlightedWord!! in engine.state.highlightedWords)
        val placement = engine.state.board!!.placements.first { it.word == highlightedWord }
        assertTrue(placement.coords.all { it in engine.state.highlightedWordCoords })

        // Matching the highlighted word clears its highlight and moves it into foundWords
        engine.onDragStart(placement.coords.first())
        engine.onDragMove(placement.coords.last())
        val outcome = engine.onDragEnd(previouslyUnlockedLevel = 1)
        assertTrue(outcome is SelectionOutcome.WordMatched)
        assertTrue(highlightedWord in engine.state.foundWords)
        assertTrue(highlightedWord !in engine.state.highlightedWords)
        assertTrue(placement.coords.none { it in engine.state.highlightedWordCoords })
    }

    @Test
    fun `candy shop displays all six puzzle grid themes and confirms locked theme purchase`() {
        var selectedTheme: BoardTheme? = null
        composeRule.setContent {
            WordSplashTheme {
                ShopScreen(
                    progress = GameProgress(
                        coins = 500,
                        hints = 3,
                        ownedBoardThemes = setOf(BoardTheme.CANDY_CLASSIC.id),
                        activeBoardTheme = BoardTheme.CANDY_CLASSIC,
                        language = AppLanguage.EN
                    ),
                    adsStatus = UnityAdsStatus(),
                    onWatchRewardedAd = {},
                    onBuyHints = { _, _ -> },
                    onBuyOrEquipTheme = { selectedTheme = it },
                    onToggleLanguage = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("candy_shop_banner").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 6 Grid Themes Unlocked").assertIsDisplayed()

        BoardTheme.entries.forEach { theme ->
            composeRule.onNodeWithTag("shop_theme_${theme.id}").assertExists()
        }

        composeRule.onNodeWithTag("shop_theme_berry").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_unlock_theme_dialog").assertIsDisplayed()

        composeRule.onNodeWithTag("shop_confirm_unlock_button").performClick()
        composeRule.waitForIdle()
        assertEquals(BoardTheme.BERRY_BLAST, selectedTheme)
    }

    @Test
    fun `gameplay grid renders particle burst canvas when a word is matched`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level1 = runBlocking { repo.getLevel(AppLanguage.EN, 1) }

        val engine = WordSplashGameEngine()
        engine.startLevel(level1, previouslyUnlockedLevel = 1)
        val firstPlacement = engine.state.board!!.placements.first()

        engine.onDragStart(firstPlacement.coords.first())
        engine.onDragMove(firstPlacement.coords.last())
        val outcome = engine.onDragEnd(previouslyUnlockedLevel = 1) as SelectionOutcome.WordMatched

        val gameplayState = GameplayUiState(
            levelData = engine.state.levelData,
            board = engine.state.board,
            foundWords = engine.state.foundWords,
            foundCellMap = engine.state.foundCellMap,
            confettiBurstId = 1,
            lastMatchedBurst = WordMatchBurst(
                id = 1,
                coords = outcome.coords,
                word = outcome.word
            ),
            isLoading = false
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WordSplashTheme {
                GameplayScreen(
                    progress = GameProgress(language = AppLanguage.EN),
                    gameplay = gameplayState,
                    onDragStart = {},
                    onDragMove = {},
                    onDragEnd = {},
                    onUseHint = {},
                    onUseWordHint = {},
                    onShuffle = {},
                    onToggleLanguage = {},
                    onNavigateHome = {},
                    onNavigateLevels = {},
                    onNavigateSettings = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(250)
        composeRule.onNodeWithTag("word_search_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("grid_particle_canvas").assertExists()
        composeRule.onNodeWithTag("confetti_canvas").assertExists()
    }

    @Test
    fun `closest coord and pointer angle snapping accurately track finger position`() {
        // 6x6 grid in 625px square with 5px spacing => tile = (625 - 25)/6 = 100px, pitch = 105px
        // Center of (0,0) = (50, 50), center of (5,5) = (575, 575)
        val topLeftEdge = WordGridGenerator.findClosestCoord(
            xPx = -8f,
            yPx = -6f,
            areaWidthPx = 625f,
            areaHeightPx = 625f,
            spacingPx = 5f,
            gridSize = 6
        )
        assertEquals(GridCoord(0, 0), topLeftEdge)

        // Touching just right of the gap between col 1 (center 155) and col 2 (center 260) => gap midpoint 207.5
        val nearCol2 = WordGridGenerator.findClosestCoord(
            xPx = 212f,
            yPx = 155f,
            areaWidthPx = 625f,
            areaHeightPx = 625f,
            spacingPx = 5f,
            gridSize = 6
        )
        assertEquals(GridCoord(1, 2), nearCol2)

        // Dragging vertically from (1,1) [center (155, 155)] down 2 rows with slight horizontal drift (+45px, +210px)
        // Must stay strictly vertical [(1,1), (2,1), (3,1)] rather than jumping diagonally!
        val verticalSnapped = WordGridGenerator.computeSnappedLineFromPointer(
            anchor = GridCoord(1, 1),
            pointerXPx = 200f,
            pointerYPx = 365f,
            areaWidthPx = 625f,
            areaHeightPx = 625f,
            spacingPx = 5f,
            gridSize = 6
        )
        assertEquals(listOf(GridCoord(1, 1), GridCoord(2, 1), GridCoord(3, 1)), verticalSnapped)
    }

    @Test
    fun `grid drag threshold ignores accidental micro taps and triggers on real drag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level1 = runBlocking { repo.getLevel(AppLanguage.EN, 1) }

        val engine = WordSplashGameEngine()
        engine.startLevel(level1, previouslyUnlockedLevel = 1)
        val gameplayState = GameplayUiState(
            levelData = engine.state.levelData,
            board = engine.state.board,
            isLoading = false
        )

        var dragStarts = 0
        var dragEnds = 0

        composeRule.setContent {
            WordSplashTheme {
                GameplayScreen(
                    progress = GameProgress(language = AppLanguage.EN),
                    gameplay = gameplayState,
                    onDragStart = { dragStarts++ },
                    onDragMove = {},
                    onDragEnd = { dragEnds++ },
                    onUseHint = {},
                    onUseWordHint = {},
                    onShuffle = {},
                    onToggleLanguage = {},
                    onNavigateHome = {},
                    onNavigateLevels = {},
                    onNavigateSettings = {}
                )
            }
        }

        composeRule.waitForIdle()

        // Tiny accidental tap/jitter (< 8dp) should NOT trigger drag selection
        composeRule.onNodeWithTag("word_search_grid").performTouchInput {
            down(center)
            moveBy(Offset(2f, 2f))
            up()
        }
        composeRule.waitForIdle()
        assertEquals(0, dragStarts)
        assertEquals(0, dragEnds)

        // Real drag (> 8dp) MUST trigger dragStart and dragEnd
        composeRule.onNodeWithTag("word_search_grid").performTouchInput {
            down(center)
            moveBy(Offset(80f, 0f))
            up()
        }
        composeRule.waitForIdle()
        assertEquals(1, dragStarts)
        assertEquals(1, dragEnds)
    }

    @Test
    fun `unity ads automatically prepares next rewarded ad after reward is earned`() {
        var rewardCount = 0
        var skippedCount = 0
        var reloadCount = 0

        UnityAdsManager.handleRewardedShowComplete(
            completed = true,
            onRewardEarned = { rewardCount++ },
            onUnavailableOrSkipped = { skippedCount++ },
            reloadNextAd = { reloadCount++ }
        )

        assertEquals(1, rewardCount)
        assertEquals(0, skippedCount)
        assertEquals(1, reloadCount)
    }
}
