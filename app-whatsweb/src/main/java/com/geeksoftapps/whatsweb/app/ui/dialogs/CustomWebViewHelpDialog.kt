package com.geeksoftapps.whatsweb.app.ui.dialogs

import android.content.Context
import com.geeksoftapps.whatsweb.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


object CustomWebViewHelpDialog {
    fun builder(context: Context): MaterialAlertDialogBuilder {

        return MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.how_it_works))
            .setMessage(context.getString(R.string.whats_web_how_it_works))
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                dialog.dismiss()
                return@setPositiveButton
            }
    }
}