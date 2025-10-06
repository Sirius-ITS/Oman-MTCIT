package com.informatique.mtcit.ui.screens

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.informatique.mtcit.worker.PdfBitmapConverter

// Custom data class for search results
data class SearchResults(
    val page: Int,
    val results: List<RectF>
)

@Composable
fun PdfViewerScreen(
    modifier: Modifier = Modifier,
    fileUri: Uri? = null,
    title: String = "Document Viewer",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val pdfBitmapConverter = remember {
        PdfBitmapConverter(context)
    }
    var pdfUri by remember {
        mutableStateOf(fileUri)
    }
    var renderedPages by remember {
        mutableStateOf<List<Bitmap>>(emptyList())
    }
    var searchText by remember {
        mutableStateOf("")
    }
    var searchResults by remember {
        mutableStateOf<List<SearchResults>>(emptyList())
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri ->
            isLoading = true
            errorMessage = null
            try {
                renderedPages = pdfBitmapConverter.pdfToBitmaps(uri)
                if (renderedPages.isEmpty()) {
                    errorMessage = "Failed to load PDF or PDF is empty"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading PDF: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val choosePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        pdfUri = uri
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Custom Header (replacing experimental TopAppBar)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (pdfUri == null) {
            // No PDF selected - show file picker
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No document selected",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        choosePdfLauncher.launch("application/pdf")
                    }) {
                        Text(text = "Choose PDF")
                    }
                }
            }
        } else {
            // PDF selected - show content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search bar (if PDF is loaded successfully)
                if (renderedPages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Search in document") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        if (searchText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    searchResults = emptyList()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                }

                // Content area
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading document...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        choosePdfLauncher.launch("application/pdf")
                                    }
                                ) {
                                    Text("Choose Another PDF")
                                }
                            }
                        }
                    }

                    renderedPages.isNotEmpty() -> {
                        // Show PDF pages
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(renderedPages) { index, bitmap ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Page ${index + 1}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        AsyncImage(
                                            model = bitmap,
                                            contentDescription = "PDF Page ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .drawWithContent {
                                                    drawContent()

                                                    // Highlight search results if any
                                                    searchResults.find { it.page == index }?.results?.forEach { rect ->
                                                        val scaledRect = RectF(
                                                            rect.left * size.width,
                                                            rect.top * size.height,
                                                            rect.right * size.width,
                                                            rect.bottom * size.height
                                                        )

                                                        drawRoundRect(
                                                            color = Color.Yellow.copy(alpha = 0.3f),
                                                            topLeft = Offset(scaledRect.left, scaledRect.top),
                                                            size = Size(
                                                                scaledRect.width(),
                                                                scaledRect.height()
                                                            ),
                                                            cornerRadius = CornerRadius(4.dp.toPx())
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
            }
        }
    }
}