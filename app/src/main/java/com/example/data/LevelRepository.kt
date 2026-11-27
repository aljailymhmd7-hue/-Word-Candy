package com.example.data

import android.content.Context
import com.example.model.AppLanguage
import com.example.model.LevelData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class WordPools(
    val easy34: List<String>,
    val medium5: List<String>,
    val hard68: List<String>
)

object ProgressiveDifficulty {

    fun wordsCountForLevel(levelNumber: Int): Int {
        val safeLevel = levelNumber.coerceAtLeast(1)
        return (3 + (safeLevel / 3)).coerceAtMost(10)
    }

    fun gridSizeForWordsCount(wordsCount: Int): Int = when {
        wordsCount <= 4 -> 6
        wordsCount <= 6 -> 7
        wordsCount <= 8 -> 8
        else -> 9
    }

    fun poolForLevel(levelNumber: Int, pools: WordPools): List<String> =
        when (levelNumber.coerceAtLeast(1)) {
            in 1..10 -> pools.easy34
            in 11..20 -> pools.medium5
            else -> pools.hard68 + pools.medium5
        }

    fun difficultyLabel(levelNumber: Int, language: AppLanguage): String = when {
        levelNumber <= 10 -> if (language == AppLanguage.EN) "Easy" else "Facile"
        levelNumber <= 20 -> if (language == AppLanguage.EN) "Medium" else "Moyen"
        else -> if (language == AppLanguage.EN) "Hard" else "Difficile"
    }

    fun parseWords5000Json(rawJson: String): WordPools {
        val root = JSONObject(rawJson)
        return WordPools(
            easy34 = readUpperAlphaArray(root.optJSONArray("easy_3_4")),
            medium5 = readUpperAlphaArray(root.optJSONArray("medium_5")),
            hard68 = readUpperAlphaArray(root.optJSONArray("hard_6_8"))
        )
    }

    private fun readUpperAlphaArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val w = arr.optString(i).trim().uppercase()
            if (w.isNotEmpty() && w.all { it in 'A'..'Z' }) {
                list.add(w)
            }
        }
        return list.distinct()
    }
}

class LevelRepository(private val context: Context) {

    data class LevelMetadata(
        val level: Int,
        val theme: String,
        val world: String,
        val colorsHex: List<String>
    )

    private val mutex = Mutex()
    private val metadataCache = mutableMapOf<AppLanguage, List<LevelMetadata>>()
    private var englishPools5000Cache: WordPools? = null
    private var frenchPoolsCache: WordPools? = null

    suspend fun loadEnglishWordPools5000(): WordPools = mutex.withLock {
        englishPools5000Cache?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val raw = context.assets.open("words_5000.json").bufferedReader().use { it.readText() }
            ProgressiveDifficulty.parseWords5000Json(raw)
        }
        englishPools5000Cache = loaded
        loaded
    }

    suspend fun getWordPools(language: AppLanguage): WordPools {
        if (language == AppLanguage.EN) {
            return loadEnglishWordPools5000()
        }
        ensureMetadataLoaded(language)
        return mutex.withLock {
            frenchPoolsCache ?: WordPools(emptyList(), emptyList(), emptyList())
        }
    }

    suspend fun getLevelMetadata(language: AppLanguage, levelNumber: Int): LevelMetadata {
        val list = ensureMetadataLoaded(language)
        val idx = (levelNumber - 1).coerceIn(0, list.lastIndex)
        return list[idx]
    }

    private suspend fun ensureMetadataLoaded(language: AppLanguage): List<LevelMetadata> = mutex.withLock {
        metadataCache[language]?.let { return it }
        val (metaList, frenchPools) = withContext(Dispatchers.IO) {
            val fileName = if (language == AppLanguage.EN) {
                "levels_EN_1000.json"
            } else {
                "levels_FR_1000.json"
            }
            val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val arr = root.getJSONArray("levels")
            val result = ArrayList<LevelMetadata>(arr.length())
            val easyFr = LinkedHashSet<String>()
            val mediumFr = LinkedHashSet<String>()
            val hardFr = LinkedHashSet<String>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (language == AppLanguage.FR) {
                    val wordsJson = obj.getJSONArray("words")
                    for (w in 0 until wordsJson.length()) {
                        val cleaned = wordsJson.getString(w).uppercase().filter { it in 'A'..'Z' }
                        when (cleaned.length) {
                            in 3..4 -> easyFr.add(cleaned)
                            5 -> mediumFr.add(cleaned)
                            in 6..8 -> hardFr.add(cleaned)
                        }
                    }
                }
                val colorsJson = obj.optJSONArray("colors")
                val colors = ArrayList<String>()
                if (colorsJson != null) {
                    for (c in 0 until colorsJson.length()) {
                        colors.add(colorsJson.getString(c))
                    }
                }
                result.add(
                    LevelMetadata(
                        level = obj.getInt("level"),
                        theme = obj.getString("theme"),
                        world = obj.getString("world"),
                        colorsHex = colors
                    )
                )
            }
            val frPools = if (language == AppLanguage.FR) {
                WordPools(
                    easy34 = easyFr.toList(),
                    medium5 = mediumFr.toList(),
                    hard68 = hardFr.toList()
                )
            } else {
                null
            }
            result to frPools
        }
        metadataCache[language] = metaList
        if (frenchPools != null) {
            frenchPoolsCache = frenchPools
        }
        metaList
    }

    suspend fun getLevels(language: AppLanguage): List<LevelData> {
        val metadata = ensureMetadataLoaded(language)
        val pools = getWordPools(language)
        return metadata.map { meta ->
            buildRandomLevel(meta, language, pools, System.currentTimeMillis() + meta.level * 37L)
        }
    }

    suspend fun getLevel(
        language: AppLanguage,
        levelNumber: Int,
        seedOffset: Int = 0
    ): LevelData {
        val meta = getLevelMetadata(language, levelNumber)
        val pools = getWordPools(language)
        val seed = System.currentTimeMillis() + seedOffset.toLong() * 7919L
        return buildRandomLevel(meta, language, pools, seed)
    }

    private fun buildRandomLevel(
        meta: LevelMetadata,
        language: AppLanguage,
        pools: WordPools,
        seed: Long
    ): LevelData {
        val wordsCount = ProgressiveDifficulty.wordsCountForLevel(meta.level)
        val gridSize = ProgressiveDifficulty.gridSizeForWordsCount(wordsCount)
        val pool = ProgressiveDifficulty.poolForLevel(meta.level, pools)
            .filter { it.length <= gridSize }
        val takeCount = wordsCount.coerceAtMost(pool.size)
        val rng = Random(seed)
        val selected = if (takeCount >= pool.size) {
            pool.shuffled(rng)
        } else {
            val picked = LinkedHashSet<String>(takeCount)
            while (picked.size < takeCount) {
                picked.add(pool[rng.nextInt(pool.size)])
            }
            picked.toList()
        }
        return LevelData(
            level = meta.level,
            theme = meta.theme,
            words = selected,
            gridSize = gridSize,
            world = meta.world,
            difficulty = ProgressiveDifficulty.difficultyLabel(meta.level, language),
            colorsHex = meta.colorsHex
        )
    }
}
