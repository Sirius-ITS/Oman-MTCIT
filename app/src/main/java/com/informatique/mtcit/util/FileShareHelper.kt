package com.informatique.mtcit.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Helper class to share files with external apps
 * Handles both content:// and file:// URIs properly
 */
object FileShareHelper {

    /**
     * Get a shareable URI for external apps
     * For content:// URIs, we copy them to cache to avoid permission issues
     * For file:// URIs, converts them to FileProvider URIs
     */
    fun getShareableUri(context: Context, uri: Uri, fileName: String?): Uri? {
        return try {
            android.util.Log.d("FileShareHelper", "Getting shareable URI for: $uri, fileName: $fileName")

            when (uri.scheme) {
                "content" -> {
                    // For content URIs, we need to copy to cache because:
                    // 1. We may not have persistent permission to grant to other apps
                    // 2. External apps may not have access to the content provider
                    android.util.Log.d("FileShareHelper", "Content URI detected, copying to cache for sharing")
                    copyToCache(context, uri, fileName)
                }

                "file" -> {
                    // File URIs need to be converted to FileProvider URIs
                    val file = File(uri.path ?: return null)
                    if (!file.exists()) {
                        android.util.Log.e("FileShareHelper", "File does not exist: ${file.absolutePath}")
                        return null
                    }

                    try {
                        val providerUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        android.util.Log.d("FileShareHelper", "Converted file:// to FileProvider URI: $providerUri")
                        providerUri
                    } catch (e: IllegalArgumentException) {
                        android.util.Log.w("FileShareHelper", "File is not in FileProvider paths, copying to cache")
                        // File is not in FileProvider paths, need to copy it
                        copyToCache(context, uri, fileName)
                    }
                }

                else -> {
                    // Unknown scheme, try to copy to cache
                    android.util.Log.w("FileShareHelper", "Unknown URI scheme: ${uri.scheme}, attempting to copy")
                    copyToCache(context, uri, fileName)
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("FileShareHelper", "Error creating shareable URI: ${e.message}", e)
            null
        }
    }

    /**
     * Copy file from URI to cache directory
     * This ensures we have full control over the file and can share it via FileProvider
     */
    private fun copyToCache(context: Context, uri: Uri, fileName: String?): Uri? {
        return try {
            // Create shared cache directory
            val sharedDir = File(context.cacheDir, "shared")
            if (!sharedDir.exists()) {
                val created = sharedDir.mkdirs()
                android.util.Log.d("FileShareHelper", "Shared directory created: $created")
            }

            // Clean up old files in shared directory (older than 1 hour)
            cleanupOldFiles(sharedDir)

            // Determine file name
            val actualFileName = fileName ?: getFileNameFromUri(context, uri) ?: "file_${System.currentTimeMillis()}"
            android.util.Log.d("FileShareHelper", "Using file name: $actualFileName")

            // Create destination file
            val destFile = File(sharedDir, actualFileName)

            // If file already exists and is recent (less than 5 minutes old), reuse it
            if (destFile.exists() && (System.currentTimeMillis() - destFile.lastModified() < 5 * 60 * 1000)) {
                android.util.Log.d("FileShareHelper", "Reusing existing cached file: ${destFile.absolutePath}")
            } else {
                // Copy content from source URI to destination file
                var bytesCopied = 0L

                // Try to take persistent permission if we don't have it
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    android.util.Log.d("FileShareHelper", "Successfully took persistent URI permission")
                } catch (e: SecurityException) {
                    android.util.Log.w("FileShareHelper", "Could not take persistent permission, proceeding anyway")
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        bytesCopied = input.copyTo(output)
                    }
                } ?: run {
                    android.util.Log.e("FileShareHelper", "Failed to open input stream from URI")
                    return null
                }

                android.util.Log.d("FileShareHelper", "Copied $bytesCopied bytes to ${destFile.absolutePath}")
            }

            // Generate FileProvider URI for the copied file
            val providerUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )

            android.util.Log.d("FileShareHelper", "Generated FileProvider URI: $providerUri")
            providerUri

        } catch (e: Exception) {
            android.util.Log.e("FileShareHelper", "Error copying to cache: ${e.message}", e)
            e.printStackTrace()
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
                    android.util.Log.d("FileShareHelper", "Deleted old file: ${file.name}")
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
            // First try to get from query
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }

            // If that fails, try lastPathSegment
            fileName ?: uri.lastPathSegment
        } catch (e: Exception) {
            android.util.Log.e("FileShareHelper", "Error getting file name from URI: ${e.message}")
            uri.lastPathSegment
        }
    }
}
