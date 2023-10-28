package ui.customwebview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.geeksoftapps.whatsweb.commons.*

import com.geeksoftapps.whatsweb.app.databinding.ActivityCustomWebViewBinding
import com.geeksoftapps.whatsweb.app.ui.ads.*
import com.geeksoftapps.whatsweb.app.ui.customwebview.CustomWebView
import com.geeksoftapps.whatsweb.app.ui.dialogs.AudioRestartDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.CameraRestartDialog
import com.geeksoftapps.whatsweb.app.ui.dialogs.CustomWebViewHelpDialog
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.app.utils.slideVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.RuntimePermissions
import ui.ads.getMaxBannerAdUnitId
import ui.ads.getMaxInterstitialAdUnitId
import kotlin.math.abs

@RuntimePermissions
class CustomWebViewActivity : BasicActivity(), CustomWebView.OnWhatsWebViewActionListener {

    private var lastTouchClick: Long = 0
    private var lastXClick = 0.0f
    private var lastYClick = 0.0f

    private lateinit var binding: ActivityCustomWebViewBinding

    private lateinit var state: State

    inner class State {

        private var _actionsExpanded = true
        private var _keyboard = WhatsWebPreferences.isKeyboardEnabled
        private var _fullscreen = WhatsWebPreferences.isWebViewFullscreenEnabled
        private var _darkMode = CommonUtils.isDarkMode(this@CustomWebViewActivity)

        var keyboard: Boolean
            get() = _keyboard
            set(value) {
                _keyboard = value
                toggleKeyboard(_keyboard)
                WhatsWebPreferences.isKeyboardEnabled = _keyboard
                binding.actionBar.itemKeyboard.isActive = _keyboard
            }
        var fullscreen: Boolean
            get() = _fullscreen
            set(value) {
                _fullscreen = value
                toggleFullScreen(_fullscreen)
                WhatsWebPreferences.isWebViewFullscreenEnabled = _fullscreen
                binding.actionBar.itemFullScreen.isActive = _fullscreen
            }
        var actionsExpanded: Boolean
            get() = _actionsExpanded
            set(value) {
                _actionsExpanded = value
                if (_actionsExpanded) {
                    showActions()
                } else {
                    hideActions()
                }
            }
        var darkMode: Boolean
            get() = _darkMode
            set(value) {
                _darkMode = value
                binding.webView.toggleDarkMode(_darkMode)
                binding.actionBar.itemDarkMode.isActive = _darkMode
            }
        fun setActionsExpandedValue(value: Boolean) { _actionsExpanded = value }
    }

    private var hideActionsJob: Job? = null

    override val isAdsEnabled: Boolean
        get() = !WhatsWebPreferences.isFullVersionEnabled

    override val maxBannerAdUnitId = getMaxBannerAdUnitId(BannerAdLocation.ACTIVITY_WEB_VIEW)
    override val maxInterstitialAdUnitId = getMaxInterstitialAdUnitId(InterstitialAdLocation.ACTIVITY_WEB_VIEW)

    override fun getBannerAdView(): BannerAdViewGroups =
        BannerAdViewGroups(
            bannerAdViewGroup = binding.bannerInclude.bannerAdView,
            bannerContainer = binding.bannerInclude.bannerContainer
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_custom_web_view)

        window?.setSoftInputMode(WindowManager.LayoutParams.ANIMATION_CHANGED)

        state = State()

        setupWebView(savedInstanceState)

        setupActions()

        showActions(showActionsForTime = 2000)

