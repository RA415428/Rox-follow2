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

    private boolean isSdkInitialized = false;
    private boolean isAdLoaded = false;
    private PluginCall pendingCall = null;

    @Override
    public void load() {
        super.load();
        initializeUnityAds();
    }

    private void initializeUnityAds() {
        Activity act = getActivity();
        UnityAds.initialize(act.getApplicationContext(), GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                isSdkInitialized = true;
                Log.d(TAG, "Unity Ads initialized");
                preloadRewardedAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                isSdkInitialized = false;
                Log.e(TAG, "Unity Ads init failed: " + message);
            }
        });
    }

    private void preloadRewardedAd() {
        UnityAds.load(REWARDED_PLACEMENT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                isAdLoaded = true;
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                isAdLoaded = false;
                Log.e(TAG, "Rewarded ad failed to load: " + message);
            }
        });
    }

    @PluginMethod
    public void isAdReady(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("ready", isAdLoaded);
        call.resolve(ret);
    }

    @PluginMethod
    public void showRewardedAd(PluginCall call) {
        if (!isSdkInitialized) {
            JSObject ret = new JSObject();
            ret.put("status", "NOT_INITIALIZED");
            call.resolve(ret);
            return;
        }

        if (!isAdLoaded) {
            JSObject ret = new JSObject();
            ret.put("status", "NOT_READY");
            call.resolve(ret);
            preloadRewardedAd();
            return;
        }

        pendingCall = call;
        Activity act = getActivity();

        UnityAds.show(act, REWARDED_PLACEMENT_ID, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Log.e(TAG, "Rewarded ad show failed: " + message);
                isAdLoaded = false;
                resolveShow("FAILED");
                preloadRewardedAd();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                isAdLoaded = false;
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    resolveShow("REWARDED");
                } else {
                    resolveShow("SKIPPED");
                }
                preloadRewardedAd();
            }
        });
    }

    private void resolveShow(String status) {
        JSObject ret = new JSObject();
        ret.put("status", status);
        if (pendingCall != null) {
            pendingCall.resolve(ret);
            pendingCall = null;
        }
    }
}
