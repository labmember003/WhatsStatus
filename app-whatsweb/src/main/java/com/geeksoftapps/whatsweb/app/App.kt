package com.geeksoftapps.whatsweb.app

import android.app.Application
import android.content.res.Resources
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.commons.FirebaseRemoteConfigHelper
import com.geeksoftapps.whatsweb.status.IStatusRepo
import com.geeksoftapps.whatsweb.status.StatusRepo
import com.geeksoftapps.whatsweb.status.whatsapp_saved_status_file
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import com.preference.PowerPreference
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import java.io.File


class App: Application(), KodeinAware {
    companion object {
        @JvmStatic
        private lateinit var mInstance: App

        @JvmStatic
        private lateinit var res: Resources

        @JvmStatic
        fun getInstance(): App {
            return mInstance
        }

        @JvmStatic
        fun getAppResources(): Resources {
            return res
        }

        @JvmStatic
        private var internalStorageDir: File? = null

        @JvmStatic
        fun getInternalStorageDir(): File? {
            return internalStorageDir
        }

    }

    val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override val kodein = Kodein.lazy {
        import(androidXModule(this@App))

        bind<IStatusRepo>() with factory { documentFile: DocumentFile ->
            StatusRepo(
                instance(),
                documentFile,
                whatsapp_saved_status_file
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        res = resources
        internalStorageDir = filesDir
        PowerPreference.init(this)
        AndroidThreeTen.init(this)
        setUpDarkMode()
        initFirebaseRemoteConfig()
    }

    private fun initFirebaseRemoteConfig() {
        GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            FirebaseRemoteConfigHelper.init()
            FirebaseRemoteConfigHelper.fetch()
            FirebaseRemoteConfigHelper.addOnConfigUpdateListener();
        }
    }

    private fun setUpDarkMode() {
        when (WhatsWebPreferences.darkMode) {
            WhatsWebPreferences.DARK_MODE_ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            WhatsWebPreferences.DARK_MODE_OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}