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
    private static final String PLACEMENT_ID = "Rewarded_Android";
    private static final boolean TEST_MODE = false;

    private boolean initialized = false;
    private boolean loaded = false;
    private PluginCall pendingCall;

    @Override
    public void load() {
        super.load();
        initialize();
    }

    private void initialize() {
        Activity activity = getActivity();

        UnityAds.initialize(
            activity.getApplicationContext(),
            GAME_ID,
            TEST_MODE,
            new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    initialized = true;
                    Log.d(TAG, "Unity Ads initialized");
                    loadRewarded();
                }

                @Override
                public void onInitializationFailed(
                    UnityAds.UnityAdsInitializationError error,
                    String message
                ) {
                    initialized = false;
                    Log.e(TAG, "Unity Ads initialization failed: " + message);
                }
            }
        );
    }

    private void loadRewarded() {
        UnityAds.load(PLACEMENT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                loaded = true;
                Log.d(TAG, "Rewarded ad loaded");
            }

            @Override
            public void onUnityAdsFailedToLoad(
                String placementId,
                UnityAds.UnityAdsLoadError error,
                String message
            ) {
                loaded = false;
                Log.e(TAG, "Rewarded load failed: " + message);
            }
        });
    }

    @PluginMethod
    public void isAdReady(PluginCall call) {
        JSObject result = new JSObject();
        result.put("ready", loaded);
        call.resolve(result);
    }

    @PluginMethod
    public void showRewardedAd(PluginCall call) {
        if (!initialized) {
            JSObject result = new JSObject();
            result.put("status", "NOT_INITIALIZED");
            call.resolve(result);
            return;
        }

        if (!loaded) {
            JSObject result = new JSObject();
            result.put("status", "NOT_READY");
            call.resolve(result);
            loadRewarded();
            return;
        }

        pendingCall = call;
        loaded = false;

        UnityAds.show(
            getActivity(),
            PLACEMENT_ID,
            new UnityAdsShowOptions(),
            new IUnityAdsShowListener() {
                @Override
                public void onUnityAdsShowFailure(
                    String placementId,
                    UnityAds.UnityAdsShowError error,
                    String message
                ) {
                    Log.e(TAG, "Rewarded show failed: " + message);
                    resolve("FAILED");
                    loadRewarded();
                }

                @Override
                public void onUnityAdsShowStart(String placementId) {}

                @Override
                public void onUnityAdsShowClick(String placementId) {}

                @Override
                public void onUnityAdsShowComplete(
                    String placementId,
                    UnityAds.UnityAdsShowCompletionState state
                ) {
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        resolve("REWARDED");
                    } else {
                        resolve("SKIPPED");
                    }
                    loadRewarded();
                }
            }
        );
    }

    private void resolve(String status) {
        if (pendingCall == null) return;

        JSObject result = new JSObject();
        result.put("status", status);
        pendingCall.resolve(result);
        pendingCall = null;
    }
}
