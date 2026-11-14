package com.comedyarena.politicalfight

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdManager handles Google AdMob Testing Ads (Banner, Interstitial, and Rewarded).
 * Uses official Google sample test ad units.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob Test Ad Unit IDs (Testing ke liye)
    // Jab live AdMob account ban jaye, tab in IDs ko apne original Ad Unit IDs se replace karein.
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    /**
     * Initialize Mobile Ads SDK. Call once in MainActivity / MenuActivity.
     */
    fun init(context: Context) {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob SDK Initialized: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob SDK", e)
        }
    }

    /**
     * Creates and loads a Banner AdView.
     */
    fun createBannerAd(context: Context, adSize: AdSize = AdSize.BANNER): AdView {
        val adView = AdView(context)
        adView.adUnitId = BANNER_TEST_ID
        adView.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        return adView
    }

    /**
     * Preloads an Interstitial Ad in background.
     */
    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_TEST_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial Ad Loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Interstitial Ad Failed to Load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows Interstitial Ad if available, then executes onDismiss callback.
     * If not available, triggers onDismiss immediately so user flow is never blocked.
     */
    fun showInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onDismiss()
        }
    }

    /**
     * Preloads a Rewarded Ad in background.
     */
    fun loadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_TEST_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded Ad Loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Rewarded Ad Failed to Load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows Rewarded Ad. Triggers onRewardEarned only when user completes watching.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity)
                    if (earned) onRewardEarned()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewarded(activity)
                    onDismiss()
                }
            }
            ad.show(activity) { rewardItem ->
                earned = true
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        } else {
            loadRewarded(activity)
            onDismiss()
        }
    }
}

