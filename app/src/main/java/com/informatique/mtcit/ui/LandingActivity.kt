package com.informatique.mtcit.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.informatique.mtcit.R
import com.informatique.mtcit.navigation.NavHost
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.navigation.NavigationManagerImpl
import com.informatique.mtcit.ui.base.BaseActivity
import com.informatique.mtcit.ui.theme.AppTheme
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.LandingViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import com.informatique.mtcit.ui.providers.LocalCategories
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LandingActivity: BaseActivity() {

    @Inject
    lateinit var navigationManager: NavigationManagerImpl

    // Apply a safe fontScale before any resources are accessed to avoid
    // "getResources() or getAssets() has already been called" when calling
    // applyOverrideConfiguration later in lifecycle. Doing it here ensures
    // the adjusted context is attached early.
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        val originalScale = configuration.fontScale
        val limitedScale = originalScale.coerceIn(0.85f, 1f)

        if (originalScale != limitedScale) {
            configuration.fontScale = limitedScale
            val wrapped = newBase.createConfigurationContext(configuration)
            super.attachBaseContext(wrapped)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for proper Android 36 support
        enableEdgeToEdge()

        setContent {
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val landingViewModel: LandingViewModel = hiltViewModel()

            val lang by languageViewModel.languageFlow.collectAsState(initial = "ar")
            val currentLocale = Locale(lang)
            val themeOption by themeViewModel.theme.collectAsState(initial = ThemeOption.SYSTEM_DEFAULT)
            val categories by landingViewModel.categories.collectAsState()

            // State to control splash visibility
            var showSplash by remember { mutableStateOf(true) }

            CompositionLocalProvider(
                LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalAppLocale provides currentLocale,
                LocalCategories provides categories
            ) {
                AppTheme(themeOption = themeOption) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main content (always rendered but hidden behind splash)
                        AnimatedVisibility(
                            visible = !showSplash,
                            enter = fadeIn(tween(400)),
                            exit = fadeOut(tween(400))
                        ) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                NavHost(
                                    themeViewModel = themeViewModel,
                                    navigationManager = navigationManager
                                )
                            }
                        }

                        // Splash screen overlay
                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(tween(400))
                        ) {
                            ModernSplashScreen(onSplashFinished = { showSplash = false })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernSplashScreen(onSplashFinished: () -> Unit) {
        // Omani flag colors
        val omanBlue = Color(0xFF1568B8)
        val omanRed = Color(0xFFB52133)

        // Animation states - matching iOS values
        var stripesOffset by remember { mutableFloatStateOf(-200f) }
        var particleOpacity by remember { mutableFloatStateOf(0f) }
        var logoOffset by remember { mutableFloatStateOf(30f) }
        var logoOpacity by remember { mutableFloatStateOf(0f) }
        var textOpacity by remember { mutableFloatStateOf(0f) }

        // Stripes animation
        val stripesAnimOffset by animateFloatAsState(
            targetValue = stripesOffset,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            label = "stripes"
        )

        // Particle opacity animation
        val particleAnimOpacity by animateFloatAsState(
            targetValue = particleOpacity,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "particle"
        )

        // Logo drop animation
        val logoAnimOffset by animateFloatAsState(
            targetValue = logoOffset,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessLow
            ),
            label = "logoOffset"
        )

        val logoAnimOpacity by animateFloatAsState(
            targetValue = logoOpacity,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessLow
            ),
            label = "logoOpacity"
        )

        // Text fade animation
        val textAnimOpacity by animateFloatAsState(
            targetValue = textOpacity,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "textOpacity"
        )

        // Trigger animations
        LaunchedEffect(Unit) {
            delay(100)
            stripesOffset = 1000f  // Absolute maximum - increased from 900f
            particleOpacity = 1f

            delay(300)
            logoOffset = 0f
            logoOpacity = 1f

            delay(700)
            textOpacity = 1f

            // Wait for full 3 seconds total, then hide splash
            delay(1900) // 100+300+700+1900 = 3000ms
            onSplashFinished()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Diagonal stripes background - density-aware maximum extension
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(particleAnimOpacity)
                    .rotate(25f)
            ) {
                // Calculate screen-aware parameters
                val screenWidth = size.width
                val screenHeight = size.height
                val screenDiagonal = kotlin.math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight)

                // Dynamic stripe width based on screen size (proportional to screen width)
                val stripeWidth = (screenWidth * 0.12f).coerceIn(120.dp.toPx(), 180.dp.toPx())

                // Dynamic stripe spacing based on screen width
                val stripeSpacing = (screenWidth * 0.10f).coerceIn(100.dp.toPx(), 150.dp.toPx())

                // Dynamic stripe count based on screen size
                val stripeCount = ((screenWidth + screenHeight) / stripeSpacing).toInt().coerceIn(12, 18)

                // Draw stripes with screen-aware positioning
                for (i in 0 until stripeCount) {
                    val color = if (i % 2 == 0) {
                        omanBlue.copy(alpha = 0.08f)
                    } else {
                        omanRed.copy(alpha = 0.08f)
                    }

                    // Screen-aware offset calculation
                    val startOffset = -screenWidth * 0.35f  // Start 35% before left edge
                    val xOffset = stripesAnimOffset + (i * stripeSpacing) + startOffset

                    // Draw rectangles with screen-proportional height
                    drawRect(
                        color = color,
                        topLeft = Offset(xOffset, -screenHeight * 0.5f),
                        size = androidx.compose.ui.geometry.Size(stripeWidth, screenHeight * 3.0f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Main logo area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(35.dp),
                    modifier = Modifier.offset(y = logoAnimOffset.dp)
                ) {
                    // Logo card with shadows
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.alpha(logoAnimOpacity)
                    ) {
                        // Shadow layers
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .offset(x = (-8).dp, y = 8.dp)
                                .background(
                                    omanRed.copy(alpha = 0.1f),
                                    RoundedCornerShape(30.dp)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .offset(x = 8.dp, y = (-8).dp)
                                .background(
                                    omanBlue.copy(alpha = 0.1f),
                                    RoundedCornerShape(30.dp)
                                )
                        )

                        // Main logo card
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    spotColor = Color.Black.copy(alpha = 0.1f)
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFAFAFB)
                                        )
                                    ),
                                    shape = RoundedCornerShape(30.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Gradient border
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                omanBlue.copy(alpha = 0.3f),
                                                omanRed.copy(alpha = 0.3f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(30.dp)
                                    )
                                    .padding(2.dp)
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(28.dp)
                                    )
                            )

                            // Logo content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.oman_flag),
                                    contentDescription = "National Emblem of Oman",
                                    modifier = Modifier.size(75.dp)
                                )

                                Text(
                                    text = "MTCIT",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF262626),
                                    letterSpacing = 4.sp,
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(3.dp)
                                        .background(
                                            omanRed,
                                            RoundedCornerShape(1.5.dp)
                                        )
                                )
                            }
                        }
                    }

                    // Title section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 40.dp)
                            .alpha(textAnimOpacity)
                    ) {
                        Text(
                            text = "وزارة النقل والاتصالات وتقنية المعلومات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(omanBlue, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(omanRed, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(omanBlue, CircleShape)
                            )
                        }

                        Text(
                            text = "OMAN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF808080),
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))

                // Bottom section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .padding(bottom = 80.dp)
                        .alpha(textAnimOpacity)
                ) {
                    AnimatedLoaderBars(omanBlue, omanRed)

                    Text(
                        text = "سلطنة عُمان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF4D4D4D),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    private fun AnimatedLoaderBars(omanBlue: Color, omanRed: Color) {
        val infiniteTransition = rememberInfiniteTransition(label = "loader")

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (i in 0..4) {
                val animatedOpacity by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * 150)
                    ),
                    label = "bar_$i"
                )

                Box(
                    modifier = Modifier
                        .width(25.dp)
                        .height(3.dp)
                        .alpha(animatedOpacity)
                        .background(
                            color = if (i < 3) omanBlue else omanRed,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
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
