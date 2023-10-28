package com.geeksoftapps.whatsweb.status

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StatusesLiveData (
    private val statusDirectory: DocumentFile
): LiveData<List<DocumentFile>>(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext
        get() = job + Dispatchers.IO

    private var statusFileList = mutableListOf<DocumentFile>()

    private fun loadStatuses() {
        launch {
            statusFileList = statusDirectory.listFiles()
                .filter { it.statusType() != StatusType.OTHER }
                .sortedByDescending { it.lastModified() }
                .toMutableList()
            postValue(statusFileList)
        }
    }

    fun reload() {
        loadStatuses()
    }

    override fun onActive() {
        super.onActive()
        job = Job()
        loadStatuses()
    }

    override fun onInactive() {
        super.onInactive()
        job.cancel()
    }
}