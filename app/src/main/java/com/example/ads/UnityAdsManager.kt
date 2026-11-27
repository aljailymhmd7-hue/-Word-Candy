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
import com.unity3d.ads.metadata.MediationMetaData
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.IUnityBannerListener
import com.unity3d.services.banners.UnityBannerSize
import com.unity3d.services.banners.UnityBanners
import com.unity3d.services.core.properties.ClientProperties
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

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
    const val BANNER_PLACEMENT = "Banner_Android"

    internal const val BANNER_INIT_DELAY_MS = 1_000L
    internal const val BANNER_NO_FILL_RETRY_DELAY_MS = 30_000L
    private const val MAX_REWARDED_RETRIES = 4
    private const val MAX_BANNER_RETRIES = 5

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(UnityAdsStatus())
    val status: StateFlow<UnityAdsStatus> = _status.asStateFlow()

    private var activityRef: WeakReference<Activity>? = null
    private var bannerActivityRef: WeakReference<Activity>? = null
    private var bannerView: BannerView? = null
    private var isBannerLoading = false
    private var isBannerInitDelayPending = false
    private var isInterstitialLoading = false
    private var rewardedRetryAttempt = 0
    private var bannerRetryAttempt = 0

    private val rewardedRetryRunnable = Runnable { preloadRewarded() }
    private val bannerRetryRunnable = Runnable { preloadBanner(activityRef?.get()) }
    private val bannerInitDelayRunnable = Runnable {
        isBannerInitDelayPending = false
        preloadBanner(activityRef?.get())
    }

    val unityBannerListener: IUnityBannerListener = object : IUnityBannerListener {
        override fun onUnityBannerLoaded(placementId: String?, view: View?) {
            Log.d(TAG, "UnityBanner loaded: placement=$placementId")
        }

        override fun onUnityBannerUnloaded(placementId: String?) {
            Log.d(TAG, "UnityBanner unloaded: placement=$placementId")
        }

        override fun onUnityBannerShow(placementId: String?) {
            Log.d(TAG, "UnityBanner shown: placement=$placementId")
        }

        override fun onUnityBannerClick(placementId: String?) {
            Log.d(TAG, "UnityBanner clicked: placement=$placementId")
        }

        override fun onUnityBannerHide(placementId: String?) {
            Log.d(TAG, "UnityBanner hidden: placement=$placementId")
        }

        override fun onUnityBannerError(message: String?) {
            Log.e(TAG, "UnityBanner error: ${message.orEmpty()}")
        }
    }

    fun initialize(context: Context) {
        findActivity(context)?.let { activityRef = WeakReference(it) }
        UnityBanners.setBannerListener(unityBannerListener)

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
            if (!isBannerInitDelayPending) {
                preloadBanner(activityRef?.get())
            }
            preloadInterstitial()
            preloadRewarded()
            return
        }

        val appContext = context.applicationContext
        configureNonGmsHttpClient(appContext)

        UnityAds.initialize(
            appContext,
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
                        preloadInterstitial()
                        preloadRewarded()
                        scheduleBannerLoadAfterInit()
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

    internal fun configureNonGmsHttpClient(context: Context) {
        val appContext = context.applicationContext ?: context
        if (ClientProperties.getApplicationContext() == null) {
            ClientProperties.setApplicationContext(appContext)
        }
        runCatching {
            val traits = JSONObject().put("useHttpClient", "OKHTTP3")
            MediationMetaData(appContext).apply {
                set("traits", traits)
                commit()
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to commit OKHTTP3 trait for Unity Ads", error)
        }
    }

    internal fun scheduleBannerLoadAfterInit() {
        mainHandler.removeCallbacks(bannerInitDelayRunnable)
        mainHandler.removeCallbacks(bannerRetryRunnable)
        isBannerInitDelayPending = true
        mainHandler.postDelayed(bannerInitDelayRunnable, BANNER_INIT_DELAY_MS)
    }

    fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
        if (UnityAds.isInitialized) {
            if (!isBannerInitDelayPending && !_status.value.isBannerReady && !isBannerLoading) {
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
            val owner = bannerActivityRef?.get()
            if (owner === activity || (bannerView != null && owner == null)) {
                mainHandler.removeCallbacks(bannerInitDelayRunnable)
                mainHandler.removeCallbacks(bannerRetryRunnable)
                isBannerInitDelayPending = false
                isBannerLoading = false
                bannerView?.let { bv ->
                    (bv.parent as? ViewGroup)?.removeView(bv)
                    bv.destroy()
                }
                runCatching { UnityBanners.destroy() }
                bannerView = null
                bannerActivityRef = null
                _status.update { it.copy(isBannerReady = false) }
            }
        }
    }

    fun getBannerView(): BannerView? = bannerView

    fun preloadBanner(activity: Activity? = activityRef?.get()) {
        runOnMainThread {
            if (activity == null || activity.isFinishing || activity.isDestroyed || !UnityAds.isInitialized) {
                return@runOnMainThread
            }
            mainHandler.removeCallbacks(bannerRetryRunnable)
            UnityBanners.setBannerListener(unityBannerListener)

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
                            unityBannerListener.onUnityBannerLoaded(BANNER_PLACEMENT, bannerAdView)
                        }
                    }

                    override fun onBannerShown(bannerAdView: BannerView?) {
                        runOnMainThread {
                            bannerAdView?.visibility = View.VISIBLE
                            unityBannerListener.onUnityBannerShow(BANNER_PLACEMENT)
                        }
                    }

                    override fun onBannerClick(bannerAdView: BannerView?) {
                        runOnMainThread {
                            unityBannerListener.onUnityBannerClick(BANNER_PLACEMENT)
                        }
                    }

                    override fun onBannerFailedToLoad(
                        bannerAdView: BannerView?,
                        errorInfo: BannerErrorInfo?
                    ) {
                        runOnMainThread {
                            handleBannerLoadFailure(
                                errorCode = errorInfo?.errorCode,
                                errorMessage = errorInfo?.errorMessage.orEmpty()
                            )
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

    internal fun handleBannerLoadFailure(
        errorCode: BannerErrorCode?,
        errorMessage: String,
        scheduleRetry: (Long) -> Unit = { delayMs ->
            mainHandler.removeCallbacks(bannerRetryRunnable)
            mainHandler.postDelayed(bannerRetryRunnable, delayMs)
        }
    ) {
        isBannerLoading = false
        val codeName = errorCode?.name ?: "UNKNOWN"
        val formattedError = "Banner '$BANNER_PLACEMENT' failed ($codeName): $errorMessage"
        unityBannerListener.onUnityBannerError(formattedError)

        val isNoFill = errorCode == BannerErrorCode.NO_FILL ||
            errorMessage.contains("NO_FILL", ignoreCase = true) ||
            errorMessage.contains("no fill", ignoreCase = true)
        val missing = errorMessage.contains("Placement not found", ignoreCase = true)

        _status.update {
            it.copy(
                isBannerReady = false,
                isPlacementMissing = it.isPlacementMissing || missing,
                lastError = formattedError
            )
        }

        val retryDelayMs = computeBannerRetryDelayMs(
            errorCode = errorCode,
            errorMessage = errorMessage,
            currentAttempt = bannerRetryAttempt
        )
        if (retryDelayMs != null) {
            if (!isNoFill) {
                bannerRetryAttempt++
            }
            Log.w(TAG, "Retrying banner '$BANNER_PLACEMENT' in ${retryDelayMs}ms (noFill=$isNoFill)")
            scheduleRetry(retryDelayMs)
        }
    }

    internal fun computeBannerRetryDelayMs(
        errorCode: BannerErrorCode?,
        errorMessage: String,
        currentAttempt: Int
    ): Long? {
        val isNoFill = errorCode == BannerErrorCode.NO_FILL ||
            errorMessage.contains("NO_FILL", ignoreCase = true) ||
            errorMessage.contains("no fill", ignoreCase = true)
        if (isNoFill) return BANNER_NO_FILL_RETRY_DELAY_MS

        val missing = errorMessage.contains("Placement not found", ignoreCase = true)
        if (missing || currentAttempt >= MAX_BANNER_RETRIES) return null

        return (currentAttempt + 1) * 4_000L
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
    modifier: Modifier = Modifier,
    bannerViewProvider: () -> View? = { UnityAdsManager.getBannerView() }
) {
    if (!adsStatus.isInitialized || !adsStatus.isBannerReady) return
    val bannerView = bannerViewProvider() ?: return
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
            .height(50.dp)
            .testTag("unity_banner_ad"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
                bannerView.visibility = View.VISIBLE
                if (bannerView.parent !== container) {
                    (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                    container.removeAllViews()
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
