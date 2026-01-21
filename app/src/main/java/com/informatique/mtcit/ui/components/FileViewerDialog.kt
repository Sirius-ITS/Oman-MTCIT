package com.informatique.mtcit.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * ✅ File Viewer Dialog - Displays files in a dialog overlay
 * - PDFs & Images: Display internally using NativeFileViewer
 * - Office files (Word, Excel, etc.): Open with external app
 * This preserves the form state instead of navigating to a separate screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerDialog(
    isOpen: Boolean,
    fileUri: String,
    fileName: String,
    mimeType: String,
    onDismiss: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current

    // ✅ Check if this is an HTTP/HTTPS URL (from API) or local URI
    val isHttpUrl = remember(fileUri) {
        fileUri.startsWith("http://", ignoreCase = true) ||
        fileUri.startsWith("https://", ignoreCase = true)
    }

    // Check if file can be displayed internally
    val canDisplayInternally = remember(mimeType, fileUri, isHttpUrl) {
        // HTTP URLs are always displayed in WebView
        isHttpUrl ||
        mimeType.startsWith("image/") ||
        mimeType == "application/pdf" ||
        fileUri.endsWith(".pdf", ignoreCase = true)
    }

    // For files that must open externally (Office docs), open and auto-dismiss
    LaunchedEffect(isOpen, fileUri, canDisplayInternally, isHttpUrl) {
        if (isOpen && fileUri.isNotEmpty() && !canDisplayInternally && !isHttpUrl) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri.toUri(), mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("FileViewerDialog", "Failed to open file: ${e.message}")
            }

            // Auto-dismiss after opening external app
            onDismiss()
        }
    }

    // For PDFs, images, and HTTP URLs, show dialog with internal viewer
    if (isOpen && canDisplayInternally) {
        // Handle back button to close dialog
        BackHandler(enabled = true) {
            onDismiss()
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = extraColors.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Bar
                    Surface(
                        color = extraColors.cardBackground,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close Button
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = extraColors.whiteInDarkMode
                                )
                            }

                            // File Name
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.whiteInDarkMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            )

                            // Share Button
                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = mimeType
                                            putExtra(Intent.EXTRA_STREAM, fileUri.toUri())
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, fileName)
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "FileViewerDialog",
                                            "Failed to share file: ${e.message}"
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = extraColors.whiteInDarkMode
                                )
                            }
                        }
                    }

                    // File Content - Use NativeFileViewer for internal display
                    // Pass HTTP URL as string, or convert local URI
                    if (isHttpUrl) {
                        // For HTTP URLs, pass directly as string
                        NativeFileViewerWithUrl(
                            url = fileUri,
                            fileName = fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // For local URIs, convert and use normal viewer
                        NativeFileViewer(
                            uri = fileUri.toUri(),
                            fileName = fileName,
                            modifier = Modifier.fillMaxSize(),
                            onOpenExternal = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(fileUri.toUri(), mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.util.Log.e(
                                        "FileViewerDialog",
                                        "Failed to open external: ${e.message}"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Native file viewer for HTTP/HTTPS URLs
 * Uses WebView to display remote files
 */
@Composable
fun NativeFileViewerWithUrl(
    url: String,
    fileName: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Use WebView to display the URL
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                android.webkit.WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    webViewClient = android.webkit.WebViewClient()

                    println("🌐 Loading URL in WebView: $url")
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
