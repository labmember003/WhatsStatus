package com.geeksoftapps.whatsweb.app.ui.ads

import com.geeksoftapps.whatsweb.app.utils.Constants
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

fun getMaxBannerAdUnitId(bannerAdLocation: BannerAdLocation): String {
    return when(bannerAdLocation) {
        BannerAdLocation.ACTIVITY_STATUS_SAVER -> ""
        BannerAdLocation.ACTIVITY_DIRECT_CHAT -> ""
        BannerAdLocation.ACTIVITY_MAIN -> "2b311fd263a154b1"
        BannerAdLocation.ACTIVITY_WEB_VIEW -> "1c043aeea9a6f1cd"
    }
}

fun getMaxInterstitialAdUnitId(bAdRequest: InterstitialAdLocation): String {
   return when(bAdRequest) {
        InterstitialAdLocation.ACTIVITY_DIRECT_CHAT -> "2cd92c6874d12f85"
        InterstitialAdLocation.ACTIVITY_STATUS_SAVER -> "0278dfde3b38ae80"
        InterstitialAdLocation.ACTIVITY_WEB_VIEW -> "37d085a1f046f9c2"
        InterstitialAdLocation.ACTIVITY_MAIN -> "d0f562e616bcad1d"
    }
}

fun shouldEnableAds() = FirebaseRemoteConfig.getInstance().getBoolean(Constants.SHOULD_ENABLE_ADS)

fun shouldDisableAppUpdate() = FirebaseRemoteConfig.getInstance().getBoolean(Constants.DISABLE_APP_UPDATE)

fun getAdsInterval() = FirebaseRemoteConfig.getInstance().getBoolean(Constants.ADS_INTERVAL)
