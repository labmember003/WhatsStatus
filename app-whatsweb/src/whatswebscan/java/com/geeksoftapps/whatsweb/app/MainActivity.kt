package com.geeksoftapps.whatsweb.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetailsParams
import com.applovin.sdk.AppLovinSdk
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.geeksoftapps.whatsweb.commons.log
import com.geeksoftapps.whatsweb.app.ui.dialogs.RatingDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.StartAppUpdateDialog
import com.geeksoftapps.whatsweb.app.databinding.ActivityMainBinding
import com.geeksoftapps.whatsweb.app.utils.showSafely
import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation
import com.geeksoftapps.whatsweb.app.ui.status.StatusSaverActivity
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.commons.BannerAdViewGroups
import com.geeksoftapps.whatsweb.commons.BasicActivity
import com.geeksoftapps.whatsweb.commons.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.geeksoftapps.whatsweb.app.ui.AppSettingsActivity
import com.geeksoftapps.whatsweb.app.ui.ChatActivity
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxBannerAdUnitId
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxInterstitialAdUnitId
import com.geeksoftapps.whatsweb.app.ui.ads.shouldDisableAppUpdate
import com.geeksoftapps.whatsweb.app.ui.ads.shouldEnableAds
import com.geeksoftapps.whatsweb.app.ui.bottomsheet.MoreAppsBottomSheet
import com.geeksoftapps.whatsweb.app.ui.customwebview.CustomWebViewActivity
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.isDarkMode
import com.geeksoftapps.whatsweb.app.utils.Constants
import com.geeksoftapps.whatsweb.app.utils.FullVersionUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences.DARK_MODE_OFF
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences.DARK_MODE_ON
import com.geeksoftapps.whatsweb.app.utils.log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class MainActivity : BasicActivity(), KodeinAware, InstallStateUpdatedListener,
    PurchasesUpdatedListener {

    override val kodein by closestKodein()
    private var appUpdateManager: AppUpdateManager? = null
    private val REQUEST_CODE_FLEXIBLE_UPDATE = 11213

    private lateinit var binding: ActivityMainBinding
    private var billingClient: BillingClient? = null

    override val isAdsEnabled: Boolean
        get() = WhatsWebPreferences.isAdsEnabled()

    override val maxBannerAdUnitId = getMaxBannerAdUnitId(BannerAdLocation.ACTIVITY_MAIN)
    override val maxInterstitialAdUnitId = getMaxInterstitialAdUnitId(InterstitialAdLocation.ACTIVITY_MAIN)

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
