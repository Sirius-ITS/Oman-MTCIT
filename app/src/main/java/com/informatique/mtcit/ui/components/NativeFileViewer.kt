package com.informatique.mtcit.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.graphics.createBitmap
import com.informatique.mtcit.util.UriCache
import com.informatique.mtcit.util.UriPermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Native file viewer using ONLY Android built-in APIs
 * No external libraries, no WebView
 *
 * Supported formats:
 * - Images: JPG, PNG, GIF, BMP, WEBP (using Android's BitmapFactory)
 * - PDF: Using Android's native PdfRenderer API (API 21+)
 * - Text: TXT, CSV, JSON, XML (using BufferedReader)
 * - Office docs: Shows text extraction or message to use external app
 */
@Composable
fun NativeFileViewer(
    uri: Uri,
    fileName: String?,
    modifier: Modifier = Modifier,
    onOpenExternal: () -> Unit = {}
) {
    val context = LocalContext.current

    // CRITICAL: Get the cached URI which still has the permission attached
    val actualUri = remember(uri) {
        val uriString = uri.toString()
        val cachedUri = UriCache.getUri(uriString)
        if (cachedUri != null) {
            Log.d("NativeFileViewer", "Using cached URI with permission: $cachedUri")
            cachedUri
        } else {
            Log.d("NativeFileViewer", "No cached URI found, using provided URI: $uri")
            uri
        }
    }

    val mimeType = remember(actualUri) { context.contentResolver.getType(actualUri) ?: getTypeFromFileName(fileName) }
    var error by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var isCheckingPermission by remember { mutableStateOf(true) }

    // Ensure URI permission is granted before attempting to display the file
    LaunchedEffect(actualUri) {
        isCheckingPermission = true
        Log.d("NativeFileViewer", "Checking permissions for URI: $actualUri")

        try {
            // Try to ensure we have read permission for this URI
            // This might fail for some URIs that don't support persistent permissions
            val result = UriPermissionManager.ensureReadPermission(context, actualUri)

            if (result.isFailure) {
                Log.w("NativeFileViewer", "Could not take persistent permission: ${result.exceptionOrNull()?.message}")
            }

            // The real test: can we actually read from the URI?
            val canRead = try {
                context.contentResolver.openInputStream(actualUri)?.use { stream ->
                    stream.read() // Try to read at least 1 byte
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e("NativeFileViewer", "Cannot read URI: ${e.message}", e)
                false
            }

            if (canRead) {
                Log.d("NativeFileViewer", "URI is readable - permission granted")
                permissionGranted = true
            } else {
                Log.e("NativeFileViewer", "URI is not readable - no permission or file not found")
                error = "Cannot access this file. Please try selecting it again from the file picker."
            }
        } catch (e: Exception) {
            Log.e("NativeFileViewer", "Error checking permissions", e)
            error = "Error accessing file: ${e.message}"
        } finally {
            isCheckingPermission = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isCheckingPermission -> {
                // Show loading while checking permissions
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            !permissionGranted -> {
                // Show error if permission not granted
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Access Denied",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error ?: "Cannot access this file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenExternal) {
                            Text("Try external app")
                        }
                    }
                }
            }

            else -> {
                // Permission granted - proceed with file display
                when {
            // Images: JPG, PNG, GIF, BMP, WEBP
            mimeType.startsWith("image/") -> {
                NativeImageViewer(
                    context = context,
                    uri = actualUri,
                    fileName = fileName,
                    onError = { error = it }
                )
            }

            // PDF files
            mimeType == "application/pdf" || fileName?.endsWith(".pdf", ignoreCase = true) == true -> {
                NativePdfViewer(
                    context = context,
                    uri = actualUri,
                    fileName = fileName,
                    onError = { error = it }
                )
            }

            // Text files: TXT, CSV, JSON, XML
            mimeType.startsWith("text/") ||
            fileName?.endsWith(".txt", ignoreCase = true) == true ||
            fileName?.endsWith(".csv", ignoreCase = true) == true ||
            fileName?.endsWith(".json", ignoreCase = true) == true ||
            fileName?.endsWith(".xml", ignoreCase = true) == true -> {
                NativeTextViewer(
                    context = context,
                    uri = actualUri,
                    fileName = fileName,
                    onError = { error = it }
                )
            }

            // Office documents: Show basic info and option to open externally
            mimeType.contains("word", ignoreCase = true) ||
            mimeType.contains("excel", ignoreCase = true) ||
            mimeType.contains("powerpoint", ignoreCase = true) ||
            fileName?.endsWith(".doc", ignoreCase = true) == true ||
            fileName?.endsWith(".docx", ignoreCase = true) == true ||
            fileName?.endsWith(".xls", ignoreCase = true) == true ||
            fileName?.endsWith(".xlsx", ignoreCase = true) == true ||
            fileName?.endsWith(".ppt", ignoreCase = true) == true ||
            fileName?.endsWith(".pptx", ignoreCase = true) == true -> {
                OfficeDocumentMessage(
                    uri = actualUri,
                    fileName = fileName,
                    mimeType = mimeType,
                    onOpenExternal = onOpenExternal
                )
            }

            // Unsupported file type
            else -> {
                UnsupportedFileMessage(
                    fileName = fileName,
                    mimeType = mimeType,
                    onOpenExternal = onOpenExternal
                )
            }
        }
            }
        }

        // Show error if any
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenExternal) {
                        Text("Open with external app")
                    }
                }
            }
        }
    }
}

