package com.geeksoftapps.whatsweb.app.ui.dialogs

import android.content.Context
import com.geeksoftapps.whatsweb.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


object ChatHelpDialog {
    fun builder(context: Context): MaterialAlertDialogBuilder {

        return MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.help))
            .setMessage(context.getString(R.string.direct_message_help_ans))
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                dialog.dismiss()
                return@setPositiveButton
            }
    }
}