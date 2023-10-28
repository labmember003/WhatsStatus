package com.geeksoftapps.whatsweb.app.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.geeksoftapps.whatsweb.app.R


object StartAppUpdateDialog {

    fun get(context: Context, positiveListener: (dialog: DialogInterface, which: Int) -> Unit): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.app_update_alert_dialog_title))
            .setMessage(context.getString(R.string.app_update_alert_dialog_subtitle))
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.ok)) {dialog, which ->
                positiveListener(dialog, which)
            }
    }
}