/**
 * Native Image Viewer using Android's BitmapFactory
 * Now with pinch-to-zoom and double-tap-to-zoom support
 */
@Composable
private fun NativeImageViewer(
    context: Context,
    uri: Uri,
    fileName: String?,
    onError: (String) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zoom state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Load image directly from content URI using BitmapFactory
    LaunchedEffect(uri) {
        isLoading = true
        Log.d("NativeImageViewer", "Starting to load image from URI: $uri")

        try {
            val loadedBitmap = withContext(Dispatchers.IO) {
                Log.d("NativeImageViewer", "Opening input stream for image...")

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    Log.d("NativeImageViewer", "Decoding bitmap from stream...")

                    // Decode the bitmap from the input stream
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true // First, get dimensions without loading
                    }

                    // Get image dimensions
                    BitmapFactory.decodeStream(inputStream, null, options)
                    val imageWidth = options.outWidth
                    val imageHeight = options.outHeight

                    Log.d("NativeImageViewer", "Image dimensions: ${imageWidth}x${imageHeight}")

                    // Re-open stream for actual decoding (stream was consumed)
                    context.contentResolver.openInputStream(uri)?.use { newStream ->
                        // Calculate sample size for large images
                        val sampleOptions = BitmapFactory.Options().apply {
                            inSampleSize = calculateInSampleSize(imageWidth, imageHeight, 2048, 2048)
                            inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                        }

                        BitmapFactory.decodeStream(newStream, null, sampleOptions)
                    } ?: throw Exception("Failed to re-open stream for decoding")
                } ?: throw Exception("Cannot open input stream")
            }

            if (loadedBitmap != null) {
                Log.d("NativeImageViewer", "Image loaded successfully: ${loadedBitmap.width}x${loadedBitmap.height}")
                bitmap = loadedBitmap
                isLoading = false
            } else {
                throw Exception("BitmapFactory returned null")
            }

        } catch (e: Exception) {
            Log.e("NativeImageViewer", "Error loading image: ${e.message}", e)
            errorMessage = "Failed to load image: ${e.message}"
            isLoading = false
            onError(errorMessage!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading image...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            errorMessage != null -> {
                // Error already reported via onError callback
            }

            bitmap != null -> {
                // Zoomable image with pinch-to-zoom and double-tap support
                var zooming by remember { mutableStateOf(false) }

                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zooming = true
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                // Calculate max offset based on scale
                                val maxX = (size.width * (scale - 1)) / 2f
                                val maxY = (size.height * (scale - 1)) / 2f

                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)

                                // Reset offset when zoomed out to 1x
                                if (scale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // Double tap to toggle between 1x and 2x zoom
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )

                // Show zoom indicator
                if (scale > 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Show reset zoom button when zoomed
                if (scale > 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ){
                                Text("Reset Zoom", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate sample size for downsampling large images
 */
private fun calculateInSampleSize(
    imageWidth: Int,
    imageHeight: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1

    if (imageHeight > reqHeight || imageWidth > reqWidth) {
        val halfHeight = imageHeight / 2
        val halfWidth = imageWidth / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

/**
 * Native PDF Viewer using Android's PdfRenderer API
 * Available since API 21 (Android 5.0)
 */
@Composable
private fun NativePdfViewer(
    context: Context,
    uri: Uri,
    fileName: String?,
    onError: (String) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Navigation button states
    var isBackEnabled by remember { mutableStateOf(true) }
    var isNextEnabled by remember { mutableStateOf(true) }
    val debounceDuration = 400L // ms
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uri, currentPage) {
        try {
            withContext(Dispatchers.IO) {
                // Open the PDF file
                val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw Exception("Cannot open file")

                val pdfRenderer = PdfRenderer(fileDescriptor)
                pageCount = pdfRenderer.pageCount

                // Render the current page
                if (currentPage < pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(currentPage)

                    // Create high-quality bitmap (3x for better quality)
                    val bitmap = createBitmap(
                        page.width * 3,
                        page.height * 3,
                        Bitmap.Config.ARGB_8888 // Use ARGB for better quality
                    )

                    // Fill with whiteInDarkMode background
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    // Render page to bitmap with high quality
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    page.close()
                    pageBitmap = bitmap
                }

                pdfRenderer.close()
                fileDescriptor.close()
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            onError("Error loading PDF: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE)) // Light gray background instead of dark
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // PDF Page Display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp), // Add padding around the page
                    contentAlignment = Alignment.Center
                ) {
                    pageBitmap?.let { bitmap ->
                        // Show PDF with shadow for better appearance
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page ${currentPage + 1}",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Navigation Controls
                if (pageCount > 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentPage > 0 && isBackEnabled) {
                                        currentPage--
                                        isBackEnabled = false
                                        coroutineScope.launch {
                                            delay(debounceDuration)
                                            isBackEnabled = true
                                        }
                                    }
                                },
                                enabled = currentPage > 0 && isBackEnabled
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous page"
                                )
                            }

                            Text(
                                text = "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(
                                onClick = {
                                    if (currentPage < pageCount - 1 && isNextEnabled) {
                                        currentPage++
                                        isNextEnabled = false
                                        coroutineScope.launch {
                                            delay(debounceDuration)
                                            isNextEnabled = true
                                        }
                                    }
                                },
                                enabled = currentPage < pageCount - 1 && isNextEnabled
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next page"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Text File Viewer
 */
@Composable
private fun NativeTextViewer(
    context: Context,
    uri: Uri,
    fileName: String?,
    onError: (String) -> Unit
) {
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        Log.d("NativeTextViewer", "Starting to load text file from URI: $uri")
        val startTime = System.currentTimeMillis()

        try {
            // Ensure this runs on IO thread
            val content = withContext(Dispatchers.IO) {
                Log.d("NativeTextViewer", "Opening input stream...")
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file - permission denied")

                Log.d("NativeTextViewer", "Reading file content...")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()
                reader.close()

                val elapsed = System.currentTimeMillis() - startTime
                Log.d("NativeTextViewer", "File loaded in ${elapsed}ms (${text.length} chars)")
                text
            }

            // Update UI on main thread
            textContent = content
            isLoading = false

        } catch (e: SecurityException) {
            Log.e("NativeTextViewer", "SecurityException - Permission denied!", e)
            isLoading = false
            onError("Permission denied reading file. Please select the file again.")
        } catch (e: Exception) {
            Log.e("NativeTextViewer", "Error reading text file", e)
            isLoading = false
            onError("Error reading text file: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            textContent?.let { text ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Message for Office documents - Displays inside app directly
 */
@Composable
private fun OfficeDocumentMessage(
    uri: Uri,
    fileName: String?,
    mimeType: String?,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current

    // Directly show the office document viewer
    OfficeDocumentViewer(
        context = context,
        uri = uri,
        fileName = fileName,
        mimeType = mimeType,
        onError = { /* Error handling done in viewer */ },
        onOpenExternal = onOpenExternal
    )
}

/**
 * Message for unsupported files
 */
@Composable
private fun UnsupportedFileMessage(
    fileName: String?,
    mimeType: String?,
    onOpenExternal: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ Unsupported File Type",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = fileName ?: "Unknown file",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Type: ${mimeType ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenExternal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open with External App")
                }
            }
        }
    }
}

/**
 * Helper: Get MIME type from file name
 */
private fun getTypeFromFileName(fileName: String?): String {
    return when (fileName?.substringAfterLast(".", "")?.lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "webp" -> "image/webp"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "*/*"
    }
}

/**
 * Helper: Get friendly file type description
 */
private fun getFileTypeDescription(fileName: String?, mimeType: String?): String {
    return when {
        fileName?.endsWith(".docx", ignoreCase = true) == true -> "Microsoft Word Document"
        fileName?.endsWith(".doc", ignoreCase = true) == true -> "Microsoft Word Document (Legacy)"
        fileName?.endsWith(".xlsx", ignoreCase = true) == true -> "Microsoft Excel Spreadsheet"
        fileName?.endsWith(".xls", ignoreCase = true) == true -> "Microsoft Excel Spreadsheet (Legacy)"
        fileName?.endsWith(".pptx", ignoreCase = true) == true -> "Microsoft PowerPoint Presentation"
        fileName?.endsWith(".ppt", ignoreCase = true) == true -> "Microsoft PowerPoint Presentation (Legacy)"
        else -> mimeType ?: "Unknown"
    }
}

/**
 * Office Document Viewer - Displays Word/Excel/PowerPoint files inside the app
 * Extracts and displays text content from Office documents
 */
@Composable
private fun OfficeDocumentViewer(
    context: Context,
    uri: Uri,
    fileName: String?,
    mimeType: String?,
    onError: (String) -> Unit,
    onOpenExternal: () -> Unit
) {
    var documentContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        Log.d("OfficeDocumentViewer", "Starting to load office document: $fileName from URI: $uri")

        try {
            val content = withContext(Dispatchers.IO) {
                // Extract text from the document
                extractTextFromOfficeDocument(context, uri, fileName)
            }

            documentContent = content
            isLoading = false
            Log.d("OfficeDocumentViewer", "Document loaded successfully")

        } catch (e: Exception) {
            Log.e("OfficeDocumentViewer", "Error loading document", e)
            isLoading = false
            onError("Cannot display this document format. ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading ${getFileTypeDescription(fileName, mimeType)}...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fileName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            documentContent != null -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Document info header (removed "Open in App" button)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = fileName ?: "Document",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getFileTypeDescription(fileName, mimeType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Document content
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        ) {
                            Text(
                                text = documentContent!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extract text content from Office documents (Word, Excel, PowerPoint)
 * Uses Android's built-in ZipInputStream to extract text from modern Office files
 */
private fun extractTextFromOfficeDocument(
    context: Context,
    uri: Uri,
    fileName: String?
): String {
    Log.d("extractText", "Extracting text from: $fileName")

    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            when {
                // Word documents (.docx)
                fileName?.endsWith(".docx", ignoreCase = true) == true -> {
                    extractTextFromWordDocument(inputStream)
                }

                // Excel documents (.xlsx)
                fileName?.endsWith(".xlsx", ignoreCase = true) == true -> {
                    extractTextFromExcelDocument(inputStream)
                }

                // PowerPoint documents (.pptx)
                fileName?.endsWith(".pptx", ignoreCase = true) == true -> {
                    extractTextFromPowerPointDocument(inputStream)
                }

                // Legacy formats (.doc, .xls, .ppt) - binary format, harder to parse
                fileName?.endsWith(".doc", ignoreCase = true) == true ||
                fileName?.endsWith(".xls", ignoreCase = true) == true ||
                fileName?.endsWith(".ppt", ignoreCase = true) == true -> {
                    "📄 ${getFileTypeDescription(fileName, null)}\n\n" +
                    "This is a legacy Office format that requires specialized parsing.\n\n" +
                    "To view this document, please tap 'Open in App' above and choose:\n" +
                    "• Microsoft Office (Word, Excel, PowerPoint)\n" +
                    "• Google Docs/Sheets/Slides\n" +
                    "• WPS Office\n" +
                    "• Other compatible apps"
                }

                else -> {
                    "Unable to preview this document format.\n\nPlease use the 'Open in App' button to view with full formatting."
                }
            }
        } ?: throw Exception("Cannot open file stream")
    } catch (e: Exception) {
        Log.e("extractText", "Error extracting text: ${e.message}", e)
        "Error reading document: ${e.message}\n\nPlease tap 'Open in App' above to view this document in Microsoft Office, Google Docs, or another compatible app."
    }
}

/**
 * Extract text from Word document (.docx)
 * DOCX files are ZIP archives containing XML files
 */
private fun extractTextFromWordDocument(inputStream: InputStream): String {
    return try {
        val textBuilder = StringBuilder()
        val zipInputStream = ZipInputStream(inputStream)
        var entry: ZipEntry?

        // DOCX files contain the main content in word/document.xml
        while (zipInputStream.nextEntry.also { entry = it } != null) {
            if (entry?.name == "word/document.xml") {
                // Parse the XML to extract text
                val xmlParser = Xml.newPullParser()
                xmlParser.setInput(zipInputStream, "UTF-8")

                var eventType = xmlParser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        // Text content is in <w:t> tags
                        if (xmlParser.name == "t") {
                            xmlParser.next()
                            if (xmlParser.eventType == XmlPullParser.TEXT) {
                                textBuilder.append(xmlParser.text)
                            }
                        }
                        // Paragraphs
                        else if (xmlParser.name == "p") {
                            if (textBuilder.isNotEmpty() && !textBuilder.endsWith("\n\n")) {
                                textBuilder.append("\n\n")
                            }
                        }
                    }
                    eventType = xmlParser.next()
                }
                break
            }
        }
        zipInputStream.close()

        val extractedText = textBuilder.toString().trim()
        if (extractedText.isEmpty()) {
            "📝 Word Document\n\n" +
            "This document appears to be empty or contains only formatting/images.\n\n" +
            "For the best viewing experience with full formatting, images, and features, " +
            "tap 'Open in App' above."
        } else {
            "📝 ${extractedText.length} characters extracted\n\n" +
            "──────────���─────────────────\n\n" +
            extractedText + "\n\n" +
            "───────────────���────────────\n\n" +
            "Note: This is plain text extraction. For full formatting, images, and features, " +
            "tap 'Open in App' above."
        }
    } catch (e: Exception) {
        Log.e("extractTextFromWord", "Error: ${e.message}", e)
        "📝 Microsoft Word Document\n\n" +
        "Could not extract text: ${e.message}\n\n" +
        "To view this document with full formatting, tap 'Open in App' above."
    }
}

/**
 * Extract text from Excel document (.xlsx)
 * XLSX files are ZIP archives containing XML files
 */
private fun extractTextFromExcelDocument(inputStream: InputStream): String {
    return try {
        val textBuilder = StringBuilder()
        val zipInputStream = ZipInputStream(inputStream)
        var entry: ZipEntry?
        var sheetCount = 0

        // XLSX files contain sheets in xl/worksheets/sheet*.xml
        while (zipInputStream.nextEntry.also { entry = it } != null) {
            val entryName = entry?.name ?: ""
            if (entryName.startsWith("xl/worksheets/sheet") && entryName.endsWith(".xml")) {
                sheetCount++
                textBuilder.append("Sheet $sheetCount:\n")
                textBuilder.append("═══════════════════\n\n")

                // Parse the XML to extract cell values
                val xmlParser = Xml.newPullParser()
                xmlParser.setInput(zipInputStream, "UTF-8")

                var eventType = xmlParser.eventType
                var rowNum = 0
                var inValue = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (xmlParser.name) {
                                "row" -> {
                                    rowNum++
                                    if (rowNum > 1) textBuilder.append("\n")
                                }
                                "v" -> inValue = true  // Cell value
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inValue) {
                                textBuilder.append(xmlParser.text).append(" | ")
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (xmlParser.name == "v") inValue = false
                        }
                    }
                    eventType = xmlParser.next()
                }
                textBuilder.append("\n\n")
            }
        }
        zipInputStream.close()

        val extractedText = textBuilder.toString().trim()
        if (extractedText.isEmpty()) {
            "📊 Excel Spreadsheet\n\n" +
            "This spreadsheet appears to be empty or uses complex formatting.\n\n" +
            "For the best viewing experience with formulas and formatting, " +
            "tap 'Open in App' above."
        } else {
            "📊 Excel Spreadsheet Preview\n\n" +
            "────────────────────────────\n\n" +
            extractedText + "\n" +
            "────────────────────────────\n\n" +
            "Note: This shows cell values only. For formulas, charts, and full formatting, " +
            "tap 'Open in App' above."
        }
    } catch (e: Exception) {
        Log.e("extractTextFromExcel", "Error: ${e.message}", e)
        "📊 Microsoft Excel Spreadsheet\n\n" +
        "Could not extract data: ${e.message}\n\n" +
        "To view this spreadsheet with formulas and formatting, tap 'Open in App' above."
    }
}

/**
 * Extract text from PowerPoint document (.pptx)
 * PPTX files are ZIP archives containing XML files
 */
private fun extractTextFromPowerPointDocument(inputStream: InputStream): String {
    return try {
        val textBuilder = StringBuilder()
        val zipInputStream = ZipInputStream(inputStream)
        var entry: ZipEntry?
        var slideCount = 0

        // PPTX files contain slides in ppt/slides/slide*.xml
        while (zipInputStream.nextEntry.also { entry = it } != null) {
            val entryName = entry?.name ?: ""
            if (entryName.startsWith("ppt/slides/slide") && entryName.endsWith(".xml")) {
                slideCount++
                textBuilder.append("Slide $slideCount:\n")
                textBuilder.append("═══════════════════\n\n")

                // Parse the XML to extract text
                val xmlParser = Xml.newPullParser()
                xmlParser.setInput(zipInputStream, "UTF-8")

                var eventType = xmlParser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        // Text content is in <a:t> tags in PowerPoint
                        if (xmlParser.name == "t") {
                            xmlParser.next()
                            if (xmlParser.eventType == XmlPullParser.TEXT) {
                                textBuilder.append(xmlParser.text).append(" ")
                            }
                        }
                    }
                    eventType = xmlParser.next()
                }
                textBuilder.append("\n\n")
            }
        }
        zipInputStream.close()

        val extractedText = textBuilder.toString().trim()
        if (extractedText.isEmpty()) {
            "📊 PowerPoint Presentation\n\n" +
            "This presentation appears to be empty or contains only images/graphics.\n\n" +
            "For the best viewing experience with animations and formatting, " +
            "tap 'Open in App' above."
        } else {
            "📊 PowerPoint Presentation\n" +
            "$slideCount slides found\n\n" +
            "────────────────────────────\n\n" +
            extractedText + "\n" +
            "────────────────────────────\n\n" +
            "Note: This shows text content only. For animations, images, and full formatting, " +
            "tap 'Open in App' above."
        }
    } catch (e: Exception) {
        Log.e("extractTextFromPPT", "Error: ${e.message}", e)
        "📊 Microsoft PowerPoint Presentation\n\n" +
        "Could not extract content: ${e.message}\n\n" +
        "To view this presentation with animations and formatting, tap 'Open in App' above."
    }
}
