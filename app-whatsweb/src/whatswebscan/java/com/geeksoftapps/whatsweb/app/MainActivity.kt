package com.geeksoftapps.whatsweb.app

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.geeksoftapps.whatsweb.app.databinding.ActivityMainBinding
import com.geeksoftapps.whatsweb.app.ui.ads.shouldDisableAppUpdate
import com.geeksoftapps.whatsweb.app.ui.dialogs.RatingDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.StartAppUpdateDialog
import com.geeksoftapps.whatsweb.app.ui.status.StatusSaverActivity
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.log
import com.geeksoftapps.whatsweb.app.utils.showSafely
import com.geeksoftapps.whatsweb.commons.BasicActivity
import com.geeksoftapps.whatsweb.commons.toast
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class MainActivity : BasicActivity(), KodeinAware, InstallStateUpdatedListener,
    PurchasesUpdatedListener {

    override val kodein by closestKodein()
    private var appUpdateManager: AppUpdateManager? = null
    private val REQUEST_CODE_FLEXIBLE_UPDATE = 11213

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics?.log(
            eventName = "opened_app",
            var2 = "home_screen"
        )
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        startActivity(Intent(this, StatusSaverActivity::class.java))
        RatingDialog.getDialog(this, true)?.show()
        if (!shouldDisableAppUpdate())
            initiateAppUpdateManager()
    }

    private fun initiateAppUpdateManager() {
        appUpdateManager = AppUpdateManagerFactory.create(this@MainActivity)
        appUpdateManager?.registerListener(this@MainActivity)

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager?.startUpdateFlowForResult(
                        it,
                        AppUpdateType.FLEXIBLE,
                        this@MainActivity,
                        REQUEST_CODE_FLEXIBLE_UPDATE
                    )
                } catch (e: Exception) {
                    Firebase.crashlytics.recordException(e)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.isPremium = !WhatsWebPreferences.isAdsEnabled()
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) {
                StartAppUpdateDialog.get(this@MainActivity) { dialog, which ->
                    appUpdateManager?.completeUpdate()
                    appUpdateManager?.unregisterListener(this@MainActivity)
                }.showSafely(this@MainActivity)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FLEXIBLE_UPDATE) {
            if (resultCode == RESULT_OK) {
                toast(getString(R.string.downloading_update))
            }
        }
    }

    override fun onStateUpdate(state: InstallState) {
        TODO("Not yet implemented")
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        TODO("Not yet implemented")
    }


}
