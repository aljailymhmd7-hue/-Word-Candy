package com.example.data

import android.content.Context
import com.example.model.AppLanguage
import com.example.model.LevelData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LevelRepository(private val context: Context) {

    private val mutex = Mutex()
    private val cache = mutableMapOf<AppLanguage, List<LevelData>>()

    suspend fun getLevels(language: AppLanguage): List<LevelData> = mutex.withLock {
        cache[language]?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val fileName = if (language == AppLanguage.EN) {
                "levels_EN_1000.json"
            } else {
                "levels_FR_1000.json"
            }
            val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val arr = root.getJSONArray("levels")
            val result = ArrayList<LevelData>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val wordsJson = obj.getJSONArray("words")
                val words = ArrayList<String>(wordsJson.length())
                for (w in 0 until wordsJson.length()) {
                    words.add(wordsJson.getString(w).uppercase())
                }
                val colorsJson = obj.optJSONArray("colors")
                val colors = ArrayList<String>()
                if (colorsJson != null) {
                    for (c in 0 until colorsJson.length()) {
                        colors.add(colorsJson.getString(c))
                    }
                }
                result.add(
                    LevelData(
                        level = obj.getInt("level"),
                        theme = obj.getString("theme"),
                        words = words,
                        gridSize = obj.getInt("grid_size"),
                        world = obj.getString("world"),
                        difficulty = obj.getString("difficulty"),
                        colorsHex = colors
                    )
                )
            }
            result
        }
        cache[language] = loaded
        loaded
    }

    suspend fun getLevel(language: AppLanguage, levelNumber: Int): LevelData {
        val levels = getLevels(language)
        val idx = (levelNumber - 1).coerceIn(0, levels.lastIndex)
        return levels[idx]
    }
}
