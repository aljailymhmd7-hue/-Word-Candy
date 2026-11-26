package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidHapticsController
import com.example.audio.HapticsController
import com.example.audio.SoundEffects
import com.example.audio.SoundEffectsController
import com.example.data.LevelRepository
import com.example.data.ProgressRepository
import com.example.engine.WordSplashGameEngine
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.model.GameProgress
import com.example.model.GameSessionStatus
import com.example.model.GeneratedBoard
import com.example.model.GridCoord
import com.example.model.LevelData
import com.example.model.LocalizedStrings
import com.example.model.LossSummary
import com.example.model.SelectionOutcome
import com.example.model.WinSummary
import com.example.model.WordMatchBurst
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameplayUiState(
    val levelData: LevelData? = null,
    val board: GeneratedBoard? = null,
    val foundWords: Set<String> = emptySet(),
    val foundCellMap: Map<GridCoord, Int> = emptyMap(),
    val hintedCoords: Set<GridCoord> = emptySet(),
    val highlightedWordCoords: Set<GridCoord> = emptySet(),
    val highlightedWords: Set<String> = emptySet(),
    val selectedCoords: List<GridCoord> = emptyList(),
    val remainingSeconds: Int = 120,
    val totalTimeSeconds: Int = 120,
    val elapsedSeconds: Int = 0,
    val hintsUsed: Int = 0,
    val confettiBurstId: Int = 0,
    val lastMatchedBurst: WordMatchBurst? = null,
    val sessionStatus: GameSessionStatus = GameSessionStatus.Loading,
    val winSummary: WinSummary? = null,
    val lossSummary: LossSummary? = null,
    val isLoading: Boolean = true
)

data class WordSplashUiState(
    val progress: GameProgress = GameProgress(),
    val allLevelsForLanguage: List<LevelData> = emptyList(),
    val gameplay: GameplayUiState = GameplayUiState(),
    val toastMessage: String? = null
)

class WordSplashViewModel(application: Application) : AndroidViewModel(application) {

    private val levelRepository = LevelRepository(application.applicationContext)
    private val progressRepository = ProgressRepository(application.applicationContext)
    private val gameEngine = WordSplashGameEngine()
    val soundEffects: SoundEffectsController = SoundEffects()
    val haptics: HapticsController = AndroidHapticsController(application.applicationContext)

    private val _uiState = MutableStateFlow(WordSplashUiState())
    val uiState: StateFlow<WordSplashUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var loadedLanguage: AppLanguage? = null
    private var loadedLevelNumber: Int? = null
    private var retrySeedCounter = 0
    private var completedSinceInterstitial = 0

    init {
        viewModelScope.launch {
            progressRepository.progressFlow.collectLatest { progress ->
                soundEffects.enabled = progress.soundEnabled
                haptics.enabled = progress.hapticsEnabled
                val langChanged = loadedLanguage != progress.language
                _uiState.update { it.copy(progress = progress) }
                if (langChanged) {
                    loadedLanguage = progress.language
                    val allLevels = levelRepository.getLevels(progress.language)
                    _uiState.update { it.copy(allLevelsForLanguage = allLevels) }
                    val activeLevel = loadedLevelNumber ?: progress.currentLevel
                    loadLevel(activeLevel, forceRegenerate = true)
                }
            }
        }
    }

    fun startLevel(levelNumber: Int) {
        soundEffects.playClick()
        viewModelScope.launch {
            progressRepository.setCurrentLevel(levelNumber)
            loadLevel(levelNumber, forceRegenerate = true)
        }
    }

    fun retryCurrentLevel() {
        val current = loadedLevelNumber ?: _uiState.value.progress.currentLevel
        retrySeedCounter++
        soundEffects.playClick()
        viewModelScope.launch {
            loadLevel(current, forceRegenerate = true, seedOffset = retrySeedCounter)
        }
    }

    fun buyExtraTime() {
        val lang = _uiState.value.progress.language
        viewModelScope.launch {
            val spent = progressRepository.spendCoins(30)
            if (spent) {
                soundEffects.playDing()
                gameEngine.grantExtraTime(30)
                syncEngineState()
                startTimer()
            } else {
                showToast(LocalizedStrings.notEnoughCoins(lang))
            }
        }
    }

    fun grantRewardedExtraTime() {
        soundEffects.playDing()
        gameEngine.grantExtraTime(30)
        syncEngineState()
        startTimer()
    }

    fun grantRewardedShopBonus() {
        val lang = _uiState.value.progress.language
        viewModelScope.launch {
            progressRepository.grantAdReward(coinsAmount = 25, hintsAmount = 1)
            soundEffects.playWinFanfare()
            showToast(
                if (lang == AppLanguage.EN) "+25 Coins & +1 Hint earned!"
                else "+25 Pieces & +1 Indice gagnes !"
            )
        }
    }

    fun consumeInterstitialTriggerIfDue(): Boolean {
        return if (completedSinceInterstitial >= 3) {
            completedSinceInterstitial = 0
            true
        } else {
            false
        }
    }

