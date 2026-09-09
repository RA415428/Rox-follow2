package com.roxfollow.app;

import android.app.Activity;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

@CapacitorPlugin(name = "UnityRewardedAds")
public class UnityRewardedAdsPlugin extends Plugin {

    private static final String TAG = "UnityRewardedAds";
    private static final String GAME_ID = "800368206";
    private static final String REWARDED_PLACEMENT_ID = "Rewarded_Android";
    private static final boolean TEST_MODE = false;

    private boolean sdkInitialized = false;
    private boolean adLoaded = false;
    private PluginCall pendingCall;

    @Override
    public void load() {
        super.load();
        initializeUnityAds();
    }

    private void initializeUnityAds() {
        Activity activity = getActivity();

        if (activity == null) {
            Log.e(TAG, "Activity is null");
            return;
        }

        UnityAds.initialize(
            activity.getApplicationContext(),
            GAME_ID,
            TEST_MODE,
            new IUnityAdsInitializationListener() {

                @Override
                public void onInitializationComplete() {
                    sdkInitialized = true;
                    Log.d(TAG, "Unity Ads initialized");
                    preloadRewardedAd();
                }

                @Override
                public void onInitializationFailed(
                    UnityAds.UnityAdsInitializationError error,
                    String message
                ) {
                    sdkInitialized = false;
                    Log.e(TAG, "Unity Ads initialization failed: " + message);
                }
            }
        );
    }

    private void preloadRewardedAd() {
        if (!sdkInitialized) {
            return;
        }

        UnityAds.load(
            REWARDED_PLACEMENT_ID,
            new IUnityAdsLoadListener() {

                @Override
                public void onUnityAdsAdLoaded(String placementId) {
                    adLoaded = true;
                    Log.d(TAG, "Rewarded ad loaded");
                }

                @Override
                public void onUnityAdsFailedToLoad(
                    String placementId,
                    UnityAds.UnityAdsLoadError error,
                    String message
                ) {
                    adLoaded = false;
                    Log.e(TAG, "Rewarded ad failed to load: " + message);
                }
            }
        );
    }

    @PluginMethod
    public void isAdReady(PluginCall call) {
        JSObject result = new JSObject();
        result.put("ready", adLoaded);
        call.resolve(result);
    }

    @PluginMethod
    public void showRewardedAd(PluginCall call) {

        if (!sdkInitialized) {
            JSObject result = new JSObject();
            result.put("status", "NOT_INITIALIZED");
            call.resolve(result);
            return;
        }

        if (!adLoaded) {
            JSObject result = new JSObject();
            result.put("status", "NOT_READY");
            call.resolve(result);
            preloadRewardedAd();
            return;
        }

        if (pendingCall != null) {
            JSObject result = new JSObject();
            result.put("status", "BUSY");
            call.resolve(result);
            return;
        }

        pendingCall = call;
        adLoaded = false;

        Activity activity = getActivity();

        if (activity == null) {
            resolveShow("FAILED");
            preloadRewardedAd();
            return;
        }

        UnityAds.show(
            activity,
            REWARDED_PLACEMENT_ID,
            new UnityAdsShowOptions(),
            new IUnityAdsShowListener() {

                @Override
                public void onUnityAdsShowFailure(
                    String placementId,
                    UnityAds.UnityAdsShowError error,
                    String message
                ) {
                    Log.e(TAG, "Rewarded ad show failed: " + message);
                    resolveShow("FAILED");
                    preloadRewardedAd();
                }

                @Override
                public void onUnityAdsShowStart(String placementId) {
                    Log.d(TAG, "Rewarded ad started");
                }

                @Override
                public void onUnityAdsShowClick(String placementId) {
                }

                @Override
                public void onUnityAdsShowComplete(
                    String placementId,
                    UnityAds.UnityAdsShowCompletionState state
                ) {
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        resolveShow("REWARDED");
                    } else {
                        resolveShow("SKIPPED");
                    }

                    preloadRewardedAd();
                }
            }
        );
    }

    private void resolveShow(String status) {
        JSObject result = new JSObject();
        result.put("status", status);

        if (pendingCall != null) {
            pendingCall.resolve(result);
            pendingCall = null;
        }
    }
}
