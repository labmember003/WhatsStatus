package com.geeksoftapps.whatsweb.app.appconfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExternalAppConfigData(
    val showRecoverMessagesApp: Boolean = false,
    val recoverMessagesAppId: String = "",
    val showCleanerApp: Boolean = false,
    val cleanerAppId: String = ""
)

object ExternalAppConfig {
    private const val EXTERNAL_APP_CONFIG_KEY = "external_app_config"

    fun getConfig(callback: (ExternalAppConfigData) -> Unit) {
        val remoteConfig = Firebase.remoteConfig
        val jsonString = remoteConfig.getString(EXTERNAL_APP_CONFIG_KEY)
        val data = try {
            Json { ignoreUnknownKeys = true }.decodeFromString(ExternalAppConfigData.serializer(), jsonString)
        } catch (e: Exception) {
            ExternalAppConfigData()
        }
        callback(data)
    }
}
