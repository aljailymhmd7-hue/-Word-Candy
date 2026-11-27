package com.example

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ads.UnityAdsManager
import com.example.ads.UnityAdsStatus
import com.example.ads.UnityBannerAdBar
import com.example.data.LevelRepository
import com.example.engine.WordSplashGameEngine
import com.example.model.AppLanguage
import com.example.model.GameProgress
import com.example.ui.GameplayUiState
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.theme.WordSplashTheme
import com.unity3d.ads.core.domain.HttpClientProvider
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.core.device.StorageManager
import com.unity3d.services.core.di.ServiceKey
import com.unity3d.services.core.di.ServiceProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnityAdsHttpClientTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `configureNonGmsHttpClient selects OkHttp3Client without GMS Cronet`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        UnityAdsManager.configureNonGmsHttpClient(context)

        val memoryStorage = StorageManager.getStorage(StorageManager.StorageType.MEMORY)
        val traits = memoryStorage?.get("mediation.traits.value") as? JSONObject
        assertEquals("OKHTTP3", traits?.optString("useHttpClient"))

        val registry = ServiceProvider.getRegistry()
        val readerClass = Class.forName("com.unity3d.ads.core.configuration.MediationTraitsMetadataReader").kotlin
        val reader = registry.resolveService<Any>(ServiceKey("", readerClass))
        val getStringTrait = readerClass.java.getMethod("getStringTrait", String::class.java)
        assertEquals("OKHTTP3", getStringTrait.invoke(reader, "useHttpClient"))

        val httpClientProvider = registry.resolveService<HttpClientProvider>(
            ServiceKey("", HttpClientProvider::class)
        )
        val client = httpClientProvider.invoke()
        assertEquals("OkHttp3Client", client::class.java.simpleName)
    }

    @Test
    fun `banner uses BP_Banner_Android primary with Banner_Android fallback, 2s init delay, and 30s retry on NO_FILL`() {
        assertEquals("Banner_Android", UnityAdsManager.BANNER_PLACEMENT)
        assertEquals(listOf("BP_Banner_Android", "Banner_Android"), UnityAdsManager.BANNER_PLACEMENT_CANDIDATES)
        assertEquals(2_000L, UnityAdsManager.BANNER_INIT_DELAY_MS)
        assertEquals(5_000L, UnityAdsManager.BANNER_FALLBACK_TIMEOUT_MS)
        assertEquals(30_000L, UnityAdsManager.BANNER_NO_FILL_RETRY_DELAY_MS)

        var fallbackInvoked = 0
        UnityAdsManager.handleBannerLoadFailure(
            placementId = "BP_Banner_Android",
            errorCode = BannerErrorCode.NATIVE_ERROR,
            errorMessage = "Placement not found",
            onFallbackNextPlacement = { fallbackInvoked++ }
        )
        assertEquals(1, fallbackInvoked)
        assertEquals("Banner_Android", UnityAdsManager.currentBannerPlacement())
        assertEquals(false, UnityAdsManager.status.value.isPlacementMissing)

        var scheduledDelayMs: Long? = null
        UnityAdsManager.handleBannerLoadFailure(
            placementId = "Banner_Android",
            errorCode = BannerErrorCode.NO_FILL,
            errorMessage = "No fill",
            scheduleRetry = { delay -> scheduledDelayMs = delay }
        )

        assertEquals(30_000L, scheduledDelayMs)
        assertEquals(false, UnityAdsManager.status.value.isBannerReady)
        assertTrue(UnityAdsManager.status.value.showBannerFallbackText)
        assertTrue(UnityAdsManager.status.value.lastError.orEmpty().contains("NO_FILL"))
    }

    @Test
    fun `banner ad renders 60dp black bar at bottom of gameplay screen without overlapping buttons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val level4 = runBlocking { repo.getLevel(AppLanguage.EN, 4) }
        val engine = WordSplashGameEngine()
        engine.startLevel(level4, previouslyUnlockedLevel = 4)
        val gameplayState = GameplayUiState(
            levelData = engine.state.levelData,
            board = engine.state.board,
            isLoading = false
        )
        val adsStatus = UnityAdsStatus(isConfigured = true, isInitialized = true, isBannerReady = true)
        val fakeBannerView = View(context)

        composeRule.setContent {
            WordSplashTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
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
                    UnityBannerAdBar(
                        adsStatus = adsStatus,
                        bannerViewProvider = { fakeBannerView }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val bannerNode = composeRule.onNodeWithTag("unity_banner_ad").assertIsDisplayed()
        val bannerBounds = bannerNode.getUnclippedBoundsInRoot()
        val hintBounds = composeRule.onNodeWithTag("hint_button").assertIsDisplayed().getUnclippedBoundsInRoot()
        val shuffleBounds = composeRule.onNodeWithTag("shuffle_button").assertIsDisplayed().getUnclippedBoundsInRoot()

        val bannerHeightDp = (bannerBounds.bottom - bannerBounds.top).value
        assertEquals(60f, bannerHeightDp, 0.5f)
        assertTrue(bannerBounds.top >= hintBounds.bottom - 0.5.dp)
        assertTrue(bannerBounds.top >= shuffleBounds.bottom - 0.5.dp)
    }

    @Test
    fun `banner ad black bar stays visible at bottom of level map screen and displays Advertisement fallback when ad fails`() {
        val adsStatus = UnityAdsStatus(
            isConfigured = true,
            isInitialized = true,
            isBannerReady = false,
            showBannerFallbackText = true
        )

        composeRule.setContent {
            WordSplashTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        LevelMapScreen(
                            progress = GameProgress(language = AppLanguage.EN, unlockedLevel = 4),
                            onSelectLevel = {},
                            onToggleLanguage = {}
                        )
                    }
                    UnityBannerAdBar(
                        adsStatus = adsStatus,
                        bannerViewProvider = { null }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val bannerBounds = composeRule.onNodeWithTag("unity_banner_ad")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val playButtonBounds = composeRule.onNodeWithTag("map_play_current_button")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertEquals(60f, (bannerBounds.bottom - bannerBounds.top).value, 0.5f)
        assertTrue(bannerBounds.top >= playButtonBounds.bottom - 0.5.dp)
        composeRule.onNodeWithText("Advertisement").assertIsDisplayed()
    }
}
