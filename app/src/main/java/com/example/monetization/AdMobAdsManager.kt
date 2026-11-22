package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production Google Mobile Ads (AdMob) controller managing preloaded Rewarded (`+5 stories`)
 * and Interstitial (`AD_BREAK_STORY_INTERVAL`) ad units sourced from Secrets/BuildConfig.
 *
 * Zero fake reward grants: [AdShowResult.RewardEarned] is invoked ONLY from AdMob's verified
 * [com.google.android.gms.ads.OnUserEarnedRewardListener] after dismissal.
 */
class AdMobAdsManager(context: Context) : AdsGateway {

    private val appContext = context.applicationContext
    private val initialized = AtomicBoolean(false)
    private val rewardedLoading = AtomicBoolean(false)
    private val interstitialLoading = AtomicBoolean(false)

    @Volatile
    private var rewardedAd: RewardedAd? = null

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    private val _isRewardedReady = MutableStateFlow(false)
    override val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(false)
    override val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    override fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        try {
            MobileAds.initialize(appContext) {
                preloadRewardedAd()
                preloadInterstitialAd()
            }
        } catch (e: Exception) {
            initialized.set(false)
            Log.w(TAG, "MobileAds.initialize failed cleanly", e)
        }
    }

    override fun preloadRewardedAd() {
        val unitId = BuildConfig.ADMOB_REWARDED_UNIT_ID.trim()
        if (unitId.isEmpty() || rewardedAd != null || !rewardedLoading.compareAndSet(false, true)) return

        try {
            RewardedAd.load(
                appContext,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedLoading.set(false)
                        rewardedAd = ad
                        _isRewardedReady.value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedLoading.set(false)
                        rewardedAd = null
                        _isRewardedReady.value = false
                        Log.w(TAG, "RewardedAd failed to load: ${error.code} / ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            rewardedLoading.set(false)
            Log.w(TAG, "RewardedAd.load exception", e)
        }
    }

    override fun preloadInterstitialAd() {
        val unitId = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID.trim()
        if (unitId.isEmpty() || interstitialAd != null || !interstitialLoading.compareAndSet(false, true)) return

        try {
            InterstitialAd.load(
                appContext,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialLoading.set(false)
                        interstitialAd = ad
                        _isInterstitialReady.value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialLoading.set(false)
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        Log.w(TAG, "InterstitialAd failed to load: ${error.code} / ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            interstitialLoading.set(false)
            Log.w(TAG, "InterstitialAd.load exception", e)
        }
    }

    override fun showRewardedAd(activity: Activity?, onResult: (AdShowResult) -> Unit) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onResult(AdShowResult.FailedToShow("No foreground activity"))
            return
        }

        val readyAd = rewardedAd
        if (readyAd != null) {
            presentRewardedAd(activity, readyAd, onResult)
            return
        }

        // Attempt single on-demand load if cache is empty
        val unitId = BuildConfig.ADMOB_REWARDED_UNIT_ID.trim()
        if (unitId.isEmpty()) {
            onResult(AdShowResult.FailedToShow("Missing rewarded ad unit ID"))
            return
        }
        try {
            RewardedAd.load(
                activity,
                unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (activity.isFinishing || activity.isDestroyed) {
                            rewardedAd = ad
                            _isRewardedReady.value = true
                            onResult(AdShowResult.FailedToShow("Activity closed"))
                        } else {
                            presentRewardedAd(activity, ad, onResult)
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        _isRewardedReady.value = false
                        Log.w(TAG, "On-demand RewardedAd failed: ${error.message}")
                        onResult(AdShowResult.FailedToShow(error.message))
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "On-demand RewardedAd exception", e)
            onResult(AdShowResult.FailedToShow(e.message))
        }
    }

    private fun presentRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        onResult: (AdShowResult) -> Unit
    ) {
        rewardedAd = null
        _isRewardedReady.value = false

        var rewardEarned = false
        var callbackDispatched = false

        fun finishOnce(result: AdShowResult) {
            if (!callbackDispatched) {
                callbackDispatched = true
                preloadRewardedAd()
                onResult(result)
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                finishOnce(if (rewardEarned) AdShowResult.RewardEarned else AdShowResult.ClosedWithoutReward)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "RewardedAd failed to show: ${adError.message}")
                finishOnce(AdShowResult.FailedToShow(adError.message))
            }
        }

        ad.show(activity) {
            rewardEarned = true
        }
    }

    override fun showInterstitialAd(activity: Activity?, onClosed: () -> Unit) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onClosed()
            return
        }
        val readyAd = interstitialAd
        if (readyAd == null) {
            preloadInterstitialAd()
            onClosed()
            return
        }

        interstitialAd = null
        _isInterstitialReady.value = false

        var finished = false
        fun closeOnce() {
            if (!finished) {
                finished = true
                preloadInterstitialAd()
                onClosed()
            }
        }

        readyAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                closeOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "InterstitialAd failed to show: ${adError.message}")
                closeOnce()
            }
        }

        readyAd.show(activity)
    }

    companion object {
        private const val TAG = "AdMobAdsManager"
    }
}
