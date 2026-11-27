package com.example.model

object LocalizedStrings {
    fun play(lang: AppLanguage) = if (lang == AppLanguage.EN) "PLAY" else "JOUER"
    fun playLevel(lang: AppLanguage, level: Int) =
        if (lang == AppLanguage.EN) "PLAY LEVEL $level" else "JOUER NIVEAU $level"

    fun levelsReady(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "1000+ LEVELS READY TO PLAY!" else "1000+ NIVEAUX PRETS A JOUER !"

    fun dailyReward(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "DAILY REWARD +50 COINS FREE" else "CADEAU DU JOUR +50 PIECES"

    fun dailyClaimed(lang: AppLanguage, hoursLeft: Int) =
        if (lang == AppLanguage.EN) "NEXT REWARD IN ${hoursLeft}H" else "PROCHAIN CADEAU DANS ${hoursLeft}H"

    fun navHome(lang: AppLanguage) = if (lang == AppLanguage.EN) "HOME" else "ACCUEIL"
    fun navLevels(lang: AppLanguage) = if (lang == AppLanguage.EN) "LEVELS" else "NIVEAUX"
    fun navShop(lang: AppLanguage) = if (lang == AppLanguage.EN) "SHOP" else "BOUTIQUE"
    fun navCollections(lang: AppLanguage) = if (lang == AppLanguage.EN) "COLLECTIONS" else "TROPHEES"
    fun navSettings(lang: AppLanguage) = if (lang == AppLanguage.EN) "SETTINGS" else "REGLAGES"

    fun levelProgressSummary(lang: AppLanguage, current: Int, completed: Int, percent: Int): String =
        if (lang == AppLanguage.EN) {
            "Level $current / 1000 - $percent% - $completed/1000 LEVELS COMPLETE"
        } else {
            "Niveau $current / 1000 - $percent% - $completed/1000 NIVEAUX TERMINES"
        }

    fun levelBadge(lang: AppLanguage, level: Int) =
        if (lang == AppLanguage.EN) "LEVEL $level" else "NIVEAU $level"

    fun findTheWords(lang: AppLanguage, level: Int, totalWords: Int) =
        if (lang == AppLanguage.EN) "Level $level: Find $totalWords Words"
        else "Niveau $level : Trouvez $totalWords Mots"

    fun wordsFoundRatio(lang: AppLanguage, found: Int, total: Int) =
        if (lang == AppLanguage.EN) "$found/$total WORDS FOUND"
        else "$found/$total MOTS TROUVES"

    fun hintLeft(lang: AppLanguage, hints: Int) =
        if (lang == AppLanguage.EN) "HINT ($hints LEFT)" else "INDICE ($hints)"

    fun shuffle(lang: AppLanguage) = if (lang == AppLanguage.EN) "Shuffle" else "Melanger"

    fun youWin(lang: AppLanguage) = if (lang == AppLanguage.EN) "YOU WIN!" else "BRAVO !"
    fun levelComplete(lang: AppLanguage, level: Int) =
        if (lang == AppLanguage.EN) "Level $level Complete!" else "Niveau $level Termine !"

    fun rewardCoins(lang: AppLanguage, coins: Int) =
        if (lang == AppLanguage.EN) "REWARD +$coins COINS" else "GAIN +$coins PIECES"

    fun bonusPoints(lang: AppLanguage, points: Int) =
        if (lang == AppLanguage.EN) "BONUS: +$points POINTS" else "BONUS : +$points POINTS"

    fun amazingSummary(lang: AppLanguage, found: Int, total: Int, seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        val time = "%02d:%02d".format(m, s)
        return if (lang == AppLanguage.EN) {
            "Amazing! $found/$total words found in $time!"
        } else {
            "Genial ! $found/$total mots trouves en $time !"
        }
    }

    fun levelUnlocked(lang: AppLanguage, level: Int) =
        if (lang == AppLanguage.EN) "Level $level unlocked!" else "Niveau $level debloque !"

    fun nextLevel(lang: AppLanguage) = if (lang == AppLanguage.EN) "NEXT LEVEL" else "NIVEAU SUIVANT"

    fun shopTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "CANDY SHOP" else "BOUTIQUE BONBON"
    fun hintPack3(lang: AppLanguage) = if (lang == AppLanguage.EN) "3 Hints Pack" else "Pack 3 Indices"
    fun hintPack10(lang: AppLanguage) = if (lang == AppLanguage.EN) "10 Hints Mega Pack" else "Mega Pack 10 Indices"
    fun boardThemesTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "Board Color Themes" else "Themes de Grille"
    fun equipped(lang: AppLanguage) = if (lang == AppLanguage.EN) "EQUIPPED" else "EQUIPE"
    fun equip(lang: AppLanguage) = if (lang == AppLanguage.EN) "EQUIP" else "EQUIPER"
    fun notEnoughCoins(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Need more coins! Win levels or claim Daily Reward." else "Pas assez de pieces ! Gagnez des niveaux."

    fun collectionsTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "COLLECTIONS" else "COLLECTIONS"
    fun statsStars(lang: AppLanguage) = if (lang == AppLanguage.EN) "Total Stars" else "Etoiles"
    fun statsWords(lang: AppLanguage) = if (lang == AppLanguage.EN) "Words Found" else "Mots Trouves"
    fun statsPoints(lang: AppLanguage) = if (lang == AppLanguage.EN) "Total Score" else "Score Total"

    fun settingsTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "SETTINGS" else "REGLAGES"
    fun languageLabel(lang: AppLanguage) = if (lang == AppLanguage.EN) "Language" else "Langue"
    fun soundLabel(lang: AppLanguage) = if (lang == AppLanguage.EN) "Sound Effects" else "Effets Sonores"
    fun hapticsLabel(lang: AppLanguage) = if (lang == AppLanguage.EN) "Vibration Feedback" else "Vibrations"
    fun resetProgress(lang: AppLanguage) = if (lang == AppLanguage.EN) "Reset Progress" else "Reinitialiser"
    fun resetConfirmTitle(lang: AppLanguage) = if (lang == AppLanguage.EN) "Reset all progress?" else "Tout reinitialiser ?"
    fun resetConfirmBody(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "This clears unlocked levels, stars, and coins." else "Cela efface vos niveaux, etoiles et pieces."
    fun confirm(lang: AppLanguage) = if (lang == AppLanguage.EN) "Reset" else "Confirmer"
    fun cancel(lang: AppLanguage) = if (lang == AppLanguage.EN) "Cancel" else "Annuler"

    fun timesUp(lang: AppLanguage) = if (lang == AppLanguage.EN) "TIME'S UP!" else "TEMPS ECOULE !"
    fun levelFailed(lang: AppLanguage, level: Int) =
        if (lang == AppLanguage.EN) "Level $level Failed" else "Niveau $level Echoue"
    fun foundOutOf(lang: AppLanguage, found: Int, total: Int) =
        if (lang == AppLanguage.EN) "You found $found of $total words!" else "Vous avez trouve $found sur $total mots !"
    fun extraTime(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "+30s EXTRA TIME (30 COINS)" else "+30s TEMPS EXTRA (30 PIECES)"
    fun tryAgain(lang: AppLanguage) = if (lang == AppLanguage.EN) "TRY AGAIN" else "REESSAYER"

    fun watchAdCoinsTitle(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Watch Video Ad" else "Regarder une Video"
    fun watchAdCoinsSubtitle(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Earn +25 Coins & +1 Hint free" else "Gagnez +25 Pieces & +1 Indice"
    fun watchAdButton(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "WATCH AD" else "VOIR PUB"
    fun adLoadingButton(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "LOADING..." else "CHARGEMENT..."
    fun adConfigureHint(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Set UNITY_GAME_ID in Secrets" else "Definir UNITY_GAME_ID"
    fun adAndroidIdHint(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Use Android Game ID" else "ID Android requis"
    fun adUnavailable(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "UNAVAILABLE" else "INDISPONIBLE"
    fun watchAdExtraTime(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "WATCH AD • +30s FREE" else "VOIR PUB • +30s GRATUIT"

    fun hintDialogTitle(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "POWER HINTS" else "INDICES BONUS"
    fun revealLetterTitle(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Reveal a Letter" else "Reveler une Lettre"
    fun revealLetterDesc(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Spot one letter of a hidden word" else "Montre une lettre d'un mot cache"
    fun highlightWordTitle(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Highlight a Word" else "Surligner un Mot"
    fun highlightWordDesc(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Glow an entire hidden word on the grid" else "Illumine tout un mot sur la grille"
    fun hintTokenCost(lang: AppLanguage, count: Int) =
        if (lang == AppLanguage.EN) "1 HINT ($count)" else "1 INDICE ($count)"
    fun coinsCost(lang: AppLanguage, cost: Int) =
        if (lang == AppLanguage.EN) "$cost COINS" else "$cost PIECES"
    fun wordHighlightedToast(lang: AppLanguage, word: String) =
        if (lang == AppLanguage.EN) "Word highlighted: $word!" else "Mot eclaire : $word !"
    fun letterRevealedToast(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "Letter revealed!" else "Lettre revelee !"
    fun allWordsHighlightedToast(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "All remaining words are already highlighted!" else "Tous les mots restants sont deja eclaires !"

    fun themesUnlockedSummary(lang: AppLanguage, owned: Int, total: Int) =
        if (lang == AppLanguage.EN) "$owned / $total Grid Themes Unlocked" else "$owned / $total Themes Debloques"
    fun unlockThemeTitle(lang: AppLanguage, title: String) =
        if (lang == AppLanguage.EN) "Unlock $title?" else "Debloquer $title ?"
    fun unlockThemeBody(lang: AppLanguage, price: Int) =
        if (lang == AppLanguage.EN) "Spend $price coins to unlock and equip this puzzle grid theme." else "Depensez $price pieces pour debloquer et equiper ce theme."
    fun unlockButton(lang: AppLanguage) =
        if (lang == AppLanguage.EN) "UNLOCK" else "DEBLOQUER"
    fun themeUnlockedToast(lang: AppLanguage, title: String) =
        if (lang == AppLanguage.EN) "Unlocked & equipped: $title!" else "Debloque et equipe : $title !"
    fun themeEquippedToast(lang: AppLanguage, title: String) =
        if (lang == AppLanguage.EN) "Equipped theme: $title" else "Theme equipe : $title"
}
