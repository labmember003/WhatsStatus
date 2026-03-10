package com.geeksoftapps.whatsweb.app.appconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

data class ExternalAppConfigData(
    val showRecoverMessagesApp: Boolean = false,
    val recoverMessagesAppId: String = "",
    val showCleanerApp: Boolean = false,
    val cleanerAppId: String = ""
)

object ExternalAppConfig {
    private const val KEY_SHOW_RECOVER_MESSAGES_APP = "show_recover_messages_app"
    private const val KEY_RECOVER_MESSAGES_APP_ID = "recover_messages_app_id"
    private const val KEY_SHOW_CLEANER_APP = "show_cleaner_app"
    private const val KEY_CLEANER_APP_ID = "cleaner_app_id"

    fun getConfig(callback: (ExternalAppConfigData) -> Unit) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val config = ExternalAppConfigData(
            showRecoverMessagesApp = remoteConfig.getBoolean(KEY_SHOW_RECOVER_MESSAGES_APP),
            recoverMessagesAppId = remoteConfig.getString(KEY_RECOVER_MESSAGES_APP_ID),
            showCleanerApp = remoteConfig.getBoolean(KEY_SHOW_CLEANER_APP),
            cleanerAppId = remoteConfig.getString(KEY_CLEANER_APP_ID)
        )
        callback(config)
    }
}
