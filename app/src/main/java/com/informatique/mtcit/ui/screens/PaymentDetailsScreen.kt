//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDetailsScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(extraColors.background)
//    ) {
//        // Background gradient header
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(180.dp)
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            extraColors.blue1,
//                            extraColors.blue2
//                        )
//                    )
//                )
//        )
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//        ) {
//            // Top Bar
//            TopBar(
//                title = "تفاصيل الدفع",
//                onBackClick = { navController.popBackStack() }
//            )
//
//            // Content
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//                    .padding(horizontal = 20.dp)
//            ) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Success Icon Card
//                SuccessIconCard()
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Title Card
//                TitleCard(
//                    title = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية"
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Info Message
//                InfoMessageCard(
//                    message = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية، ثم المتابعة باختيار وسيلة الدفع المفضلة لديكم"
//                )
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Payment Details Card
//                PaymentDetailsCard()
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Action Buttons
//                ActionButtons(
//                    onPayClick = { /* Handle payment */ },
//                    onCancelClick = { navController.popBackStack() }
//                )
//
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun TopBar(
//    title: String,
//    onBackClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical = 16.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        IconButton(
//            onClick = onBackClick,
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.2f))
//        ) {
//            Icon(
//                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                contentDescription = "رجوع",
//                tint = Color.White
//            )
//        }
//
//        Text(
//            text = title,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Medium,
//            color = Color.White,
//            modifier = Modifier.weight(1f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(40.dp))
//    }
//}
//
//@Composable
//private fun SuccessIconCard() {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier.fillMaxWidth(),
//        contentAlignment = Alignment.Center
//    ) {
//        Box(
//            modifier = Modifier
//                .size(80.dp)
//                .clip(CircleShape)
//                .background(extraColors.cardBackground)
//                .border(3.dp, extraColors.blue1.copy(alpha = 0.3f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.CheckCircle,
//                contentDescription = null,
//                tint = extraColors.blue1,
//                modifier = Modifier.size(48.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun TitleCard(title: String) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Text(
//            text = title,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Medium,
//            color = extraColors.whiteInDarkMode,
//            textAlign = TextAlign.Center,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            lineHeight = 24.sp
//        )
//    }
//}
//
//@Composable
//private fun InfoMessageCard(message: String) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.blue1.copy(alpha = 0.1f)
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Info,
//                contentDescription = null,
//                tint = extraColors.blue1,
//                modifier = Modifier.size(24.dp)
//            )
//            Text(
//                text = message,
//                fontSize = 13.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
//                lineHeight = 20.sp,
//                modifier = Modifier.weight(1f)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentDetailsCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Service Name
//            PaymentDetailRow(
//                label = "اسم الخدمة",
//                value = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية"
//            )
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            // Required Payment
//            PaymentDetailRow(
//                label = "المبلغ الواجب دفعه",
//                value = "55 ريال عماني",
//                valueColor = extraColors.blue1,
//                isBold = true
//            )
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            // Due Amount
//            PaymentDetailRow(
//                label = "المبلغ المستحق",
//                value = "25,000"
//            )
//
//            // Fees Drawing
//            PaymentDetailRow(
//                label = "الرسوم الرقم لطلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
//                value = "30,000"
//            )
//
//            // Total Amount
//            PaymentDetailRow(
//                label = "رسوم الخدمة",
//                value = "الرسوم الرقم لطلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية"
//            )
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            // Total
//            PaymentDetailRow(
//                label = "التكلفة الكلية",
//                value = "55,000",
//                valueColor = extraColors.blue1,
//                isBold = true,
//                isLarge = true
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentDetailRow(
//    label: String,
//    value: String,
//    valueColor: Color? = null,
//    isBold: Boolean = false,
//    isLarge: Boolean = false
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.Top
//    ) {
//        Text(
//            text = label,
//            fontSize = if (isLarge) 15.sp else 13.sp,
//            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
//            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//            modifier = Modifier.weight(1f),
//            lineHeight = 20.sp
//        )
//
//        Spacer(modifier = Modifier.width(16.dp))
//
//        Text(
//            text = value,
//            fontSize = if (isLarge) 16.sp else 14.sp,
//            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
//            color = valueColor ?: extraColors.whiteInDarkMode,
//            textAlign = TextAlign.End,
//            lineHeight = 20.sp
//        )
//    }
//}
//
//@Composable
//private fun ActionButtons(
//    onPayClick: () -> Unit,
//    onCancelClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Cancel Button
//        OutlinedButton(
//            onClick = onCancelClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(52.dp),
//            shape = RoundedCornerShape(14.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode
//            ),
//            border = androidx.compose.foundation.BorderStroke(
//                1.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.3f)
//            )
//        ) {
//            Text(
//                text = "إلغاء",
//                fontSize = 15.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
//
//        // Pay Button
//        Button(
//            onClick = onPayClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(52.dp),
//            shape = RoundedCornerShape(14.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.blue1,
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.buttonElevation(
//                defaultElevation = 0.dp,
//                pressedElevation = 0.dp
//            )
//        ) {
//            Text(
//                text = "دفع",
//                fontSize = 15.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//}
//
//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.Receipt
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.components.localizedApp
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDetailsScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(extraColors.background)
//    ) {
//        // Background gradient header with wave
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(240.dp)
//        ) {
//            // Gradient
//            Box(
//                modifier = Modifier
//                    .matchParentSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                extraColors.blue1,
//                                extraColors.blue2
//                            )
//                        )
//                    ),
//            )
//
//            // Wave overlay
//            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
//                val w = size.width
//                val h = size.height
//                val path = androidx.compose.ui.graphics.Path().apply {
//                    moveTo(0f, h * 0.65f)
//                    quadraticBezierTo(
//                        x1 = w * 0.5f,
//                        y1 = h * 0.45f,
//                        x2 = w,
//                        y2 = h * 0.55f
//                    )
//                    lineTo(w, h)
//                    lineTo(0f, h)
//                    close()
//                }
//                drawPath(path = path, color = Color.White.copy(alpha = 0.06f))
//            }
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//        ) {
//            // Top Bar
//            TopBar(
//                title = "تفاصيل الدفع",
//                onBackClick = { navController.popBackStack() }
//            )
//
//            // Content
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//            ) {
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Success Icon with message
//                SuccessHeaderSection()
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Main Content Card with shadow
//                MainContentCard()
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Action Buttons
//                ActionButtons(
//                    onPayClick = { /* Handle payment */ },
//                    onCancelClick = { navController.popBackStack() }
//                )
//
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun TopBar(
//    title: String,
//    onBackClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical = 12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(42.dp)
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.2f))
//                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "رجوع",
//                    tint = Color.White,
//                    modifier = Modifier.size(22.dp)
//                )
//            }
//        }
//
//        Text(
//            text = title,
//            fontSize = 19.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = Color.White,
//            modifier = Modifier.weight(1f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(42.dp))
//    }
//}
//
//@Composable
//private fun SuccessHeaderSection() {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Success Icon
//        Box(
//            modifier = Modifier
//                .size(90.dp)
//                .shadow(
//                    elevation = 8.dp,
//                    shape = CircleShape,
//                    ambientColor = extraColors.blue1.copy(alpha = 0.3f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.3f)
//                )
//                .clip(CircleShape)
//                .background(Color.White)
//                .border(4.dp, extraColors.blue1.copy(alpha = 0.2f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.CheckCircle,
//                contentDescription = null,
//                tint = extraColors.blue1,
//                modifier = Modifier.size(50.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Title
//        Text(
//            text = "إصدار شهادة تسجيل مؤقتة للسفينة أو\nالوحدة البحرية",
//            fontSize = 17.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = extraColors.whiteInDarkMode,
//            textAlign = TextAlign.Center,
//            lineHeight = 24.sp
//        )
//    }
//}
//
//@Composable
//private fun MainContentCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp),
//        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            // Info Banner
//            InfoBanner()
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Payment Details Section
//            PaymentDetailsSection()
//        }
//    }
//}
//
//@Composable
//private fun InfoBanner() {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(16.dp))
//            .background(extraColors.blue1.copy(alpha = 0.08f))
//            .border(
//                1.dp,
//                extraColors.blue1.copy(alpha = 0.15f),
//                RoundedCornerShape(16.dp)
//            )
//            .padding(16.dp)
//    ) {
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(12.dp),
//            verticalAlignment = Alignment.Top
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(36.dp)
//                    .clip(CircleShape)
//                    .background(extraColors.blue1.copy(alpha = 0.15f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Info,
//                    contentDescription = null,
//                    tint = extraColors.blue1,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//
//            Text(
//                text = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية، ثم المتابعة باختيار وسيلة الدفع المفضلة لديكم",
//                fontSize = 13.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.85f),
//                lineHeight = 20.sp,
//                modifier = Modifier.weight(1f)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentDetailsSection() {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        // Section Header
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Default.Receipt,
//                contentDescription = null,
//                tint = extraColors.blue1,
//                modifier = Modifier.size(22.dp)
//            )
//            Text(
//                text = "تفاصيل الدفع",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = extraColors.whiteInDarkMode
//            )
//        }
//
//        Spacer(modifier = Modifier.height(4.dp))
//
//        // Service Name Box
//        ServiceNameBox(
//            serviceName = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية"
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Payment breakdown
//        PaymentBreakdownSection()
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Total Amount Card
//        TotalAmountCard(amount = "55,000")
//    }
//}
//
//@Composable
//private fun ServiceNameBox(serviceName: String) {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(14.dp))
//            .background(extraColors.cardBackground2.copy(alpha = 0.3f))
//            .border(
//                1.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                RoundedCornerShape(14.dp)
//            )
//            .padding(16.dp)
//    ) {
//        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//            Text(
//                text = "اسم الخدمة",
//                fontSize = 12.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
//                fontWeight = FontWeight.Medium
//            )
//            Text(
//                text = serviceName,
//                fontSize = 14.sp,
//                color = extraColors.whiteInDarkMode,
//                lineHeight = 20.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentBreakdownSection() {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        PaymentItemRow(
//            label = "المبلغ المستحق",
//            value = "25,000",
//            isHighlighted = false
//        )
//
//        PaymentItemRow(
//            label = "الرسوم الرقم لطلب معاينة",
//            value = "30,000",
//            isHighlighted = false
//        )
//
//        HorizontalDivider(
//            color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//            thickness = 1.dp,
//            modifier = Modifier.padding(vertical = 4.dp)
//        )
//
//        PaymentItemRow(
//            label = "المبلغ الواجب دفعه",
//            value = "55 ريال عماني",
//            isHighlighted = true
//        )
//    }
//}
//
//@Composable
//private fun PaymentItemRow(
//    label: String,
//    value: String,
//    isHighlighted: Boolean
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = label,
//            fontSize = if (isHighlighted) 14.sp else 13.sp,
//            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
//            color = extraColors.whiteInDarkMode.copy(
//                alpha = if (isHighlighted) 0.9f else 0.6f
//            ),
//            modifier = Modifier.weight(1f)
//        )
//
//        Text(
//            text = value,
//            fontSize = if (isHighlighted) 15.sp else 14.sp,
//            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
//            color = if (isHighlighted) extraColors.blue1 else extraColors.whiteInDarkMode,
//            textAlign = TextAlign.End
//        )
//    }
//}
//
//@Composable
//private fun TotalAmountCard(amount: String) {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(16.dp))
//            .background(
//                Brush.horizontalGradient(
//                    colors = listOf(
//                        extraColors.blue1.copy(alpha = 0.12f),
//                        extraColors.blue2.copy(alpha = 0.08f)
//                    )
//                )
//            )
//            .border(
//                1.5.dp,
//                extraColors.blue1.copy(alpha = 0.3f),
//                RoundedCornerShape(16.dp)
//            )
//            .padding(20.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                Text(
//                    text = "التكلفة الإجمالية",
//                    fontSize = 13.sp,
//                    color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
//                    fontWeight = FontWeight.Medium
//                )
//                Text(
//                    text = amount,
//                    fontSize = 24.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = extraColors.blue1
//                )
//            }
//
//            Box(
//                modifier = Modifier
//                    .size(50.dp)
//                    .clip(CircleShape)
//                    .background(extraColors.blue1.copy(alpha = 0.15f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Receipt,
//                    contentDescription = null,
//                    tint = extraColors.blue1,
//                    modifier = Modifier.size(26.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun ActionButtons(
//    onPayClick: () -> Unit,
//    onCancelClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Cancel Button
//        OutlinedButton(
//            onClick = onCancelClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode
//            ),
//            border = androidx.compose.foundation.BorderStroke(
//                1.5.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.3f)
//            )
//        ) {
//            Text(
//                text = "إلغاء",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//
//        // Pay Button with gradient
//        Button(
//            onClick = onPayClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp)
//                .shadow(
//                    elevation = 8.dp,
//                    shape = RoundedCornerShape(16.dp),
//                    ambientColor = extraColors.blue1.copy(alpha = 0.3f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.3f)
//                ),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.blue1,
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.buttonElevation(
//                defaultElevation = 0.dp,
//                pressedElevation = 0.dp
//            )
//        ) {
//            Text(
//                text = "متابعة للدفع",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}

//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.components.localizedApp
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDetailsScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(extraColors.background)
//    ) {
//        // Background gradient header with wave
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(220.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .matchParentSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                extraColors.blue1,
//                                extraColors.blue2
//                            )
//                        ),
//                        shape = RoundedCornerShape(
//                            bottomStart = 36.dp,
//                            bottomEnd = 36.dp)
//                    )
//            )
//
//            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
//                val w = size.width
//                val h = size.height
//                val path = androidx.compose.ui.graphics.Path().apply {
//                    moveTo(0f, h * 0.72f)
//                    quadraticBezierTo(w * 0.5f, h * 0.5f, w, h * 0.62f)
//                    lineTo(w, h)
//                    lineTo(0f, h)
//                    close()
//                }
//                drawPath(path = path, color = Color.White.copy(alpha = 0.06f))
//            }
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//        ) {
//            // Top Bar
//            TopBar(
//                title = "تفاصيل الدفع",
//                onBackClick = { navController.popBackStack() }
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//                    .padding(horizontal = 20.dp)
//            ) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Success Icon
//                SuccessIcon()
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Service Title Card
//                ServiceTitleCard(
//                    title = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية"
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Info Message
//                InfoMessage(
//                    message = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية، ثم المتابعة باختيار وسيلة الدفع المفضلة لديكم"
//                )
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Payment Details Card
//                PaymentDetailsCard()
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Action Buttons
//                ActionButtons(
//                    onPayClick = { /* Handle payment */ },
//                    onCancelClick = { navController.popBackStack() }
//                )
//
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun TopBar(
//    title: String,
//    onBackClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical = 16.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.2f))
//                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "رجوع",
//                    tint = Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//
//        Text(
//            text = title,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = Color.White,
//            modifier = Modifier.weight(1f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(40.dp))
//    }
//}
//
//@Composable
//private fun SuccessIcon() {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier.fillMaxWidth(),
//        contentAlignment = Alignment.Center
//    ) {
//        Box(
//            modifier = Modifier
//                .size(85.dp)
//                .shadow(
//                    elevation = 12.dp,
//                    shape = CircleShape,
//                    ambientColor = extraColors.blue1.copy(alpha = 0.25f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.25f)
//                )
//                .clip(CircleShape)
//                .background(Color.White)
//                .border(3.dp, extraColors.blue1.copy(alpha = 0.2f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.CheckCircle,
//                contentDescription = null,
//                tint = extraColors.blue1,
//                modifier = Modifier.size(48.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun ServiceTitleCard(title: String) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(18.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Text(
//            text = title,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Medium,
//            color = extraColors.whiteInDarkMode,
//            textAlign = TextAlign.Center,
//            lineHeight = 24.sp,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        )
//    }
//}
//
//@Composable
//private fun InfoMessage(message: String) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.blue1.copy(alpha = 0.08f)
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .border(
//                    1.dp,
//                    extraColors.blue1.copy(alpha = 0.15f),
//                    RoundedCornerShape(16.dp)
//                )
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp),
//            verticalAlignment = Alignment.Top
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(36.dp)
//                    .clip(CircleShape)
//                    .background(extraColors.blue1.copy(alpha = 0.15f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Info,
//                    contentDescription = null,
//                    tint = extraColors.blue1,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//
//            Text(
//                text = message,
//                fontSize = 13.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.85f),
//                lineHeight = 20.sp,
//                modifier = Modifier.weight(1f)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentDetailsCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Header
//            Text(
//                text = "تفاصيل الدفع",
//                fontSize = 17.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = extraColors.whiteInDarkMode
//            )
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            // Service Name Section
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text(
//                    text = "اسم الخدمة",
//                    fontSize = 13.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
//                )
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clip(RoundedCornerShape(12.dp))
//                        .background(extraColors.cardBackground2.copy(alpha = 0.3f))
//                        .padding(14.dp)
//                ) {
//                    Text(
//                        text = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
//                        fontSize = 14.sp,
//                        color = extraColors.whiteInDarkMode,
//                        lineHeight = 20.sp
//                    )
//                }
//            }
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            // Payment Breakdown
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                PaymentRow(
//                    label = "المبلغ الواجب دفعه",
//                    value = "55 ريال عماني",
//                    isHighlighted = true
//                )
//
//                PaymentRow(
//                    label = "المبلغ المستحق",
//                    value = "25,000"
//                )
//
//                PaymentRow(
//                    label = "الرسوم الرقم لطلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
//                    value = "30,000"
//                )
//
//                PaymentRow(
//                    label = "رسوم الخدمة",
//                    value = "الرسوم الرقم لطلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية"
//                )
//            }
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.15f),
//                thickness = 1.5.dp
//            )
//
//            // Total Amount
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(14.dp))
//                    .background(
//                        Brush.horizontalGradient(
//                            colors = listOf(
//                                extraColors.blue1.copy(alpha = 0.1f),
//                                extraColors.blue2.copy(alpha = 0.08f)
//                            )
//                        )
//                    )
//                    .border(
//                        1.5.dp,
//                        extraColors.blue1.copy(alpha = 0.25f),
//                        RoundedCornerShape(14.dp)
//                    )
//                    .padding(16.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "التكلفة الكلية",
//                        fontSize = 15.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = extraColors.whiteInDarkMode
//                    )
//
//                    Text(
//                        text = "55,000",
//                        fontSize = 22.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = extraColors.blue1
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun PaymentRow(
//    label: String,
//    value: String,
//    isHighlighted: Boolean = false
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.Top
//    ) {
//        Text(
//            text = label,
//            fontSize = if (isHighlighted) 14.sp else 13.sp,
//            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
//            color = extraColors.whiteInDarkMode.copy(
//                alpha = if (isHighlighted) 0.9f else 0.7f
//            ),
//            modifier = Modifier.weight(1f),
//            lineHeight = 19.sp
//        )
//
//        Spacer(modifier = Modifier.width(16.dp))
//
//        Text(
//            text = value,
//            fontSize = if (isHighlighted) 15.sp else 14.sp,
//            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
//            color = if (isHighlighted) extraColors.blue1 else extraColors.whiteInDarkMode,
//            textAlign = TextAlign.End,
//            lineHeight = 19.sp
//        )
//    }
//}
//
//@Composable
//private fun ActionButtons(
//    onPayClick: () -> Unit,
//    onCancelClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Cancel Button
//        OutlinedButton(
//            onClick = onCancelClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode
//            ),
//            border = androidx.compose.foundation.BorderStroke(
//                1.5.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.3f)
//            )
//        ) {
//            Text(
//                text = "إلغاء",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//
//        // Pay Button
//        Button(
//            onClick = onPayClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp)
//                .shadow(
//                    elevation = 8.dp,
//                    shape = RoundedCornerShape(16.dp),
//                    ambientColor = extraColors.blue1.copy(alpha = 0.3f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.3f)
//                ),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.blue1,
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.buttonElevation(
//                defaultElevation = 0.dp,
//                pressedElevation = 0.dp
//            )
//        ) {
//            Text(
//                text = "دفع",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}

//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Description
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.Payment
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.components.localizedApp
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDetailsScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(extraColors.background)
//    ) {
//        // Background gradient header with wave
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(280.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .matchParentSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                extraColors.blue1,
//                                extraColors.blue2
//                            )
//                        ),
//                        shape = RoundedCornerShape(
//                            bottomStart = 24.dp,
//                            bottomEnd = 24.dp
//                        )
//                    )
//            )
//
//            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
//                val w = size.width
//                val h = size.height
//                val path = androidx.compose.ui.graphics.Path().apply {
//                    moveTo(0f, h * 0.72f)
//                    quadraticBezierTo(w * 0.5f, h * 0.5f, w, h * 0.62f)
//                    lineTo(w, h)
//                    lineTo(0f, h)
//                    close()
//                }
//                drawPath(path = path, color = Color.White.copy(alpha = 0.06f))
//            }
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//        ) {
//            // Top Bar
//            TopBar(
//                title = "تفاصيل الدفع",
//                onBackClick = { navController.popBackStack() }
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//                    .padding(horizontal = 16.dp)
//            ) {
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Success Header Card (Combined)
//                SuccessHeaderCard()
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Service Details Card
//                ServiceDetailsCard()
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Payment Breakdown Card
//                PaymentBreakdownCard()
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Action Buttons
//                ActionButtons(
//                    onPayClick = { /* Handle payment */ },
//                    onCancelClick = { navController.popBackStack() }
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun TopBar(
//    title: String,
//    onBackClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical= 16.dp),
//    verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.2f))
//                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "رجوع",
//                    tint = Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//
//        Text(
//            text = title,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = Color.White,
//            modifier = Modifier.weight(1f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(40.dp))
//    }
//}
//
//@Composable
//private fun SuccessHeaderCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Success Icon
//            Box(
//                modifier = Modifier
//                    .size(70.dp)
//                    .shadow(
//                        elevation = 8.dp,
//                        shape = CircleShape,
//                        ambientColor = Color(0xFF0EBD48).copy(alpha = 0.3f),
//                        spotColor = Color(0xFF0EBD48).copy(alpha = 0.3f)
//                    )
//                    .clip(CircleShape)
//                    .background(Color(0xFF0EBD48).copy(alpha = 0.15f))
//                    .border(2.dp, Color(0xFF0EBD48).copy(alpha = 0.3f), CircleShape),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.CheckCircle,
//                    contentDescription = null,
//                    tint = Color(0xFF0EBD48),
//                    modifier = Modifier.size(40.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Title
//            Text(
//                text = "جاهز للدفع",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                color = extraColors.whiteInDarkMode
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            Text(
//                text = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية",
//                fontSize = 14.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//                textAlign = TextAlign.Center,
//                lineHeight = 20.sp
//            )
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Info Banner
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(14.dp))
//                    .background(extraColors.blue1.copy(alpha = 0.08f))
//                    .border(
//                        1.dp,
//                        extraColors.blue1.copy(alpha = 0.15f),
//                        RoundedCornerShape(14.dp)
//                    )
//                    .padding(14.dp),
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.Top
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Info,
//                    contentDescription = null,
//                    tint = extraColors.blue1,
//                    modifier = Modifier.size(20.dp)
//                )
//
//                Text(
//                    text = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية",
//                    fontSize = 12.sp,
//                    color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
//                    lineHeight = 18.sp,
//                    modifier = Modifier.weight(1f)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun ServiceDetailsCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(extraColors.blue1.copy(alpha = 0.12f)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Description,
//                        contentDescription = null,
//                        tint = extraColors.blue1,
//                        modifier = Modifier.size(22.dp)
//                    )
//                }
//
//                Text(
//                    text = "تفاصيل الخدمة",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = extraColors.whiteInDarkMode
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Service Name
//            Text(
//                text = "اسم الخدمة",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium,
//                color = extraColors.whiteInDarkMode,
//                lineHeight = 20.sp
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentBreakdownCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(extraColors.blue1.copy(alpha = 0.12f)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Payment,
//                        contentDescription = null,
//                        tint = extraColors.blue1,
//                        modifier = Modifier.size(22.dp)
//                    )
//                }
//
//                Text(
//                    text = "تفاصيل المبلغ",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = extraColors.whiteInDarkMode
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Payment Items
//            PaymentRow(
//                label = "المبلغ المستحق",
//                value = "25,00"
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            PaymentRow(
//                label = "رسوم المعاينة الاستثنائية",
//                value = "30,00"
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.15f),
//                thickness = 1.dp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Required Payment (highlighted)
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(12.dp))
//                    .background(extraColors.blue1.copy(alpha = 0.08f))
//                    .padding(14.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "المبلغ الواجب دفعه",
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = extraColors.whiteInDarkMode
//                )
//
//                Text(
//                    text = "55 ريال عماني",
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = extraColors.blue1
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
////            HorizontalDivider(
////                color = extraColors.whiteInDarkMode.copy(alpha = 0.15f),
////                thickness = 1.5.dp
////            )
//
////            Spacer(modifier = Modifier.height(16.dp))
////
////            // Total Amount
////            Box(
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .clip(RoundedCornerShape(14.dp))
////                    .background(
////                        Brush.horizontalGradient(
////                            colors = listOf(
////                                extraColors.blue1.copy(alpha = 0.15f),
////                                extraColors.blue2.copy(alpha = 0.1f)
////                            )
////                        )
////                    )
////                    .border(
////                        2.dp,
////                        extraColors.blue1.copy(alpha = 0.3f),
////                        RoundedCornerShape(14.dp)
////                    )
////                    .padding(18.dp)
////            ) {
////                Row(
////                    modifier = Modifier.fillMaxWidth(),
////                    horizontalArrangement = Arrangement.SpaceBetween,
////                    verticalAlignment = Alignment.CenterVertically
////                ) {
////                    Column {
////                        Text(
////                            text = "التكلفة الإجمالية",
////                            fontSize = 13.sp,
////                            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
////                        )
////                        Spacer(modifier = Modifier.height(4.dp))
////                        Text(
////                            text = "55,000",
////                            fontSize = 26.sp,
////                            fontWeight = FontWeight.Bold,
////                            color = extraColors.blue1
////                        )
////                    }
////
////                    Box(
////                        modifier = Modifier
////                            .size(50.dp)
////                            .clip(CircleShape)
////                            .background(extraColors.blue1.copy(alpha = 0.2f)),
////                        contentAlignment = Alignment.Center
////                    ) {
////                        Icon(
////                            imageVector = Icons.Default.Payment,
////                            contentDescription = null,
////                            tint = extraColors.blue1,
////                            modifier = Modifier.size(26.dp)
////                        )
////                    }
////                }
////            }
//        }
//    }
//}
//
//@Composable
//private fun PaymentRow(
//    label: String,
//    value: String
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = label,
//            fontSize = 13.sp,
//            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//            modifier = Modifier.weight(1f)
//        )
//
//        Text(
//            text = value,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Medium,
//            color = extraColors.whiteInDarkMode,
//            textAlign = TextAlign.End
//        )
//    }
//}
//
//@Composable
//private fun ActionButtons(
//    onPayClick: () -> Unit,
//    onCancelClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        verticalAlignment = Alignment.Bottom
//    ) {
//        // Cancel Button
//        OutlinedButton(
//            onClick = onCancelClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode
//            ),
//            border = androidx.compose.foundation.BorderStroke(
//                1.5.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.3f)
//            )
//        ) {
//            Text(
//                text = "إلغاء",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//
//        // Pay Button
//        Button(
//            onClick = onPayClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp)
//                .shadow(
//                    elevation = 8.dp,
//                    shape = RoundedCornerShape(16.dp),
//                    ambientColor = extraColors.blue1.copy(alpha = 0.3f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.3f)
//                ),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.blue1,
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.buttonElevation(
//                defaultElevation = 0.dp,
//                pressedElevation = 0.dp
//            )
//        ) {
//            Text(
//                text = "دفع",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}
//
//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Description
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.Payment
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDetailsScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    val scrollState = rememberScrollState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(extraColors.background)
//    ) {
//        // Background gradient header with wave
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(210.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .matchParentSize()
//                    .background(
//                        Brush.verticalGradient(
//                            colors = listOf(
//                                extraColors.blue1,
//                                extraColors.blue2
//                            )
//                        ),
//                        shape = RoundedCornerShape(
//                            bottomStart = 26.dp,
//                            bottomEnd = 26.dp
//                        )
//                    )
//            )
//
////            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
////                val w = size.width
////                val h = size.height
////                val path = androidx.compose.ui.graphics.Path().apply {
////                    moveTo(0f, h * 0.72f)
////                    quadraticBezierTo(w * 0.5f, h * 0.5f, w, h * 0.62f)
////                    lineTo(w, h)
////                    lineTo(0f, h)
////                    close()
////                }
////                drawPath(path = path, color = Color.White.copy(alpha = 0.06f))
////            }
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .statusBarsPadding()
//        ) {
//            // Top Bar
//            TopBar(
//                title = "تفاصيل الدفع",
//                onBackClick = { navController.popBackStack() }
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//                    .padding(horizontal = 16.dp)
//            ) {
//                // Success Header Card (Combined)
//                SuccessHeaderCard()
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Service Details Card
//                ServiceDetailsCard()
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Payment Breakdown Card
//                PaymentBreakdownCard()
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Action Buttons
//                ActionButtons(
//                    onPayClick = { /* Handle payment */ },
//                    onCancelClick = { navController.popBackStack() }
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun TopBar(
//    title: String,
//    onBackClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical = 16.dp),
//    verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.2f))
//                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "رجوع",
//                    tint = Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//
//        Text(
//            text = title,
//            fontSize = 18.sp,
//            fontWeight = FontWeight.SemiBold,
//            color = Color.White,
//            modifier = Modifier.weight(1f),
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.size(40.dp))
//    }
//}
//
//@Composable
//private fun SuccessHeaderCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Success Icon
//            Box(
//                modifier = Modifier
//                    .size(70.dp)
//                    .shadow(
//                        elevation = 8.dp,
//                        shape = CircleShape,
//                        ambientColor = Color(0xFF0EBD48).copy(alpha = 0.3f),
//                        spotColor = Color(0xFF0EBD48).copy(alpha = 0.3f)
//                    )
//                    .clip(CircleShape)
//                    .background(extraColors.success)
//                    .border(2.dp, Color(0xFF0EBD48).copy(alpha = 0.3f), CircleShape),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.CheckCircle,
//                    contentDescription = null,
//                    tint = Color(0xFF0EBD48),
//                    modifier = Modifier.size(40.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Title
//            Text(
//                text = "جاهز للدفع",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Normal,
//                color = extraColors.whiteInDarkMode
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            Text(
//                text = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية",
//                fontSize = 14.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//                textAlign = TextAlign.Center,
//                lineHeight = 20.sp
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Info Banner
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(14.dp))
//                    .background(extraColors.cardBackground2.copy(alpha = 0.1f))
//                    .border(
//                        1.dp,
//                        extraColors.blue1.copy(alpha = 0.15f),
//                        RoundedCornerShape(14.dp)
//                    )
//                    .padding(8.dp),
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Info,
//                    contentDescription = null,
//                    tint = extraColors.iconLightBlue,
//                    modifier = Modifier.size(20.dp)
//                )
//
//                Text(
//                    text = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية",
//                    fontSize = 12.sp,
//                    color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//                    lineHeight = 18.sp,
//                    modifier = Modifier.weight(1f)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun ServiceDetailsCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(extraColors.iconLightBlueBackground),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Description,
//                        contentDescription = null,
//                        tint = extraColors.iconLightBlue,
//                        modifier = Modifier.size(22.dp)
//                    )
//                }
//
//                Text(
//                    text = "تفاصيل الخدمة",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = extraColors.whiteInDarkMode
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Service Name
//            Text(
//                text = "اسم الخدمة",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium,
//                color = extraColors.whiteInDarkMode,
//                lineHeight = 20.sp
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentBreakdownCard() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(extraColors.iconLightBlueBackground),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Payment,
//                        contentDescription = null,
//                        tint = extraColors.iconLightBlue,
//                        modifier = Modifier.size(22.dp)
//                    )
//                }
//
//                Text(
//                    text = "تفاصيل المبلغ",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = extraColors.whiteInDarkMode
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
//                thickness = 1.dp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Payment Items
//            PaymentRow(
//                label = "المبلغ المستحق",
//                value = "25,00"
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            PaymentRow(
//                label = "رسوم المعاينة الاستثنائية",
//                value = "30,00"
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//            HorizontalDivider(
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.15f),
//                thickness = 1.dp
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(14.dp))
//                    .background(extraColors.cardBackground2.copy(alpha = 0.1f))
//                    .border(
//                        1.dp,
//                        extraColors.blue1.copy(alpha = 0.3f),
//                        RoundedCornerShape(14.dp)
//                    )
//                    .padding(16.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column {
//                        Text(
//                            text = "التكلفة الإجمالية",
//                            fontSize = 13.sp,
//                            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(
//                            text = "55,00 ر.ع",
//                            fontSize = 26.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = extraColors.whiteInDarkMode
//                        )
//                    }
//
//                    Box(
//                        modifier = Modifier
//                            .size(50.dp)
//                            .clip(CircleShape)
//                            .background(extraColors.iconLightBlueBackground),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Payment,
//                            contentDescription = null,
//                            tint = extraColors.iconLightBlue,
//                            modifier = Modifier.size(26.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun PaymentRow(
//    label: String,
//    value: String
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = label,
//            fontSize = 13.sp,
//            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//            modifier = Modifier.weight(1f)
//        )
//
//        Text(
//            text = value,
//            fontSize = 15.sp,
//            fontWeight = FontWeight.Medium,
//            color = extraColors.whiteInDarkMode,
//            textAlign = TextAlign.End
//        )
//    }
//}
//
//@Composable
//private fun ActionButtons(
//    onPayClick: () -> Unit,
//    onCancelClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Cancel Button
//        OutlinedButton(
//            onClick = onCancelClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode,
//
//            ),
//            border = androidx.compose.foundation.BorderStroke(
//                1.5.dp,
//                extraColors.whiteInDarkMode.copy(alpha = 0.3f)
//            )
//        ) {
//            Text(
//                text = "إلغاء",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//
//        // Pay Button
//        Button(
//            onClick = onPayClick,
//            modifier = Modifier
//                .weight(1f)
//                .height(54.dp)
//                .shadow(
//                    elevation = 8.dp,
//                    shape = RoundedCornerShape(16.dp),
//                    ambientColor = extraColors.blue1.copy(alpha = 0.3f),
//                    spotColor = extraColors.blue1.copy(alpha = 0.3f)
//                ),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.buttonLightBlue,
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.buttonElevation(
//                defaultElevation = 0.dp,
//                pressedElevation = 0.dp
//            )
//        ) {
//            Text(
//                text = "دفع",
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreen(navController: NavController) {
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
                            text = "تفاصيل الدفع",
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
private fun TopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp).padding(top = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = 0.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.1f)
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun SuccessHeaderCard() {
    val extraColors = LocalExtraColors.current

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
                    text = "جاهز للدفع",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "إصدار شهادة تسجيل مؤقتة للسفينة أو الوحدة البحرية",
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
                        text = "يرجى مراجعة الخدمة المختارة وإجمالي المبلغ المستحق بعناية",
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
                    text = "تفاصيل الخدمة",
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
                text = "اسم الخدمة",
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
                    text = "طلب معاينة / معاينة استثنائية للسفينة أو الوحدة البحرية",
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
                    text = "تفاصيل المبلغ",
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
                label = "المبلغ المستحق",
                value = "25,00 ر.ع"
            )

            Spacer(modifier = Modifier.height(10.dp))

            PaymentRow(
                label = "رسوم المعاينة الاستثنائية",
                value = "30,00 ر.ع"
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
                            text = "التكلفة الإجمالية",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "55,00 ر.ع",
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
                text = "إلغاء",
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
                    text = "دفع",
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