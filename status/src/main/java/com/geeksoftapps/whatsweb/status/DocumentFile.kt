package com.geeksoftapps.whatsweb.status

import androidx.documentfile.provider.DocumentFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif")
private val videoExtensions = setOf("mp4", "3gp", "mkv", "webm", "mov", "avi")

fun DocumentFile.statusType(): StatusType = statusTypeOf(type, name)

/**
 * Works out whether a status file is a photo or a video. Uses the MIME type the storage
 * provider reports and falls back to the file extension when the provider has none
 * (or reports application/octet-stream).
 */
fun statusTypeOf(mimeType: String?, fileName: String?): StatusType {
    when {
        mimeType?.startsWith("image") == true -> return StatusType.IMAGE
        mimeType?.startsWith("video") == true -> return StatusType.VIDEO
    }
    val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: return StatusType.OTHER
    return when (extension) {
        in imageExtensions -> StatusType.IMAGE
        in videoExtensions -> StatusType.VIDEO
        else -> StatusType.OTHER
    }
}
