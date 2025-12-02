package com.informatique.mtcit.data.helpers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.informatique.mtcit.data.model.EngineFileUpload
import java.io.InputStream

/**
 * Helper class for handling file uploads
 * Converts URI to ByteArray and extracts file metadata
 */
object FileUploadHelper {

    /**
     * Convert URI to EngineFileUpload with file bytes
     */
    fun uriToFileUpload(context: Context, uri: Uri): EngineFileUpload? {
        return try {
            val fileName = getFileName(context, uri) ?: "document_${System.currentTimeMillis()}"
            val mimeType = getMimeType(context, uri) ?: "application/octet-stream"
            val fileBytes = uriToByteArray(context, uri)

            if (fileBytes != null) {
                EngineFileUpload(
                    fileName = fileName,
                    fileUri = uri.toString(),
                    fileBytes = fileBytes,
                    mimeType = mimeType
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Error converting URI to file upload: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Get file name from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        // Try to get the file name from the URI
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }

        // Fallback to path segment
        if (fileName == null) {
            fileName = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }

        return fileName
    }

    /**
     * Get MIME type from URI
     */
    private fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }

    /**
     * Convert URI to ByteArray
     */
    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            println("❌ Error reading file from URI: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Validate file size (max 10MB)
     */
    fun isFileSizeValid(fileBytes: ByteArray, maxSizeInMB: Int = 10): Boolean {
        val maxSizeInBytes = maxSizeInMB * 1024 * 1024
        return fileBytes.size <= maxSizeInBytes
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

