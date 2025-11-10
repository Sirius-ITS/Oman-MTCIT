package com.informatique.mtcit.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.informatique.mtcit.navigation.NavHost
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.ui.base.BaseActivity
import com.informatique.mtcit.ui.theme.AppTheme
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.LandingViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import com.informatique.mtcit.ui.providers.LocalCategories
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LandingActivity: BaseActivity() {

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
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val landingViewModel: LandingViewModel = hiltViewModel()

            val lang by languageViewModel.languageFlow.collectAsState(initial = "ar")
            val currentLocale = Locale(lang)
            val themeOption by themeViewModel.theme.collectAsState(initial = ThemeOption.SYSTEM_DEFAULT)
            val categories by landingViewModel.categories.collectAsState()

            CompositionLocalProvider(
                LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalAppLocale provides currentLocale,
                LocalCategories provides categories
            ) {
                AppTheme(themeOption = themeOption) {
                    // Remove Scaffold padding to allow edge-to-edge content
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavHost(themeViewModel = themeViewModel)
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

// Animation functions for navigation transitions (Navigation Compose 2.8.0+)
fun defaultEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 300)
    ) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(durationMillis = 300)
    )
}

fun defaultExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 300)
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(durationMillis = 300)
    )
}
