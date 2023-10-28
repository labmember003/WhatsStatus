package com.geeksoftapps.whatsweb.app.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager

fun View.setVisibility(visibility: Int, transition: Transition, duration: Long = 400) {
    transition.duration = duration
    transition.addTarget(this)
    TransitionManager.beginDelayedTransition(this.parent as ViewGroup, transition)
    this.visibility = visibility
}

fun View.slideVisibility(visibility: Int, slideEdge: Int, duration: Long = 400) {
    this.setVisibility(visibility, Slide(slideEdge), duration)
}

inline fun <T : Activity?> T?.runSafely(crossinline block: Activity.() -> Unit) {
    this?.takeIf { !this.isFinishing and !this.isDestroyed }?.run { block() }
}


inline fun AlertDialog.Builder.showSafely(activity: Activity) {
    activity.runSafely {
        show()
    }
}