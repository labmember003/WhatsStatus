package com.geeksoftapps.whatsweb.app.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Outline
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AlertDialog
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.utils.Constants.CLEANER_APP_ID
import com.geeksoftapps.whatsweb.app.utils.Constants.DEVELOPER_EMAIL
import com.geeksoftapps.whatsweb.app.utils.Constants.FEEDBACK_EMAIL
import com.geeksoftapps.whatsweb.app.utils.Constants.QR_CODE_APP_ID
import com.geeksoftapps.whatsweb.app.utils.Constants.RECOVER_MESSAGES_APP_ID
import com.geeksoftapps.whatsweb.commons.toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.net.URLEncoder


object CommonUtils {

    fun getQrCodeScannerId() = FirebaseRemoteConfig.getInstance().getString(QR_CODE_APP_ID)

    fun getCleanerId() = FirebaseRemoteConfig.getInstance().getString(CLEANER_APP_ID)

    fun getRecoverMessagesId() = FirebaseRemoteConfig.getInstance().getString(RECOVER_MESSAGES_APP_ID)

    fun copyToClipboard(context: Context?, text: String) {
        context?.run {
            val myClip = ClipData.newPlainText("text", text)
            val myClipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            myClipboard.setPrimaryClip(myClip)
        }
    }

    fun getAppName(context: Context): String {
        return context.getString(R.string.app_name)
    }

    fun getAppVersion(context: Context): String {
        val manager = context.packageManager
        val info = manager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        return info.versionName
    }

    fun openBrowser(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW)
        browserIntent.data = Uri.parse(url)
        try {
            context.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            toast("Error: Browser app not found")
        }
    }

    fun reportBug(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.preference_title_bug_report) + " for " + getAppName(
                context
            )
        )
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.describe_bug) + ": \n\n\n\n\n" + (getAppUserAgent(context) ?: ""))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Error: Email app not found")
        }
    }

    fun getAppUserAgent(context: Context): String? {
        return try {
            ("App Version : " + getAppVersion(context)
                    + "\nDevice Manufacturer : " + URLEncoder.encode(Build.MANUFACTURER, "UTF-8")
                    + "\nDevice Brand : " + URLEncoder.encode(Build.BRAND, "UTF-8")
                    + "\nDevice Model : " + Build.MODEL)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(
                Exception("Could not urlencode device params for useragent", e)
            )
            "App Version : " + Build.VERSION.RELEASE
        }
    }

    fun sendFeedback(context: Context, feedback: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            "${context.getString(R.string.regarding_app)} ${getAppName(context)}"
        )
        intent.putExtra(Intent.EXTRA_TEXT, feedback)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Error: Email app not found")
        }
    }

    fun share(context: Context, shareContent: String): Intent = Intent(Intent.ACTION_SEND)
        .apply {
            putExtra(
                Intent.EXTRA_TEXT,
                """
                    $shareContent
                    
                    ${getAppName(context)}: http://play.google.com/store/apps/details?id=${context.packageName}
                """.trimIndent()
            )
            type = "text/plain"
        }

    fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun startNewActivity(context: Context, packageName: String) {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, "..."))
    }

    fun isDarkMode(context: Context): Boolean {
        return when(WhatsWebPreferences.darkMode) {
            WhatsWebPreferences.DARK_MODE_OFF -> false
            WhatsWebPreferences.DARK_MODE_ON -> true
            WhatsWebPreferences.DARK_MODE_SYSTEM_DEFAULT -> {
                when (context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO -> false
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> false
                    else -> false
                }
            }
            else -> false
        }
    }

    fun dpToPixel(resources: Resources, sizeInDp: Double): Int {
        val scale = resources.displayMetrics.density
        return (sizeInDp * scale + 0.5f).toInt()
    }

    fun clipTopView(view: View?, radius: Float) {
        view ?: return
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0, view.width,
                    (view.height + radius).toInt(), radius
                )
            }
        }
        view.clipToOutline = true
    }

    fun Activity.openThirdPartyAppPlayStore(packageName: String, title: String, message: String) {
        val isAppInstalled =
            CommonUtils.isPackageInstalled(packageName, packageManager)
        if (isAppInstalled) {
            startNewActivity(this, packageName)
        } else {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { dialog, which ->
                    startNewActivity(this, packageName)
                }.setNegativeButton(R.string.cancel) { dialog, which ->
                    dialog.dismiss()
                }
                .showSafely(this)
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}