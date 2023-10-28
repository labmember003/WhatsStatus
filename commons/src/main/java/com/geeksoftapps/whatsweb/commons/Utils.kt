package com.geeksoftapps.whatsweb.commons

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.net.URI

object Utils {

    fun getViewIntent(context: Context, documentFile: DocumentFile, authority: String): Intent {
        val uri = if (documentFile.uri.scheme?.contains("file", true) == true) {
            FileProvider.getUriForFile(context, authority, File(URI(documentFile.uri.toString())))
        } else {
            documentFile.uri
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, documentFile.type)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return intent
    }
}