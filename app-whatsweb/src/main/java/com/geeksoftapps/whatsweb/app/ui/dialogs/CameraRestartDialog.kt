package com.geeksoftapps.whatsweb.app.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import com.geeksoftapps.whatsweb.app.R


object CameraRestartDialog {
    fun builder(context: Context): AlertDialog.Builder {

        return AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.retry_camera_capture))
            .setMessage(context.getString(R.string.message_retry_camera_capture))
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                dialog.dismiss()
                return@setPositiveButton
            }
    }
}