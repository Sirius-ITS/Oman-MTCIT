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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSuccessScreen(navController: NavController) {
    val extraColors = LocalExtraColors.current
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
        // Background gradient header
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
                            text = "إيصال دفع",
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
                                    color = Color(0xFF4A7BA7),
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
                                contentDescription = "رجوع",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // تحميل الإيصال
                        OutlinedButton(
                            onClick = { /* Download */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = extraColors.whiteInDarkMode,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                extraColors.whiteInDarkMode.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "تحميل الإيصال",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // طباعة الإيصال
                        Button(
                            onClick = {
                                navController.navigate("mainCategoriesScreen")
                            },
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
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "طباعة الإيصال",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(
                            animationSpec = tween(600),
                            initialOffsetY = { it / 4 }
                        )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Success Header
                    SuccessHeader()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Receipt Card
                    ReceiptCard(
                        serviceName = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية",
                        paymentNumber = "283648",
                        amount = "55,00",
                        date = "10/08/2025",
                        vesselName = "BAMBOUS DAMRICUS",
                        recipient = "Mohammed AL-Amri"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Message
                    InfoMessage()

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SuccessHeader() {
    val extraColors = LocalExtraColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
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
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Success Icon
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

                Text(
                    text = "تمت عملية الدفع بنجاح",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReceiptCard(
    serviceName: String,
    paymentNumber: String,
    amount: String,
    date: String,
    vesselName: String,
    recipient: String
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = extraColors.iconLightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "إيصال دفع",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
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
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Service Name
            ReceiptInfoRow(
                label = "اسم الخدمة",
                value = serviceName
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Number
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "رقم الدفع",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                    )
                    Text(
                        text = paymentNumber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = extraColors.whiteInDarkMode,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
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
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المبلغ الإجمالي",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$amount ر.ع",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Additional Info
            ReceiptInfoRow(label = "التاريخ", value = date)

            Spacer(modifier = Modifier.height(8.dp))

            ReceiptInfoRow(label = "اسم السفينة", value = vesselName)

            Spacer(modifier = Modifier.height(8.dp))

            ReceiptInfoRow(label = "المستلم", value = recipient)
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String
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
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun InfoMessage() {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0EBD48).copy(alpha = 0.15f),
                        Color(0xFF0EBD48).copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFF0EBD48).copy(alpha = 0.25f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0EBD48).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF0EBD48),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "تم إصدار هذا الإيصال من الجهة المختصة لتوثيق استلام المبلغ وفق الإجراءات المالية المعتمدة",
                fontSize = 12.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.75f),
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}