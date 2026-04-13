package com.geeksoftapps.whatsweb.status

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
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
            val fileName = statusFile.name ?: return@suspendCoroutine cont.resume(false)

            val destFile = File(savedStatusFile, fileName)
            FileUtils.copyInputStreamToFile(
                context.contentResolver.openInputStream(statusFile.uri),
                destFile
            )

            // Also copy to public storage so the file is visible in the gallery
            copyToMediaStore(destFile, fileName, statusFile.type)

            refresh()
            cont.resume(true)

        } catch (e: NullPointerException) {
            cont.resume(false)
        } catch (e: IOException) {
            cont.resume(false)
        }
    }

    /**
     * Copies the saved file to the device's public MediaStore so it appears in the gallery.
     * Uses MediaStore API on Android 10+ (scoped storage), and direct file + MediaScanner on older versions.
     */
    private fun copyToMediaStore(sourceFile: File, fileName: String, mimeType: String?) {
        try {
            val videoExtensions = setOf("mp4", "3gp", "mkv", "avi", "webm")
            val isVideo = mimeType?.contains("video") == true
                    || sourceFile.extension.lowercase() in videoExtensions
            val resolvedMimeType = mimeType
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    sourceFile.extension.lowercase()
                )
                ?: if (isVideo) "video/mp4" else "image/jpeg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — use MediaStore
                val collection = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val relativePath = if (isVideo) {
                    Environment.DIRECTORY_MOVIES + "/Status Saver"
                } else {
                    Environment.DIRECTORY_PICTURES + "/Status Saver"
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, resolvedMimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(collection, values) ?: return

                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

            } else {
                // Android 9 and below — copy to public directory and scan
                val publicDir = if (isVideo) {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                } else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                }
                val statusSaverDir = File(publicDir, "Status Saver")
                if (!statusSaverDir.exists()) {
                    statusSaverDir.mkdirs()
                }

                val destFile = File(statusSaverDir, fileName)
                FileUtils.copyFile(sourceFile, destFile)

                // Notify gallery via MediaScanner
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(resolvedMimeType),
                    null
                )
            }
        } catch (e: Exception) {
            // Don't fail the save if gallery copy fails — the file is still saved in-app
            e.printStackTrace()
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