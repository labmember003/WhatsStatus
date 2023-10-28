package com.geeksoftapps.whatsweb.status

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StatusRepo(
    private val context: Context,
    private val statusDirectory: DocumentFile,
    private val savedStatusFile: File
) : IStatusRepo {

    private val savedStatusDocumentFile: DocumentFile

    init {
        if (!savedStatusFile.exists()) {
            savedStatusFile.mkdirs()
        }
        savedStatusDocumentFile = DocumentFile.fromFile(savedStatusFile)
    }

    private val statuses by lazy {
        StatusesLiveData(statusDirectory)
    }

    private val savedStatuses by lazy {
        StatusesLiveData(savedStatusDocumentFile)
    }

    override suspend fun get(): LiveData<List<DocumentFile>> = statuses

    override suspend fun getSaved(): LiveData<List<DocumentFile>> = savedStatuses

    override suspend fun save(statusFile: DocumentFile): Boolean = suspendCoroutine { cont ->
        try {

            FileUtils.copyInputStreamToFile(
                context.contentResolver.openInputStream(statusFile.uri),
                File(savedStatusFile, statusFile.name)
            )
            refresh()
            cont.resume(true)

        } catch (e: NullPointerException) {
            cont.resume(false)
        } catch (e: IOException) {
            cont.resume(false)
        }
    }

    override suspend fun removeSaved(file: DocumentFile): Boolean {
        return file.delete().also {
            refresh()
        }
    }

    override suspend fun removeAllSaved(): Boolean {
        try {
            savedStatusDocumentFile.listFiles().forEach { it.delete() }
            refresh()
            return true
        } catch (e: IOException) {
            return false
        } catch (e: IllegalStateException) {
            return false
        }
    }

    override fun refresh() {
        statuses.reload()
        savedStatuses.reload()
    }
}