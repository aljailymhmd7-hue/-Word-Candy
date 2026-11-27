package com.example.model

enum class AppLanguage(val code: String) {
    EN("EN"),
    FR("FR")
}

enum class BoardTheme(
    val id: String,
    val titleEn: String,
    val titleFr: String,
    val subtitleEn: String,
    val subtitleFr: String,
    val priceCoins: Int
) {
    CANDY_CLASSIC(
        id = "classic",
        titleEn = "Candy Classic",
        titleFr = "Classique Bonbon",
        subtitleEn = "Bright rainbow sugar tiles",
        subtitleFr = "Tuiles arc-en-ciel sucrees",
        priceCoins = 0
    ),
    BERRY_BLAST(
        id = "berry",
        titleEn = "Berry Blast",
        titleFr = "Explosion Baies",
        subtitleEn = "Juicy raspberry & plum jelly",
        subtitleFr = "Gelee framboise et prune",
        priceCoins = 150
    ),
    MINT_CHILL(
        id = "mint",
        titleEn = "Mint Lagoon",
        titleFr = "Lagon Menthe",
        subtitleEn = "Cool frosted spearmint drops",
        subtitleFr = "Menthe givree et lagon",
        priceCoins = 250
    ),
    STAR_GALAXY(
        id = "galaxy",
        titleEn = "Starry Sugar",
        titleFr = "Sucre Etoile",
        subtitleEn = "Cosmic neon twilight candy",
        subtitleFr = "Bonbons cosmiques etoiles",
        priceCoins = 400
    ),
    CITRUS_SODA(
        id = "citrus",
        titleEn = "Citrus Fizz",
        titleFr = "Agrumes Petillants",
        subtitleEn = "Zesty lemon, lime & tangerine",
        subtitleFr = "Citron, lime et mandarine",
        priceCoins = 550
    ),
    ROYAL_VELVET(
        id = "velvet",
        titleEn = "Royal Cocoa",
        titleFr = "Cacao Royal",
        subtitleEn = "Rich caramel gold & berry fudge",
        subtitleFr = "Caramel dore et fondant",
        priceCoins = 750
    )
}

data class LevelData(
    val level: Int,
    val theme: String,
    val words: List<String>,
    val gridSize: Int,
    val world: String,
    val difficulty: String,
    val colorsHex: List<String>
)

data class GridCoord(
    val row: Int,
    val col: Int
)

data class WordPlacement(
    val word: String,
    val coords: List<GridCoord>,
    val colorIndex: Int
)

data class GeneratedBoard(
    val gridSize: Int,
    val letters: List<List<Char>>,
    val colorIndices: List<List<Int>>,
    val placements: List<WordPlacement>
)

data class WinSummary(
    val level: Int,
    val nextLevel: Int,
    val wordsFound: Int,
    val totalWords: Int,
    val elapsedSeconds: Int,
    val starsEarned: Int,
    val coinsReward: Int,
    val pointsBonus: Int,
    val newlyUnlockedLevel: Int?
)

enum class LossReason {
    TIME_EXPIRED
}

data class LossSummary(
    val level: Int,
    val wordsFound: Int,
    val totalWords: Int,
    val timeLimitSeconds: Int,
    val reason: LossReason = LossReason.TIME_EXPIRED
)

sealed interface GameSessionStatus {
    data object Loading : GameSessionStatus
    data class Playing(
        val remainingSeconds: Int,
        val totalTimeSeconds: Int,
        val elapsedSeconds: Int
    ) : GameSessionStatus
    data class Won(val summary: WinSummary) : GameSessionStatus
    data class Lost(val summary: LossSummary) : GameSessionStatus
}

sealed interface SelectionOutcome {
    data object Ignored : SelectionOutcome
    data object NoMatch : SelectionOutcome
    data class WordMatched(
        val word: String,
        val coords: List<GridCoord>,
        val colorIndex: Int,
        val isLevelComplete: Boolean
    ) : SelectionOutcome
}

data class WordMatchBurst(
    val id: Int,
    val coords: List<GridCoord>,
    val word: String
)

data class WorldInfo(
    val worldIndex: Int, // 0..9
    val startLevel: Int,
    val endLevel: Int,
    val nameEn: String,
    val nameFr: String
)

data class GameProgress(
    val unlockedLevel: Int = 1,
    val currentLevel: Int = 1,
    val coins: Int = 150,
    val hints: Int = 3,
    val totalPoints: Int = 0,
    val language: AppLanguage = AppLanguage.EN,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val completedLevels: Set<Int> = emptySet(),
    val starsByLevel: Map<Int, Int> = emptyMap(),
    val lastDailyRewardMs: Long = 0L,
    val activeBoardTheme: BoardTheme = BoardTheme.CANDY_CLASSIC,
    val ownedBoardThemes: Set<String> = setOf(BoardTheme.CANDY_CLASSIC.id),
    val totalWordsFound: Int = 0,
    val adminGa4PropertyId: String = ""
)

val ALL_WORLDS = listOf(
    WorldInfo(0, 1, 100, "WORLD 1 - CANDY MEADOW", "MONDE 1 - PRAIRIE BONBON"),
    WorldInfo(1, 101, 200, "WORLD 2 - SUGAR VALLEY", "MONDE 2 - VALLEE SUCREE"),
    WorldInfo(2, 201, 300, "WORLD 3 - BERRY BEACH", "MONDE 3 - PLAGE BAIES"),
    WorldInfo(3, 301, 400, "WORLD 4 - LEMON LAGOON", "MONDE 4 - LAGON CITRON"),
    WorldInfo(4, 401, 500, "WORLD 5 - MINT MOUNTAIN", "MONDE 5 - MONT MENTHE"),
    WorldInfo(5, 501, 600, "WORLD 6 - CARAMEL CASTLE", "MONDE 6 - CHATEAU CARAMEL"),
    WorldInfo(6, 601, 700, "WORLD 7 - DONUT DESERT", "MONDE 7 - DESERT BEIGNET"),
    WorldInfo(7, 701, 800, "WORLD 8 - GUMMY GARDEN", "MONDE 8 - JARDIN GOMME"),
    WorldInfo(8, 801, 900, "WORLD 9 - FROSTING FJORD", "MONDE 9 - FJORD GLACE"),
    WorldInfo(9, 901, 1000, "WORLD 10 - RAINBOW REALM", "MONDE 10 - ROYAUME ARC")
)
