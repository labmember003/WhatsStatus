package com.geeksoftapps.whatsweb.app.appconfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

data class ActPremiumConfig(
    val shouldActPremium: Boolean = false
)

object ActPremiumAppConfig {
    private const val SHOULD_ACT_PREMIUM = "should_act_premium"
    private var config: ActPremiumConfig? = null

    private fun getConfig(): ActPremiumConfig {
        return config ?: run {
            val remoteConfig = Firebase.remoteConfig
            val shouldActPremium = remoteConfig.getBoolean(SHOULD_ACT_PREMIUM)
            ActPremiumConfig(shouldActPremium = shouldActPremium).also { config = it }
        }
    }

    suspend fun getConfig(callback: (ActPremiumConfig) -> Unit) {
        callback(getConfig())
    }
}
