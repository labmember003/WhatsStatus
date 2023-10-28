package com.geeksoftapps.whatsweb.app.ui.status.viewmodels

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geeksoftapps.whatsweb.status.IStatusRepo
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StatusPreviewViewModel(
    private val statusRepo: IStatusRepo
) : ViewModel() {

    val getAllStatuses by lazy {
        viewModelScope.async(Dispatchers.IO, LAZY) {
            statusRepo.get()
        }
    }

    val getAllSavedStatuses by lazy {
        viewModelScope.async(Dispatchers.IO, LAZY) {
            statusRepo.getSaved()
        }
    }

    fun saveStatusAsync(statusFile: DocumentFile) = viewModelScope.async(Dispatchers.IO) {
        return@async statusRepo.save(statusFile)
    }

    fun deleteSavedStatusAsync(statusFile: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            statusRepo.removeSaved(statusFile)
        }
    }
}