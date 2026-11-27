package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.model.GameProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "word_splash_prefs")

class ProgressRepository(private val context: Context) {

    private object Keys {
        val UNLOCKED_LEVEL = intPreferencesKey("unlocked_level")
        val CURRENT_LEVEL = intPreferencesKey("current_level")
        val COINS = intPreferencesKey("coins")
        val HINTS = intPreferencesKey("hints")
        val TOTAL_POINTS = intPreferencesKey("total_points")
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val COMPLETED_LEVELS = stringPreferencesKey("completed_levels")
        val STARS_BY_LEVEL = stringPreferencesKey("stars_by_level")
        val LAST_DAILY_REWARD_MS = longPreferencesKey("last_daily_reward_ms")
        val ACTIVE_BOARD_THEME = stringPreferencesKey("active_board_theme")
        val OWNED_BOARD_THEMES = stringPreferencesKey("owned_board_themes")
        val TOTAL_WORDS_FOUND = intPreferencesKey("total_words_found")
        val GA4_PROPERTY_ID = stringPreferencesKey("ga4_property_id")
    }

    val progressFlow: Flow<GameProgress> = context.dataStore.data.map { prefs ->
        val completed = prefs[Keys.COMPLETED_LEVELS]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        val stars = prefs[Keys.STARS_BY_LEVEL]
            ?.split(",")
            ?.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val lvl = parts[0].toIntOrNull()
                    val st = parts[1].toIntOrNull()
                    if (lvl != null && st != null) lvl to st else null
                } else null
            }?.toMap() ?: emptyMap()

        val ownedThemes = prefs[Keys.OWNED_BOARD_THEMES]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: setOf(BoardTheme.CANDY_CLASSIC.id)

        val activeThemeId = prefs[Keys.ACTIVE_BOARD_THEME] ?: BoardTheme.CANDY_CLASSIC.id
        val activeTheme = BoardTheme.entries.firstOrNull { it.id == activeThemeId }
            ?: BoardTheme.CANDY_CLASSIC

        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.EN.code
        val lang = if (langCode == AppLanguage.FR.code) AppLanguage.FR else AppLanguage.EN

        GameProgress(
            unlockedLevel = prefs[Keys.UNLOCKED_LEVEL] ?: 1,
            currentLevel = prefs[Keys.CURRENT_LEVEL] ?: 1,
            coins = prefs[Keys.COINS] ?: 150,
            hints = prefs[Keys.HINTS] ?: 3,
            totalPoints = prefs[Keys.TOTAL_POINTS] ?: 0,
            language = lang,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            completedLevels = completed,
            starsByLevel = stars,
            lastDailyRewardMs = prefs[Keys.LAST_DAILY_REWARD_MS] ?: 0L,
            activeBoardTheme = activeTheme,
            ownedBoardThemes = ownedThemes,
            totalWordsFound = prefs[Keys.TOTAL_WORDS_FOUND] ?: 0,
            adminGa4PropertyId = prefs[Keys.GA4_PROPERTY_ID] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setCurrentLevel(level: Int) {
        context.dataStore.edit { it[Keys.CURRENT_LEVEL] = level.coerceIn(1, 1000) }
    }

    suspend fun spendHint(): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentHints = prefs[Keys.HINTS] ?: 3
            if (currentHints > 0) {
                prefs[Keys.HINTS] = currentHints - 1
                success = true
            }
        }
        return success
    }

    suspend fun recordLevelWin(
        level: Int,
        starsEarned: Int,
        coinsReward: Int,
        pointsBonus: Int,
        wordsCount: Int
    ) {
        context.dataStore.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_LEVEL] ?: 1
            val newUnlocked = maxOf(unlocked, (level + 1).coerceAtMost(1000))
            prefs[Keys.UNLOCKED_LEVEL] = newUnlocked
            prefs[Keys.CURRENT_LEVEL] = (level + 1).coerceAtMost(1000)
            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 150) + coinsReward
            prefs[Keys.TOTAL_POINTS] = (prefs[Keys.TOTAL_POINTS] ?: 0) + pointsBonus
            prefs[Keys.TOTAL_WORDS_FOUND] = (prefs[Keys.TOTAL_WORDS_FOUND] ?: 0) + wordsCount

            val existingCompleted = prefs[Keys.COMPLETED_LEVELS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableSet() ?: mutableSetOf()
            existingCompleted.add(level)
            prefs[Keys.COMPLETED_LEVELS] = existingCompleted.sorted().joinToString(",")

            val existingStars = prefs[Keys.STARS_BY_LEVEL]
                ?.split(",")
                ?.mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val l = parts[0].toIntOrNull()
                        val s = parts[1].toIntOrNull()
                        if (l != null && s != null) l to s else null
                    } else null
                }?.toMap()?.toMutableMap() ?: mutableMapOf()
            val prev = existingStars[level] ?: 0
            existingStars[level] = maxOf(prev, starsEarned)
            prefs[Keys.STARS_BY_LEVEL] = existingStars.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun claimDailyReward(nowMs: Long): Boolean {
        var claimed = false
        context.dataStore.edit { prefs ->
            val last = prefs[Keys.LAST_DAILY_REWARD_MS] ?: 0L
            val cooldownMs = 24L * 60L * 60L * 1000L
            if (nowMs - last >= cooldownMs) {
                prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 150) + 50
                prefs[Keys.HINTS] = (prefs[Keys.HINTS] ?: 3) + 1
                prefs[Keys.LAST_DAILY_REWARD_MS] = nowMs
                claimed = true
            }
        }
        return claimed
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var spent = false
        context.dataStore.edit { prefs ->
            val coins = prefs[Keys.COINS] ?: 150
            if (coins >= amount) {
                prefs[Keys.COINS] = coins - amount
                spent = true
            }
        }
        return spent
    }

    suspend fun grantAdReward(coinsAmount: Int, hintsAmount: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 150) + coinsAmount
            prefs[Keys.HINTS] = (prefs[Keys.HINTS] ?: 3) + hintsAmount
        }
    }

    suspend fun buyHints(hintAmount: Int, costCoins: Int): Boolean {
        var bought = false
        context.dataStore.edit { prefs ->
            val coins = prefs[Keys.COINS] ?: 150
            if (coins >= costCoins) {
                prefs[Keys.COINS] = coins - costCoins
                prefs[Keys.HINTS] = (prefs[Keys.HINTS] ?: 3) + hintAmount
                bought = true
            }
        }
        return bought
    }

    suspend fun buyOrEquipTheme(theme: BoardTheme): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val owned = prefs[Keys.OWNED_BOARD_THEMES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet() ?: mutableSetOf(BoardTheme.CANDY_CLASSIC.id)
            if (theme.id in owned) {
                prefs[Keys.ACTIVE_BOARD_THEME] = theme.id
                success = true
            } else {
                val coins = prefs[Keys.COINS] ?: 150
                if (coins >= theme.priceCoins) {
                    prefs[Keys.COINS] = coins - theme.priceCoins
                    owned.add(theme.id)
                    prefs[Keys.OWNED_BOARD_THEMES] = owned.joinToString(",")
                    prefs[Keys.ACTIVE_BOARD_THEME] = theme.id
                    success = true
                }
            }
        }
        return success
    }

    suspend fun setAdminGa4PropertyId(propertyId: String) {
        context.dataStore.edit {
            it[Keys.GA4_PROPERTY_ID] = propertyId.trim().removePrefix("properties/")
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
