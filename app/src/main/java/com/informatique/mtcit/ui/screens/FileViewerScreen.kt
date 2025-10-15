package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.informatique.mtcit.ui.components.NativeFileViewer
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.R

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
    onOpenExternal: () -> Unit
) {
    val uri = remember(fileUri) { fileUri.toUri() }

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
                    IconButton(onClick = onOpenExternal) {
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
            uri = uri,
            fileName = fileName,
            modifier = Modifier.padding(paddingValues),
            onOpenExternal = onOpenExternal
        )
    }
}
