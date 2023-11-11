package com.geeksoftapps.whatsweb.commons

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.preference.PowerPreference

data class BannerAdViewGroups(
    val bannerAdViewGroup: ViewGroup,
    val bannerContainer: ViewGroup
)

private val MAX_ATTEMPTS = 4

abstract class BasicActivity: AppCompatActivity() {
    protected open val isAdsEnabled: Boolean = false
    protected open val maxBannerAdUnitId = ""
    protected open val maxInterstitialAdUnitId = ""


    private var maxAdLoadAttempt = 0
    private var maxInterstitialAdLoadAttempt = 0
    private var finishOnResume = false
    protected var onCreateTimeStamp: Long = 0

    open fun getBannerAdView(): BannerAdViewGroups? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateTimeStamp = System.currentTimeMillis()
    }


    override fun onResume() {
        super.onResume()
        if (finishOnResume) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        var totalTimeSpent = PowerPreference.getDefaultFile().getLong("key_preference_time_spent_on_screens", 0)
        totalTimeSpent += ((System.currentTimeMillis() - onCreateTimeStamp)/1000)
        PowerPreference.getDefaultFile().putLong("key_preference_time_spent_on_screens", totalTimeSpent)
        super.onStop()
    }


}