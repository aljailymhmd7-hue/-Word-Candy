package com.example.engine

import com.example.data.WordGridGenerator
import com.example.model.GameSessionStatus
import com.example.model.GeneratedBoard
import com.example.model.GridCoord
import com.example.model.LevelData
import com.example.model.LossReason
import com.example.model.LossSummary
import com.example.model.SelectionOutcome
import com.example.model.WinSummary

data class EngineSessionState(
    val levelData: LevelData? = null,
    val board: GeneratedBoard? = null,
    val foundWords: Set<String> = emptySet(),
    val foundCellMap: Map<GridCoord, Int> = emptyMap(),
    val hintedCoords: Set<GridCoord> = emptySet(),
    val highlightedWordCoords: Set<GridCoord> = emptySet(),
    val highlightedWords: Set<String> = emptySet(),
    val dragAnchor: GridCoord? = null,
    val selectedCoords: List<GridCoord> = emptyList(),
    val hintsUsed: Int = 0,
    val status: GameSessionStatus = GameSessionStatus.Loading
) {
    val isActive: Boolean get() = status is GameSessionStatus.Playing
}

/**
 * Pure Kotlin core game engine for Word Splash.
 * Manages word grid generation, 8-direction drag gesture selection,
 * hints/shuffles, and Win/Loss countdown state transitions.
 */
class WordSplashGameEngine {

    var state: EngineSessionState = EngineSessionState()
        private set

    fun startLevel(levelData: LevelData, previouslyUnlockedLevel: Int, seedOffset: Int = 0): EngineSessionState {
        val board = WordGridGenerator.generate(levelData, seedOffset = seedOffset)
        val totalSeconds = timeLimitForLevel(levelData)
        state = EngineSessionState(
            levelData = levelData,
            board = board,
            foundWords = emptySet(),
            foundCellMap = emptyMap(),
            hintedCoords = emptySet(),
            highlightedWordCoords = emptySet(),
            highlightedWords = emptySet(),
            dragAnchor = null,
            selectedCoords = emptyList(),
            hintsUsed = 0,
            status = GameSessionStatus.Playing(
                remainingSeconds = totalSeconds,
                totalTimeSeconds = totalSeconds,
                elapsedSeconds = 0
            )
        )
        return state
    }

    fun timeLimitForLevel(levelData: LevelData): Int = when {
        levelData.gridSize <= 6 -> 120
        levelData.gridSize <= 8 -> 150
        levelData.gridSize <= 10 -> 180
        else -> 210
    }

    fun onDragStart(coord: GridCoord): EngineSessionState {
        val board = state.board ?: return state
        if (!state.isActive) return state
        val clamped = clampCoord(coord, board.gridSize)
        state = state.copy(
            dragAnchor = clamped,
            selectedCoords = listOf(clamped)
        )
        return state
    }

    fun onDragMove(coord: GridCoord): EngineSessionState {
        val board = state.board ?: return state
        val anchor = state.dragAnchor ?: return state
        if (!state.isActive) return state
        val clamped = clampCoord(coord, board.gridSize)
        val snappedLine = WordGridGenerator.computeLineCoords(anchor, clamped, board.gridSize)
        if (snappedLine != state.selectedCoords) {
            state = state.copy(selectedCoords = snappedLine)
        }
        return state
    }

    fun onDragEnd(previouslyUnlockedLevel: Int): SelectionOutcome {
        val board = state.board ?: return SelectionOutcome.Ignored
        val levelData = state.levelData ?: return SelectionOutcome.Ignored
        val playing = state.status as? GameSessionStatus.Playing ?: return SelectionOutcome.Ignored
        val coords = state.selectedCoords

        if (coords.size < 2) {
            state = state.copy(dragAnchor = null, selectedCoords = emptyList())
            return SelectionOutcome.Ignored
        }

        val forwardText = coords.joinToString("") { board.letters[it.row][it.col].toString() }
        val reverseText = forwardText.reversed()

        val matchedWord = levelData.words.firstOrNull { word ->
            word !in state.foundWords && (word == forwardText || word == reverseText)
        }

        if (matchedWord == null) {
            state = state.copy(dragAnchor = null, selectedCoords = emptyList())
            return SelectionOutcome.NoMatch
        }

        val newFoundWords = state.foundWords + matchedWord
        val colorIndex = levelData.words.indexOf(matchedWord).coerceAtLeast(0) % 7
        val updatedCellMap = state.foundCellMap.toMutableMap()
        coords.forEach { coord ->
            if (!updatedCellMap.containsKey(coord)) {
                updatedCellMap[coord] = colorIndex
            }
        }

        val isComplete = newFoundWords.size == levelData.words.size
        val nextStatus = if (isComplete) {
            val elapsed = playing.elapsedSeconds
            val stars = when {
                elapsed <= (playing.totalTimeSeconds * 0.45f).toInt() && state.hintsUsed == 0 -> 3
                elapsed <= (playing.totalTimeSeconds * 0.80f).toInt() && state.hintsUsed <= 1 -> 2
                else -> 1
            }
            val coinsReward = 50 + (stars - 1) * 10
            val pointsBonus = 100 + maxOf(0, playing.remainingSeconds * 2)
            val nextLvl = (levelData.level + 1).coerceAtMost(1000)
            val newlyUnlocked = if (nextLvl > previouslyUnlockedLevel) nextLvl else null
            GameSessionStatus.Won(
                WinSummary(
                    level = levelData.level,
                    nextLevel = nextLvl,
                    wordsFound = newFoundWords.size,
                    totalWords = levelData.words.size,
                    elapsedSeconds = elapsed,
                    starsEarned = stars,
                    coinsReward = coinsReward,
                    pointsBonus = pointsBonus,
                    newlyUnlockedLevel = newlyUnlocked
                )
            )
        } else {
            playing
        }

        state = state.copy(
            foundWords = newFoundWords,
            foundCellMap = updatedCellMap,
            hintedCoords = state.hintedCoords - coords.toSet(),
            highlightedWordCoords = state.highlightedWordCoords - coords.toSet(),
            highlightedWords = state.highlightedWords - matchedWord,
            dragAnchor = null,
            selectedCoords = emptyList(),
            status = nextStatus
        )

        return SelectionOutcome.WordMatched(
            word = matchedWord,
            coords = coords,
            colorIndex = colorIndex,
            isLevelComplete = isComplete
        )
    }

