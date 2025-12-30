package com.informatique.mtcit.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

/**
 * OAuth WebView Screen for Keycloak authentication
 *
 * This screen:
 * 1. Opens Keycloak authorization URL
 * 2. User authenticates in the WebView
 * 3. Intercepts redirect to extract authorization code
 * 4. Returns the code to the caller
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthWebViewScreen(
    navController: NavController,
    authUrl: String,
    redirectUri: String,
    onAuthCodeReceived: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل الدخول") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
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
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("OAuthWebView", "📄 Page started loading: $url")
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("OAuthWebView", "✅ Page finished loading: $url")
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                Log.d("OAuthWebView", "🔗 Intercepting URL: $url")

                                // Check if this is the redirect URI with authorization code
                                if (url.startsWith(redirectUri)) {
                                    Log.d("OAuthWebView", "✅ Redirect URI detected!")

                                    // Extract authorization code from URL
                                    val uri = android.net.Uri.parse(url)
                                    val code = uri.getQueryParameter("code")

                                    if (code != null) {
                                        Log.d("OAuthWebView", "✅ Authorization code extracted: $code")
                                        onAuthCodeReceived(code)
                                        return true
                                    } else {
                                        val error = uri.getQueryParameter("error")
                                        val errorDescription = uri.getQueryParameter("error_description")
                                        Log.e("OAuthWebView", "❌ OAuth error: $error - $errorDescription")
                                        loadError = errorDescription ?: error ?: "Unknown error"
                                    }
                                }

                                return false
                            }
                        }

                        loadUrl(authUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            loadError?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "حدث خطأ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { navController.popBackStack() }) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }
}

