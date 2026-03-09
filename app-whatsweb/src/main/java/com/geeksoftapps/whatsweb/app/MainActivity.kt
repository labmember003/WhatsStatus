package com.geeksoftapps.whatsweb.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.geeksoftapps.whatsweb.app.appconfig.ActPremiumAppConfig
import com.geeksoftapps.whatsweb.app.appconfig.ExternalAppConfig
import com.geeksoftapps.whatsweb.app.databinding.ActivityMainBinding
import com.geeksoftapps.whatsweb.app.ui.AppSettingsActivity
import com.geeksoftapps.whatsweb.app.ui.ChatActivity
import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxBannerAdUnitId
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxInterstitialAdUnitId
import com.geeksoftapps.whatsweb.app.ui.customwebview.CustomWebViewActivity
import com.geeksoftapps.whatsweb.app.ui.dialogs.RatingDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.StartAppUpdateDialog
import com.geeksoftapps.whatsweb.app.ui.status.StatusSaverActivity
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.FullVersionUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.showSafely
import com.geeksoftapps.whatsweb.commons.*
import com.geeksoftapps.whatsweb.commons.log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class MainActivity : BasicActivity(), KodeinAware, InstallStateUpdatedListener,
    PurchasesUpdatedListener {

    override val kodein by closestKodein()
    private var appUpdateManager: AppUpdateManager? = null
    private val REQUEST_CODE_FLEXIBLE_UPDATE = 11213

    private lateinit var binding: ActivityMainBinding
    private var billingClient: BillingClient? = null

    override val isAdsEnabled: Boolean
        get() = !WhatsWebPreferences.isFullVersionEnabled

    override val maxBannerAdUnitId = getMaxBannerAdUnitId(BannerAdLocation.ACTIVITY_MAIN)
    override val maxInterstitialAdUnitId = getMaxInterstitialAdUnitId(InterstitialAdLocation.ACTIVITY_MAIN)

    override fun getBannerAdView(): BannerAdViewGroups =
        BannerAdViewGroups(
            bannerAdViewGroup = binding.bannerInclude.bannerAdView,
            bannerContainer = binding.bannerInclude.bannerContainer
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        RatingDialog.getDialog(this, true)?.show()
        setLayout()
        setOnClickListeners()
        loadBillingClient()
        initiateAppUpdateManager()
        WhatsWebPreferences.isFullVersionEnabled = true
    }

    private fun setLayout() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.mainGrid.columnCount = 3
        } else {
            binding.mainGrid.columnCount = 2
        }
    }

    private fun loadBillingClient() {
        lifecycleScope.launch(Dispatchers.IO) {
            billingClient = BillingClient.newBuilder(this@MainActivity)
                .enablePendingPurchases().setListener(this@MainActivity).build()
        }
    }

    private fun initiateAppUpdateManager() {
        appUpdateManager = AppUpdateManagerFactory.create(this@MainActivity)
        appUpdateManager?.registerListener(this@MainActivity)

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    @Suppress("DEPRECATION")
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

    private fun setOnClickListeners(){
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        binding.ivShare.setOnClickListener {
            try {
                startActivity(CommonUtils.share(this))
            } catch (anfe: ActivityNotFoundException) {
                toast("Sorry, we are not able to find any mailing app in your phone. Please install a mailing app or contactus.")
            }
        }
        binding.cvWhatsWeb.circle.setOnClickListener {
            startActivity(Intent(this, CustomWebViewActivity::class.java))
        }
        binding.cvWhatsDeleted.circle.setOnClickListener {
            handleRecoverMessagesStart()
        }
        binding.cvStatusSaver.circle.setOnClickListener {
            if(!CommonUtils.isPackageInstalled("com.whatsapp",packageManager)) {
                toast(getString(R.string.whatsapp_not_installed))
            } else startActivity(Intent(this, StatusSaverActivity::class.java))
        }
        binding.cvWhatsCleaner.circle.setOnClickListener {
            handleCleanerStart()
        }
        binding.cvDirectChat.circle.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        binding.cvRemoveAds.circle.setOnClickListener {
            if (binding.isPremium == true) {
                startActivity(Intent(this, AppSettingsActivity::class.java))
            } else {
                onPurchasePremiumClicked()
            }
        }
    }

    private fun handleRecoverMessagesStart() {
        toast(getString(R.string.loading))
        ExternalAppConfig.getConfig {
            lifecycleScope.launch(Dispatchers.Main) {
                if (it.showRecoverMessagesApp) {
                    val isAppInstalled = CommonUtils.isPackageInstalled(it.recoverMessagesAppId, packageManager)
                    if (isAppInstalled) {
                        CommonUtils.startNewActivity(this@MainActivity, it.recoverMessagesAppId)
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.download_recover_messages_app))
                            .setMessage(getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app))
                            .setPositiveButton(R.string.ok) { dialog, which ->
                                CommonUtils.startNewActivity(this@MainActivity, it.recoverMessagesAppId)
                            }.setNegativeButton(R.string.cancel) { dialog, which ->
                                dialog.dismiss()
                            }
                            .showSafely(this@MainActivity)
                    }
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.download_recover_messages_app))
                        .setMessage(getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app))
                        .setPositiveButton(R.string.ok) { dialog, which ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=recover%20deleted%20messages&c=apps")))
                        }.setNegativeButton(R.string.cancel) { dialog, which ->
                            dialog.dismiss()
                        }
                        .showSafely(this@MainActivity)
                }
            }
        }
    }

    private fun handleCleanerStart() {
        toast(getString(R.string.loading))
        ExternalAppConfig.getConfig {
            lifecycleScope.launch(Dispatchers.Main) {
                if (it.showCleanerApp) {
                    val isAppInstalled = CommonUtils.isPackageInstalled(it.cleanerAppId, packageManager)
                    if (isAppInstalled) {
                        CommonUtils.startNewActivity(this@MainActivity, it.cleanerAppId)
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.download_cleaner))
                            .setMessage(getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app))
                            .setPositiveButton(R.string.ok) { dialog, which ->
                                CommonUtils.startNewActivity(this@MainActivity, it.cleanerAppId)
                            }.setNegativeButton(R.string.cancel) { dialog, which ->
                                dialog.dismiss()
                            }
                            .showSafely(this@MainActivity)
                    }
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.download_cleaner))
                        .setMessage(getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app))
                        .setPositiveButton(R.string.ok) { dialog, which ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=wa%20cleaner&c=apps")))
                        }.setNegativeButton(R.string.cancel) { dialog, which ->
                            dialog.dismiss()
                        }
                        .showSafely(this@MainActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.isPremium = true
        lifecycleScope.launch {
            ActPremiumAppConfig.getConfig {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (!it.shouldActPremium) {
                        binding.isPremium = WhatsWebPreferences.isFullVersionEnabled
                    } else {
                        binding.isPremium = true
                    }
                }
            }
        }
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener {
            if (it.installStatus() == InstallStatus.DOWNLOADED) {
                StartAppUpdateDialog.get(this@MainActivity) { _, _ ->
                    appUpdateManager?.completeUpdate()
                    appUpdateManager?.unregisterListener(this@MainActivity)
                }.showSafely(this@MainActivity)
            }
        }
        Firebase.remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    log("Appconfig: success")
                } else {
                    log("Appconfig: failed")
                }
            }
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
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

    private fun popupSnackbarForCompleteUpdate() {
        analytics.logEvent("app_update_downloaded", null)
        Snackbar.make(
            binding.parentLayout,
            getString(R.string.update_downloaded_message),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.restart)) { appUpdateManager?.completeUpdate() }
            show()
        }
    }

    /**
     *  Purchase Premium Flow
     */

    private fun onPurchasePremiumClicked() {
        analytics.logEvent("OnPurchaseClicked", null)
        if (billingClient?.isReady == true) {
            queryExistingPurchases { purchases ->
                if (purchases != null && purchases.isNotEmpty()) {
                    showToastAndRestartActivity(getString(R.string.premium_unlocked))
                } else {
                    initiatePurchase()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                billingClient =
                    BillingClient.newBuilder(this@MainActivity).enablePendingPurchases().setListener(
                        this@MainActivity
                    ).build()
                billingClient?.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryExistingPurchases { purchases ->
                                if (purchases != null && purchases.isNotEmpty()) {
                                    showToastAndRestartActivity(getString(R.string.premium_unlocked))
                                } else {
                                    initiatePurchase()
                                }
                            }
                        } else {
                            toggleProgressDialog(false)
                            showToastIfActivityExists(getString(R.string.try_again))
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        toggleProgressDialog(false)
                    }
                })
            }
        }
        toggleProgressDialog(true)
    }

    private fun queryExistingPurchases(callback: (List<Purchase>?) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                callback(purchases)
            } else {
                callback(null)
            }
        }
    }

    private fun showToastAndRestartActivity(message: String) {
        WhatsWebPreferences.isFullVersionEnabled = true
        runOnUiThread {
            toggleProgressDialog(false)
            showToastIfActivityExists(message)
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition( 0, 0)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition( 0, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binding.pb.isVisible)
            toggleProgressDialog(false)
        billingClient?.endConnection()
    }

    private fun showToastIfActivityExists(message: String) {
        if (!isFinishing && !isDestroyed)
            runOnUiThread { toast(message) }
    }

    private fun toggleProgressDialog(showDialog: Boolean) {
        if (!isFinishing && !isDestroyed) {
            runOnUiThread {
                if (showDialog) {
                    window?.setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    binding.pbBackground.visibility = View.VISIBLE
                    binding.pb.visibility = View.VISIBLE
                } else  {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    binding.pbBackground.visibility = View.GONE
                    binding.pb.visibility = View.GONE
                }
            }
        }
    }

    private fun initiatePurchase() = lifecycleScope.launch(Dispatchers.IO) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(FullVersionUtils.fullVersionPurchaseId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    val productDetailsParams = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetailsList[0])
                            .build()
                    )
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParams)
                        .build()
                    runOnUiThread {
                        billingClient?.launchBillingFlow(this@MainActivity, flowParams)
                    }
                } else {
                    toggleProgressDialog(false)
                    showToastIfActivityExists(getString(R.string.purchase_item_not_found))
                }
            } else {
                toggleProgressDialog(false)
                showToastIfActivityExists(getString(R.string.error) + " " + billingResult.debugMessage)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryExistingPurchases { alreadyPurchases ->
                alreadyPurchases?.let { handlePurchases(it) } ?: run {
                    toggleProgressDialog(false)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            toggleProgressDialog(false)
            showToastIfActivityExists(getString(R.string.purchase_canceled))
        } else {
            toggleProgressDialog(false)
            showToastIfActivityExists(getString(R.string.error) + " " + billingResult.debugMessage)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (FullVersionUtils.fullVersionPurchaseId == purchase.products[0] && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams, ackPurchase)
                } else {
                    toggleProgressDialog(false)
                    showToastAndRestartActivity(getString(R.string.premium_unlocked))
                }
            } else if (FullVersionUtils.fullVersionPurchaseId == purchase.products[0] && purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                toggleProgressDialog(false)
                showToastIfActivityExists(getString(R.string.pending_purchase))
            } else if (FullVersionUtils.fullVersionPurchaseId == purchase.products[0] && purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                toggleProgressDialog(false)
                showToastIfActivityExists(getString(R.string.purchase_status_not_known))
            }
        }
    }

    private var ackPurchase =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                analytics.logEvent("OnProductPurchased", null)
                showToastAndRestartActivity(getString(R.string.premium_unlocked))
            } else toggleProgressDialog(false)
        }

}
