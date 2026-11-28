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
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val showBannerFallbackText: Boolean = false,
    val bannerViewEpoch: Int = 0,
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
    private const val BANNER_LOG_TAG = "UNITY_BANNER"
    private const val UNITY_GAME_ID = "800453986"
    private val TEST_MODE = BuildConfig.DEBUG

    // Unity Ads Android Network Placement IDs
    private const val INTERSTITIAL_PLACEMENT = "BP_Interstitial_Android"
    private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
    const val BANNER_PLACEMENT = "Banner_Android"
    internal val BANNER_PLACEMENT_CANDIDATES = listOf("BP_Banner_Android", BANNER_PLACEMENT)

    internal const val BANNER_INIT_DELAY_MS = 2_000L
    internal const val BANNER_FALLBACK_TIMEOUT_MS = 5_000L
    internal const val BANNER_NO_FILL_RETRY_DELAY_MS = 30_000L
    private const val MAX_REWARDED_RETRIES = 4
    private const val MAX_BANNER_RETRIES = 5

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(UnityAdsStatus())
    val status: StateFlow<UnityAdsStatus> = _status.asStateFlow()

    private var activityRef: WeakReference<Activity>? = null
    private var bannerActivityRef: WeakReference<Activity>? = null
    private var bannerView: BannerView? = null
    private var bannerPlacementIndex = 0
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
    private val bannerFallbackTimeoutRunnable = Runnable {
        if (!_status.value.isBannerReady) {
            Log.w(BANNER_LOG_TAG, "Banner did not load within 5s; showing Advertisement fallback text")
            _status.update { it.copy(showBannerFallbackText = true) }
        }
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
            Log.w(TAG, "UnityBanner notice: ${message.orEmpty()}")
        }
    }

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
            if (!isBannerInitDelayPending) {
                preloadBanner(activityRef?.get())
            }
            preloadInterstitial()
            preloadRewarded()
            return
        }

        val appContext = context.applicationContext
        configureNonGmsHttpClient(appContext)
        scheduleBannerFallbackTimeout()

        UnityAds.initialize(
            appContext,
            UNITY_GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    runOnMainThread {
                        Log.i(
                            BANNER_LOG_TAG,
                            "UnityAds init complete (Game ID: $UNITY_GAME_ID, testMode=$TEST_MODE)"
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
                        Log.e(BANNER_LOG_TAG, "Unity Ads init failed ($error): $message")
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
        scheduleBannerFallbackTimeout()
    }

    private fun scheduleBannerFallbackTimeout() {
        mainHandler.removeCallbacks(bannerFallbackTimeoutRunnable)
        if (!_status.value.isBannerReady) {
            mainHandler.postDelayed(bannerFallbackTimeoutRunnable, BANNER_FALLBACK_TIMEOUT_MS)
        }
    }

    fun currentBannerPlacement(): String =
        BANNER_PLACEMENT_CANDIDATES.getOrElse(bannerPlacementIndex) { BANNER_PLACEMENT }

    fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
        bannerView?.let { bv ->
            ensureBannerLaidOut(bv)
        }
        if (UnityAds.isInitialized) {
            if (!isBannerInitDelayPending &&
                !_status.value.isBannerReady &&
                !isBannerLoading &&
                !_status.value.isPlacementMissing
            ) {
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
                mainHandler.removeCallbacks(bannerFallbackTimeoutRunnable)
                isBannerInitDelayPending = false
                isBannerLoading = false
                bannerView?.let { bv ->
                    (bv.parent as? ViewGroup)?.removeView(bv)
                    bv.destroy()
                }
                bannerView = null
                bannerActivityRef = null
                _status.update {
                    it.copy(
                        isBannerReady = false,
                        bannerViewEpoch = it.bannerViewEpoch + 1
                    )
                }
            }
        }
    }

    fun onBannerViewDetached(detachedView: View) {
        runOnMainThread {
            if (detachedView === bannerView) {
                Log.d(BANNER_LOG_TAG, "Active BannerView detached from window; resetting for clean reload")
                mainHandler.removeCallbacks(bannerRetryRunnable)
                isBannerLoading = false
                bannerView?.destroy()
                bannerView = null
                bannerActivityRef = null
                _status.update {
                    it.copy(
                        isBannerReady = false,
                        bannerViewEpoch = it.bannerViewEpoch + 1
                    )
                }
                val active = activityRef?.get()
                if (active != null && !active.isFinishing && !active.isDestroyed && UnityAds.isInitialized) {
                    mainHandler.post(bannerRetryRunnable)
                }
            }
        }
    }

    fun getBannerView(): BannerView? = bannerView

    fun preloadBanner(activity: Activity? = activityRef?.get()) {
        runOnMainThread {
            if (activity == null || activity.isFinishing || activity.isDestroyed || !UnityAds.isInitialized) {
                Log.d(BANNER_LOG_TAG, "Skipping banner load: activity=${activity != null}, init=${UnityAds.isInitialized}")
                return@runOnMainThread
            }
            mainHandler.removeCallbacks(bannerRetryRunnable)

            val activePlacement = currentBannerPlacement()
            val existingOwner = bannerActivityRef?.get()
            if (bannerView != null && (existingOwner !== activity || bannerView?.placementId != activePlacement)) {
                (bannerView?.parent as? ViewGroup)?.removeView(bannerView)
                bannerView?.destroy()
                bannerView = null
                isBannerLoading = false
                _status.update {
                    it.copy(
                        isBannerReady = false,
                        bannerViewEpoch = it.bannerViewEpoch + 1
                    )
                }
            }

            if (bannerView != null && (_status.value.isBannerReady || isBannerLoading)) {
                return@runOnMainThread
            }

            var createdNewView = false
            val bv = bannerView ?: BannerView(
                activity,
                activePlacement,
                UnityBannerSize(320, 50)
            ).also { created ->
                createdNewView = true
                created.visibility = View.VISIBLE
                created.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        if (child != null) {
                            configureBannerChildView(child)
                        }
                        ensureBannerLaidOut(created)
                        mainHandler.postDelayed({ ensureBannerLaidOut(created) }, 120L)
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) = Unit
                })
                created.listener = object : BannerView.Listener() {
                    override fun onBannerLoaded(bannerAdView: BannerView?) {
                        runOnMainThread {
                            mainHandler.removeCallbacks(bannerFallbackTimeoutRunnable)
                            bannerRetryAttempt = 0
                            isBannerLoading = false
                            Log.i(BANNER_LOG_TAG, "Banner LOADED for placement $activePlacement")
                            _status.update {
                                it.copy(
                                    isBannerReady = true,
                                    showBannerFallbackText = false,
                                    isPlacementMissing = false,
                                    lastError = null
                                )
                            }
                            bannerAdView?.let { adView ->
                                ensureBannerLaidOut(adView)
                                mainHandler.postDelayed({ ensureBannerLaidOut(adView) }, 120L)
                            }
                            unityBannerListener.onUnityBannerLoaded(activePlacement, bannerAdView)
                        }
                    }

                    override fun onBannerShown(bannerAdView: BannerView?) {
                        runOnMainThread {
                            bannerAdView?.let { adView ->
                                adView.visibility = View.VISIBLE
                                adView.alpha = 1f
                                ensureBannerLaidOut(adView)
                                mainHandler.postDelayed({ ensureBannerLaidOut(adView) }, 120L)
                            }
                            Log.i(BANNER_LOG_TAG, "Banner SHOWN for placement $activePlacement")
                            unityBannerListener.onUnityBannerShow(activePlacement)
                        }
                    }

                    override fun onBannerClick(bannerAdView: BannerView?) {
                        runOnMainThread {
                            unityBannerListener.onUnityBannerClick(activePlacement)
                        }
                    }

                    override fun onBannerFailedToLoad(
                        bannerAdView: BannerView?,
                        errorInfo: BannerErrorInfo?
                    ) {
                        runOnMainThread {
                            handleBannerLoadFailure(
                                placementId = activePlacement,
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
            _status.update {
                it.copy(
                    isBannerReady = false,
                    bannerViewEpoch = if (createdNewView) it.bannerViewEpoch + 1 else it.bannerViewEpoch
                )
            }
            scheduleBannerFallbackTimeout()
            Log.d(BANNER_LOG_TAG, "Trying to load $activePlacement, init=${UnityAds.isInitialized}")
            bv.load()
        }
    }

    internal fun configureBannerChildView(child: View) {
        child.visibility = View.VISIBLE
        child.alpha = 1f
        val lp = child.layoutParams
        if (lp == null || lp.width <= 0 || lp.height <= 0) {
            child.layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (child is WebView) {
            runCatching {
                child.onResume()
                child.resumeTimers()
            }
        } else if (child is ViewGroup) {
            for (i in 0 until child.childCount) {
                configureBannerChildView(child.getChildAt(i))
            }
        }
    }

    private var lastTargetWidthPx: Int = 0
    private var lastTargetHeightPx: Int = 0

    internal fun ensureBannerLaidOut(
        view: View,
        targetWidthPx: Int = 0,
        targetHeightPx: Int = 0
    ) {
        if (targetWidthPx > 0) lastTargetWidthPx = targetWidthPx
        if (targetHeightPx > 0) lastTargetHeightPx = targetHeightPx
        val effectiveTargetWidth = if (targetWidthPx > 0) targetWidthPx else lastTargetWidthPx
        val effectiveTargetHeight = if (targetHeightPx > 0) targetHeightPx else lastTargetHeightPx

        view.visibility = View.VISIBLE
        view.alpha = 1f
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                configureBannerChildView(view.getChildAt(i))
            }
        }
        view.requestLayout()
        view.invalidate()

        mainHandler.post {
            performBannerLayoutPass(view, effectiveTargetWidth, effectiveTargetHeight)
        }
        if (view.isAttachedToWindow) {
            view.post {
                performBannerLayoutPass(view, effectiveTargetWidth, effectiveTargetHeight)
            }
        }
    }

    private fun performBannerLayoutPass(
        view: View,
        targetWidthPx: Int,
        targetHeightPx: Int
    ) {
        val width = if (view.width > 0) view.width else targetWidthPx
        val height = if (view.height > 0) view.height else targetHeightPx
        if (width > 0 && height > 0) {
            var needsManualLayout = view.width != width || view.height != height
            if (!needsManualLayout && view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child.width <= 0 || child.height <= 0) {
                        needsManualLayout = true
                        break
                    }
                }
            }
            if (needsManualLayout) {
                val wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                view.measure(wSpec, hSpec)
                view.layout(view.left, view.top, view.left + width, view.top + height)
            }
        }
        view.invalidate()
    }

    internal fun handleBannerLoadFailure(
        placementId: String = currentBannerPlacement(),
        errorCode: BannerErrorCode?,
        errorMessage: String,
        scheduleRetry: (Long) -> Unit = { delayMs ->
            mainHandler.removeCallbacks(bannerRetryRunnable)
            mainHandler.postDelayed(bannerRetryRunnable, delayMs)
        },
        onFallbackNextPlacement: () -> Unit = { preloadBanner(activityRef?.get()) }
    ) {
        isBannerLoading = false
        val codeName = errorCode?.name ?: "UNKNOWN"
        val formattedError = "Banner '$placementId' failed ($codeName): $errorMessage"
        Log.w(BANNER_LOG_TAG, formattedError)
        unityBannerListener.onUnityBannerError(formattedError)

        val isNoFill = errorCode == BannerErrorCode.NO_FILL ||
            errorMessage.contains("NO_FILL", ignoreCase = true) ||
            errorMessage.contains("no fill", ignoreCase = true)
        val missing = errorMessage.contains("Placement not found", ignoreCase = true)

        if (missing && bannerPlacementIndex < BANNER_PLACEMENT_CANDIDATES.lastIndex) {
            bannerPlacementIndex++
            (bannerView?.parent as? ViewGroup)?.removeView(bannerView)
            bannerView?.destroy()
            bannerView = null
            _status.update {
                it.copy(
                    isBannerReady = false,
                    isPlacementMissing = false,
                    bannerViewEpoch = it.bannerViewEpoch + 1,
                    lastError = formattedError
                )
            }
            Log.w(BANNER_LOG_TAG, "Banner placement '$placementId' not found; trying fallback '${currentBannerPlacement()}'")
            onFallbackNextPlacement()
            return
        }

        _status.update {
            it.copy(
                isBannerReady = false,
                showBannerFallbackText = true,
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
            Log.w(TAG, "Retrying banner '$placementId' in ${retryDelayMs}ms (noFill=$isNoFill)")
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
    val bannerView = remember(adsStatus.bannerViewEpoch, adsStatus.isBannerReady) {
        bannerViewProvider()
    }
    val density = LocalDensity.current
    val bannerWidthPx = remember(density) { with(density) { 320.dp.roundToPx() } }
    val bannerHeightPx = remember(density) { with(density) { 50.dp.roundToPx() } }

    DisposableEffect(bannerView) {
        onDispose {
            if (bannerView != null) {
                (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                if (bannerView === UnityAdsManager.getBannerView()) {
                    UnityAdsManager.onBannerViewDetached(bannerView)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black)
            .testTag("unity_banner_ad"),
        contentAlignment = Alignment.Center
    ) {
        if (bannerView != null) {
            key(adsStatus.bannerViewEpoch) {
                AndroidView(
                    modifier = Modifier
                        .width(320.dp)
                        .height(50.dp),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(bannerWidthPx, bannerHeightPx)
                            (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                            bannerView.visibility = View.VISIBLE
                            addView(
                                bannerView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    Gravity.CENTER
                                )
                            )
                            UnityAdsManager.ensureBannerLaidOut(bannerView, bannerWidthPx, bannerHeightPx)
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
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    Gravity.CENTER
                                )
                            )
                        }
                        UnityAdsManager.ensureBannerLaidOut(bannerView, bannerWidthPx, bannerHeightPx)
                    }
                )
            }
        }

        if (!adsStatus.isBannerReady && adsStatus.showBannerFallbackText) {
            Text(
                text = "Advertisement",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("unity_banner_fallback_text")
            )
        }
    }
}
