package com.geeksoftapps.whatsweb.app.ui.dialogs

import android.app.Activity
import com.codemybrainsout.ratingdialog.RatingDialog
import com.geeksoftapps.whatsweb.app.App
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.commons.toast

import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.Constants
import com.geeksoftapps.whatsweb.app.utils.Constants.APP_RATE_DIALOG_INTERVAL
import com.geeksoftapps.whatsweb.app.utils.Constants.APP_RATING_FLOW_V2
import com.geeksoftapps.whatsweb.app.utils.Constants.RATING_THRESHOLD
import com.geeksoftapps.whatsweb.app.utils.Constants.TOTAL_TIME_SPENT_THRESHOLD
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

object RatingDialog {

    fun getDialog(activity: Activity, session: Boolean): RatingDialog? {
        if (shouldShowRatingDialog())
            return getDialog(
                activity,
                getSessionCount(session)
            )
        return null
    }

    fun getDialog(activity: Activity, sessionCount: Int): RatingDialog {
        var userRating = 0F
        return RatingDialog.Builder(activity)
            .session(sessionCount)
            .threshold(getRatingThreshold())
            .title(activity.getString(R.string.rate_app))
            .positiveButtonText(activity.getString(R.string.rate_later))
            .positiveButtonTextColor(R.color.colorPrimary)
            .negativeButtonTextColor(R.color.grey_400)
            .ratingBarColor(R.color.colorPrimary)
            .ratingBarBackgroundColor(R.color.grey_200)
            .playstoreUrl("http://play.google.com/store/apps/details?id=${activity.packageName}")
            .onRatingChanged { rating, thresholdCleared ->
                userRating = rating
                if (thresholdCleared) {
                    toast(activity.getString(R.string.rateUs))
                    FirebaseAnalytics.getInstance(App.getInstance())
                        ?.log("rating_flow_submit_threshold_cleared", userRating.toString(), "threshold_cleared")

                    WhatsWebPreferences.userRatedVersion = CommonUtils.getAppVersion(App.getInstance())
                } else {
                    FirebaseAnalytics.getInstance(App.getInstance())
                        ?.log("rating_flow", userRating.toString(), "threshold_not_cleared")
                }
            }
            .onRatingBarFormSumbit { feedback ->
                toast(activity.getString(R.string.send_feedback))
                CommonUtils.sendFeedback(activity, "$feedback\n\nRating: $userRating")
                FirebaseAnalytics.getInstance(App.getInstance())
                    ?.log("rating_flow", userRating.toString(), "onRatingBarFormSumbit", feedback.toString())
            }
            .build()
    }

    private fun getSessionCount(session: Boolean) : Int {
        if (isAppRatingFlowV2() && hasTotalTimeSpentThresholdReached()) {
            WhatsWebPreferences.totalTimeSpentOnScreens = 0
            return 1
        } else if (session) {
            return getAppRateDialogInterval()
        } else return 1
    }

    private fun hasTotalTimeSpentThresholdReached() : Boolean {
        return WhatsWebPreferences.totalTimeSpentOnScreens > totalAppTimeThreshold()
    }

    private fun shouldShowRatingDialog(): Boolean {
        if (hasUserRatedCurrentVersion())
            return false
        return true
    }

    private fun getRatingThreshold() = FirebaseRemoteConfig.getInstance().getValue(
        RATING_THRESHOLD
    ).asDouble().toFloat()

    private fun totalAppTimeThreshold() = FirebaseRemoteConfig.getInstance().getValue(
        TOTAL_TIME_SPENT_THRESHOLD
    ).asLong()

    private fun isAppRatingFlowV2() = FirebaseRemoteConfig.getInstance().getValue(
        APP_RATING_FLOW_V2
    ).asBoolean()

    private fun hasUserRatedCurrentVersion() = WhatsWebPreferences.userRatedVersion == CommonUtils.getAppVersion(App.getInstance())

    private fun getAppRateDialogInterval() = FirebaseRemoteConfig.getInstance().getValue(
        APP_RATE_DIALOG_INTERVAL
    ).asLong().toInt()
}