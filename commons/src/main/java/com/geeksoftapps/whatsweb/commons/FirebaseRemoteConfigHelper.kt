package com.geeksoftapps.whatsweb.commons

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class FirebaseRemoteConfigHelper {

    companion object {
        private const val CACHE_TIMEOUT_SECS: Long = 10 * 60 // 10 min

        private val remoteConfig by lazy {
            Firebase.remoteConfig
        }
        @JvmStatic
        fun init() {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = CACHE_TIMEOUT_SECS
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.default_remote_config)
        }

        @JvmStatic
        fun fetch() {
            var cacheExpiration: Long = CACHE_TIMEOUT_SECS
            // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
            // retrieve values from the service.
            if (BuildConfig.DEBUG) {
                cacheExpiration = 0
            }

            remoteConfig.fetch(cacheExpiration).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // After config data is successfully fetched, it must be activated before newly fetched
                    // values are returned.
                    remoteConfig.activate()
                } else {

                }
            }
        }

        fun addOnConfigUpdateListener() {
            try {
                remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate: ConfigUpdate) {
                        remoteConfig.activate()
                    }

                    override fun onError(error: FirebaseRemoteConfigException) {
                    }
                })
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
