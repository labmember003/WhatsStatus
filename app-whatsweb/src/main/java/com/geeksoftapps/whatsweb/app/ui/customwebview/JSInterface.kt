package com.geeksoftapps.whatsweb.app.ui.customwebview

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.view.setPadding
import com.anggrayudi.storage.media.FileDescription
import com.anggrayudi.storage.media.MediaStoreCompat
import com.geeksoftapps.whatsweb.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.geeksoftapps.whatsweb.commons.logError
import com.geeksoftapps.whatsweb.commons.toast
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.showSafely
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JSInterface(val context: Context) {
    @JavascriptInterface
    @Throws(IOException::class)
    fun getBase64FromBlobData(base64Data: String?, mimetype: String, defaultFileName: String): String {
        return runBlocking(Dispatchers.Main) {
            try {
                val fileAsBytes = withContext(Dispatchers.IO) { Base64.decode(
                    base64Data?.replaceFirst(
                        Regex("^data:$mimetype;base64,"),
                        ""
                    ), 0
                )}
                val fileName: String = getFileNameFromDialog(defaultFileName)
                val fileUri = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveFileAfterAndroidQAndAbove(fileName, defaultFileName, fileAsBytes, mimetype)
                    } else {
                        saveFileTillAndroidP(fileName, defaultFileName, fileAsBytes, mimetype)
                    }
                }
                toast(context.getString(R.string.download_has_finished_message))
                try {
                    context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                } catch (e: Exception) {
                    // os cant handle
                }
                return@runBlocking fileUri.toString()
            } catch (e: Exception) {
                toast(context.getString(R.string.could_not_download_file))
                FirebaseCrashlytics.getInstance().recordException(
                    Exception("Exception maybe caused due to unknown mime type $mimetype, first 200 bytes are: ${base64Data?.substring(0, 200)}", e)
                )
                logError(e.stackTrace.toString())
                return@runBlocking ""
            }
        }
    }

    private fun saveFileTillAndroidP(
        fileName: String,
        defaultFileName: String,
        fileAsBytes: ByteArray,
        mimetype: String
    ): Uri {
        val downloadDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadedFile = File(
            downloadDirectory,
            getDownloadedFileName(fileName, defaultFileName)
        )

        downloadDirectory.mkdirs()

        val fos = FileOutputStream(downloadedFile, false)
        fos.write(fileAsBytes)
        fos.flush()
        fos.close()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.addCompletedDownload(
            downloadedFile.name,
            "File is downloaded using ${CommonUtils.getAppName(context)}",
            true,
            mimetype,
            downloadedFile.path,
            downloadedFile.length(),
            true
        )
        return downloadedFile.toUri()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileAfterAndroidQAndAbove(
        fileName: String,
        defaultFileName: String,
        fileAsBytes: ByteArray,
        mimetype: String
    ): Uri? {
        val mediaFile = MediaStoreCompat.createDownload(context, FileDescription(
            name = getDownloadedFileName(fileName, defaultFileName),
            mimeType = mimetype
        )) ?: return null
        val fos = mediaFile.openOutputStream(false)
        fos?.write(fileAsBytes)
        fos?.flush()
        fos?.close()

        return mediaFile.uri
    }

    private fun getDownloadedFileName(userFileName: String, defaultFileName: String): String {
        if (userFileName == defaultFileName) return userFileName

        fun String.extension() = substringAfterLast('.', "")

        return when {
            userFileName.extension().isNotBlank() -> userFileName
            defaultFileName.extension().isNotBlank() ->
                "$userFileName.${defaultFileName.extension()}"
            else -> userFileName
        }
    }

    private suspend fun getFileNameFromDialog(defaultFileName: String): String {
        return suspendCoroutine { cont ->
            val etFileName = EditText(context).apply {
                hint = context.getString(R.string.enter_file_name)
                setText(defaultFileName)
                setPadding(CommonUtils.dpToPixel(resources, 20.0))
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.downloading_file))
                .setMessage(context.getString(R.string.enter_file_name))
                .setView(etFileName)
                .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                    if (etFileName.text.toString().isBlank()) {
                        toast(context.getString(R.string.enter_valid_file_name))
                    } else {
                        dialog.dismiss()
                        cont.resume(etFileName.text.toString().replace(" ", "_").replace("/", "_"))
                    }
                }.setOnCancelListener {
                    cont.resume(defaultFileName)
                }.run {
                    if (context is Activity) {
                        showSafely(context as Activity)
                    } else {
                        show()
                    }
                }
        }
    }
}