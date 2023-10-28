package ui.ads

import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation

fun getMaxBannerAdUnitId(bannerAdLocation: BannerAdLocation): String {
    return when(bannerAdLocation) {
        BannerAdLocation.ACTIVITY_STATUS_SAVER -> "5b0de51bbf9eca44"
        BannerAdLocation.ACTIVITY_DIRECT_CHAT -> "bf1451f54349ca12"
        BannerAdLocation.ACTIVITY_MAIN -> "2ef6a3a8d1b0d9c2"
        BannerAdLocation.ACTIVITY_WEB_VIEW -> "fa9f5b083d2f1e26"
    }
}

fun getMaxInterstitialAdUnitId(bAdRequest: InterstitialAdLocation): String {
   return when(bAdRequest) {
        InterstitialAdLocation.ACTIVITY_DIRECT_CHAT -> "804d7bd1275b6857"
        InterstitialAdLocation.ACTIVITY_STATUS_SAVER -> "b7f7321c2609390a"
        InterstitialAdLocation.ACTIVITY_WEB_VIEW -> "9ae58485588fba10"
        InterstitialAdLocation.ACTIVITY_MAIN -> "7a7bb0db2ef3d376"
    }
}