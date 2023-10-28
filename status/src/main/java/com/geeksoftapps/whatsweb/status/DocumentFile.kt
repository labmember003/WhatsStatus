package com.geeksoftapps.whatsweb.status

import androidx.documentfile.provider.DocumentFile

fun DocumentFile.statusType(): StatusType {
    return when {
        this.type?.contains("image") == true -> StatusType.IMAGE
        this.type?.contains("video") == true -> StatusType.VIDEO
        else -> return StatusType.OTHER
    }
}