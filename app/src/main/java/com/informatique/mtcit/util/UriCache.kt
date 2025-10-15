package com.informatique.mtcit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Singleton cache to hold URI permissions across navigation
 * This prevents losing temporary URI permissions when navigating between screens
 */
object UriCache {
    private const val TAG = "UriCache"

    // Store URIs with their granted permissions
    // Use normalized URI strings as keys to handle URL encoding differences
    private val cachedUris = mutableMapOf<String, Uri>()

    /**
     * Normalize URI to handle encoding differences
     * Converts both encoded and decoded versions to the same key
     */
    private fun normalizeUriKey(uri: Uri): String {
        // Use the URI's canonical string representation
        return uri.toString()
    }

    /**
     * Cache a URI with its permission
     * Call this right after receiving URI from file picker
     */
    fun cacheUri(context: Context, uri: Uri) {
        try {
            // CRITICAL: Grant URI permission to our own app package
            // This allows the URI to be accessed from different activities/screens
            context.grantUriPermission(
                context.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Log.d(TAG, "Granted URI permission to package: ${context.packageName}")

            // Try to take persistent permission if available
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Log.d(TAG, "Took persistent permission for: $uri")
            } catch (e: Exception) {
                // If persistent permission not available, the granted permission should work
                Log.d(TAG, "Could not take persistent permission (using granted): ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error granting permission: ${e.message}", e)
        }

        // Store using both the original URI and its normalized form
        val key = normalizeUriKey(uri)
        cachedUris[key] = uri
        // Also store with the raw string as a fallback
        cachedUris[uri.toString()] = uri
        Log.d(TAG, "Cached URI with key: $key -> $uri")
    }

    /**
     * Get a cached URI - handles both encoded and decoded URI strings
     */
    fun getUri(uriString: String): Uri? {
        // First try exact match
        cachedUris[uriString]?.let {
            Log.d(TAG, "Found cached URI (exact match): $it")
            return it
        }

        // Try parsing the string and looking up by normalized key
        try {
            val parsedUri = Uri.parse(uriString)
            val normalized = normalizeUriKey(parsedUri)
            cachedUris[normalized]?.let {
                Log.d(TAG, "Found cached URI (normalized match): $it")
                return it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse URI string: $uriString")
        }

        // Try to find by matching scheme, authority, and path
        val target = Uri.parse(uriString)
        cachedUris.values.find { cachedUri ->
            cachedUri.scheme == target.scheme &&
            cachedUri.authority == target.authority &&
            cachedUri.path == target.path
        }?.let {
            Log.d(TAG, "Found cached URI (fuzzy match): $it")
            return it
        }

        Log.w(TAG, "No cached URI found for: $uriString")
        return null
    }

    /**
     * Remove a URI from cache and revoke permissions
     */
    fun removeUri(context: Context, uriString: String) {
        getUri(uriString)?.let { uri ->
            try {
                context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "Revoked URI permission for: $uri")
            } catch (e: Exception) {
                Log.w(TAG, "Could not revoke permission: ${e.message}")
            }
            // Remove all related keys
            cachedUris.values.removeAll { it == uri }
        }
        Log.d(TAG, "Removed URI from cache: $uriString")
    }

    /**
     * Clear all cached URIs and revoke permissions
     */
    fun clear(context: Context) {
        cachedUris.values.forEach { uri ->
            try {
                context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.w(TAG, "Could not revoke permission for $uri: ${e.message}")
            }
        }
        cachedUris.clear()
        Log.d(TAG, "Cleared all cached URIs")
    }
}
