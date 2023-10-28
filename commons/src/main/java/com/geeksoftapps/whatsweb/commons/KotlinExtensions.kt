package com.geeksoftapps.whatsweb.commons

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

inline fun <T : Fragment?> T?.runSafelyFragmentActivity(crossinline block: FragmentActivity.() -> Unit) {
    this
        ?.takeIf { it?.isAdded == true }
        ?.let { fragment ->
            val activity = fragment?.activity
            activity ?: return
            activity.runSafely { block.invoke(activity) }
        }
}

inline fun <T : Activity?> T?.runSafely(crossinline block: Activity.() -> Unit) {
    this?.takeIf { !this.isFinishing and !this.isDestroyed }?.run { block() }
}