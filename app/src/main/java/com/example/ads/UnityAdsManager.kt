package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnityAdsStatus(
    val isConfigured: Boolean = true,
    val isInitialized: Boolean = false,
    val isInterstitialReady: Boolean = false,
    val isRewardedReady: Boolean = false,
    val isRewardedFailed: Boolean = false,
    val isPlatformMismatch: Boolean = false,
    val isPlacementMissing: Boolean = false,
    val lastError: String? = null
)

object UnityAdsManager {

    private const val TAG = "UnityAdsManager"
    private const val UNITY_GAME_ID = "800453986"
    private val TEST_MODE = BuildConfig.DEBUG

    // Unity Ads Android Network Placement IDs
    private const val INTERSTITIAL_PLACEMENT = "BP_Interstitial_Android"
    private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
    const val BANNER_PLACEMENT = "BP_Banner_Android"

    private val _status = MutableStateFlow(UnityAdsStatus())
    val status: StateFlow<UnityAdsStatus> = _status.asStateFlow()

    fun initialize(context: Context) {
        _status.update {
            it.copy(
                isConfigured = true,
                isPlatformMismatch = false,
                isPlacementMissing = false,
                isRewardedFailed = false,
                lastError = null
            )
        }
        if (UnityAds.isInitialized) {
            _status.update { it.copy(isInitialized = true) }
            preloadInterstitial()
            preloadRewarded()
            return
        }

        UnityAds.initialize(
            context.applicationContext,
            UNITY_GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d(TAG, "Unity Ads initialization complete (Game ID: $UNITY_GAME_ID, testMode=$TEST_MODE)")
                    _status.update {
                        it.copy(
                            isInitialized = true,
                            isPlatformMismatch = false,
                            lastError = null
                        )
                    }
                    preloadInterstitial()
                    preloadRewarded()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    Log.w(TAG, "Unity Ads init failed ($error): $message")
                    val mismatch = message.contains("mismatched platform", ignoreCase = true)
                    _status.update {
                        it.copy(
                            isInitialized = false,
                            isPlatformMismatch = mismatch,
                            lastError = "$error: $message"
                        )
                    }
                }
            }
        )
    }

    fun preloadInterstitial() {
        if (!UnityAds.isInitialized) {
            _status.update { it.copy(isInterstitialReady = false) }
            return
        }
        UnityAds.load(
            INTERSTITIAL_PLACEMENT,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    _status.update { it.copy(isInterstitialReady = true) }
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    Log.w(TAG, "Fullscreen '$placementId' failed ($error): $message")
                    val missing = message.contains("Placement not found", ignoreCase = true)
                    _status.update {
                        it.copy(
                            isInterstitialReady = false,
                            isPlacementMissing = it.isPlacementMissing || missing
                        )
                    }
                }
            }
        )
    }

    fun preloadRewarded() {
        if (!UnityAds.isInitialized) {
            _status.update { it.copy(isRewardedReady = false, isRewardedFailed = true) }
            return
        }
        UnityAds.load(
            REWARDED_PLACEMENT,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    _status.update {
                        it.copy(
                            isRewardedReady = true,
                            isRewardedFailed = false,
                            isPlacementMissing = false
                        )
                    }
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    Log.w(TAG, "Rewarded '$placementId' failed ($error): $message")
                    val missing = message.contains("Placement not found", ignoreCase = true)
                    _status.update {
                        it.copy(
                            isRewardedReady = false,
                            isRewardedFailed = true,
                            isPlacementMissing = it.isPlacementMissing || missing,
                            lastError = "$error: $message"
                        )
                    }
                }
            }
        )
    }

    fun showInterstitial(activity: Activity?, onFinished: () -> Unit) {
        if (activity == null || !UnityAds.isInitialized || !_status.value.isInterstitialReady) {
            onFinished()
            return
        }

        _status.update { it.copy(isInterstitialReady = false) }
        UnityAds.show(
            activity,
            INTERSTITIAL_PLACEMENT,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.w(TAG, "Interstitial show failed: $error - $message")
                    preloadInterstitial()
                    onFinished()
                }

                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    preloadInterstitial()
                    onFinished()
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailableOrSkipped: () -> Unit = {}
    ) {
        if (activity == null || !UnityAds.isInitialized || !_status.value.isRewardedReady) {
            onUnavailableOrSkipped()
            return
        }

        _status.update { it.copy(isRewardedReady = false) }
        UnityAds.show(
            activity,
            REWARDED_PLACEMENT,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.w(TAG, "Rewarded show failed: $error - $message")
                    preloadRewarded()
                    onUnavailableOrSkipped()
                }

                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    preloadRewarded()
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onRewardEarned()
                    } else {
                        onUnavailableOrSkipped()
                    }
                }
            }
        )
    }
}

@Composable
fun UnityBannerAdBar(
    isInitialized: Boolean,
    reloadKey: Any? = null,
    modifier: Modifier = Modifier
) {
    if (!isInitialized) return
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bannerWidthPx = remember(density) { with(density) { 320.dp.roundToPx() } }
    val bannerHeightPx = remember(density) { with(density) { 50.dp.roundToPx() } }

    var bannerFailed by remember { mutableStateOf(false) }
    var placementMissing by remember { mutableStateOf(false) }
    val bannerView = remember(activity) {
        BannerView(activity, UnityAdsManager.BANNER_PLACEMENT, UnityBannerSize(320, 50))
    }

    DisposableEffect(bannerView) {
        bannerView.listener = object : BannerView.Listener() {
            override fun onBannerLoaded(bannerAdView: BannerView?) {
                bannerFailed = false
                placementMissing = false
            }

            override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                bannerFailed = true
                val msg = errorInfo?.errorMessage.orEmpty()
                if (msg.contains("Placement not found", ignoreCase = true)) {
                    placementMissing = true
                }
                Log.w("UnityBannerAdBar", "Banner failed: $msg")
            }
        }
        bannerView.load()
        onDispose {
            bannerView.destroy()
        }
    }

    androidx.compose.runtime.LaunchedEffect(reloadKey) {
        if (bannerFailed && !placementMissing) {
            bannerFailed = false
            bannerView.load()
        }
    }

    if (bannerFailed) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("unity_banner_ad"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    (bannerView.parent as? android.view.ViewGroup)?.removeView(bannerView)
                    addView(
                        bannerView,
                        FrameLayout.LayoutParams(
                            bannerWidthPx,
                            bannerHeightPx,
                            Gravity.CENTER
                        )
                    )
                }
            },
            update = { container ->
                if (bannerView.parent !== container) {
                    (bannerView.parent as? android.view.ViewGroup)?.removeView(bannerView)
                    container.removeAllViews()
                    container.addView(
                        bannerView,
                        FrameLayout.LayoutParams(
                            bannerWidthPx,
                            bannerHeightPx,
                            Gravity.CENTER
                        )
                    )
                }
            }
        )
    }
}
