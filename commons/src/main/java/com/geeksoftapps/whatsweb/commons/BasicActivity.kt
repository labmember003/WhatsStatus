package com.geeksoftapps.whatsweb.commons

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.preference.PowerPreference
import kotlinx.coroutines.delay

data class BannerAdViewGroups(
    val bannerAdViewGroup: ViewGroup,
    val bannerContainer: ViewGroup
)

private val MAX_ATTEMPTS = 4

abstract class BasicActivity: AppCompatActivity() {
    protected open val isAdsEnabled: Boolean = false
    protected open val maxBannerAdUnitId = ""
    protected open val maxInterstitialAdUnitId = ""

    private var maxAdView: MaxAdView? = null
    private var maxInterstitialAd: MaxInterstitialAd? = null
    private var maxAdLoadAttempt = 0
    private var maxInterstitialAdLoadAttempt = 0
    private var finishOnResume = false
    protected var onCreateTimeStamp: Long = 0

    open fun getBannerAdView(): BannerAdViewGroups? = null

    private fun startMaxAdView() {
        if (!isAdsEnabled || maxBannerAdUnitId.isBlank()) {
            return
        }
        maxAdView = MaxAdView(maxBannerAdUnitId, this)
        getBannerAdView()?.let { viewGroups ->
            maxAdView?.let { maxAdView ->
                maxAdView.visibility = View.VISIBLE
                maxAdView.startAutoRefresh()

                val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.banner_height))
                viewGroups.bannerAdViewGroup.visibility = View.VISIBLE
                viewGroups.bannerContainer.addView(maxAdView, -1 , layoutParams)

                maxAdView.setListener(object: MaxAdViewAdListener {
                    override fun onAdLoaded(ad: MaxAd?) {}
                    override fun onAdDisplayed(ad: MaxAd?) {}
                    override fun onAdHidden(ad: MaxAd?) {}
                    override fun onAdClicked(ad: MaxAd?) {}
                    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                        runOnUiThread {
                            try {
                                maxAdView.visibility = View.GONE
                                maxAdView.stopAutoRefresh()
                                viewGroups.bannerContainer.removeViewAt(viewGroups.bannerContainer.indexOfChild(maxAdView))
                                if (maxAdLoadAttempt < MAX_ATTEMPTS) {
                                    lifecycleScope.launchWhenStarted {
                                        delay(2000)
                                        maxAdLoadAttempt++
                                        startMaxAdView()
                                    }
                                }
                            } catch (e: Exception) {

                            }
                            viewGroups.bannerAdViewGroup.visibility = View.GONE
                        }
                    }
                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}
                    override fun onAdExpanded(ad: MaxAd?) {}
                    override fun onAdCollapsed(ad: MaxAd?) {}
                })
                maxAdView.loadAd()
            } ?: run {
                viewGroups.bannerAdViewGroup.visibility = View.GONE
            }
        }
    }

    private fun startMaxInterstitialAdView() {
        if (!isAdsEnabled || maxInterstitialAdUnitId.isBlank()) {
            return
        }
        maxInterstitialAd = MaxInterstitialAd(maxInterstitialAdUnitId, this)
        maxInterstitialAd?.setListener(object: MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {}
            override fun onAdDisplayed(ad: MaxAd?) {
                finishOnResume = true
            }
            override fun onAdHidden(ad: MaxAd?) {
                // maxInterstitialAd?.loadAd()
                finishOnResume = true
            }
            override fun onAdClicked(ad: MaxAd?) {}
            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                if (maxInterstitialAdLoadAttempt < MAX_ATTEMPTS) {
                    lifecycleScope.launchWhenStarted {
                        delay(2000)
                        maxInterstitialAdLoadAttempt++
                        startMaxInterstitialAdView()
                    }
                }
            }
            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                // maxInterstitialAd?.loadAd()
                finishOnResume = true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateTimeStamp = System.currentTimeMillis()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (isAdsEnabled) {
            AppLovinSdk.getInstance( this ).settings.setVerboseLogging( true )
            AppLovinSdk.getInstance( this ).mediationProvider = "max"
            AppLovinSdk.getInstance( this ).initializeSdk { configuration: AppLovinSdkConfiguration ->
                startMaxAdView()
                startMaxInterstitialAdView()
            }
        } else {
            getBannerAdView()?.let { viewGroups ->
                viewGroups.bannerAdViewGroup.visibility = View.GONE
            }
        }
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