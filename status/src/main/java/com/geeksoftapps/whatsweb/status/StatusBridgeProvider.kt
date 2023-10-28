
package com.geeksoftapps.whatsweb.status

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import java.io.File

class StatusBridgeProvider: ContentProvider() {

    companion object {
        @JvmStatic
        private var mAppContext: Context? = null

        @JvmStatic
        private var internalStorageDir: File? = null

        @JvmStatic
        fun getAppContext(): Context? {
            return mAppContext
        }

        @JvmStatic
        fun getInternalStorageDir(): File? {
            return internalStorageDir
        }
    }

    override fun onCreate(): Boolean {
        mAppContext = context?.applicationContext
        internalStorageDir = mAppContext?.filesDir
        return true
    }

    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        if (info == null) {
            throw NullPointerException("StatusBridgeProvider ProviderInfo cannot be null.")
        }
        if ("com.geeksoftapps.whatsweb.status_saver.statussaverinitprovider" == info.authority) {
            throw IllegalStateException("Incorrect provider authority in manifest. Most likely due to a "
                    + "missing applicationId variable in application\'s build.gradle.")
        }
        super.attachInfo(context, info)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = null
}