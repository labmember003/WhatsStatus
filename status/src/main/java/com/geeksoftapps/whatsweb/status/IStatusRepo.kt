package com.geeksoftapps.whatsweb.status

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData

interface IStatusRepo {

    suspend fun get(): LiveData<List<DocumentFile>>

    suspend fun getSaved(): LiveData<List<DocumentFile>>

    suspend fun save(statusFile: DocumentFile): Boolean

    suspend fun removeSaved(file: DocumentFile): Boolean

    suspend fun removeAllSaved(): Boolean

    fun refresh()
}