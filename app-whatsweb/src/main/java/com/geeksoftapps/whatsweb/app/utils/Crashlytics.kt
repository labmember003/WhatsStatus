package com.geeksoftapps.whatsweb.app.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.geeksoftapps.whatsweb.commons.toast

fun logCrashlytics(
    message: String,
    showToast: Boolean = true,
    toastMessage: String = "Something went wrong. This bug will be fixed in the next update."
) {
    FirebaseCrashlytics.getInstance().recordException(Exception(message))
    if (showToast) {
        toast(toastMessage)
    }
}
