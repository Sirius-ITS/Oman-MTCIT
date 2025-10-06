package com.informatique.mtcit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.ui.base.BaseActivity
import com.informatique.mtcit.ui.screens.LoginScreen
import com.informatique.mtcit.ui.screens.MainScreen
import com.informatique.mtcit.ui.screens.defaultEnterTransition2
import com.informatique.mtcit.ui.screens.settings.SettingsScreen
import com.informatique.mtcit.ui.theme.AppTheme
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LandingActivity : BaseActivity() {

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔤 تحديد حجم الخط المسموح
        val configuration = resources.configuration
        val originalScale = configuration.fontScale
        val limitedScale = originalScale.coerceIn(0.85f, 1.15f)
        if (originalScale != limitedScale) {
            configuration.fontScale = limitedScale
            val newContext = createConfigurationContext(configuration)
            applyOverrideConfiguration(newContext.resources.configuration)
        }

        installSplashScreen()

        setContent {
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val sharedUserViewModel: SharedUserViewModel = hiltViewModel()
            val themeViewModel: ThemeViewModel = hiltViewModel()

            val lang by languageViewModel.languageFlow.collectAsState(initial = "en")
            val currentLocale = Locale(lang)
            val themeOption by themeViewModel.theme.collectAsState(initial = ThemeOption.SYSTEM_DEFAULT)

            CompositionLocalProvider(
                LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalAppLocale provides currentLocale
            ) {
                AppTheme(themeOption = themeOption) {
                    Surface {
                        val navController = rememberAnimatedNavController()
                        var startDestination by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            val token = TokenManager.getToken(applicationContext)
                            startDestination = if (token.isNullOrEmpty()) "login" else "home"
                        }

                        if (startDestination == null) {
                            SplashAnimationScreen()
                        } else {
                            AnimatedNavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            ) {
                                // 🔑 شاشة تسجيل الدخول
                                composable(
                                    "login",
                                    enterTransition = { defaultEnterTransition2() }
                                ) {
                                    val sharedUserViewModel: SharedUserViewModel =
                                        hiltViewModel(LocalContext.current as ComponentActivity)
                                    LoginScreen(navController, sharedUserViewModel)
                                }

                                // ⚙️ شاشة الإعدادات (اختيار الثيم)
                                composable(
                                    "settings_screen",
                                    enterTransition = { defaultEnterTransition2() }
                                ) {
                                    SettingsScreen(
                                        navController = navController,
                                        sharedUserViewModel = sharedUserViewModel,
                                        viewModel = themeViewModel
                                    )
                                }

                                // 🏠 الشاشة الرئيسية
                                composable(
                                    "main",
                                    enterTransition = { defaultEnterTransition2() }
                                ) {
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
        CircularProgressIndicator()
    }
}
