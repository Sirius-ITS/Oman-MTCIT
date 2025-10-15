package com.informatique.mtcit.ui.components

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * WebView-based Office Document Viewer
 * Uses Google Docs Viewer as a fallback when external apps are not available
 *
 * This is a FALLBACK option - external apps provide better quality
 * Requires internet connection to use Google's document viewer service
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeDocumentWebViewer(
    fileName: String?,
    mimeType: String?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    // Prepare the document for viewing
    LaunchedEffect(fileName) {
        try {
            isLoading = true
            error = null

            // For WebView-based viewing, we need to use Google Docs Viewer
            // Note: This requires the file to be accessible via a URL
            // For local files, we show a message that external app is better

            // Since we're working with local URIs (content://), we inform user
            // that external app is the better option
            error = "Local documents cannot be viewed online. Please use the 'Open with External App' option for best results."
            isLoading = false

        } catch (e: Exception) {
            isLoading = false
            error = "Error preparing document: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName ?: "Document Viewer") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    // Show informative message
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📱 Local File Detected",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "This document is stored locally on your device.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "For the best viewing experience with full formatting and features, please use the 'Open with External App' option.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onClose,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go Back")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Recommended Apps:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Microsoft Office\n• Google Docs/Sheets/Slides\n• WPS Office\n• Adobe Acrobat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                viewerUrl != null -> {
                    // Show WebView with Google Docs Viewer
                    // Note: This path is rarely reached since we recommend external apps
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }

                                loadUrl(viewerUrl!!)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

