import { Capacitor, registerPlugin } from '@capacitor/core';

interface UnityRewardedAdsPlugin {
  showRewardedAd(): Promise<{ status: string }>;
  isAdReady(): Promise<{ ready: boolean }>;
}

const UnityRewardedAds =
  registerPlugin<UnityRewardedAdsPlugin>('UnityRewardedAds');

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
  if (!Capacitor.isNativePlatform()) {
    onUnavailable();
    return;
  }

  try {
    const result = await UnityRewardedAds.showRewardedAd();

    if (result?.status === 'REWARDED') {
      onReward();
    } else {
      onFailedOrCancelled(result?.status || 'UNKNOWN');
    }
  } catch (err: any) {
    onFailedOrCancelled(err?.message || 'NATIVE_AD_ERROR');
  }
};
