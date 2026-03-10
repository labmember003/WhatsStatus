package com.geeksoftapps.whatsweb.commons

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class Analytics internal constructor() {
    fun log(eventName: String, itemName: String? = null) {
        try {
            val bundle = Bundle()
            itemName?.let { bundle.putString("item_name", it) }
            Firebase.analytics.logEvent(eventName, bundle)
        } catch (e: Exception) {
            // Analytics failures should not crash the app
        }
    }
}
