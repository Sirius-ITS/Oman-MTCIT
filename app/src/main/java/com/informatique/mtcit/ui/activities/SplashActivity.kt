package com.informatique.mtcit.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.LandingActivity
import com.informatique.mtcit.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Only use modern splash screen on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { !isReady }
        }

        super.onCreate(savedInstanceState)

        // For Android 9 and older, use Compose-based splash screen
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setContent {
                AppTheme {
                    LegacySplashScreen()
                }
            }
        }

        // Perform initialization tasks
        lifecycleScope.launch {
            performInitialization()
            navigateToMainActivity()
        }
    }

    @Composable
    private fun LegacySplashScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = "MTCIT Logo",
                modifier = Modifier.size(120.dp)
            )
        }
    }

    private suspend fun performInitialization() {
        // Simulate loading time for demo - remove or adjust in production
        delay(2500)

        // Add your actual initialization logic here:
        // - Load user preferences
        // - Initialize database
        // - Check authentication status
        // - Load configuration
        // - Prepare app data

        // Mark as ready
        isReady = true
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, LandingActivity::class.java)
        startActivity(intent)
        finish() // Close splash activity
    }
}
