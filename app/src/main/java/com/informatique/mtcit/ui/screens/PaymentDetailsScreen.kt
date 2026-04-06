package com.informatique.mtcit.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreen(navController: NavController) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"
    val scrollState = rememberScrollState()

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.background)
    ) {
        // Enhanced Background with animated gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                extraColors.blue1,
                                extraColors.blue2,
                                extraColors.blue2.copy(alpha = 0.95f)
                            )
                        ),
                        shape = RoundedCornerShape(
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        )
                    )
            )

            // Decorative circles
            Box(
                modifier = Modifier
                    .offset(x = (-40).dp, y = 30.dp)
                    .size(120.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .offset(x = 350.dp, y = 90.dp)
                    .size(100.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isAr) "تفاصيل الدفع" else "Payment Details",
                            fontSize = 22.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4A7BA7 ),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                                )
                                .background(extraColors.iconBackBackground2)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = extraColors.iconBack2
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4A7BA7 ),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                                )
                                .background(extraColors.iconBackBackground2)
                                .clickable { navController.navigate("settings_screen") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = extraColors.iconBack2
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = extraColors.background,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ActionButtons(
                        onPayClick = { navController.navigate(NavRoutes.PaymentSuccessRoute.route) },
                        onCancelClick = { navController.popBackStack() },
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(
                                animationSpec = tween(600),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    Column {
                        // Success Header Card
                        SuccessHeaderCard()

                        Spacer(modifier = Modifier.height(12.dp))

                        // Service Details Card
                        ServiceDetailsCard()

                        Spacer(modifier = Modifier.height(12.dp))

                        // Payment Breakdown Card
                        PaymentBreakdownCard()

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessHeaderCard() {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"
    val infiniteTransition = rememberInfiniteTransition(label = "success_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp , vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon with animation
                Box(
                modifier = Modifier
                    .size(70.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF0EBD48).copy(alpha = 0.3f),
                        spotColor = Color(0xFF0EBD48).copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(extraColors.success)
                    .border(2.dp, Color(0xFF0EBD48).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF0EBD48),
                    modifier = Modifier.size(40.dp)
                )
            }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = if (isAr) "جاهز للدفع" else "Ready to Pay",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isAr) "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية" else "Issuing Temporary Registration Certificate for Ship or Marine Unit",
                    fontSize = 14.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    extraColors.iconLightBlueBackground.copy(alpha = 0.3f),
                                    extraColors.iconLightBlueBackground.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            extraColors.iconLightBlue.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                extraColors.iconLightBlueBackground,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = extraColors.iconLightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (isAr) "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية" else "Please carefully review the selected service and total amount due",
                        fontSize = 12.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceDetailsCard() {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp , vertical = 12.dp)
        ) {
            // Header with icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    extraColors.iconLightBlueBackground,
                                    extraColors.iconLightBlueBackground.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = extraColors.iconLightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (isAr) "تفاصيل الخدمة" else "Service Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                extraColors.whiteInDarkMode.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Service Name
            Text(
                text = if (isAr) "اسم الخدمة" else "Service Name",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                extraColors.iconLightBlueBackground.copy(alpha = 0.3f),
                                extraColors.iconLightBlueBackground.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        extraColors.iconLightBlue.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية" else "Inspection Request / Exceptional Inspection for Ship or Marine Unit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentBreakdownCard() {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp , vertical = 12.dp)
        ) {
            // Header with icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    extraColors.iconLightBlueBackground,
                                    extraColors.iconLightBlueBackground.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = extraColors.iconLightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (isAr) "تفاصيل المبلغ" else "Amount Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                extraColors.whiteInDarkMode.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Items
            PaymentRow(
                label = if (isAr) "المبلغ المستحق" else "Amount Due",
                value = if (isAr) "25,00 ر.ع" else "25.00 OMR"
            )

            Spacer(modifier = Modifier.height(10.dp))

            PaymentRow(
                label = if (isAr) "رسوم المعاينة الاستثنائية" else "Exceptional Inspection Fees",
                value = if (isAr) "30,00 ر.ع" else "30.00 OMR"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Total Amount Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                extraColors.iconLightBlueBackground.copy(alpha = 0.4f),
                                extraColors.iconLightBlueBackground.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        extraColors.iconLightBlue.copy(alpha = 0.3f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 18.dp , vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAr) "التكلفة الإجمالية" else "Total Cost",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) "55,00 ر.ع" else "55.00 OMR",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(
                                elevation = 0.dp,
                                shape = CircleShape,
                                ambientColor = extraColors.iconLightBlue.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        extraColors.iconLightBlueBackground,
                                        extraColors.iconLightBlueBackground.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = extraColors.iconLightBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    label: String,
    value: String,
) {
    val extraColors = LocalExtraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        extraColors.iconLightBlueBackground.copy(alpha = 0.3f),
                        extraColors.iconLightBlueBackground.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                1.dp,
                extraColors.iconLightBlue.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = extraColors.whiteInDarkMode,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ActionButtons(
    onPayClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cancel Button
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(
                    elevation = 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = extraColors.whiteInDarkMode,
            ),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                extraColors.whiteInDarkMode.copy(alpha = 0.25f)
            )
        ) {
            Text(
                text = if (isAr) "إلغاء" else "Cancel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Pay Button
        Button(
            onClick = onPayClick,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = extraColors.blue1.copy(alpha = 0.4f),
                    spotColor = extraColors.blue1.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extraColors.buttonLightBlue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "دفع" else "Pay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}