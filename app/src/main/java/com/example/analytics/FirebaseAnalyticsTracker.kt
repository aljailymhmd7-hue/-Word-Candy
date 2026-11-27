package com.example.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

/**
 * Lightweight Firebase Analytics wrapper.
 *
 * Enabling collection via [FirebaseAnalytics.setAnalyticsCollectionEnabled] (together with the
 * manifest `firebase_analytics_collection_enabled` metadata) ensures Firebase automatically tracks
 * `first_open` on first app launch after install and `app_remove` on uninstall.
 */
object FirebaseAnalyticsTracker {

    private const val TAG = "FirebaseAnalytics"

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context): FirebaseAnalytics? {
        val appContext = context.applicationContext ?: context
        return runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            val instance = Firebase.analytics
            instance.setAnalyticsCollectionEnabled(true)
            analytics = instance
            Log.i(TAG, "Firebase Analytics enabled (automatic first_open & app_remove active)")
            instance
        }.onFailure { error ->
            Log.w(TAG, "Firebase Analytics init skipped/unavailable: ${error.message}")
        }.getOrNull()
    }

    fun isInitialized(): Boolean = analytics != null

    fun trackLevelDownload(levelNumber: Int) {
        logEvent("level_download") {
            putString("level_name", "Level $levelNumber")
            putInt("level_id", levelNumber)
        }
    }

    fun trackLevelComplete(levelNumber: Int) {
        logEvent(FirebaseAnalytics.Event.LEVEL_UP) {
            putInt(FirebaseAnalytics.Param.LEVEL, levelNumber)
        }
    }

    fun logLevelStart(levelNumber: Int, wordsCount: Int, gridSize: Int, language: String) {
        logEvent(FirebaseAnalytics.Event.LEVEL_START) {
            putLong(FirebaseAnalytics.Param.LEVEL, levelNumber.toLong())
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Level_$levelNumber")
            putLong("words_count", wordsCount.toLong())
            putLong("grid_size", gridSize.toLong())
            putString("language", language)
        }
    }

    fun logLevelEnd(levelNumber: Int, wordsFound: Int, starsEarned: Int, success: Boolean) {
        logEvent(FirebaseAnalytics.Event.LEVEL_END) {
            putLong(FirebaseAnalytics.Param.LEVEL, levelNumber.toLong())
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Level_$levelNumber")
            putLong("words_found", wordsFound.toLong())
            putLong("stars_earned", starsEarned.toLong())
            putLong(FirebaseAnalytics.Param.SUCCESS, if (success) 1L else 0L)
        }
    }

    private inline fun logEvent(name: String, paramsBuilder: Bundle.() -> Unit) {
        val instance = analytics ?: return
        runCatching {
            val bundle = Bundle().apply(paramsBuilder)
            instance.logEvent(name, bundle)
        }
    }
}
