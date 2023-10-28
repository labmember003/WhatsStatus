package ui.status.fragments

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

import com.geeksoftapps.whatsweb.app.databinding.FragmentStatusSaverBinding
import com.geeksoftapps.whatsweb.app.ui.status.adapters.StatusViewPagerAdapter
import com.geeksoftapps.whatsweb.app.utils.WhatsWebPreferences
import com.geeksoftapps.whatsweb.status.status_scoped_storage_uri
import com.geeksoftapps.whatsweb.status.whatsapp_storage_file
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class StatusContainerFragment : BasicFragment(), KodeinAware {
    override val kodein by closestKodein()

    private val REQUEST_CODE_SAF = 12123

    private lateinit var binding: FragmentStatusSaverBinding

    companion object {
        val TAG: String = StatusContainerFragment::class.java.simpleName
        const val WHATSAPP_STORAGE_URI = "WHATSAPP_STORAGE_URI"
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
        acquirePermissions()
    }

    override fun onResume() {
        super.onResume()
        (activity as? StatusSaverFragmentActions)?.setToolBarTitle(getString(R.string.app_name))
    }

    private fun setUpViewPager(whatsAppStorageUri: Uri) {
        val adapter =
            StatusViewPagerAdapter(
                childFragmentManager
            )

        adapter.addFragment(
            StatusFragment().apply {
                arguments = Bundle().apply { putParcelable(WHATSAPP_STORAGE_URI, whatsAppStorageUri) }
            }, getString(R.string.available_statuses)
        )

        adapter.addFragment(
            SavedStatusFragment().apply {
                arguments = Bundle().apply { putParcelable(WHATSAPP_STORAGE_URI, whatsAppStorageUri) }
            }, getString(R.string.saved_statuses)
        )

        binding.viewPager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewPager)
        binding.tabs.getTabAt(0)?.setText(R.string.available_statuses)
        binding.tabs.getTabAt(1)?.setText(R.string.saved_statuses)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.ivBack.setOnClickListener {
            (activity as? StatusSaverFragmentActions)?.onHomePress() ?: run {
                activity?.finish()
            }
        }
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
            it.setOnClickListener {  }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SAF) {
            if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                //this is the uri user has provided us
                val treeUri: Uri? = data.data
                if (treeUri != null) {
                    val uriString = Uri.decode(treeUri.toString())
                    if (!uriString.endsWith("WhatsApp", true)) {
                        acquirePermissions()
                        return
                    }
                    val documentFile = DocumentFile.fromTreeUri(requireContext(), treeUri)
                        ?.findFile("Media")
                        ?.findFile(".Statuses")
                    if (documentFile == null) {
                        toast(getString(R.string.whatsapp_directory_does_not_contain_statuses))
                        acquirePermissions()
                        return
                    }
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    activity?.contentResolver?.takePersistableUriPermission(treeUri,
                        takeFlags)

                    WhatsWebPreferences.whatsAppStorageUri = treeUri.toString()
                    setUpViewPager(treeUri)
                }
            } else {
                toast(getString(R.string.try_again))
                activity?.finish()
            }
        }
    }

    private fun acquirePermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context ?: return,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                    log( "uri not stored")
                    openDocumentTree()
                }
                arePermissionsGranted(uriString) -> {
                    setUpViewPager(Uri.parse(uriString))
                }
                else -> {
                    log("uri permission not stored")
                    openDocumentTree()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openDocumentTree() {
        toast(getString(R.string.please_select_whatsapp_directory))
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(status_scoped_storage_uri))
        }
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        try {
            startActivityForResult(intent, REQUEST_CODE_SAF)
        } catch (anfe: ActivityNotFoundException) {
            //There is no file manager present to process this request
            //finish
            toast("Sorry, we are not able to find any file manager in your phone. Please install a file manager or contactus.")
            activity?.finish()
        }
    }

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
