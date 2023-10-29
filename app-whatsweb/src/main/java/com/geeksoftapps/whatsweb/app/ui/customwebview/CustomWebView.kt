package com.geeksoftapps.whatsweb.app.ui.customwebview

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.webkit.*
import com.geeksoftapps.whatsweb.app.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.geeksoftapps.whatsweb.commons.logError
import com.geeksoftapps.whatsweb.commons.toast
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import com.geeksoftapps.whatsweb.app.utils.Constants
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CustomWebView: WebView {

    companion object {
        // Fix for issues on Android 6/7
//        const val initialUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3112.113 Safari/537.36"
//        const val initialUserAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:46.0) Gecko/20100101 Firefox/61.0"
//        const val initialUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"
        val WHATSAPP_WEB_URL
            get() = "https://web.whatsapp.com/\uD83C\uDF10/${Locale.getDefault().language}"
        private const val REQUEST_CODE_FILE_UPLOAD = 4321
        private const val REQUEST_CODE_AUDIO_CAPTURE = 4322
        private const val REQUEST_CODE_VIDEO_CAPTURE = 4325
    }

    val userAgent = Firebase.remoteConfig.getString(Constants.USER_AGENT).let {
        it.ifBlank { Constants.defaultUserAgent }
    }

    constructor(context: Context) : super(context) {
        mContext = context
        initiate()
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mContext = context
        initiate()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mContext = context
        initiate()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initiate()
    }

    private var whatsWebChromeClient: CustomWebView.WhatsWebChromeClient? = null
    private lateinit var mContext: Context
    private var onWhatsWebViewActionListener: OnWhatsWebViewActionListener? = null
    private var darkModeEnabled: Boolean = false

    fun initiate() {
        try {
            whatsWebChromeClient = WhatsWebChromeClient()
            webChromeClient = whatsWebChromeClient

            webViewClient = NavWebViewClient()

            settings.apply {
                //This the the enabling of the zoom controls
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                databaseEnabled = true
                domStorageEnabled = true
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = userAgent
            }

            scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = true

            addJavascriptInterface(JSInterface(mContext), "Android")
            addJavascriptInterface(this, "WebView")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val serviceWorkerController = ServiceWorkerController.getInstance()
                    serviceWorkerController.setServiceWorkerClient(object : ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
                            if (request != null && request.url.toString()
                                    .startsWith("https://web.whatsapp.com")
                            ) {
                                val okHttpClient = OkHttpClient()
                                val call = okHttpClient.newCall(
                                    Request.Builder()
                                        .url(request.url.toString())
                                        .method(request.method, null)
                                        .headers(request.requestHeaders.filter {
                                            return@filter !it.key.equals("User-Agent", true)
                                        }.toHeaders())
                                        .addHeader(
                                            "User-Agent",
                                            userAgent
                                        )
                                        .build()
                                )
                                return try {
                                    val response = call.execute()
                                    val responseHeaders = response.headers
                                    WebResourceResponse(
                                        responseHeaders["content-type"],
                                        responseHeaders["content-encoding"],
                                        response.body?.byteStream()
                                    )
                                } catch (e: IOException) {
                                    logError(e.stackTrace)
                                    null
                                }
                            } else {
                                return super.shouldInterceptRequest(request)
                            }
                        }
                    })
                } catch (e: AbstractMethodError) {

                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                handleDownload(url, userAgent, contentDisposition, mimetype, contentLength)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            if (e.message?.contains("webview") == true) {
                toast(mContext.getString(R.string.try_again))
                onWhatsWebViewActionListener?.finishActivity()
            }
        }
    }

    @JavascriptInterface
    fun onBlobDownloadCompleted(uriString: String) {
        onWhatsWebViewActionListener?.onBlobDownloadCompleted(uriString)
    }

    fun handleDownload(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
        val downloadName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        try {
            if(url?.startsWith("blob") == true) {
                evaluateJavascript(
                    """
                    try {
                        var xhr = new XMLHttpRequest();
                        xhr.open('GET', '$url', true);
                        xhr.setRequestHeader('Content-type','$mimetype');
                        xhr.responseType = 'blob';
                        xhr.onload = function(e) {
                            if (this.status == 200) {
                                var blobData = this.response;
                                var reader = new FileReader();
                                reader.readAsDataURL(blobData);
                                reader.onloadend = function() {
                                    base64data = reader.result;
                                    var uriString = Android.${JSInterface::getBase64FromBlobData.name}(base64data, '$mimetype', '$downloadName');
                                    WebView.${this::onBlobDownloadCompleted.name}(uriString)
                                }
                            }
                        };
                        xhr.send();
                    } catch(err) {
                        
                    }
                    """, null
                )
            } else {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimetype)
                    addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading file...")
                    setTitle(downloadName)
                    allowScanningByMediaScanner()
                    //Notify client once download is completed!
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadName)
                }

                val dm = mContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
            }

            toast(mContext.getString(R.string.download_file))
        } catch (e: Exception) {
            toast(mContext.getString(R.string.couldnt_download_file))
            FirebaseCrashlytics.getInstance().recordException(e)
            logError(e.stackTrace.toString())
        }
    }

    fun loadWebWhatsapp() {
        loadUrl(WHATSAPP_WEB_URL, mapOf("user-agent" to userAgent))
    }

    fun reset() {
        clearHistory()
        clearCache(true)
        evaluateJavascript("localStorage.clear()", null)
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies {
            loadWebWhatsapp()
        }
        CookieManager.getInstance().flush()
    }

    fun toggleDarkMode(enableDarkMode: Boolean) {
        evaluateJavascript(javascriptDarkMode) {
            val value = enableDarkMode.toString()
            evaluateJavascript("toggleDarkMode($value);") {
                this.darkModeEnabled = enableDarkMode
            }
        }
    }

    suspend fun isDarkModeEnabled(): Boolean = suspendCoroutine { cont ->
        evaluateJavascript(javascriptDarkMode) {
            evaluateJavascript("isDarkModeEnabled();") {
                cont.resume(it == "true")
            }
        }
    }

    fun setOnWhatsWebViewActionListener(listener: OnWhatsWebViewActionListener) {
        onWhatsWebViewActionListener = listener
    }

    /**
     * @return If activity result was handled
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return when(requestCode) {
            REQUEST_CODE_FILE_UPLOAD -> {
                return whatsWebChromeClient?.let {
                    it.receiveFileUploadResult(resultCode, data)
                    return@let true
                } ?: false
            }
            else -> false
        }
    }

    private inner class NavWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(webView: WebView, str: String): Boolean {
            return when {
                Build.VERSION.SDK_INT >= 24 -> {
                    super.shouldOverrideUrlLoading(webView, str)
                }
                str.contains("web.whatsapp.com") -> {
                    false
                }
                str.contains("www.whatsapp.com") -> {
                    loadWebWhatsapp()
                    true
                }
                else -> {
                    handleViewUri(Uri.parse(str))
                }
            }
        }

        override fun shouldOverrideUrlLoading(webView: WebView, webResourceRequest: WebResourceRequest): Boolean {
            when {
                Build.VERSION.SDK_INT < 24 -> {
                    return super.shouldOverrideUrlLoading(webView, webResourceRequest)
                }
                webResourceRequest.url.toString().contains("web.whatsapp.com/serviceworker.js") -> {
                    return true
                }
                webResourceRequest.url.toString().contains("web.whatsapp.com") -> {
                    return false
                }
                webResourceRequest.url.toString().contains("www.whatsapp.com") -> {
                    loadWebWhatsapp()
                    return true
                }
                else -> {
                    return handleViewUri(webResourceRequest.url)
                }
            }
        }

        private fun handleViewUri(uri: Uri): Boolean {
            if (uri.host?.contains(".whatsapp.com") == true) {
                return false
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mContext.packageManager?.let {
                if (intent.resolveActivity(it) != null) {
                    mContext.startActivity(intent)
                    return true
                } else {
                    return false
                }
            }
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            scrollTo(0, 0)
            if (url?.contains("web.whatsapp.com") == true) {
                view?.evaluateJavascript(javascriptDarkMode) {
                    if (darkModeEnabled) {
                        view.evaluateJavascript("enableDarkMode();", null)
                    } else {
                        view.evaluateJavascript("disableDarkMode();", null)
                    }
                }
            }
        }
    }

    private inner class WhatsWebChromeClient: WebChromeClient() {

        var mUploadMessage: ValueCallback<Array<Uri?>>? = null

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            return if (filePathCallback != null && fileChooserParams != null) {
                if (onWhatsWebViewActionListener != null) {
                    mUploadMessage = filePathCallback
                    onWhatsWebViewActionListener?.startFileChooser(fileChooserParams, REQUEST_CODE_FILE_UPLOAD)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        fun receiveFileUploadResult(resultCode: Int, data: Intent?) {
            mUploadMessage?.let { mUploadMessage ->
                if (resultCode == Activity.RESULT_CANCELED || data?.data == null) {
                    mUploadMessage.onReceiveValue(null)
                } else {
                    mUploadMessage.onReceiveValue(arrayOf(data.data))
                }
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.resources?.forEach { permission ->
                when (permission) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        if (onWhatsWebViewActionListener != null) {
                            onWhatsWebViewActionListener?.startAudioCapture(request, REQUEST_CODE_AUDIO_CAPTURE)
                        }
                    }
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        if (onWhatsWebViewActionListener != null) {
                            onWhatsWebViewActionListener?.startVideoCapture(request, REQUEST_CODE_VIDEO_CAPTURE)
                        }
                    }
                }
            }
        }
    }

    interface OnWhatsWebViewActionListener {
        fun finishActivity()
        fun startFileChooser(fileChooserParams: WebChromeClient.FileChooserParams, requestCode: Int)
        fun startAudioCapture(request: PermissionRequest, requestCode: Int)
        fun startVideoCapture(request: PermissionRequest, requestCode: Int)
        fun onBlobDownloadCompleted(uriString: String)
    }

}

private val javascriptDarkMode =
    """
function toggleDarkMode(enable) {
    try {
        if (enable == undefined) {
            if(!isDarkModeEnabled()) {
                disableDarkMode()
            } else {
                disableDarkMode()
            }      
        } else if (enable == true) {
            enableDarkMode()
        } else {
            disableDarkMode()
        }
        return isDarkModeEnabled()
    } catch(err) {
        return false
    }
}
function isDarkModeEnabled() {
    try {
        var classes = document.body.getAttribute('class').split(' ');
        return classes.indexOf('dark') > -1;
    } catch(err) {
        return false
    }
}
function enableDarkMode() {
    try {
        document.body.setAttribute(
            'class', 
            document.body.getAttribute('class')
                .split(' ')
                .concat('dark')
                .join(' ')
        )
    } catch(err) {
        
    }
}
function disableDarkMode() {
    try {
        document.body.setAttribute(
            'class', 
            document.body.getAttribute('class')
                .split(' ')
                .filter(function(c) {
                    return c !== "dark"
                })
                .join(' ')
        )
    } catch(err) {
        
    }
}
""".trimIndent()