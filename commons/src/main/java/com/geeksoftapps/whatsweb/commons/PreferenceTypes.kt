package com.geeksoftapps.whatsweb.commons

import com.preference.Preference
import kotlin.reflect.KProperty

abstract class PreferenceTypes {
    protected abstract val preference: Preference

    inner class Int(val name: kotlin.String, private val defaultValue: kotlin.Int = 0) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.Int {
            return preference.getInt(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.Int) {
            preference.setInt(name, value)
        }
    }

    inner class Long(val name: kotlin.String, private val defaultValue: kotlin.Long = 0L) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.Long {
            return preference.getLong(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.Long) {
            preference.setLong(name, value)
        }
    }

    inner class Double(val name: kotlin.String, private val defaultValue: kotlin.Double = 0.0) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.Double {
            return preference.getDouble(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.Double) {
            preference.setDouble(name, value)
        }
    }

    inner class Float(val name: kotlin.String, private val defaultValue: kotlin.Float = 0f) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.Float {
            return preference.getFloat(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.Float) {
            preference.setFloat(name, value)
        }
    }

    inner class Boolean(val name: kotlin.String, private val defaultValue: kotlin.Boolean = false) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.Boolean {
            return preference.getBoolean(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.Boolean) {
            preference.setBoolean(name, value)
        }
    }

    inner class String(val name: kotlin.String, private val defaultValue: kotlin.String = "") {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): kotlin.String {
            return preference.getString(name, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: kotlin.String) {
            preference.setString(name, value)
        }
    }
}