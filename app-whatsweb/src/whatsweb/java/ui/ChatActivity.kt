package ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.geeksoftapps.whatsweb.commons.*

import com.geeksoftapps.whatsweb.app.databinding.ActivityDirectChatBinding
import com.geeksoftapps.whatsweb.app.ui.ads.BannerAdLocation
import com.geeksoftapps.whatsweb.app.ui.ads.InterstitialAdLocation
import com.geeksoftapps.whatsweb.app.ui.dialogs.ChatHelpDialog
import com.geeksoftapps.whatsweb.app.utils.CommonUtils
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import ui.ads.getMaxBannerAdUnitId
import ui.ads.getMaxInterstitialAdUnitId
import java.net.URLEncoder

class ChatActivity : BasicActivity() {
    private val phone
        get() = "${binding.countryCodePicker.selectedCountryCode}${binding.etWhatsAppNumber.text}"
    private val message
        get() = binding.etWhatsAppMessage.text.toString()

    override val isAdsEnabled: Boolean
        get() = !WhatsWebPreferences.isFullVersionEnabled

    override val maxBannerAdUnitId = getMaxBannerAdUnitId(BannerAdLocation.ACTIVITY_DIRECT_CHAT)
    override val maxInterstitialAdUnitId = getMaxInterstitialAdUnitId(InterstitialAdLocation.ACTIVITY_DIRECT_CHAT)

    override fun getBannerAdView(): BannerAdViewGroups =
        BannerAdViewGroups(
            bannerAdViewGroup = binding.bannerInclude.bannerAdView,
            bannerContainer = binding.bannerInclude.bannerContainer
        )

    private lateinit var binding: ActivityDirectChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_direct_chat)
        binding.ivHelpIcon.setOnClickListener {
            ChatHelpDialog.builder(this).show()
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.etWhatsAppNumber.setupClearButtonWithAction(R.drawable.ic_clear)
        binding.etWhatsAppMessage.setupClearButtonWithAction(R.drawable.ic_clear)
        binding.btnSend.setOnClickListener {
            if (isPhoneEmpty()) {
                toast(getString(R.string.enter_a_phone_number))
            } else {
                analytics.log(eventName = "DMFrag_OnSendClick")
                startDirectMessageIntent(phone, message)
            }
        }
        binding.btnDirectLink.setOnClickListener {
            analytics.log(eventName = "DMFrag_OnDirectLinkClick")
            if (isPhoneEmpty()) {
                toast(getString(R.string.enter_a_phone_number))
            } else {
                CommonUtils.copyToClipboard(this, getDirectMessageLink(phone, message))
                toast(getString(R.string.direct_link_copied_to_clipboard))
            }
        }
    }
    private fun startDirectMessageIntent(phone: String, message: String) {
        val packageManager: PackageManager? = packageManager
        if (packageManager != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            val intentBusiness = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(message, "UTF-8")}"
            try {
                intent.setPackage("com.whatsapp")
                intent.data = Uri.parse(url)

                intentBusiness.setPackage("com.whatsapp.w4b")
                intentBusiness.data = Uri.parse(url)

                val i = Intent.createChooser(intent, "Open in...")
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intentBusiness))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.resolveActivity(packageManager)?.let {
                    startActivity(i)
                } ?: run {
                    toast(getString(R.string.whatsapp_not_installed))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                toast(getString(R.string.whatsapp_not_installed))
            }
        }
    }

    private fun getDirectMessageLink(phone: String, message: String = ""): String {
        val textAppend = if (message == "") ""
        else "?text=${URLEncoder.encode(message, "UTF-8")}"
        return "https://wa.me/$phone$textAppend"
    }
    private fun isPhoneEmpty() = binding.etWhatsAppNumber.text.isBlank()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
