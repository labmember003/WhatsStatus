package com.geeksoftapps.whatsweb.app.ui.status

import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.ui.status.fragments.StatusContainerFragment
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.databinding.ActivityStatusSaverBinding
import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation
import com.geeksoftapps.whatsweb.commons.BannerAdViewGroups
import com.geeksoftapps.whatsweb.commons.BasicActivity


class StatusSaverActivity : BasicActivity(), StatusContainerFragment.StatusSaverFragmentActions {

    private lateinit var binding: ActivityStatusSaverBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_status_saver)
        setSupportActionBar(binding.toolbar)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setToolBarTitle(title: String) {

    }

    override fun onHomePress() {
        finish()
    }
}
