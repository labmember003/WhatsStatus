package com.geeksoftapps.whatsweb.commons

import com.applovin.mediation.ads.MaxInterstitialAd
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.preference.PowerPreference
import com.preference.Preference

private val preference: Preference
    get() = PowerPreference.getDefaultFile()

private fun intervalPrefKey(placementId: String) = "MaxInterstitialAd.$placementId"

fun showInterval() = FirebaseRemoteConfig.getInstance().getValue("ads_interval").asLong().toInt()

fun MaxInterstitialAd.showAdIfValidLoaded(): Boolean {
    if (!this.isReady) return false
    this.showAd()
    return true
}

fun MaxInterstitialAd.loadAdIfValidInterval() {
    val intervalPrefKey = intervalPrefKey(this.adUnitId)
    val count = preference.getInt(intervalPrefKey, 0).let {
        (it + 1) % showInterval()
    }
    if (count == 0) {
        this.loadAd()
    }
}

/**
 * returns if ad was shown or not
 */
fun MaxInterstitialAd.showAdIfValidLoadedInterval(): Boolean {
    val intervalPrefKey = intervalPrefKey(this.adUnitId)
    val count = preference.getInt(intervalPrefKey, 0).let {
        (it + 1) % showInterval()
    }
    return if (count == 0) {
        if (showAdIfValidLoaded()) {
            preference.putInt(intervalPrefKey, count)
            true
        } else {
            false
        }
    } else {
        preference.putInt(intervalPrefKey, count)
        false
    }
}