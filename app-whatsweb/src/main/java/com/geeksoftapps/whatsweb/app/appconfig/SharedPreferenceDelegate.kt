package com.geeksoftapps.whatsweb.app.appconfig

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPreferenceDelegate<T>(
        private val sharedPreferences: SharedPreferences,
        private val key: String?,
        private val decode: (str: String) -> T,
        private val encode: (obj: T) -> String,
        private val defaultValue: T
) {
    private var cache = defaultValue
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (key == null) {
            return cache
        }
        return decode(sharedPreferences.getString(key, null) ?: return defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (key == null) {
            cache = value
            return
        }
        sharedPreferences.edit().putString(key, encode(value)).commit()
    }
}

class SharedPreferenceDelegateNullable<T>(
        private val sharedPreferences: SharedPreferences,
        private val key: String?,
        private val decode: (str: String) -> T,
        private val encode: (obj: T) -> String,
        private val defaultValue: T?
) {
    private var cache = defaultValue
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (key == null) {
            return cache
        }
        return decode(sharedPreferences.getString(key, null) ?: return defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (key == null) {
            cache = value
            return
        }
        if (value == null) {
            sharedPreferences.edit().remove(key).commit()
        } else {
            sharedPreferences.edit().putString(key, encode(value)).commit()
        }
    }
}