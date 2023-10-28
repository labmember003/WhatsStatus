package ui.utils

import com.geeksoftapps.whatsweb.app.BuildConfig


object Constants {
    const val DEVELOPER_EMAIL = "geeksoftapps@gmail.com"
    const val FEEDBACK_EMAIL = DEVELOPER_EMAIL

    const val APP_RATE_DIALOG_INTERVAL = 10

    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/whatsweb-geeksoftapps"
    const val TERMS_CONDITIONS_URL = "https://sites.google.com/view/whatsweb-geeksoftapps/tnc"

    const val defaultUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"
}