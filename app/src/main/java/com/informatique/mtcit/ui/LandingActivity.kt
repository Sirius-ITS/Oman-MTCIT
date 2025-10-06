package com.informatique.mtcit.ui


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.ui.base.BaseActivity
import com.informatique.mtcit.ui.screens.LoginScreen
import com.informatique.mtcit.ui.screens.MainScreen
import com.informatique.mtcit.ui.theme.AppTheme
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LandingActivity: BaseActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for proper Android 36 support
        enableEdgeToEdge()

        val configuration = resources.configuration

// ناخد قيمة fontScale من الجهاز
        val originalScale = configuration.fontScale

// نحدد الحدود اللي نسمح بيها
        val limitedScale = originalScale.coerceIn(0.85f, 1.15f)

// لو كانت القيمة برا الرينج، نعدلها
        if (originalScale != limitedScale) {
            configuration.fontScale = limitedScale
            val newContext = createConfigurationContext(configuration)
            applyOverrideConfiguration(newContext.resources.configuration)
        }


        installSplashScreen()
        setContent {
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val sharedUserViewModel: SharedUserViewModel = hiltViewModel()
            val lang by languageViewModel.languageFlow.collectAsState(initial = "en")
            val currentLocale = Locale(lang)

            CompositionLocalProvider(
                LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalAppLocale provides currentLocale
            ) {
                AppTheme {
                    // Remove Scaffold padding to allow edge-to-edge content
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navController = rememberNavController()
                        var startDestination by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            val token = TokenManager.getToken(applicationContext)
                            startDestination = if (token.isNullOrEmpty()) "login" else "main"
                        }

                        if (startDestination == null) {
                            SplashAnimationScreen()
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            ) {
                                composable("login") {
                                    val sharedUserViewModel: SharedUserViewModel =
                                        hiltViewModel(LocalContext.current as ComponentActivity)
                                    LoginScreen(navController, sharedUserViewModel)
                                }

                                composable("main") {
                                    MainScreen(sharedUserViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashAnimationScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator() // ممكن تحط لوجو هنا أو انيميشن جاهز
    }
}
