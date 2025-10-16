package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.informatique.mtcit.ui.components.NativeFileViewer
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * File Viewer Screen - Native implementation
 * Uses only built-in Android components (no WebView, no external dependencies)
 * - Images: Display directly using Coil
 * - PDFs, Word, Excel, etc: Open with system's default app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    fileUri: String,
    fileName: String?,
    onNavigateBack: () -> Unit,
    onOpenExternal: (android.net.Uri?, String?) -> Unit
) {
    val uri = remember(fileUri) { fileUri.toUri() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the cached URI with permissions (same as NativeFileViewer does)
    val actualUri = remember(uri) {
        val uriString = uri.toString()
        val cachedUri = com.informatique.mtcit.util.UriCache.getUri(uriString)
        if (cachedUri != null) {
            android.util.Log.d("FileViewerScreen", "Using cached URI with permission: $cachedUri")
            cachedUri
        } else {
            android.util.Log.d("FileViewerScreen", "No cached URI found, using provided URI: $uri")
            uri
        }
    }

    // Pre-cache the file for external sharing while we still have access
    var cachedShareableUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPreparingCache by remember { mutableStateOf(false) }

    LaunchedEffect(actualUri) {
        // Pre-cache content URIs to avoid permission issues later
        if (actualUri.scheme == "content") {
            isPreparingCache = true
            scope.launch {
                cachedShareableUri = withContext(Dispatchers.IO) {
                    try {
                        android.util.Log.d("FileViewerScreen", "Pre-caching file for external sharing using actual URI")
                        // Use the actualUri which has permissions, not the original uri
                        com.informatique.mtcit.util.FileShareHelper.getShareableUri(
                            context,
                            actualUri,
                            fileName
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FileViewerScreen", "Failed to pre-cache file: ${e.message}", e)
                        null
                    }
                }
                isPreparingCache = false
                android.util.Log.d("FileViewerScreen", "Pre-caching complete: ${cachedShareableUri != null}")
            }
        } else {
            // For non-content URIs (like file://), we can generate on-demand
            cachedShareableUri = actualUri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName ?: localizedApp(R.string.file_viewer)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = localizedApp(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Pass the cached shareable URI to the callback
                            onOpenExternal(cachedShareableUri, fileName)
                        },
                        enabled = !isPreparingCache
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in external app"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        NativeFileViewer(
            uri = uri, // Pass the original URI - NativeFileViewer will look up the cached version
            fileName = fileName,
            modifier = Modifier.padding(paddingValues),
            onOpenExternal = { onOpenExternal(cachedShareableUri, fileName) }
        )
    }
}
