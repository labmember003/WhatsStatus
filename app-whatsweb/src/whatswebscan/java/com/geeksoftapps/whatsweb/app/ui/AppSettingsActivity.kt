package com.geeksoftapps.whatsweb.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.geeksoftapps.whatsweb.app.BuildConfig
import com.geeksoftapps.whatsweb.app.R
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.geeksoftapps.whatsweb.commons.BasicActivity
import com.geeksoftapps.whatsweb.commons.log

import com.geeksoftapps.whatsweb.app.databinding.ActivitySettingsBinding
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.Constants
import com.geeksoftapps.whatsweb.app.utils.log
import java.util.*


class AppSettingsActivity : BasicActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings,
                SettingsFragment()
            )
            .commit()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            getAllPreferences().forEach {
                it.onPreferenceClickListener = this@SettingsFragment
                it.onPreferenceChangeListener = this@SettingsFragment
                setSummary(it)
            }
        }

        private fun setSummary(preference: Preference) {
            when(preference.key) {
                getString(R.string.key_preference_app_version) -> {
                    preference.summary = BuildConfig.VERSION_NAME
                }
            }
        }

        override fun onPreferenceClick(preference: Preference?): Boolean {
            return when(preference?.key) {
                getString(R.string.key_preference_bug_report) -> {
                    //Show a dialog and get feedback and post it somewhere else
                    //no need to clutter email
                    context?.let { CommonUtils.reportBug(it) }
                    true
                }
                getString(R.string.key_preference_open_source_licenses) -> {
                    startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
                    true
                }
                getString(R.string.key_preference_privacy_policy) -> {
                    context?.let {context ->
                        CommonUtils.openBrowser(context, Constants.PRIVACY_POLICY_URL)
                    }
                    true
                }
                getString(R.string.key_preference_send_feedback) -> {
                    context?.let { context->
                        CommonUtils.sendFeedback(context)
                    }
                    true
                }
                else -> true
            }
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            return when(preference?.key) {
                getString(R.string.key_preference_dark_mode) -> {
                    val value = newValue as String
                    when(value) {
                        WhatsWebPreferences.DARK_MODE_ON -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                        WhatsWebPreferences.DARK_MODE_OFF -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }
                        WhatsWebPreferences.DARK_MODE_SYSTEM_DEFAULT -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                    return true
                }
                else -> true
            }
        }

        private fun getAllPreferences(): List<Preference> {
            val allPreferences = mutableListOf<Preference>()
            val queue: Queue<Preference> = LinkedList()
            queue.add(preferenceScreen)
            while (!queue.isEmpty()) {
                val preference = queue.poll()
                if (preference is PreferenceCategory || preference is PreferenceGroup) {
                    val group = (preference as PreferenceGroup)
                    val count = group.preferenceCount
                    for (i in 0 until count) {
                        queue.add(group.getPreference(i))
                    }
                } else {
                    allPreferences.add(preference)
                }
            }
            return allPreferences
        }
    }
}