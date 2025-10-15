package com.informatique.mtcit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Manages URI permissions to prevent loss during navigation and app lifecycle changes
 * Specifically designed for Android 16+ strict permission handling
 */
object UriPermissionManager {
    private const val TAG = "UriPermissionManager"

    /**
     * Ensures URI has persistent read permission
     * Handles all edge cases for Android 16+
     */
    fun ensureReadPermission(context: Context, uri: Uri): Result<Unit> {
        return try {
            // First, check if we already have persistent permission
            val persistedUris = context.contentResolver.persistedUriPermissions
            val alreadyPersisted = persistedUris.any {
                it.uri == uri && it.isReadPermission
            }

            if (!alreadyPersisted) {
                // Try to take persistent permission
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Log.d(TAG, "Successfully took persistent permission for: $uri")
            } else {
                Log.d(TAG, "Already have persistent permission for: $uri")
            }

            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException taking permission for $uri: ${e.message}")
            // Permission might not be available for this URI type
            // This is OK for content:// URIs from some providers
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "IllegalArgumentException for $uri: ${e.message}")
            // URI doesn't support persistent permissions
            // This is normal for some content providers
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error ensuring permission for $uri", e)
            Result.failure(e)
        }
    }

    /**
     * Releases persistent permission for a URI
     * Call this when file is no longer needed
     */
    fun releasePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Log.d(TAG, "Released permission for: $uri")
        } catch (e: Exception) {
            Log.w(TAG, "Could not release permission for $uri: ${e.message}")
        }
    }

    /**
     * Checks if we can read from the URI
     * Returns true if readable, false otherwise
     */
    fun canReadUri(context: Context, uri: Uri): Boolean {
        return try {
            Log.d(TAG, "Checking if URI is readable: $uri")

            // First check if we have the permission flag
            val hasPermission = context.checkCallingOrSelfUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Has URI permission flag: $hasPermission")

            // Try to actually open the stream
            val canOpen = context.contentResolver.openInputStream(uri)?.use { stream ->
                // Try to read at least 1 byte to verify it's actually readable
                stream.read()
                true
            } ?: false

            Log.d(TAG, "Can open and read URI: $canOpen")
            canOpen
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - no permission to read URI $uri: ${e.message}")
            false
        } catch (e: java.io.FileNotFoundException) {
            Log.e(TAG, "FileNotFoundException - URI not found $uri: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Cannot read URI $uri: ${e.message}", e)
            false
        }
    }

    /**
     * Gets all currently persisted URI permissions
     */
    fun getPersistedUris(context: Context): List<Uri> {
        return try {
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting persisted URIs", e)
            emptyList()
        }
    }

    /**
     * Clears all persisted URI permissions
     * Use with caution - typically only on logout or app reset
     */
    fun clearAllPermissions(context: Context) {
        try {
            val persistedUris = context.contentResolver.persistedUriPermissions
            persistedUris.forEach { permission ->
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        permission.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not release ${permission.uri}: ${e.message}")
                }
            }
            Log.d(TAG, "Cleared all URI permissions")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing permissions", e)
        }
    }
}
