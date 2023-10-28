package com.geeksoftapps.whatsweb.app.ui.status.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geeksoftapps.whatsweb.status.IStatusRepo
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class StatusSaverViewModel(
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
}