    fun cancelDrag(): EngineSessionState {
        state = state.copy(dragAnchor = null, selectedCoords = emptyList())
        return state
    }

    fun canRevealLetterHint(): Boolean {
        val board = state.board ?: return false
        if (!state.isActive) return false
        return board.placements.any { placement ->
            placement.word !in state.foundWords &&
                placement.word !in state.highlightedWords &&
                placement.coords.any { it !in state.hintedCoords }
        }
    }

    fun canHighlightWordHint(): Boolean {
        val board = state.board ?: return false
        if (!state.isActive) return false
        return board.placements.any { placement ->
            placement.word !in state.foundWords && placement.word !in state.highlightedWords
        }
    }

    fun revealLetterHint(): GridCoord? = revealHint()

    fun revealHint(): GridCoord? {
        val board = state.board ?: return null
        if (!state.isActive) return null
        val unfoundPlacement = board.placements.firstOrNull { placement ->
            placement.word !in state.foundWords &&
                placement.word !in state.highlightedWords &&
                placement.coords.any { it !in state.hintedCoords }
        } ?: board.placements.firstOrNull { it.word !in state.foundWords } ?: return null
        val targetCoord = unfoundPlacement.coords.firstOrNull { it !in state.hintedCoords }
            ?: unfoundPlacement.coords.firstOrNull()
            ?: return null
        state = state.copy(
            hintedCoords = state.hintedCoords + targetCoord,
            hintsUsed = state.hintsUsed + 1
        )
        return targetCoord
    }

    fun revealWordHint(): String? {
        val board = state.board ?: return null
        if (!state.isActive) return null
        val targetPlacement = board.placements.firstOrNull {
            it.word !in state.foundWords && it.word !in state.highlightedWords
        } ?: return null
        state = state.copy(
            highlightedWordCoords = state.highlightedWordCoords + targetPlacement.coords,
            highlightedWords = state.highlightedWords + targetPlacement.word,
            hintsUsed = state.hintsUsed + 1
        )
        return targetPlacement.word
    }

    fun shuffleFillerLetters(): EngineSessionState {
        val board = state.board ?: return state
        if (!state.isActive) return state
        state = state.copy(
            board = WordGridGenerator.shuffleFillerOnly(board),
            dragAnchor = null,
            selectedCoords = emptyList()
        )
        return state
    }

    fun tickSecond(): GameSessionStatus {
        val playing = state.status as? GameSessionStatus.Playing ?: return state.status
        val levelData = state.levelData ?: return state.status
        val nextRemaining = (playing.remainingSeconds - 1).coerceAtLeast(0)
        val nextElapsed = playing.elapsedSeconds + 1

        val nextStatus = if (nextRemaining == 0) {
            GameSessionStatus.Lost(
                LossSummary(
                    level = levelData.level,
                    wordsFound = state.foundWords.size,
                    totalWords = levelData.words.size,
                    timeLimitSeconds = playing.totalTimeSeconds,
                    reason = LossReason.TIME_EXPIRED
                )
            )
        } else {
            playing.copy(remainingSeconds = nextRemaining, elapsedSeconds = nextElapsed)
        }

        state = state.copy(
            dragAnchor = if (nextStatus is GameSessionStatus.Lost) null else state.dragAnchor,
            selectedCoords = if (nextStatus is GameSessionStatus.Lost) emptyList() else state.selectedCoords,
            status = nextStatus
        )
        return nextStatus
    }

    fun grantExtraTime(seconds: Int = 30): EngineSessionState {
        val lost = state.status as? GameSessionStatus.Lost ?: return state
        state = state.copy(
            status = GameSessionStatus.Playing(
                remainingSeconds = seconds,
                totalTimeSeconds = lost.summary.timeLimitSeconds + seconds,
                elapsedSeconds = lost.summary.timeLimitSeconds
            )
        )
        return state
    }

    private fun clampCoord(coord: GridCoord, gridSize: Int): GridCoord =
        GridCoord(
            row = coord.row.coerceIn(0, gridSize - 1),
            col = coord.col.coerceIn(0, gridSize - 1)
        )
}
