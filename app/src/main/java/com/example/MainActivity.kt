package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.UnityAdsManager
import com.example.ads.UnityBannerAdBar
import com.example.audio.LocalHaptics
import com.example.audio.LocalSoundEffects
import com.example.model.LocalizedStrings
import com.example.ui.WordSplashViewModel
import com.example.ui.components.BottomCandyNavBar
import com.example.ui.components.CandyGradientBackground
import com.example.ui.components.CandyNavItem
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.LossScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.WinScreen
import com.example.ui.theme.WordSplashTheme

const val ROUTE_HOME = "home"
const val ROUTE_LEVEL_MAP = "level_map"
const val ROUTE_GAMEPLAY = "gameplay"
const val ROUTE_SHOP = "shop"
const val ROUTE_COLLECTIONS = "collections"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        UnityAdsManager.initialize(this)
        setContent {
            WordSplashTheme {
                WordSplashApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UnityAdsManager.onActivityResumed(this)
    }

    override fun onDestroy() {
        UnityAdsManager.releaseBanner(this)
        super.onDestroy()
    }
}

@Composable
fun WordSplashApp(
    viewModel: WordSplashViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val adsStatus by UnityAdsManager.status.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val progress = state.progress
    val lang = progress.language
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    var previousRoute by remember { mutableStateOf<String?>(null) }
    val lastRouteRef = remember { object { var value: String = currentRoute } }
    val transitionFromRoute = if (lastRouteRef.value != currentRoute) {
        lastRouteRef.value
    } else {
        previousRoute
    }
    SideEffect {
        if (lastRouteRef.value != currentRoute) {
            previousRoute = lastRouteRef.value
            lastRouteRef.value = currentRoute
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val navItems = remember(lang) {
        listOf(
            CandyNavItem(ROUTE_HOME, LocalizedStrings.navHome(lang), Icons.Rounded.Home, "nav_home"),
            CandyNavItem(ROUTE_LEVEL_MAP, LocalizedStrings.navLevels(lang), Icons.Rounded.Map, "nav_levels"),
            CandyNavItem(ROUTE_SHOP, LocalizedStrings.navShop(lang), Icons.Rounded.CardGiftcard, "nav_shop"),
            CandyNavItem(ROUTE_COLLECTIONS, LocalizedStrings.navCollections(lang), Icons.Rounded.EmojiEvents, "nav_collections"),
            CandyNavItem(ROUTE_SETTINGS, LocalizedStrings.navSettings(lang), Icons.Rounded.Settings, "nav_settings")
        )
    }

    CompositionLocalProvider(
        LocalSoundEffects provides viewModel.soundEffects,
        LocalHaptics provides viewModel.haptics
    ) {
    CandyGradientBackground(boardTheme = progress.activeBoardTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            progress = progress,
                            onPlayClick = {
                                viewModel.startLevel(progress.unlockedLevel)
                                navController.navigate(ROUTE_GAMEPLAY) {
                                    launchSingleTop = true
                                }
                            },
                            onDailyRewardClick = { viewModel.claimDailyReward() },
                            onToggleLanguage = { viewModel.toggleLanguage() }
                        )
                    }

                    composable(ROUTE_LEVEL_MAP) { entry ->
                        val fromGameplay = transitionFromRoute == ROUTE_GAMEPLAY
                        DirectionalSlideIn(
                            fromDirection = if (fromGameplay) -1 else 0,
                            slideKey = entry.id,
                            testTag = "level_map_slide_container"
                        ) {
                            LevelMapScreen(
                                progress = progress,
                                onSelectLevel = { level ->
                                    viewModel.startLevel(level)
                                    navController.navigate(ROUTE_GAMEPLAY) {
                                        launchSingleTop = true
                                    }
                                },
                                onToggleLanguage = { viewModel.toggleLanguage() }
                            )
                        }
                    }

                    composable(ROUTE_GAMEPLAY) { entry ->
                        val fromLevelMap = transitionFromRoute == ROUTE_LEVEL_MAP
                        DirectionalSlideIn(
                            fromDirection = if (fromLevelMap) 1 else 0,
                            slideKey = entry.id,
                            testTag = "gameplay_slide_container"
                        ) {
                            GameplayScreen(
                                progress = progress,
                                gameplay = state.gameplay,
                                onDragStart = { viewModel.onDragStart(it) },
                                onDragMove = { viewModel.onDragMove(it) },
                                onDragEnd = { viewModel.onDragEnd() },
                                onUseHint = { viewModel.useHint() },
                                onUseWordHint = { viewModel.useWordHint() },
                                onShuffle = { viewModel.shuffleBoard() },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onNavigateHome = {
                                    viewModel.soundEffects.playClick()
                                    viewModel.stopTimer()
                                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                                },
                                onNavigateLevels = {
                                    viewModel.soundEffects.playClick()
                                    viewModel.stopTimer()
                                    val popped = navController.popBackStack(ROUTE_LEVEL_MAP, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(ROUTE_LEVEL_MAP) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onNavigateSettings = {
                                    viewModel.soundEffects.playClick()
                                    viewModel.stopTimer()
                                    navController.navigate(ROUTE_SETTINGS) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }

                    composable(ROUTE_SHOP) {
                        ShopScreen(
                            progress = progress,
                            adsStatus = adsStatus,
                            onWatchRewardedAd = {
                                viewModel.soundEffects.playClick()
                                UnityAdsManager.showRewarded(
                                    activity = activity,
                                    onRewardEarned = { viewModel.grantRewardedShopBonus() }
                                )
                            },
                            onBuyHints = { amt, cost -> viewModel.buyHints(amt, cost) },
                            onBuyOrEquipTheme = { viewModel.buyOrEquipTheme(it) },
                            onToggleLanguage = { viewModel.toggleLanguage() }
                        )
                    }

                    composable(ROUTE_COLLECTIONS) {
                        CollectionsScreen(
                            progress = progress,
                            onToggleLanguage = { viewModel.toggleLanguage() }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            progress = progress,
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onSetSoundEnabled = { viewModel.setSoundEnabled(it) },
                            onSetHapticsEnabled = { viewModel.setHapticsEnabled(it) },
                            onResetProgress = { viewModel.resetProgress() }
                        )
                    }
                }

                // Win / Loss Celebration Overlays
                val winSummary = state.gameplay.winSummary
                val lossSummary = state.gameplay.lossSummary
                if (currentRoute == ROUTE_GAMEPLAY && winSummary != null) {
                    WinScreen(
                        summary = winSummary,
                        language = lang,
                        onNextLevel = {
                            if (viewModel.consumeInterstitialTriggerIfDue()) {
                                UnityAdsManager.showInterstitial(activity) {
                                    viewModel.startLevel(winSummary.nextLevel)
                                }
                            } else {
                                viewModel.startLevel(winSummary.nextLevel)
                            }
                        },
                        onHome = {
                            viewModel.soundEffects.playClick()
                            navController.popBackStack(ROUTE_HOME, inclusive = false)
                        }
                    )
                } else if (currentRoute == ROUTE_GAMEPLAY && lossSummary != null) {
                    LossScreen(
                        summary = lossSummary,
                        coins = progress.coins,
                        language = lang,
                        isRewardedAdReady = adsStatus.isConfigured && adsStatus.isRewardedReady,
                        onWatchAdExtraTime = {
                            viewModel.soundEffects.playClick()
                            UnityAdsManager.showRewarded(
                                activity = activity,
                                onRewardEarned = { viewModel.grantRewardedExtraTime() }
                            )
                        },
                        onExtraTime = { viewModel.buyExtraTime() },
                        onRetry = { viewModel.retryCurrentLevel() },
                        onHome = {
                            viewModel.soundEffects.playClick()
                            navController.popBackStack(ROUTE_HOME, inclusive = false)
                        }
                    )
                }
            }

            Box(
                modifier = if (currentRoute == ROUTE_GAMEPLAY) {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier
                }
            ) {
                UnityBannerAdBar(adsStatus = adsStatus)
            }

            if (currentRoute != ROUTE_GAMEPLAY) {
                BottomCandyNavBar(
                    items = navItems,
                    selectedRoute = currentRoute,
                    onSelect = { route ->
                        viewModel.soundEffects.playClick()
                        navController.navigate(route) {
                            popUpTo(ROUTE_HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
        )
    }
    }
}

/**
 * Slides screen content horizontally into place when transitioning between
 * LevelMapScreen and GameplayScreen (fromDirection: +1 = from right, -1 = from left, 0 = instant).
 */
@Composable
fun DirectionalSlideIn(
    fromDirection: Int,
    slideKey: Any?,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (fromDirection == 0) {
        Box(modifier = modifier.fillMaxSize().testTag(testTag)) {
            content()
        }
        return
    }

    val visibleState = remember(slideKey, fromDirection) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> if (fromDirection > 0) fullWidth else -fullWidth }
        ) + fadeIn(animationSpec = tween(durationMillis = 240)),
        modifier = modifier.fillMaxSize().testTag(testTag)
    ) {
        content()
    }
}
