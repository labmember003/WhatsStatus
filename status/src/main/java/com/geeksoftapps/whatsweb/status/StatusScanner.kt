package com.geeksoftapps.whatsweb.status

import androidx.documentfile.provider.DocumentFile

/**
 * Lists every photo and video status in WhatsApp's .Statuses folder.
 *
 * Contact statuses and Group Status updates (WhatsApp, Oct 2026) are cached in the same
 * folder once viewed. Newer WhatsApp builds can also keep them in sub-folders (for example
 * one per account), so this looks [MAX_DEPTH] levels down instead of only at the top level.
 */
object StatusScanner {

    const val MAX_DEPTH = 2

    fun scan(directory: DocumentFile): List<DocumentFile> {
        val found = mutableListOf<DocumentFile>()
        collect(directory, 0, found)
        return found
            .distinctBy { (it.name ?: it.uri.toString()) + "|" + it.length() }
            .sortedByDescending { it.lastModified() }
    }

    private fun collect(directory: DocumentFile, depth: Int, into: MutableList<DocumentFile>) {
        val children = try {
            directory.listFiles()
        } catch (e: Exception) {
            // A folder WhatsApp removed while we were reading it, or one we may not open
            return
        }
        for (child in children) {
            if (child.isDirectory) {
                if (depth < MAX_DEPTH) collect(child, depth + 1, into)
            } else if (isStatusMedia(child.name, child.type, child.length())) {
                into.add(child)
            }
        }
    }

    /** True for a real photo/video; false for .nomedia, temp/partial files and empty files. */
    fun isStatusMedia(name: String?, mimeType: String?, length: Long): Boolean {
        if (name == null || name.startsWith(".")) return false
        if (length <= 0L) return false
        val lower = name.lowercase()
        if (lower.endsWith(".tmp") || lower.endsWith(".part") || lower.endsWith(".pending")) return false
        return statusTypeOf(mimeType, name) != StatusType.OTHER
    }
}
