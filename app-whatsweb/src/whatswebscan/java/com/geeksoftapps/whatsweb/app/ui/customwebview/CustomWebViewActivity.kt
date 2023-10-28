package com.geeksoftapps.whatsweb.app.ui.customwebview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.geeksoftapps.whatsweb.app.App
import com.geeksoftapps.whatsweb.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.geeksoftapps.whatsweb.app.databinding.ActivityCustomWebViewBinding
import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation
import com.geeksoftapps.whatsweb.app.ui.dialogs.AudioRestartDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.CameraRestartDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.CustomWebViewHelpDialog
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.slideVisibility
import com.geeksoftapps.whatsweb.commons.BannerAdViewGroups
import com.geeksoftapps.whatsweb.commons.BasicActivity
import com.geeksoftapps.whatsweb.commons.log
import com.geeksoftapps.whatsweb.commons.toast
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.RuntimePermissions
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxBannerAdUnitId
import com.geeksoftapps.whatsweb.app.ui.ads.getMaxInterstitialAdUnitId
import com.geeksoftapps.whatsweb.app.ui.ads.shouldEnableAds
import com.geeksoftapps.whatsweb.app.utils.log
import com.geeksoftapps.whatsweb.app.utils.runSafely
import com.geeksoftapps.whatsweb.app.utils.showSafely
import com.geeksoftapps.whatsweb.commons.FirebaseRemoteConfigHelper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.math.abs

@RuntimePermissions
class CustomWebViewActivity : BasicActivity(), CustomWebView.OnWhatsWebViewActionListener {

    override val isAdsEnabled: Boolean
        get() = WhatsWebPreferences.isAdsEnabled()

    override val maxBannerAdUnitId = getMaxBannerAdUnitId(BannerAdLocation.ACTIVITY_WEB_VIEW)
    override val maxInterstitialAdUnitId = getMaxInterstitialAdUnitId(InterstitialAdLocation.ACTIVITY_WEB_VIEW)

    private var keyboardEnabled = false
    private var lastTouchClick: Long = 0
    private var lastXClick = 0.0f
    private var lastYClick = 0.0f

    private var isDarkModeEnabled = CommonUtils.isDarkMode(App.getInstance().applicationContext)
    private var isFullScreen = false

    private lateinit var binding: ActivityCustomWebViewBinding

    override fun getBannerAdView(): BannerAdViewGroups =
        BannerAdViewGroups(
            bannerAdViewGroup = binding.bannerInclude.bannerAdView,
            bannerContainer = binding.bannerInclude.bannerContainer
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_custom_web_view)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window?.setSoftInputMode(WindowManager.LayoutParams.ANIMATION_CHANGED)

//        WebView.setWebContentsDebuggingEnabled(true)