        Firebase.remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    log("Appconfig: success")
                } else {
                    log("Appconfig: failed")
                }
            }
    }

    private fun applyState() {
        state.apply {
            keyboard = keyboard
            fullscreen = fullscreen
            darkMode = darkMode
        }
    }

    private fun setupActions() {
        binding.actionBar.itemRefresh.cvWhatsWeb.setOnClickListener {
            state.actionsExpanded = false
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.reset_reload))
                .setMessage(getString(R.string.reload_warning))
                .setPositiveButton(getString(R.string.reload_only)) { dialog, which ->
                    binding.webView.loadWebWhatsapp()
                }
                .setNegativeButton(getString(R.string.reset_only)) { dialog, which ->
                    binding.webView.reset()
                }
                .show()
        }

        binding.actionBar.itemFullScreen.cvWhatsWeb.setOnClickListener {
            state.actionsExpanded = false
            state.fullscreen = !state.fullscreen
        }

        binding.actionBar.itemDarkMode.cvWhatsWeb.setOnClickListener {
            state.darkMode = !state.darkMode
        }

        binding.actionBar.itemKeyboard.cvWhatsWeb.setOnClickListener {
            state.keyboard = !state.keyboard
            state.actionsExpanded = false
        }

        binding.actionBar.itemHowItWorks.cvWhatsWeb.setOnClickListener {
            state.actionsExpanded = false
            CustomWebViewHelpDialog.builder(this).show()
        }

        binding.actionBar.ivClose.setOnClickListener {
            finish()
        }

        binding.actionBarShow.setOnClickListener {
            state.actionsExpanded = true
        }

        binding.actionBarHide.setOnClickListener {
            state.actionsExpanded = false
        }
    }

    private fun setupWebView(savedInstanceState: Bundle?) {
        binding.webView.apply {
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
                            notifyUser(getString(R.string.keyboard_enable_webview_message))
                            lastTouchClick = 0
                            showActions(showActionsForTime = 3000)
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

            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
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
        binding.webView.handleDownload(url, userAgent, contentDisposition, mimetype, contentLength)
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onExternalStorageWritePermissionDenied() {
        toast(getString(R.string.storage_perm_reqd))
    }

    override fun finishActivity() {
        finish()
    }

    override fun startFileChooser(
        fileChooserParams: WebChromeClient.FileChooserParams,
        requestCode: Int
    ) {
        startActivityForResult(fileChooserParams.createIntent(), requestCode)
    }

    override fun startAudioCapture(request: PermissionRequest, requestCode: Int) {
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
            AudioRestartDialog.builder(this).show()
        }
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onAudioRecordPermissionDenied() {
        toast(getString(R.string.audio_perm_reqd))
    }

    override fun startVideoCapture(request: PermissionRequest, requestCode: Int) {
        if (PermissionUtils.hasSelfPermissions(this, Manifest.permission.CAMERA)) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            request.deny()
            getCameraPermissionWithPermissionCheck(true)
        }
    }

    override fun onBlobDownloadCompleted(uriString: String) {
        // startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun getCameraPermission(showRetryDialog: Boolean) {
        if (showRetryDialog) {
            CameraRestartDialog.builder(this).show()
        }
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraPermissionDenied() {
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
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (state.fullscreen) {
                state.fullscreen = true
            }
            applyState()
        }
    }

    private fun showActions(duration: Long = 300, showActionsForTime: Long = 0L) {
        binding.actionBar.root.slideVisibility(View.VISIBLE, Gravity.TOP, duration)
        binding.actionBarHide.slideVisibility(View.VISIBLE, Gravity.TOP, duration)
        binding.actionBarShow.slideVisibility(View.INVISIBLE, Gravity.TOP, duration)
        state.setActionsExpandedValue(true)

        hideActionsJob?.cancel()
        hideActionsJob = null
        
        if (showActionsForTime != 0L) {
            hideActionsJob = lifecycleScope.launch(Dispatchers.Main) {
                delay(showActionsForTime)
                hideActions(duration)
            }
        }
    }

    private fun hideActions(duration: Long = 300) {
        binding.actionBar.root.slideVisibility(View.INVISIBLE, Gravity.TOP, duration)
        binding.actionBarHide.slideVisibility(View.INVISIBLE, Gravity.TOP, duration)
        binding.actionBarShow.slideVisibility(View.VISIBLE, Gravity.TOP, duration)
        state.setActionsExpandedValue(false)
    }

    private fun toggleFullScreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else {
            window.decorView.systemUiVisibility = 0
        }
    }

    private fun toggleKeyboard(isKeyboard: Boolean) {
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
    }

    private fun notifyUser(message: String, showSnackbar: Boolean = false) {
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
