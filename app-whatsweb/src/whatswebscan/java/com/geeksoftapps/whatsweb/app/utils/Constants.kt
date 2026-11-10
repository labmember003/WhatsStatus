package com.geeksoftapps.whatsweb.app.utils

import com.geeksoftapps.whatsweb.app.BuildConfig


object Constants {
    const val DEVELOPER_EMAIL = "usarcompanion@gmail.com"
    const val FEEDBACK_EMAIL = DEVELOPER_EMAIL

    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/whatsscanfalcon/home"

    //Firebase Remote Config
    const val DISABLE_APP_UPDATE = "disable_app_update"
    const val APP_RATE_DIALOG_INTERVAL = "app_rate_dialog_interval"
    const val APP_RATING_FLOW_V2 = "app_rating_flow_v2"
    const val TOTAL_TIME_SPENT_THRESHOLD = "total_time_spent_threshold"
    const val RATING_THRESHOLD = "rating_threshold"
}