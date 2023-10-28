package ui.status.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.geeksoftapps.whatsweb.commons.BasicFragment
import com.geeksoftapps.whatsweb.commons.log

import com.geeksoftapps.whatsweb.app.databinding.FragmentStatusBinding
import com.geeksoftapps.whatsweb.app.utils.logCrashlytics
import com.geeksoftapps.whatsweb.app.ui.status.adapters.StatusAdapter
import ui.status.fragments.StatusContainerFragment.Companion.WHATSAPP_STORAGE_URI
import com.geeksoftapps.whatsweb.app.ui.status.preview.StatusPreviewActivity
import com.geeksoftapps.whatsweb.app.ui.status.viewmodels.StatusSaverViewModel
import com.geeksoftapps.whatsweb.status.whatsapp_saved_status_file
import com.geeksoftapps.whatsweb.status.IStatusRepo
import com.geeksoftapps.whatsweb.status.StatusRepo

import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import java.io.File

class StatusFragment : BasicFragment(), KodeinAware, StatusAdapter.EventListener {
    override val kodein by closestKodein()

    private lateinit var statussaverViewModel: StatusSaverViewModel
    private lateinit var statusAdapter: StatusAdapter
    private lateinit var allStatusesLiveData: LiveData<List<DocumentFile>>

    private lateinit var statusRepo: IStatusRepo

    private var whatsAppDocumentFile: DocumentFile? = null
    private lateinit var binding: FragmentStatusBinding

    companion object {
        val TAG: String = StatusContainerFragment::class.java.simpleName
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_status, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusAdapter = StatusAdapter(requireContext())

        binding.rvStatus.adapter = statusAdapter
        binding.rvStatus.layoutManager = StaggeredGridLayoutManager(
                context ?.let {context ->
                    calculateNoOfColumns(context, 180)
                } ?: 3, LinearLayoutManager.VERTICAL
        ).also { it.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE }

        statusAdapter.setEventListener(this)

        val uri = arguments?.getParcelable<Uri>(WHATSAPP_STORAGE_URI)
        if (uri == null) {
            logCrashlytics("uri is null in ${StatusFragment::class.simpleName}")
            activity?.finish()
            return
        }
        if (uri.scheme == "file") {
            whatsAppDocumentFile = DocumentFile.fromFile(File(uri.path))
        } else {
            whatsAppDocumentFile = DocumentFile.fromTreeUri(requireContext(), uri)
        }

        val statusDocumentFile = whatsAppDocumentFile?.findFile("Media")?.findFile(".Statuses")
        if (statusDocumentFile == null) {
            logCrashlytics("statusDocumentFile is null in ${StatusFragment::class.simpleName}")
            activity?.finish()
            return
        }

        statusRepo = StatusRepo(
            requireContext(),
            statusDocumentFile,
            whatsapp_saved_status_file
        )
        statussaverViewModel = StatusSaverViewModel(statusRepo)
        bindUI()
    }

    private fun bindUI() = lifecycleScope.launch {
        val allStatuses = statussaverViewModel.getAllStatuses
        allStatusesLiveData = allStatuses.await()
        allStatusesLiveData.observe(viewLifecycleOwner, Observer { statusFiles ->
            if (statusFiles == null) return@Observer
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            listOf(binding.avNoSavedStatuses, binding.tvNoSavedStatusHeading).forEach {
                it.visibility = if (statusFiles.isEmpty()) View.VISIBLE else View.GONE
            }
            statusAdapter.setStatusFilesList(statusFiles)
        })
    }

    override fun onStatusItemClick(statusFile: DocumentFile) = startStatusPreview(statusFile, StatusPreviewActivity.TYPE_UNSAVED)

    private fun startStatusPreview(statusFile: DocumentFile, type: Int) {
        context?.let {
            FirebaseAnalytics.getInstance(it)?.log("StatusSaverFrag_OnItemClick", itemId = when (type) {
                StatusPreviewActivity.TYPE_UNSAVED -> "TYPE_UNSAVED"
                StatusPreviewActivity.TYPE_SAVED -> "TYPE_SAVED"
                else -> "TYPE_UNKNOWN"
            })
        }
        startActivity(
            Intent(context, StatusPreviewActivity::class.java).apply {
                putExtra(WHATSAPP_STORAGE_URI, whatsAppDocumentFile?.uri)
                putExtra(StatusPreviewActivity.EXTRA_STATUS_NAME, statusFile.uri)
                putExtra(StatusPreviewActivity.EXTRA_STATUS_TYPE, type)
            })
    }
    private fun calculateNoOfColumns(context: Context, itemHeight: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / itemHeight).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.shimmerViewContainer.startShimmer()
    }

    override fun onPause() {
        binding.shimmerViewContainer.stopShimmer()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.shimmerViewContainer.stopShimmer()
    }
}