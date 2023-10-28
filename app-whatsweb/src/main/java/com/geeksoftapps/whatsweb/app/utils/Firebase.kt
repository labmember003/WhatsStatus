package com.geeksoftapps.whatsweb.app.utils

import android.os.Bundle
import com.geeksoftapps.whatsweb.app.App
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun FirebaseAnalytics.log(
    eventName: String,
    var1: String? = null,
    var2: String? = null,
    var3: String? = null) {
    try {
        val bundle = Bundle().apply {
            if(var1 != null) putString("var1", var1)
            if(var2 != null) putString("var2", var2)
            if(var3 != null) putString("var3", var3)
            putString("app_version", CommonUtils.getAppVersion(App.getInstance().applicationContext))
            putString("is_internet_connected", CommonUtils.isNetworkAvailable(App.getInstance().applicationContext).toString())
        }
        logEvent(eventName, bundle)
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}