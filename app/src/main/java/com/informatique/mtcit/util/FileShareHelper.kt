package com.informatique.mtcit.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Helper class to share files with external apps
 * Copies content URI files to cache directory and provides shareable FileProvider URIs
 */
object FileShareHelper {

    /**
     * Copies a file from content URI to cache directory and returns a shareable FileProvider URI
     * This allows external apps to access the file without permission issues
     */
    fun getShareableUri(context: Context, contentUri: Uri, fileName: String?): Uri? {
        return try {
            // Create shared cache directory
            val sharedDir = File(context.cacheDir, "shared")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }

            // Clean up old files in shared directory (older than 1 hour)
            cleanupOldFiles(sharedDir)

            // Determine file name
            val actualFileName = fileName ?: getFileNameFromUri(context, contentUri) ?: "file"

            // Create destination file
            val destFile = File(sharedDir, actualFileName)

            // Copy content from source URI to destination file
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Generate FileProvider URI for the copied file
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
        } catch (e: Exception) {
            android.util.Log.e("FileShareHelper", "Error creating shareable URI: ${e.message}", e)
            null
        }
    }

    /**
     * Clean up files older than 1 hour from shared cache directory
     */
    private fun cleanupOldFiles(directory: File) {
        try {
            val currentTime = System.currentTimeMillis()
            val oneHourInMillis = 60 * 60 * 1000 // 1 hour

            directory.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > oneHourInMillis) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileShareHelper", "Error cleaning up old files: ${e.message}")
        }
    }

    /**
     * Get file name from content URI
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    uri.lastPathSegment
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}

