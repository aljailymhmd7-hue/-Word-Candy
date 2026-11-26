package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnityAdsStatus(
    val isConfigured: Boolean = true,
    val isInitialized: Boolean = false,
    val isBannerReady: Boolean = false,
    val isInterstitialReady: Boolean = false,
    val isRewardedReady: Boolean = false,
    val isRewardedLoading: Boolean = false,
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

    private const val MAX_REWARDED_RETRIES = 4
    private const val MAX_BANNER_RETRIES = 5

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(UnityAdsStatus())
    val status: StateFlow<UnityAdsStatus> = _status.asStateFlow()

    private var activityRef: WeakReference<Activity>? = null
    private var bannerActivityRef: WeakReference<Activity>? = null
    private var bannerView: BannerView? = null
    private var isBannerLoading = false
    private var isInterstitialLoading = false
    private var rewardedRetryAttempt = 0
    private var bannerRetryAttempt = 0

    private val rewardedRetryRunnable = Runnable { preloadRewarded() }
    private val bannerRetryRunnable = Runnable { preloadBanner(activityRef?.get()) }

    fun initialize(context: Context) {
        findActivity(context)?.let { activityRef = WeakReference(it) }

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
            preloadAll()
            return
        }

        UnityAds.initialize(
            context.applicationContext,
            UNITY_GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    runOnMainThread {
                        Log.d(
                            TAG,
                            "Unity Ads initialization complete (Game ID: $UNITY_GAME_ID, testMode=$TEST_MODE)"
                        )
                        _status.update {
                            it.copy(
                                isInitialized = true,
                                isPlatformMismatch = false,
                                lastError = null
                            )
                        }
                        preloadAll()
                    }
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    runOnMainThread {
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
            }
        )
    }

    fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
        if (UnityAds.isInitialized) {
            if (!_status.value.isBannerReady && !isBannerLoading) {
                bannerRetryAttempt = 0
                preloadBanner(activity)
            }
            if (!_status.value.isRewardedReady && !_status.value.isRewardedLoading) {
                rewardedRetryAttempt = 0
                preloadRewarded()
            }
            if (!_status.value.isInterstitialReady && !isInterstitialLoading) {
                preloadInterstitial()
            }
        }
    }

    fun releaseBanner(activity: Activity) {
        runOnMainThread {
            if (bannerActivityRef?.get() === activity) {
                mainHandler.removeCallbacks(bannerRetryRunnable)
                isBannerLoading = false
                bannerView?.let { bv ->
                    (bv.parent as? ViewGroup)?.removeView(bv)
                    bv.destroy()
                }
                bannerView = null
                bannerActivityRef = null
                _status.update { it.copy(isBannerReady = false) }
            }
        }
    }

    fun getBannerView(): BannerView? = bannerView

    private fun preloadAll() {
        preloadBanner(activityRef?.get())
        preloadInterstitial()
        preloadRewarded()
    }

    fun preloadBanner(activity: Activity? = activityRef?.get()) {
        runOnMainThread {
            if (activity == null || activity.isFinishing || activity.isDestroyed || !UnityAds.isInitialized) {
                return@runOnMainThread
            }
            mainHandler.removeCallbacks(bannerRetryRunnable)

            val existingOwner = bannerActivityRef?.get()
            if (bannerView != null && existingOwner !== activity) {
                (bannerView?.parent as? ViewGroup)?.removeView(bannerView)
                bannerView?.destroy()
                bannerView = null
                isBannerLoading = false
                _status.update { it.copy(isBannerReady = false) }
            }

            if (bannerView != null && (_status.value.isBannerReady || isBannerLoading)) {
                return@runOnMainThread
            }

            val bv = bannerView ?: BannerView(
                activity,
                BANNER_PLACEMENT,
                UnityBannerSize(320, 50)
            ).also { created ->
                created.listener = object : BannerView.Listener() {
                    override fun onBannerLoaded(bannerAdView: BannerView?) {
                        runOnMainThread {
                            bannerRetryAttempt = 0
                            isBannerLoading = false
                            _status.update {
                                it.copy(
                                    isBannerReady = true,
                                    isPlacementMissing = false,
                                    lastError = null
                                )
                            }
                        }
                    }

                    override fun onBannerFailedToLoad(
                        bannerAdView: BannerView?,
                        errorInfo: BannerErrorInfo?
                    ) {
                        runOnMainThread {
                            isBannerLoading = false
                            val msg = errorInfo?.errorMessage.orEmpty()
                            val missing = msg.contains("Placement not found", ignoreCase = true)
                            Log.w(TAG, "Banner '$BANNER_PLACEMENT' failed: $msg")
                            _status.update {
                                it.copy(
                                    isBannerReady = false,
                                    isPlacementMissing = it.isPlacementMissing || missing,
                                    lastError = msg.ifBlank { it.lastError }
                                )
                            }
                            if (!missing && bannerRetryAttempt < MAX_BANNER_RETRIES) {
                                bannerRetryAttempt++
                                mainHandler.postDelayed(
                                    bannerRetryRunnable,
                                    bannerRetryAttempt * 4000L
                                )
                            }
                        }
                    }
                }
                bannerView = created
                bannerActivityRef = WeakReference(activity)
            }

            isBannerLoading = true
            _status.update { it.copy(isBannerReady = false) }
            bv.load()
        }
    }

    fun preloadInterstitial() {
        runOnMainThread {
            if (!UnityAds.isInitialized) {
                _status.update { it.copy(isInterstitialReady = false) }
                return@runOnMainThread
            }
            if (isInterstitialLoading || _status.value.isInterstitialReady) {
                return@runOnMainThread
            }
            isInterstitialLoading = true
            UnityAds.load(
                INTERSTITIAL_PLACEMENT,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        runOnMainThread {
                            isInterstitialLoading = false
                            _status.update { it.copy(isInterstitialReady = true) }
                        }
                    }

                    override fun onUnityAdsFailedToLoad(
                        placementId: String,
                        error: UnityAds.UnityAdsLoadError,
                        message: String
                    ) {
                        runOnMainThread {
                            isInterstitialLoading = false
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
                }
            )
        }
    }

    fun preloadRewarded() {
        runOnMainThread {
            if (!UnityAds.isInitialized) {
                _status.update {
                    it.copy(
                        isRewardedReady = false,
                        isRewardedLoading = false,
                        isRewardedFailed = true
                    )
                }
                return@runOnMainThread
            }
            if (_status.value.isRewardedLoading || _status.value.isRewardedReady) {
                return@runOnMainThread
            }

            mainHandler.removeCallbacks(rewardedRetryRunnable)
            _status.update {
                it.copy(
                    isRewardedReady = false,
                    isRewardedLoading = true,
                    isRewardedFailed = false
                )
            }

            UnityAds.load(
                REWARDED_PLACEMENT,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        runOnMainThread {
                            rewardedRetryAttempt = 0
                            _status.update {
                                it.copy(
                                    isRewardedReady = true,
                                    isRewardedLoading = false,
                                    isRewardedFailed = false,
                                    isPlacementMissing = false
                                )
                            }
                        }
                    }

                    override fun onUnityAdsFailedToLoad(
                        placementId: String,
                        error: UnityAds.UnityAdsLoadError,
                        message: String
                    ) {
                        runOnMainThread {
                            Log.w(TAG, "Rewarded '$placementId' failed ($error): $message")
                            val missing = message.contains("Placement not found", ignoreCase = true)
                            val shouldRetry = !missing && rewardedRetryAttempt < MAX_REWARDED_RETRIES
                            _status.update {
                                it.copy(
                                    isRewardedReady = false,
                                    isRewardedLoading = false,
                                    isRewardedFailed = !shouldRetry,
                                    isPlacementMissing = it.isPlacementMissing || missing,
                                    lastError = "$error: $message"
                                )
                            }
                            if (shouldRetry) {
                                rewardedRetryAttempt++
                                mainHandler.postDelayed(
                                    rewardedRetryRunnable,
                                    rewardedRetryAttempt * 2500L
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    fun showInterstitial(activity: Activity?, onFinished: () -> Unit) {
        if (activity == null || !UnityAds.isInitialized || !_status.value.isInterstitialReady) {
            preloadInterstitial()
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
                    runOnMainThread {
                        Log.w(TAG, "Interstitial show failed: $error - $message")
                        preloadInterstitial()
                        onFinished()
                    }
                }

                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    runOnMainThread {
                        preloadInterstitial()
                        onFinished()
                    }
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
            preloadRewarded()
            onUnavailableOrSkipped()
            return
        }

        _status.update {
            it.copy(
                isRewardedReady = false,
                isRewardedLoading = false,
                isRewardedFailed = false
            )
        }
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
                    runOnMainThread {
                        Log.w(TAG, "Rewarded show failed: $error - $message")
                        onUnavailableOrSkipped()
                        rewardedRetryAttempt = 0
                        preloadRewarded()
                    }
                }

                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    runOnMainThread {
                        handleRewardedShowComplete(
                            completed = state == UnityAds.UnityAdsShowCompletionState.COMPLETED,
                            onRewardEarned = onRewardEarned,
                            onUnavailableOrSkipped = onUnavailableOrSkipped,
                            reloadNextAd = { preloadRewarded() }
                        )
                    }
                }
            }
        )
    }

    internal fun handleRewardedShowComplete(
        completed: Boolean,
        onRewardEarned: () -> Unit,
        onUnavailableOrSkipped: () -> Unit,
        reloadNextAd: () -> Unit
    ) {
        if (completed) {
            onRewardEarned()
        } else {
            onUnavailableOrSkipped()
        }
        rewardedRetryAttempt = 0
        reloadNextAd()
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}

@Composable
fun UnityBannerAdBar(
    adsStatus: UnityAdsStatus,
    modifier: Modifier = Modifier
) {
    if (!adsStatus.isInitialized || !adsStatus.isBannerReady) return
    val bannerView = UnityAdsManager.getBannerView() ?: return
    val density = LocalDensity.current
    val bannerWidthPx = remember(density) { with(density) { 320.dp.roundToPx() } }
    val bannerHeightPx = remember(density) { with(density) { 50.dp.roundToPx() } }

    DisposableEffect(bannerView) {
        onDispose {
            (bannerView.parent as? ViewGroup)?.removeView(bannerView)
        }
    }

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
                    (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                    bannerView.visibility = View.VISIBLE
                    addView(
                        bannerView,
                        FrameLayout.LayoutParams(
                            bannerWidthPx,
                            bannerHeightPx,
                            Gravity.CENTER
                        )
                    )
                    bannerView.requestLayout()
                }
            },
            update = { container ->
                if (bannerView.parent !== container) {
                    (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                    container.removeAllViews()
                    bannerView.visibility = View.VISIBLE
                    container.addView(
                        bannerView,
                        FrameLayout.LayoutParams(
                            bannerWidthPx,
                            bannerHeightPx,
                            Gravity.CENTER
                        )
                    )
                    bannerView.requestLayout()
                }
            }
        )
    }
}
