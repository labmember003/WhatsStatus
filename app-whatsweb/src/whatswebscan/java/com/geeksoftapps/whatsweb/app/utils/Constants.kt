package com.geeksoftapps.whatsweb.app.utils

import com.geeksoftapps.whatsweb.app.BuildConfig


object Constants {
    const val DEVELOPER_EMAIL = "codekraftapps@gmail.com"
    const val FEEDBACK_EMAIL = DEVELOPER_EMAIL

    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/whatsweb-codekraftapps/home"

    const val defaultUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    //Firebase Remote Config
    const val SHOULD_ENABLE_ADS = "should_enable_ads"
    const val USER_AGENT = "user_agent"
    const val DISABLE_APP_UPDATE = "disable_app_update"
    const val ADS_INTERVAL = "ads_interval"
    const val QR_CODE_APP_ID = "qr_code_app_id"
    const val CLEANER_APP_ID = "cleaner_app_id"
    const val RECOVER_MESSAGES_APP_ID = "recover_messages_app_id"
    const val APP_RATE_DIALOG_INTERVAL = "app_rate_dialog_interval"
    const val APP_RATING_FLOW_V2 = "app_rating_flow_v2"
    const val TOTAL_TIME_SPENT_THRESHOLD = "total_time_spent_threshold"
    const val RATING_THRESHOLD = "rating_threshold"
}