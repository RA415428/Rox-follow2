package com.roxfollow.app

import android.app.Activity
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Bridges the web app's Store screen "Watch Ad" button to a real Unity
 * REWARDED video ad. Interstitial ads are intentionally NOT used here.
 *
 * JS side calls: Capacitor.Plugins.UnityRewardedAds.showRewardedAd()
 * -> resolves { status: "REWARDED" | "SKIPPED" | "FAILED" | "NOT_READY" | "NOT_INITIALIZED" }
 * Coins must only be credited by the caller when status === "REWARDED".
 */
@CapacitorPlugin(name = "UnityRewardedAds")
class UnityRewardedAdsPlugin : Plugin() {

    companion object {
        private const val TAG = "UnityRewardedAds"

        // From Unity Dashboard -> Rox Follow app -> Rewarded placement (Android)
        private const val GAME_ID = "800368206"
        private const val REWARDED_PLACEMENT_ID = "Rewarded_Android"

        // Set to true ONLY while testing with Unity's test ads, then set back to false.
        private const val TEST_MODE = false
    }

    private var isSdkInitialized = false
    private var isAdLoaded = false
    private var pendingCall: PluginCall? = null

    override fun load() {
        super.load()
        initializeUnityAds()
    }

    private fun initializeUnityAds() {
        val act: Activity = activity
        UnityAds.initialize(act.applicationContext, GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                isSdkInitialized = true
                Log.d(TAG, "Unity Ads initialized")
                preloadRewardedAd()
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError?,
                message: String?
            ) {
                isSdkInitialized = false
                Log.e(TAG, "Unity Ads init failed: $message")
            }
        })
    }

    private fun preloadRewardedAd() {
        UnityAds.load(REWARDED_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                isAdLoaded = true
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                isAdLoaded = false
                Log.e(TAG, "Rewarded ad failed to load: $message")
            }
        })
    }

    @PluginMethod
    fun isAdReady(call: PluginCall) {
        val ret = JSObject()
        ret.put("ready", isAdLoaded)
        call.resolve(ret)
    }

    @PluginMethod
    fun showRewardedAd(call: PluginCall) {
        if (!isSdkInitialized) {
            val ret = JSObject()
            ret.put("status", "NOT_INITIALIZED")
            call.resolve(ret)
            return
        }

        if (!isAdLoaded) {
            val ret = JSObject()
            ret.put("status", "NOT_READY")
            call.resolve(ret)
            preloadRewardedAd()
            return
        }

        pendingCall = call
        val act: Activity = activity

        UnityAds.show(act, REWARDED_PLACEMENT_ID, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAds.UnityAdsShowError?,
                message: String?
            ) {
                Log.e(TAG, "Rewarded ad show failed: $message")
                isAdLoaded = false
                resolveShow("FAILED")
                preloadRewardedAd()
            }

            override fun onUnityAdsShowStart(placementId: String?) {}

            override fun onUnityAdsShowClick(placementId: String?) {}

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                isAdLoaded = false
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    resolveShow("REWARDED")
                } else {
                    resolveShow("SKIPPED")
                }
                preloadRewardedAd()
            }
        })
    }

    private fun resolveShow(status: String) {
        val ret = JSObject()
        ret.put("status", status)
        pendingCall?.resolve(ret)
        pendingCall = null
    }
}
