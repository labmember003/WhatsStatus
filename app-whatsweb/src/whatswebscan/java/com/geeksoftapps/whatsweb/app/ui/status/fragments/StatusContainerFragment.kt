package com.geeksoftapps.whatsweb.app.ui.status.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.geeksoftapps.whatsweb.commons.BasicFragment
import com.geeksoftapps.whatsweb.commons.log
import com.geeksoftapps.whatsweb.commons.toast
import com.geeksoftapps.whatsweb.app.App
import com.geeksoftapps.whatsweb.app.R

import com.geeksoftapps.whatsweb.app.databinding.FragmentStatusSaverBinding
import com.geeksoftapps.whatsweb.app.ui.status.adapters.StatusViewPagerAdapter
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.status.business_scoped_storage_uri
import com.geeksoftapps.whatsweb.status.status_scoped_storage_uri
import com.geeksoftapps.whatsweb.status.whatsapp_business_storage_file
import com.geeksoftapps.whatsweb.status.whatsapp_storage_file
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class StatusContainerFragment : BasicFragment(), KodeinAware {
    override val kodein by closestKodein()

    private val REQUEST_CODE_SAF = 12123
    private val REQUEST_CODE_SAF_BUSINESS = 12124

    private lateinit var binding: FragmentStatusSaverBinding

    // AdMob banner
    private var adView: AdView? = null

    // Tracks which app is currently selected in the toggle
    private var isBusinessSelected = false

    companion object {
        val TAG: String = StatusContainerFragment::class.java.simpleName
        const val WHATSAPP_STORAGE_URI = "WHATSAPP_STORAGE_URI"
        const val IS_BUSINESS = "IS_BUSINESS"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_status_saver, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToggle()
        loadBannerAd()
        acquirePermissions()
    }

    override fun onResume() {
        super.onResume()
        (activity as? StatusSaverFragmentActions)?.setToolBarTitle(getString(R.string.app_name))
    }

    // ─── Banner Ad ─────────────────────────────────────────────────────────────

    private fun loadBannerAd() {
        adView = AdView(requireContext()).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = getString(R.string.admob_banner_ad_unit_id)
        }
        binding.bannerContainer.removeAllViews()
        binding.bannerContainer.addView(adView)
        adView?.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroyView() {
        adView?.destroy()
        adView = null
        super.onDestroyView()
    }

    // ─── Toggle  ──────────────────────────────────────────────────────────────

    private fun setupToggle() {
        selectWhatsApp()
        binding.btnToggleWhatsApp.setOnClickListener {
            if (isBusinessSelected) {
                isBusinessSelected = false
                selectWhatsApp()
                acquirePermissions()
            }
        }
        binding.btnToggleBusiness.setOnClickListener {
            if (!isBusinessSelected) {
                isBusinessSelected = true
                selectBusiness()
                acquireBusinessPermissions()
            }
        }
    }

    private fun selectWhatsApp() {
        binding.btnToggleWhatsApp.setBackgroundResource(R.drawable.bg_toggle_selected)
        binding.btnToggleWhatsApp.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        binding.btnToggleBusiness.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.btnToggleBusiness.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.toggle_unselected_text)
        )
    }

    private fun selectBusiness() {
        binding.btnToggleBusiness.setBackgroundResource(R.drawable.bg_toggle_selected)
        binding.btnToggleBusiness.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        binding.btnToggleWhatsApp.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.btnToggleWhatsApp.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.toggle_unselected_text)
        )
    }

    // ─── ViewPager ────────────────────────────────────────────────────────────

    private fun setUpViewPager(whatsAppStorageUri: Uri, isBusiness: Boolean = false) {
        val adapter = StatusViewPagerAdapter(childFragmentManager)

        adapter.addFragment(
            StatusFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(WHATSAPP_STORAGE_URI, whatsAppStorageUri)
                    putBoolean(IS_BUSINESS, isBusiness)
                }
            }, getString(R.string.available_statuses)
        )

        adapter.addFragment(
            SavedStatusFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(WHATSAPP_STORAGE_URI, whatsAppStorageUri)
                    putBoolean(IS_BUSINESS, isBusiness)
                }
            }, getString(R.string.saved_statuses)
        )

        binding.viewPager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewPager)
        binding.tabs.getTabAt(0)?.setText(R.string.status)
        binding.tabs.getTabAt(1)?.setText(R.string.saved_statuses)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    fun onExternalStorageWritePermissionDenied() {
        binding.btnGrantPermission.visibility = View.VISIBLE
        binding.btnGrantPermission.setOnClickListener { }
    }

    fun onPermissionNeverAskAgain() {
        binding.btnGrantPermission.visibility = View.VISIBLE
        binding.btnGrantPermission.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", App.getInstance().packageName, null)
                }
            )
            toast(getString(R.string.please_grant_storage_permissions))
            it.setOnClickListener { }
        }
    }

    // ─── Activity Result ──────────────────────────────────────────────────────

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SAF -> handleSafResult(resultCode, data, isBusiness = false)
            REQUEST_CODE_SAF_BUSINESS -> handleSafResult(resultCode, data, isBusiness = true)
        }
    }

    private fun handleSafResult(resultCode: Int, data: Intent?, isBusiness: Boolean) {
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val treeUri: Uri? = data.data
            if (treeUri != null) {
                val uriString = Uri.decode(treeUri.toString())
                val expectedSuffix = if (isBusiness) "WhatsApp Business" else "WhatsApp"
                if (!uriString.endsWith(expectedSuffix, ignoreCase = true)) {
                    toast(
                        if (isBusiness) getString(R.string.wa_business_directory_invalid)
                        else getString(R.string.whatsapp_directory_does_not_contain_statuses)
                    )
                    if (isBusiness) acquireBusinessPermissions() else acquirePermissions()
                    return
                }
                val documentFile = DocumentFile.fromTreeUri(requireContext(), treeUri)
                    ?.findFile("Media")
                    ?.findFile(".Statuses")
                if (documentFile == null) {
                    toast(getString(R.string.whatsapp_directory_does_not_contain_statuses))
                    if (isBusiness) acquireBusinessPermissions() else acquirePermissions()
                    return
                }
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                activity?.contentResolver?.takePersistableUriPermission(treeUri, takeFlags)

                if (isBusiness) {
                    WhatsWebPreferences.whatsAppBusinessStorageUri = treeUri.toString()
                } else {
                    WhatsWebPreferences.whatsAppStorageUri = treeUri.toString()
                }
                setUpViewPager(treeUri, isBusiness)
            }
        } else {
            toast(getString(R.string.try_again))
            if (!isBusiness) activity?.finish()
        }
    }

    // ─── Permissions — WhatsApp ───────────────────────────────────────────────

    private fun acquirePermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context ?: return,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        setUpViewPager(DocumentFile.fromFile(whatsapp_storage_file).uri)
                    } else {
                        activity?.finish()
                    }
                }.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                setUpViewPager(DocumentFile.fromFile(whatsapp_storage_file).uri)
            }
        } else {
            val uriString = WhatsWebPreferences.whatsAppStorageUri
            when {
                uriString == "" -> {
                    log("uri not stored")
                    openDocumentTree(isBusiness = false)
                }
                arePermissionsGranted(uriString) -> {
                    setUpViewPager(Uri.parse(uriString))
                }
                else -> {
                    log("uri permission not stored")
                    openDocumentTree(isBusiness = false)
                }
            }
        }
    }

    // ─── Permissions — WhatsApp Business ──────────────────────────────────────

    private fun acquireBusinessPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context ?: return,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        if (whatsapp_business_storage_file.exists()) {
                            setUpViewPager(DocumentFile.fromFile(whatsapp_business_storage_file).uri, isBusiness = true)
                        } else {
                            toast(getString(R.string.wa_business_not_installed))
                            isBusinessSelected = false
                            selectWhatsApp()
                        }
                    }
                }.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                if (whatsapp_business_storage_file.exists()) {
                    setUpViewPager(DocumentFile.fromFile(whatsapp_business_storage_file).uri, isBusiness = true)
                } else {
                    toast(getString(R.string.wa_business_not_installed))
                    isBusinessSelected = false
                    selectWhatsApp()
                }
            }
        } else {
            val uriString = WhatsWebPreferences.whatsAppBusinessStorageUri
            when {
                uriString == "" -> {
                    log("business uri not stored")
                    openDocumentTree(isBusiness = true)
                }
                arePermissionsGranted(uriString) -> {
                    setUpViewPager(Uri.parse(uriString), isBusiness = true)
                }
                else -> {
                    log("business uri permission not stored")
                    openDocumentTree(isBusiness = true)
                }
            }
        }
    }

    // ─── SAF Picker ───────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openDocumentTree(isBusiness: Boolean) {
        // Show the visual guide dialog first, then launch the SAF picker
        DirectoryGuideDialogFragment.newInstance(isBusiness) {
            launchDocumentTreePicker(isBusiness)
        }.show(childFragmentManager, "directory_guide")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun launchDocumentTreePicker(isBusiness: Boolean) {
        val initialUri = if (isBusiness) business_scoped_storage_uri else status_scoped_storage_uri
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(initialUri))
        }
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        try {
            val requestCode = if (isBusiness) REQUEST_CODE_SAF_BUSINESS else REQUEST_CODE_SAF
            startActivityForResult(intent, requestCode)
        } catch (anfe: ActivityNotFoundException) {
            toast("Sorry, we are not able to find any file manager in your phone. Please install a file manager or contact us.")
            if (!isBusiness) activity?.finish()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun arePermissionsGranted(uriString: String): Boolean {
        val list = activity?.contentResolver?.persistedUriPermissions ?: return false
        for (i in list.indices) {
            val persistedUriString = list[i].uri.toString()
            if (persistedUriString == uriString && list[i].isWritePermission && list[i].isReadPermission) {
                return true
            }
        }
        return false
    }

    interface StatusSaverFragmentActions {
        fun setToolBarTitle(title: String)
        fun onHomePress()
    }
}
