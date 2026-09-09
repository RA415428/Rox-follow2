import { registerPlugin, Capacitor } from '@capacitor/core';

interface UnityRewardedAdsPluginInterface {
  showRewardedAd(): Promise<{ status: string }>;
  isAdReady(): Promise<{ ready: boolean }>;
}

const UnityRewardedAds = registerPlugin<UnityRewardedAdsPluginInterface>('UnityRewardedAds');

export const isNativeAdsAvailable = (): boolean => {
  try {
    return Capacitor.isNativePlatform();
  } catch {
    return false;
  }
};

interface ShowRewardedAdCallbacks {
  onReward: () => void;
  onFailedOrCancelled: (reason: string) => void;
  onUnavailable: () => void;
}

export const showNativeRewardedAd = async ({
  onReward,
  onFailedOrCancelled,
  onUnavailable
}: ShowRewardedAdCallbacks): Promise<void> => {
  if (!isNativeAdsAvailable()) {
    console.log('[UnityAds] Native platform not detected - web preview mode');
    onUnavailable();
    return;
  }

  try {
    console.log('[UnityAds] Calling native showRewardedAd()...');
    const result = await UnityRewardedAds.showRewardedAd();
    console.log('[UnityAds] Native result:', JSON.stringify(result));
    if (result && result.status === 'REWARDED') {
      onReward();
    } else {
      onFailedOrCancelled(result?.status || 'UNKNOWN');
    }
  } catch (err: any) {
    console.error('[UnityAds] Native call error:', err);
    onFailedOrCancelled(err?.message || 'NATIVE_AD_ERROR');
  }
};
