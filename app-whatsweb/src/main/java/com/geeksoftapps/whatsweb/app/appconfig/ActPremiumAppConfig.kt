package com.geeksoftapps.whatsweb.app.appconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

data class AppConfig(
    val shouldActPremium: Boolean = false
)

object ActPremiumAppConfig {
    private const val KEY_ACT_PREMIUM = "act_premium"

    fun getConfig(callback: (AppConfig) -> Unit) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val shouldActPremium = remoteConfig.getBoolean(KEY_ACT_PREMIUM)
        callback(AppConfig(shouldActPremium = shouldActPremium))
    }
}
