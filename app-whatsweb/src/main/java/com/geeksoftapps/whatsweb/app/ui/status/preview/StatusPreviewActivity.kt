package com.geeksoftapps.whatsweb.app.ui.status.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.geeksoftapps.whatsweb.app.R
import com.geeksoftapps.whatsweb.commons.log
import com.geeksoftapps.whatsweb.commons.media.MediaActivity
import com.geeksoftapps.whatsweb.commons.toast

import com.geeksoftapps.whatsweb.app.ui.status.fragments.StatusContainerFragment.Companion.WHATSAPP_STORAGE_URI
import com.geeksoftapps.whatsweb.app.ui.status.viewmodels.StatusPreviewViewModel
import com.geeksoftapps.whatsweb.app.utils.logCrashlytics
import com.geeksoftapps.whatsweb.status.IStatusRepo
import com.geeksoftapps.whatsweb.status.StatusRepo
import com.geeksoftapps.whatsweb.status.whatsapp_saved_status_file
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.geeksoftapps.whatsweb.app.utils.Constants
import com.geeksoftapps.whatsweb.app.utils.log
import java.io.File
import java.net.URI
import kotlin.properties.Delegates

class StatusPreviewActivity : MediaActivity(), KodeinAware {

    companion object {
        const val EXTRA_STATUS_TYPE = "EXTRA_STATUS_TYPE"
        const val EXTRA_STATUS_NAME = "EXTRA_STATUS_NAME"
        const val TYPE_UNSAVED = 0
        const val TYPE_SAVED = 1
        private const val INDEX_INVALID = -1
    }

    override val kodein by closestKodein()
    private var statusDocumentFileUri: Uri? = null

    private var statusType by Delegates.notNull<Int>()
    private lateinit var statusPreviewViewModel: StatusPreviewViewModel

    private lateinit var statusRepo: IStatusRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        intent?.apply {
            statusDocumentFileUri = extras?.get(EXTRA_STATUS_NAME) as Uri?
            statusType = getIntExtra(
                EXTRA_STATUS_TYPE,
                TYPE_UNSAVED
            )
        }
        super.onCreate(savedInstanceState)

        val uri = intent.extras?.getParcelable<Uri>(WHATSAPP_STORAGE_URI)

        if (uri == null) {
            logCrashlytics("uri in null in ${StatusPreviewActivity::class.simpleName}.")
            finish()
            return
        }

        var statusDocumentFile = if (uri.scheme == "file") {
            DocumentFile.fromFile(File(uri.path))
        } else {
            DocumentFile.fromTreeUri(this, uri)
        }?.findFile("Media")?.findFile(".Statuses")

        if (statusDocumentFile == null) {
            logCrashlytics("statusDocumentFile is null in ${StatusPreviewActivity::class.simpleName}")
            finish()
            return
        }

        statusRepo = StatusRepo(this, statusDocumentFile, whatsapp_saved_status_file)
        statusPreviewViewModel = StatusPreviewViewModel(statusRepo)

        bindUI()
    }

    override fun showDeleteButton(): Boolean {
        return statusType == TYPE_SAVED
    }

    override fun showSaveButton(): Boolean {
        return statusType == TYPE_UNSAVED
    }

    override fun showShareButton(): Boolean {
        return true
    }

    private fun bindUI() = lifecycleScope.launch {
        val allStatuses = if (statusType == TYPE_UNSAVED) {
            statusPreviewViewModel.getAllStatuses
        } else {
            statusPreviewViewModel.getAllSavedStatuses
        }.await()
        allStatuses.observe(this@StatusPreviewActivity, Observer { statusFiles ->
            if (statusFiles == null) return@Observer
            val currentItemIndex = getCurrentItemIndex(statusFiles, statusDocumentFileUri)
            if (currentItemIndex != INDEX_INVALID) {
                setMediaFiles(statusFiles, currentItemIndex, true)
            } else {
                setMediaFiles(statusFiles)
            }
        })
    }

    private fun getStatusFile(position: Int) =
        getItem(position)

    override fun onSaveButtonClick(position: Int) {
        getStatusFile(position)?.let {
            lifecycleScope.launch {
                if (statusPreviewViewModel.saveStatusAsync(it).await()) {
                    withContext(Dispatchers.Main) {
                        toast("Status saved successfully")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        toast("Status could not be saved")
                    }
                }
            }
        }
    }

    override fun onDeleteButtonClick(position: Int) {
        getStatusFile(position)?.let {
            if (getMediaCount() == 1) {
                statusPreviewViewModel.deleteSavedStatusAsync(it)
                finish()
            } else {
                statusPreviewViewModel.deleteSavedStatusAsync(it)
            }
        }
    }

    override fun onShareButtonClick(position: Int) {
        getStatusFile(position)?.let {
            startActivity(getShareStatusIntent(this, it))
        }
    }

    private fun getShareStatusIntent(context: Context, documentFile: DocumentFile): Intent {
        val uri = if (documentFile.uri.scheme?.contains("file", true) == true) {
            FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, File(URI(documentFile.uri.toString())))
        } else {
            documentFile.uri
        }
        val i = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = documentFile.type
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        return Intent.createChooser(i, context.getString(R.string.share_with))
    }

    private fun getCurrentItemIndex(statusFiles: List<DocumentFile>, statusDocumentFileUri: Uri?): Int {
        if (statusDocumentFileUri == null) return INDEX_INVALID
        statusFiles.indexOfFirst { it.uri.toString() == statusDocumentFileUri.toString() }.let { index ->
            return if (index != -1) index else INDEX_INVALID
        }
    }
}
