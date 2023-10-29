package com.geeksoftapps.whatsweb.app.utils

import com.geeksoftapps.whatsweb.app.R
import com.preference.PowerPreference
import com.preference.Preference
import com.geeksoftapps.whatsweb.commons.PreferenceTypes
import com.geeksoftapps.whatsweb.app.App


object WhatsWebPreferences: PreferenceTypes() {
    const val DARK_MODE_SYSTEM_DEFAULT = "system_default"
    const val DARK_MODE_ON = "dark"
    const val DARK_MODE_OFF = "light"

    private val res = App.getAppResources()
    override val preference: Preference = PowerPreference.getDefaultFile()

    var isFullVersionEnabled
            by Boolean(res.getString(R.string.key_preference_full_version),false)

    var darkMode
            by String(res.getString(R.string.key_preference_dark_mode), DARK_MODE_SYSTEM_DEFAULT)

    var isKeyboardEnabled by Boolean(res.getString(R.string.key_preference_keyboard_enabled), true)

    var isWebViewFullscreenEnabled by Boolean(res.getString(R.string.key_preference_webview_fullscreen_enabled), false)

    var whatsAppStorageUri by String(res.getString(R.string.key_preference_whatsapp_storage_uri), "")

    var userRatedVersion by String(res.getString(R.string.key_preference_user_rated_version), "")

    //In Seconds
    var totalTimeSpentOnScreens by Long(res.getString(R.string.key_preference_time_spent_on_screens), 0)
}