    private suspend fun loadLevel(
        levelNumber: Int,
        forceRegenerate: Boolean = false,
        seedOffset: Int = 0
    ) {
        val lang = _uiState.value.progress.language
        if (!forceRegenerate && loadedLevelNumber == levelNumber && gameEngine.state.board != null) {
            return
        }
        loadedLevelNumber = levelNumber
        _uiState.update { it.copy(gameplay = GameplayUiState(isLoading = true)) }
        val levelData = levelRepository.getLevel(lang, levelNumber)
        gameEngine.startLevel(
            levelData = levelData,
            previouslyUnlockedLevel = _uiState.value.progress.unlockedLevel,
            seedOffset = seedOffset
        )
        syncEngineState(confettiIncrement = 0, clearMatchedBurst = true)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!gameEngine.state.isActive) break
                val nextStatus = gameEngine.tickSecond()
                syncEngineState()
                if (nextStatus is GameSessionStatus.Lost) {
                    soundEffects.playTimeUp()
                    break
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun onDragStart(coord: GridCoord) {
        val prevCount = gameEngine.state.selectedCoords.size
        gameEngine.onDragStart(coord)
        if (gameEngine.state.selectedCoords.size > prevCount) {
            soundEffects.playPop()
            haptics.playLetterTick()
        }
        syncEngineState()
    }

    fun onDragMove(coord: GridCoord) {
        val prev = gameEngine.state.selectedCoords
        gameEngine.onDragMove(coord)
        val curr = gameEngine.state.selectedCoords
        if (curr != prev) {
            if (curr.size > prev.size) {
                soundEffects.playPop()
            }
            if (curr.isNotEmpty()) {
                haptics.playLetterTick()
            }
            syncEngineState()
        }
    }

    fun onDragEnd() {
        val prevUnlocked = _uiState.value.progress.unlockedLevel
        val draggedLength = gameEngine.state.selectedCoords.size
        when (val outcome = gameEngine.onDragEnd(prevUnlocked)) {
            is SelectionOutcome.WordMatched -> {
                val nextBurstId = _uiState.value.gameplay.confettiBurstId + 1
                val burst = WordMatchBurst(
                    id = nextBurstId,
                    coords = outcome.coords,
                    word = outcome.word
                )
                if (outcome.isLevelComplete) {
                    soundEffects.playWinFanfare()
                    haptics.playLevelComplete()
                    timerJob?.cancel()
                    completedSinceInterstitial++
                    syncEngineState(confettiIncrement = 1, matchedBurst = burst)
                    val summary = (gameEngine.state.status as? GameSessionStatus.Won)?.summary
                    if (summary != null) {
                        viewModelScope.launch {
                            progressRepository.recordLevelWin(
                                level = summary.level,
                                starsEarned = summary.starsEarned,
                                coinsReward = summary.coinsReward,
                                pointsBonus = summary.pointsBonus,
                                wordsCount = summary.wordsFound
                            )
                        }
                    }
                } else {
                    soundEffects.playDing()
                    haptics.playWordFound()
                    syncEngineState(confettiIncrement = 1, matchedBurst = burst)
                }
            }
            SelectionOutcome.NoMatch -> {
                if (draggedLength >= 2) {
                    soundEffects.playWrongGuess()
                    haptics.playWrongGuess()
                }
                syncEngineState()
            }
            SelectionOutcome.Ignored -> {
                syncEngineState()
            }
        }
    }

    fun useHint() = useLetterHint()

    fun useLetterHint() {
        if (!gameEngine.state.isActive) return
        val lang = _uiState.value.progress.language
        if (!gameEngine.canRevealLetterHint()) {
            soundEffects.playWrongGuess()
            showToast(LocalizedStrings.allWordsHighlightedToast(lang))
            return
        }
        viewModelScope.launch {
            val paid = progressRepository.spendHint() || progressRepository.spendCoins(LETTER_HINT_COIN_COST)
            if (paid) {
                if (gameEngine.revealLetterHint() != null) {
                    soundEffects.playPop()
                    syncEngineState()
                    showToast(LocalizedStrings.letterRevealedToast(lang))
                }
            } else {
                soundEffects.playWrongGuess()
                showToast(LocalizedStrings.notEnoughCoins(lang))
            }
        }
    }

    fun useWordHint() {
        if (!gameEngine.state.isActive) return
        val lang = _uiState.value.progress.language
        if (!gameEngine.canHighlightWordHint()) {
            soundEffects.playWrongGuess()
            showToast(LocalizedStrings.allWordsHighlightedToast(lang))
            return
        }
        viewModelScope.launch {
            val spent = progressRepository.spendCoins(WORD_HINT_COIN_COST)
            if (spent) {
                val highlightedWord = gameEngine.revealWordHint()
                if (highlightedWord != null) {
                    soundEffects.playDing()
                    syncEngineState()
                    showToast(LocalizedStrings.wordHighlightedToast(lang, highlightedWord))
                }
            } else {
                soundEffects.playWrongGuess()
                showToast(LocalizedStrings.notEnoughCoins(lang))
            }
        }
    }

    fun shuffleBoard() {
        if (!gameEngine.state.isActive) return
        soundEffects.playClick()
        gameEngine.shuffleFillerLetters()
        syncEngineState()
    }

    fun toggleLanguage() {
        soundEffects.playClick()
        val next = if (_uiState.value.progress.language == AppLanguage.EN) {
            AppLanguage.FR
        } else {
            AppLanguage.EN
        }
        viewModelScope.launch {
            progressRepository.setLanguage(next)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            progressRepository.setSoundEnabled(enabled)
            if (enabled) soundEffects.playClick()
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        soundEffects.playClick()
        haptics.enabled = enabled
        if (enabled) {
            haptics.playLetterTick()
        }
        viewModelScope.launch {
            progressRepository.setHapticsEnabled(enabled)
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val claimed = progressRepository.claimDailyReward(System.currentTimeMillis())
            val lang = _uiState.value.progress.language
            if (claimed) {
                soundEffects.playWinFanfare()
                showToast(
                    if (lang == AppLanguage.EN) "+50 Coins & +1 Hint claimed!"
                    else "+50 Pieces et +1 Indice recus !"
                )
            } else {
                soundEffects.playClick()
                showToast(
                    if (lang == AppLanguage.EN) "Daily reward already claimed today!"
                    else "Cadeau du jour deja reclame !"
                )
            }
        }
    }

    fun buyHints(amount: Int, cost: Int) {
        viewModelScope.launch {
            val bought = progressRepository.buyHints(amount, cost)
            val lang = _uiState.value.progress.language
            if (bought) {
                soundEffects.playDing()
                showToast(
                    if (lang == AppLanguage.EN) "+$amount Hints added!"
                    else "+$amount Indices ajoutes !"
                )
            } else {
                soundEffects.playWrongGuess()
                showToast(LocalizedStrings.notEnoughCoins(lang))
            }
        }
    }

    fun buyOrEquipTheme(theme: BoardTheme) {
        val currentProgress = _uiState.value.progress
        val lang = currentProgress.language
        val wasAlreadyOwned = theme.id in currentProgress.ownedBoardThemes
        val title = if (lang == AppLanguage.EN) theme.titleEn else theme.titleFr
        viewModelScope.launch {
            val ok = progressRepository.buyOrEquipTheme(theme)
            if (ok) {
                if (wasAlreadyOwned) {
                    soundEffects.playDing()
                    showToast(LocalizedStrings.themeEquippedToast(lang, title))
                } else {
                    soundEffects.playWinFanfare()
                    showToast(LocalizedStrings.themeUnlockedToast(lang, title))
                }
            } else {
                soundEffects.playWrongGuess()
                showToast(LocalizedStrings.notEnoughCoins(lang))
            }
        }
    }

    fun resetProgress() {
        soundEffects.playClick()
        viewModelScope.launch {
            progressRepository.resetAll()
            loadLevel(1, forceRegenerate = true)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    private fun syncEngineState(
        confettiIncrement: Int = 0,
        matchedBurst: WordMatchBurst? = null,
        clearMatchedBurst: Boolean = false
    ) {
        val engine = gameEngine.state
        val playing = engine.status as? GameSessionStatus.Playing
        val won = engine.status as? GameSessionStatus.Won
        val lost = engine.status as? GameSessionStatus.Lost

        _uiState.update { current ->
            val resolvedBurst = when {
                clearMatchedBurst -> null
                matchedBurst != null -> matchedBurst
                else -> current.gameplay.lastMatchedBurst
            }
            current.copy(
                gameplay = GameplayUiState(
                    levelData = engine.levelData,
                    board = engine.board,
                    foundWords = engine.foundWords,
                    foundCellMap = engine.foundCellMap,
                    hintedCoords = engine.hintedCoords,
                    highlightedWordCoords = engine.highlightedWordCoords,
                    highlightedWords = engine.highlightedWords,
                    selectedCoords = engine.selectedCoords,
                    remainingSeconds = playing?.remainingSeconds ?: 0,
                    totalTimeSeconds = playing?.totalTimeSeconds ?: current.gameplay.totalTimeSeconds,
                    elapsedSeconds = playing?.elapsedSeconds ?: current.gameplay.elapsedSeconds,
                    hintsUsed = engine.hintsUsed,
                    confettiBurstId = current.gameplay.confettiBurstId + confettiIncrement,
                    lastMatchedBurst = resolvedBurst,
                    sessionStatus = engine.status,
                    winSummary = won?.summary,
                    lossSummary = lost?.summary,
                    isLoading = engine.status is GameSessionStatus.Loading
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundEffects.release()
    }

    companion object {
        const val LETTER_HINT_COIN_COST = 25
        const val WORD_HINT_COIN_COST = 60
    }
}
