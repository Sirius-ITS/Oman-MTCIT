package com.informatique.mtcit.ui.components

import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.data.datastorehelper.TokenManager
import java.net.HttpURLConnection
import java.net.URL

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
                    // File Content - Use NativeFileViewer for internal display
                    // Pass HTTP URL as string, or convert local URI
                    if (isHttpUrl) {
                        // For HTTP URLs, pass directly as string
                        NativeFileViewerWithUrl(
                            url = fileUri,
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
    modifier: Modifier = Modifier
) {
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Loading) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasMainFrameError by remember { mutableStateOf(false) }
    var moduleLoadErrors by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    // Get token from TokenManager
    var authToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authToken = TokenManager.getAccessToken(context)
        if (authToken != null) {
            android.util.Log.d("FileViewerDialog", "🔑 Loaded auth token: ${authToken!!.substring(0, 30.coerceAtMost(authToken!!.length))}...")
        } else {
            android.util.Log.w("FileViewerDialog", "⚠️ No auth token available")
        }
    }

    if (authToken == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Store user agent string as remember to avoid recomposition issues
        val customUserAgent = remember {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        AndroidView(
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true

                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        setInitialScale(1)

                        userAgentString = customUserAgent

                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true

                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NARROW_COLUMNS

                        loadsImagesAutomatically = true
                        javaScriptCanOpenWindowsAutomatically = true

                        defaultTextEncodingName = "utf-8"
                        mediaPlaybackRequiresUserGesture = false

                        blockNetworkImage = false
                        blockNetworkLoads = false
                    }

                    setBackgroundColor(android.graphics.Color.WHITE)
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.url?.let { uri ->
                                if (uri.host == "oman.isfpegypt.com") {
                                    try {
                                        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
                                        connection.setRequestProperty("Authorization", "Bearer $authToken")
                                        connection.setRequestProperty("User-Agent", customUserAgent)

                                        val inputStream = connection.inputStream
                                        val mimeType = connection.contentType?.split(";")?.first() ?: "text/html"

                                        return WebResourceResponse(
                                            mimeType,
                                            "UTF-8",
                                            inputStream
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("WebView", "Failed to intercept: ${e.message}")
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                        override fun onReceivedSslError(
                            view: android.webkit.WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            val errorMessage = when (error?.primaryError) {
                                android.net.http.SslError.SSL_EXPIRED -> "SSL certificate expired"
                                android.net.http.SslError.SSL_IDMISMATCH -> "SSL hostname mismatch"
                                android.net.http.SslError.SSL_NOTYETVALID -> "SSL certificate not yet valid"
                                android.net.http.SslError.SSL_UNTRUSTED -> "SSL certificate not trusted"
                                android.net.http.SslError.SSL_DATE_INVALID -> "SSL certificate date invalid"
                                android.net.http.SslError.SSL_INVALID -> "SSL certificate invalid"
                                else -> "Unknown SSL error"
                            }
                            android.util.Log.w("WebView", "⚠️ SSL Error: $errorMessage for ${error?.url}")

                            // For development/testing: Proceed anyway
                            // For production: You might want to show an error and call handler?.cancel()
                            handler?.proceed()
                        }

                        override fun onReceivedError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)

                            val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                error?.errorCode
                            } else null

                            val description = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                error?.description?.toString()
                            } else null

                            val isMainFrame = request?.isForMainFrame == true
                            val requestUrl = request?.url?.toString() ?: "unknown"

                            // Check if it's a JS module error (chunk-*.js files)
                            val isModuleError = requestUrl.contains("chunk-") && requestUrl.endsWith(".js")

                            if (isMainFrame) {
                                android.util.Log.e("WebView", "❌ MAIN FRAME Error: $description")
                                hasMainFrameError = true
                                loadingState = LoadingState.Error(
                                    description ?: "Failed to load content"
                                )
                            } else if (isModuleError) {
                                moduleLoadErrors++
                                android.util.Log.w("WebView", "⚠️ Module load failed (non-critical): $requestUrl")
                            } else {
                                android.util.Log.d("WebView", "ℹ️ Sub-resource error (ignored): $requestUrl")
                            }
                        }

                        override fun onReceivedHttpError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)

                            val isMainFrame = request?.isForMainFrame == true
                            val statusCode = errorResponse?.statusCode ?: 0
                            val requestUrl = request?.url?.toString() ?: "unknown"

                            if (isMainFrame && statusCode >= 400) {
                                android.util.Log.e("WebView", "❌ HTTP Error $statusCode for main frame")
                                hasMainFrameError = true
                                loadingState = LoadingState.Error(
                                    "HTTP $statusCode: ${errorResponse?.reasonPhrase}"
                                )
                            } else {
                                android.util.Log.d("WebView", "ℹ️ HTTP $statusCode for: $requestUrl (non-critical)")
                            }
                        }

                        override fun onPageStarted(
                            view: android.webkit.WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            hasMainFrameError = false
                            moduleLoadErrors = 0
                            loadingState = LoadingState.Loading
                            android.util.Log.d("WebView", "📄 Loading started: $url")
                        }
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("WebView", "✅ Page finished: $url")

                            // Check if we're on Keycloak login page (not an error, just needs authentication)
                            val isKeycloakPage = url?.contains("omankeycloak.isfpegypt.com") == true

                            // Mark as success if no main frame errors
                            if (!hasMainFrameError) {
                                loadingState = LoadingState.Success(
                                    hasModuleErrors = moduleLoadErrors > 0
                                )

                                if (moduleLoadErrors > 0) {
                                    android.util.Log.w("WebView", "⚠️ Page loaded with $moduleLoadErrors module errors (non-critical)")
                                }
                            }

                            // Skip CSS injection for Keycloak login page
                            if (isKeycloakPage) {
                                android.util.Log.d("WebView", "🔐 Keycloak page detected - skipping CSS injection")
                                return
                            }

                            // Inject CSS after a longer delay to allow dynamic content and JS to fully load
                            view?.postDelayed({
                                view.evaluateJavascript("""
                                    (function() {
                                        try {
                                            // Remove existing viewport
                                            var existingMeta = document.querySelector('meta[name="viewport"]');
                                            if (existingMeta) {
                                                existingMeta.remove();
                                            }
                                            
                                            // Add proper viewport
                                            var meta = document.createElement('meta');
                                            meta.name = 'viewport';
                                            meta.content = 'width=device-width, initial-scale=0.5, minimum-scale=0.3, maximum-scale=3.0, user-scalable=yes';
                                            document.head.appendChild(meta);
                                            
                                            // Inject CSS for better rendering
                                            var style = document.createElement('style');
                                            style.textContent = `
                                                * {
                                                    box-sizing: border-box;
                                                }
                                                
                                                html, body {
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    width: 100% !important;
                                                    overflow-x: auto !important;
                                                    -webkit-text-size-adjust: none !important;
                                                }
                                                
                                                body {
                                                    padding: 8px !important;
                                                    background: white !important;
                                                }
                                                
                                                table {
                                                    width: auto !important;
                                                    max-width: none !important;
                                                    border-collapse: collapse !important;
                                                    table-layout: auto !important;
                                                    font-size: 11px !important;
                                                }
                                                
                                                td, th {
                                                    padding: 4px 6px !important;
                                                    white-space: nowrap !important;
                                                    overflow: visible !important;
                                                    border: 1px solid #ddd !important;
                                                    font-size: 11px !important;
                                                }
                                                
                                                img {
                                                    max-width: 100% !important;
                                                    height: auto !important;
                                                }
                                                
                                                p, div:not(table) {
                                                    word-wrap: break-word !important;
                                                    overflow-wrap: break-word !important;
                                                }
                                            `;
                                            document.head.appendChild(style);
                                            
                                            // Force reflow
                                            document.body.style.display = 'none';
                                            document.body.offsetHeight;
                                            document.body.style.display = '';
                                            
                                            return 'success';
                                        } catch(e) {
                                            return 'error: ' + e.message;
                                        }
                                    })();
                                """.trimIndent()) { result ->
                                    android.util.Log.d("WebView", "💉 CSS injection result: $result")
                                    view.invalidate()
                                    view.requestLayout()
                                }
                            }, 1500) // Increased delay to 1.5 seconds for dynamic content
                        }

                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            return false
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadProgress = newProgress
                        }

                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val level = when (it.messageLevel()) {
                                    android.webkit.ConsoleMessage.MessageLevel.ERROR -> "❌ ERROR"
                                    android.webkit.ConsoleMessage.MessageLevel.WARNING -> "⚠️ WARNING"
                                    android.webkit.ConsoleMessage.MessageLevel.LOG -> "📝 LOG"
                                    else -> "ℹ️ INFO"
                                }

                                // Only log important console messages
                                if (it.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                    // Check if it's a module import error (non-critical)
                                    val isModuleError = it.message()?.contains("Failed to fetch dynamically imported module") == true

                                    if (isModuleError) {
                                        android.util.Log.w("WebView-Console", "⚠️ Module error (non-critical): ${it.message()}")
                                    } else {
                                        android.util.Log.e("WebView-Console", "$level: ${it.message()} [${it.sourceId()}:${it.lineNumber()}]")
                                    }
                                } else if (it.messageLevel() != android.webkit.ConsoleMessage.MessageLevel.LOG) {
                                    android.util.Log.d("WebView-Console", "$level: ${it.message()}")
                                }
                            }
                            return true
                        }

                        override fun onJsAlert(
                            view: android.webkit.WebView?,
                            url: String?,
                            message: String?,
                            result: android.webkit.JsResult?
                        ): Boolean {
                            android.util.Log.d("WebView", "🔔 JS Alert: $message")
                            result?.confirm()
                            return true
                        }
                    }

                    android.util.Log.d("WebView", "🌐 Loading URL: $url")

                    authToken?.let { token ->
                        android.util.Log.d("WebView", "🔑 Loaded auth token: ${token.substring(0, 30.coerceAtMost(token.length))}...")

                        // Inject authentication cookie
                        try {
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Set auth token as cookie for the domain
                            val domain = "oman.isfpegypt.com"
                            val cookie = "Authorization=Bearer $token; Domain=$domain; Path=/; Secure"
                            cookieManager.setCookie("https://$domain", cookie)
                            cookieManager.flush()

                            android.util.Log.d("WebView", "🍪 Cookie set for $domain")
                        } catch (e: Exception) {
                            android.util.Log.e("WebView", "❌ Failed to set cookie: ${e.message}")
                        }
                    } ?: run {
                        android.util.Log.w("WebView", "⚠️ No auth token available - WebView may require login")
                    }

                    // Load URL
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { webView ->
                webView.requestLayout()
            }
        )

        // Loading overlay
        if (loadingState is LoadingState.Loading && loadProgress < 100) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading... $loadProgress%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Success with warning banner (optional - only if you want to show module errors)
        if (loadingState is LoadingState.Success && (loadingState as LoadingState.Success).hasModuleErrors) {
            // Optional: Show a small warning banner at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = "⚠️ Some features may not work properly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Error state (only for critical main frame errors)
        if (loadingState is LoadingState.Error) {
            val errorMessage = (loadingState as LoadingState.Error).message

            // Don't show error for auth redirects - let them load
            if (!errorMessage.contains("Session expired", ignoreCase = true) &&
                !errorMessage.contains("Authentication failed", ignoreCase = true)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load certificate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Updated LoadingState to track module errors
sealed class LoadingState {
    object Loading : LoadingState()
    data class Success(val hasModuleErrors: Boolean = false) : LoadingState()
    data class Error(val message: String) : LoadingState()
}