        binding.webView.apply {
            initiate()

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    initiateDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                } else {
                    initiateDownloadWithPermissionCheck(url, userAgent, contentDisposition, mimetype, contentLength)
                }
            }

            setOnWhatsWebViewActionListener(this@CustomWebViewActivity)

            setOnTouchListener { v, motionEvent ->
                v.performClick()
                lastTouchClick = System.currentTimeMillis()
                lastXClick = motionEvent.x
                lastYClick = motionEvent.y
                if (binding.parentView.descendantFocusability == ViewGroup.FOCUS_BLOCK_DESCENDANTS && motionEvent.action == 0 && abs(
                        motionEvent.y - binding.webView.height.toFloat()
                    ) < 160.0f
                ) {
                    when {
                        System.currentTimeMillis() - lastTouchClick >= 1300 -> {
                            lastTouchClick = System.currentTimeMillis()
                            lastXClick = motionEvent.x
                            lastYClick = motionEvent.y
                        }
                        abs(lastXClick - motionEvent.x) < 180.0f -> {
                            analytics?.log("noftify_user_keyboard_blocked",var2 = "webview_screen")
                            notifyUser("Use keyboard button on top to type")
                            lastTouchClick = 0
                        }
                        else -> {
                            lastTouchClick = System.currentTimeMillis()
                            lastXClick = motionEvent.x
                            lastYClick = motionEvent.y
                        }
                    }
                }
                false
            }

            if (savedInstanceState == null) {
                loadWebWhatsapp()
            } else {
                // savedInstanceState is present
            }
        }

        binding.fabExitFullScreen.setOnClickListener {
            analytics?.log("fab_exit_full_screen",var2 = "webview_screen")
            exitFullScreen()
        }

        binding.webView.toggleDarkMode(isDarkModeEnabled)

        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            if (e.message?.contains("webview") == true) {
                toast(getString(R.string.try_again))
                runSafely {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        keyboardEnabled = WhatsWebPreferences.isKeyboardEnabled
        if(!keyboardEnabled){
            toggleKeyboard(false)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun initiateDownload(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
        analytics?.log("webview_initiate_download", var2 = "webview_screen")
        binding.webView.handleDownload(url, userAgent, contentDisposition, mimetype, contentLength)
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onExternalStorageWritePermissionDenied() {
        analytics?.log("webview_write_permission_denied", var2 = "webview_screen")
        toast(getString(R.string.storage_perm_reqd))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview_menu, menu)
        return true
    }

    override fun finishActivity() {
        analytics?.log("webview_init_failed_finishing", var2 = "webview_screen")
        finish()
    }

    override fun startFileChooser(
        fileChooserParams: WebChromeClient.FileChooserParams,
        requestCode: Int
    ) {
        analytics?.log("webview_start_file_chooser", var2 = "webview_screen")
        startActivityForResult(fileChooserParams.createIntent(), requestCode)
    }

    override fun startAudioCapture(request: PermissionRequest, requestCode: Int) {
        analytics?.log("webview_start_audio_capture", var2 = "webview_screen")
        if (PermissionUtils.hasSelfPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
            request.deny()
            getAudioRecordPermissionWithPermissionCheck(true)
        }
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun getAudioRecordPermission(showRetryDialog: Boolean) {
        if (showRetryDialog) {
            runSafely {
                AudioRestartDialog.builder(this).show()
            }
        }
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onAudioRecordPermissionDenied() {
        analytics?.log("webview_audio_permission_denied", var2 = "webview_screen")
        toast(getString(R.string.audio_perm_reqd))
    }

    override fun startVideoCapture(request: PermissionRequest, requestCode: Int) {
        analytics?.log("webview_start_video_capture", var2 = "webview_screen")
        if (PermissionUtils.hasSelfPermissions(this, Manifest.permission.CAMERA)) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            request.deny()
            getCameraPermissionWithPermissionCheck(true)
        }
    }

    override fun onBlobDownloadCompleted(uriString: String) {
        analytics?.log("on_blob_download_completed", var2 = "webview_screen")
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun getCameraPermission(showRetryDialog: Boolean) {
        if (showRetryDialog) {
            runSafely {
                CameraRestartDialog.builder(this).show()
            }
        }
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraPermissionDenied() {
        analytics?.log("webview_camera_permission_denied", var2 = "webview_screen")
        toast(getString(R.string.camera_perm_reqd))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (binding.webView.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated function
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                analytics?.log("options_menu_back_pressed",var2 = "webview_screen")
                finish()
            }
            R.id.webview_menu_refresh -> {
                analytics?.log("options_menu_webview_refresh_tapped",var2 = "webview_screen")
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.reset_reload))
                    .setMessage(getString(R.string.reload_warning))
                    .setPositiveButton(getString(R.string.reset_only)) { dialog, which ->
                        analytics?.log("options_menu_webview_refresh_dialog_reset_tapped",var2 = "webview_screen")
                        binding.webView.reset()
                    }
                    .setNegativeButton(getString(R.string.reload_only)) { dialog, which ->
                        analytics?.log("options_menu_webview_refresh_dialog_reload_tapped",var2 = "webview_screen")
                        binding.webView.loadWebWhatsapp()
                    }
                    .showSafely(this)
            }
            R.id.webview_menu_toggle_keyboard -> {
                analytics?.log("options_menu_webview_toggle_keyboard_tapped", var1 = "keyboardEnabled: ${keyboardEnabled.not()}", var2 = "webview_screen")
                toggleKeyboard(!keyboardEnabled)
            }
            R.id.webview_menu_toggle_night_mode -> {
                isDarkModeEnabled = !isDarkModeEnabled
                binding.webView.toggleDarkMode(isDarkModeEnabled)
                analytics?.log("options_menu_webview_toggle_dark_mode_tapped", var1 = "darkMode: ${isDarkModeEnabled}", var2 = "webview_screen")
            }
            R.id.webview_fullscreen -> {
                analytics?.log("options_menu_webview_enter_fullscreen_tapped", var2 = "webview_screen")
                enterFullScreen()
            }
            R.id.webview_how_it_works -> {
                analytics?.log("options_menu_webview_how_it_works_tapped", var2 = "webview_screen")
                CustomWebViewHelpDialog.builder(this).showSafely(this)
            }
        }
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isFullScreen) enterFullScreen()
    }

    private fun enterFullScreen() {
        runSafely {
            binding.fabExitFullScreen.show()
            binding.toolbar.visibility = View.GONE
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
//                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            isFullScreen = true
        }
    }

    private fun exitFullScreen() {
        runSafely {
            binding.fabExitFullScreen.hide()
            binding.toolbar.visibility = View.VISIBLE
            window.decorView.systemUiVisibility = 0
//            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            isFullScreen = false
        }
    }

    private fun toggleKeyboard(isKeyboard: Boolean) {
        keyboardEnabled = isKeyboard
        val inputMethodManager: InputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (isKeyboard && binding.parentView.descendantFocusability == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
            binding.parentView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            notifyUser(getString(R.string.keyboard_unblocked))
        } else if (!isKeyboard) {
            binding.parentView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            notifyUser(getString(R.string.keyboard_blocked))
            binding.parentView.requestFocus()
            inputMethodManager.hideSoftInputFromWindow(binding.parentView.windowToken,0)
        }
        WhatsWebPreferences.isKeyboardEnabled = isKeyboard
    }

    private fun notifyUser(message: String, showSnackbar: Boolean = false) {
        runSafely {
            if (showSnackbar) {
                Snackbar.make(binding.parentView, message, Snackbar.LENGTH_SHORT).apply {
                    setAction(getString(R.string.dismiss)) {
                        this.dismiss()
                    }
                    setActionTextColor(Color.parseColor("#075E54"))
                }.show()
            } else {
                toast(message)
            }
        }
    }
}
