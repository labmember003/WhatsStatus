package com.geeksoftapps.whatsweb.app.ui.bottomsheet

import android.app.Activity
import android.graphics.Outline
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.clipTopView
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.getCleanerId
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.getQrCodeScannerId
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.getRecoverMessagesId
import com.geeksoftapps.whatsweb.app.utils.CommonUtils.openThirdPartyAppPlayStore
import com.geeksoftapps.whatsweb.app.utils.log
import com.geeksoftapps.whatsweb.app.utils.showSafely
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics

class MoreAppsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var item_qr_code: ConstraintLayout
    private lateinit var item_cleaner: ConstraintLayout
    private lateinit var item_recover_messages: ConstraintLayout
    private lateinit var cross_button_container: FrameLayout

    companion object {
        const val TAG = "MoreAppsBottomSheet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val view = View.inflate(
            contextThemeWrapper,
            R.layout.more_apps_bottomsheet,
            container
        )
        getDialog()?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        clipTopView(view?.findViewById<ConstraintLayout>(R.id.container), resources.getDimension(R.dimen.margin_padding_xsmall))

        dialog?.setOnShowListener { dialog ->
            (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        item_qr_code = view.findViewById(R.id.item_qr_code)
        item_cleaner = view.findViewById(R.id.item_cleaner)
        item_recover_messages = view.findViewById(R.id.item_recover_messages)
        cross_button_container = view.findViewById(R.id.cross_button_container)
        setData()
    }

    private fun setData() {
        //1st item
        context?.let {
            item_qr_code.findViewById<ImageView>(R.id.iv).setImageDrawable(ContextCompat.getDrawable(it, R.drawable.ic_whatsweb))
        }
        item_qr_code.findViewById<TextView>(R.id.tv).setText(getString(R.string.qr_scanner))
        item_qr_code.setOnClickListener {
            context?.let {
                FirebaseAnalytics.getInstance(it).log("qr_code_item_tapped",var2 = "more_apps_bottomsheet")
            }
            activity?.openThirdPartyAppPlayStore(
                getQrCodeScannerId(),
                getString(R.string.download_qrcodescanner),
                getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app)
            )
        }

        //2nd item
        context?.let {
            item_cleaner.findViewById<ImageView>(R.id.iv).setImageDrawable(ContextCompat.getDrawable(it, R.drawable.ic_cleaner))
        }
        item_cleaner.findViewById<TextView>(R.id.tv).setText(getString(R.string.cleaner))
        item_cleaner.setOnClickListener {
            context?.let {
                FirebaseAnalytics.getInstance(it).log("cleaner_item_tapped",var2 = "more_apps_bottomsheet")
            }
            activity?.openThirdPartyAppPlayStore(
                getCleanerId(),
                getString(R.string.download_cleaner),
                getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app)
            )
        }

        //3rd item
        context?.let {
            item_recover_messages.findViewById<ImageView>(R.id.iv).setImageDrawable(ContextCompat.getDrawable(it, R.drawable.ic_whats_deleted))
        }
        item_recover_messages.findViewById<TextView>(R.id.tv).setText(getString(R.string.recover_messages))

        item_recover_messages.setOnClickListener {
            context?.let {
                FirebaseAnalytics.getInstance(it).log("recover_messages_item_tapped",var2 = "more_apps_bottomsheet")
            }
            activity?.openThirdPartyAppPlayStore(
                getRecoverMessagesId(),
                getString(R.string.download_recover_messages_app),
                getString(R.string.to_use_this_feature_you_need_to_download_another_app_from_play_store_click_ok_to_open_play_store_and_download_app)
            )
        }

        cross_button_container.setOnClickListener {
            dismiss()
        }
    }

    fun Activity.openThirdPartyAppPlayStore(packageName: String, title: String, message: String) {
        val isAppInstalled =
            CommonUtils.isPackageInstalled(packageName, packageManager)
        if (isAppInstalled) {
            context?.let {
                FirebaseAnalytics.getInstance(it).log("third_party_app_installed", var1 = packageName,var2 = "more_apps_bottomsheet")
            }
            CommonUtils.startNewActivity(this, packageName)
        } else {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { dialog, which ->
                    context?.let {
                        FirebaseAnalytics.getInstance(it).log("third_party_app_dialog_ok_tapped", var1 = packageName,var2 = "more_apps_bottomsheet")
                    }
                    CommonUtils.startNewActivity(this, packageName)
                }.setNegativeButton(R.string.cancel) { dialog, which ->
                    context?.let {
                        FirebaseAnalytics.getInstance(it).log("third_party_app_dialog_cancel_tapped", var1 = packageName,var2 = "more_apps_bottomsheet")
                    }
                    dialog.dismiss()
                }
                .showSafely(this)
        }
    }
}