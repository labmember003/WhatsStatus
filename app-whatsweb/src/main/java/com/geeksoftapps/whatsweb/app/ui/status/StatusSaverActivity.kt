package com.geeksoftapps.whatsweb.app.ui.status

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.databinding.ActivityStatusSaverBinding
import com.geeksoftapps.whatsweb.app.ui.AppSettingsActivity
import com.geeksoftapps.whatsweb.app.ui.status.fragments.StatusContainerFragment
import com.geeksoftapps.whatsweb.commons.BasicActivity


class StatusSaverActivity : BasicActivity(), StatusContainerFragment.StatusSaverFragmentActions {

    private lateinit var binding: ActivityStatusSaverBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_status_saver)
        setSupportActionBar(binding.toolbar)

        val sharedPreferences = getSharedPreferences("sharedPreferencesFileName", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("isInitLaunch", false)
        editor.apply()

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        startFragment(StatusContainerFragment(), StatusContainerFragment.TAG)
    }
    private fun startFragment(fragment: Fragment, tag: String) {
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.fragment_placeholder, fragment, tag)
        transaction.commit()
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        finishAffinity()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_status_saver, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        if (item.itemId == R.id.menu_settings) {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setToolBarTitle(title: String) {

    }

    override fun onHomePress() {
        finish()
    }
}
