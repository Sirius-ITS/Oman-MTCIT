package com.informatique.mtcit.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // For Android 11 and older, use Compose-based splash screen
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setContent {
                AppTheme {
                    ModernSplashScreen()
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
    private fun ModernSplashScreen() {
        // Omani flag colors
        val omanBlue = Color(0xFF1568B8) // RGB(0.08, 0.42, 0.72)
        val omanRed = Color(0xFFB52133) // RGB(0.71, 0.13, 0.20)

        // Animation states
        var stripesOffset by remember { mutableFloatStateOf(-400f) } // ✅ Start further left
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

        // Trigger animations on appear
        LaunchedEffect(Unit) {
            delay(100)
            stripesOffset = 600f // ✅ End further right (travel distance = 1000dp)
            particleOpacity = 1f

            delay(300)
            logoOffset = 0f
            logoOpacity = 1f

            delay(700)
            textOpacity = 1f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Diagonal stripes background
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(particleAnimOpacity)
                    .rotate(25f) // ✅ Rotate the entire canvas to create diagonal stripes
            ) {
                val stripeWidth = 150.dp.toPx()

                for (i in 0..7) {
                    val color = if (i % 2 == 0) {
                        omanBlue.copy(alpha = 0.08f)
                    } else {
                        omanRed.copy(alpha = 0.08f)
                    }

                    val xOffset = stripesAnimOffset + (i * 120).toFloat()

                    // Draw vertical rectangles - canvas rotation makes them diagonal
                    drawRect(
                        color = color,
                        topLeft = Offset(xOffset, -size.height * 0.5f),
                        size = androidx.compose.ui.geometry.Size(stripeWidth, size.height * 2.5f)
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
                                // National emblem - temporarily use a placeholder
                                Image(
                                    painter = painterResource(id = R.drawable.oman_flag),
                                    contentDescription = "National Emblem of Oman",
                                    modifier = Modifier.size(75.dp)
                                )


                                // MTCIT text
                                Text(
                                    text = "MTCIT",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF262626),
                                    letterSpacing = 4.sp,
                                    textAlign = TextAlign.Center
                                )

                                // Accent line
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

                        // Dual color dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                    // Modern loader bars
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
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

    private suspend fun performInitialization() {
        // 3-second delay to match iOS (3.0 seconds)
        delay(3000)
        isReady = true
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, LandingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
