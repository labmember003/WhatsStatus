package com.geeksoftapps.whatsweb.app

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first (Google UMP: shows the GDPR form only where the law needs it), then the Mobile Ads SDK.
 * Banners call [load]; it waits until ads may be requested, so nothing loads before consent is known.
 */
object Ads {
    private val started = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private var ready = false                                   // main thread only
    private val waiting = mutableListOf<WeakReference<AdView>>() // main thread only
    var onPrivacyOptionsChanged: ((Boolean) -> Unit)? = null

    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh(activity, info) } },
            { refresh(activity, info) },   // offline etc.: fall back to the consent stored last time
        )
        refresh(activity, info)             // consent from an earlier session lets ads start right away
    }

    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            refresh(activity, UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    /** Loads the banner now if ads may be requested, otherwise as soon as they may. */
    fun load(adView: AdView) {
        if (ready) adView.loadAd(AdRequest.Builder().build()) else waiting += WeakReference(adView)
    }

    private fun refresh(context: Context, info: ConsentInformation) {
        onPrivacyOptionsChanged?.invoke(privacyOptionsRequired(context))
        // Never request ads on Google's test devices (Play pre-launch report / Firebase Test Lab robots tap things)
        if (inTestLab(context)) return
        if (info.canRequestAds() && started.compareAndSet(false, true)) {
            val app = context.applicationContext
            Thread {
                MobileAds.initialize(app) {
                    main.post {
                        ready = true
                        waiting.mapNotNull { it.get() }.forEach { it.loadAd(AdRequest.Builder().build()) }
                        waiting.clear()
                    }
                }
            }.start()
        }
    }

    private fun inTestLab(context: Context): Boolean =
        "true" == Settings.System.getString(context.contentResolver, "firebase.test.lab")
}
