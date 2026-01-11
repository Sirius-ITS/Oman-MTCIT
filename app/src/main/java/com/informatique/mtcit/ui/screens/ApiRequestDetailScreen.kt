package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.data.model.requests.RequestDetailField
import com.informatique.mtcit.data.model.requests.RequestDetailSection
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.RequestDetailViewModel
import com.informatique.mtcit.ui.viewmodels.CertificateData
import java.util.Locale

/**
 * API Request Detail Screen - Matches ReviewStep design pattern
 * Fetches and displays request details from API with expandable sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiRequestDetailScreen(
    navController: NavController,
    requestId: Int,
    requestTypeId: Int,
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val requestDetail by viewModel.requestDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val appError by viewModel.appError.collectAsState()
    val isIssuingCertificate by viewModel.isIssuingCertificate.collectAsState()
    val certificateData by viewModel.certificateData.collectAsState()

    // Allow drawing behind system bars and make status bar transparent
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    // Fetch data on first composition
    LaunchedEffect(requestId, requestTypeId) {
        println("🔍 ApiRequestDetailScreen: Loading request $requestId (type: $requestTypeId)")
        viewModel.fetchRequestDetail(requestId, requestTypeId)
    }

    // ✅ Show certificate issuance success dialog when certificateData is available
    certificateData?.let { certData ->
        println("🎉 Showing certificate dialog: ${certData.certificationNumber}")

        val isArabic = Locale.getDefault().language == "ar"
        val items = buildList {
            add(
                com.informatique.mtcit.ui.components.SuccessDialogItem(
                    label = if (isArabic) "رقم الشهادة" else "Certificate Number",
                    value = certData.certificationNumber,
                    icon = "📄"
                )
            )
            add(
                com.informatique.mtcit.ui.components.SuccessDialogItem(
                    label = if (isArabic) "تاريخ الإصدار" else "Issued Date",
                    value = certData.issuedDate,
                    icon = "📅"
                )
            )
            if (!certData.expiryDate.isNullOrEmpty()) {
                add(
                    com.informatique.mtcit.ui.components.SuccessDialogItem(
                        label = if (isArabic) "تاريخ الانتهاء" else "Expiry Date",
                        value = certData.expiryDate,
                        icon = "⏰"
                    )
                )
            }
        }

        com.informatique.mtcit.ui.components.SuccessDialog(
            title = if (isArabic) "تم إصدار الشهادة بنجاح" else "Certificate Issued Successfully",
            items = items,
            qrCode = certData.certificationQrCode,
            onDismiss = {
                println("🔄 Dialog dismissed")
                viewModel.clearCertificateData()
            }
        )
    }

    // Calculate status bar height
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {
        // Gradient background for TopAppBar only
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp + statusBarHeight)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            extraColors.blue1,
                            extraColors.iconBlueGrey
                        )
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (Locale.getDefault().language == "ar") "تفاصيل الطلب" else "Request Details",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> {
                        LoadingState(extraColors)
                    }

                    appError != null -> {
                        ErrorState(
                            error = appError!!,
                            extraColors = extraColors,
                            onRetry = { viewModel.retry(requestId, requestTypeId) }
                        )
                    }

                    requestDetail != null -> {
                        RequestDetailContent(
                            requestDetail = requestDetail!!,
                            extraColors = extraColors,
                            navController = navController,
                            viewModel = viewModel,
                            isIssuingCertificate = isIssuingCertificate,
                            certificateData = certificateData
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(extraColors: com.informatique.mtcit.ui.theme.ExtraColors) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = extraColors.blue1)
            Text(
                text = if (Locale.getDefault().language == "ar")
                    "جاري تحميل البيانات..."
                else "Loading data...",
                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: com.informatique.mtcit.common.AppError,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    onRetry: () -> Unit
) {
    val errorMessage = when (error) {
        is com.informatique.mtcit.common.AppError.ApiError -> error.message
        is com.informatique.mtcit.common.AppError.Unauthorized -> error.message
        is com.informatique.mtcit.common.AppError.Unknown -> error.message
        else -> if (Locale.getDefault().language == "ar") "حدث خطأ غير متوق��" else "An error occurred"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = errorMessage,
                fontSize = 16.sp,
                color = extraColors.whiteInDarkMode,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.blue1
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (Locale.getDefault().language == "ar") "إعادة المحاولة" else "Retry",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RequestDetailContent(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    navController: NavController,
    viewModel: RequestDetailViewModel,
    isIssuingCertificate: Boolean,
    certificateData: CertificateData?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // Space for fixed bottom button
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ...existing items...

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Status Header Card
            item {
                StatusHeaderCard(requestDetail, extraColors)
            }

            // Status Info Messages (non-actionable feedback)
            item {
                StatusInfoMessages(requestDetail, extraColors)
            }

            // Expandable Sections (like ReviewStep)
            requestDetail.sections.forEach { section ->
                item {
                    ExpandableDataSection(section, extraColors)
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Fixed Bottom Action Buttons
        BottomActionButtons(
            requestDetail = requestDetail,
            extraColors = extraColors,
            navController = navController,
            viewModel = viewModel,
            isIssuingCertificate = isIssuingCertificate,
            certificateData = certificateData,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Fixed bottom action buttons based on request status
 */
@Composable
private fun BottomActionButtons(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    navController: NavController,
    viewModel: RequestDetailViewModel,
    isIssuingCertificate: Boolean,
    certificateData: CertificateData?,
    modifier: Modifier = Modifier
) {
    val statusId = requestDetail.status.id
    val isPaid = requestDetail.isPaid

    // Only show buttons for statuses that require action
    val shouldShowButton = when (statusId) {
        1 -> true  // Draft - Continue Editing
        2, 10 -> true  // Rejected - Submit New Request
        3, 7, 11, 12 -> true  // Accepted/Approved - Show Payment or Issue Certificate button
        13, 14 -> true  // Action Taken/Issued - Show View Certificate button
        else -> false
    }

    if (shouldShowButton) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = extraColors.background,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (statusId) {
                    1 -> {
                        // Draft - Continue Editing
                        Button(
                            onClick = { /* TODO: Navigate to transaction screen */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extraColors.blue1
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (Locale.getDefault().language == "ar")
                                    "متابعة التعديل"
                                else "Continue Editing",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    2, 10 -> {
                        // Rejected - Submit New Request
                        Button(
                            onClick = { /* TODO: Navigate to new request */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extraColors.blue1
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (Locale.getDefault().language == "ar")
                                    "تقديم طلب جديد"
                                else "Submit New Request",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    3, 7, 11, 12 -> {
                        // ✅ NEW: Check isPaid to determine which button to show
                        if (isPaid == 1) {
                            // ✅ Payment completed - Show Issue Certificate button
                            IssueCertificateButton(
                                requestDetail = requestDetail,
                                navController = navController,
                                extraColors = extraColors,
                                viewModel = viewModel,
                                isIssuingCertificate = isIssuingCertificate,
                                certificateData = certificateData
                            )
                        } else {
                            // ✅ Not paid - Show Proceed to Payment button
                            ProceedToPaymentButton(
                                requestDetail = requestDetail,
                                navController = navController,
                                extraColors = extraColors
                            )
                        }
                    }

                    13, 14 -> {
                        // ✅ NEW: Certificate Already Issued - Show View/Re-issue Certificate button
                        if (isPaid == 1) {
                            IssueCertificateButton(
                                requestDetail = requestDetail,
                                navController = navController,
                                extraColors = extraColors,
                                viewModel = viewModel,
                                isIssuingCertificate = isIssuingCertificate,
                                certificateData = certificateData,
                                isAlreadyIssued = true  // ✅ Pass flag to change button text
                            )
                        } else {
                            // Edge case: Issued but not paid (shouldn't happen normally)
                            ProceedToPaymentButton(
                                requestDetail = requestDetail,
                                navController = navController,
                                extraColors = extraColors
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ NEW: Issue Certificate Button (when isPaid == 1)
 */
@Composable
private fun IssueCertificateButton(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    navController: NavController,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    viewModel: RequestDetailViewModel,
    isIssuingCertificate: Boolean,
    certificateData: CertificateData?,
    isAlreadyIssued: Boolean = false  // ✅ NEW: Flag for already issued certificates
) {

    val buttonText = when {
        isAlreadyIssued -> {
            // Certificate already issued - show "View Certificate" text
            if (Locale.getDefault().language == "ar")
                "عرض الشهادة"
            else "View Certificate"
        }
        else -> {
            // Not yet issued - show "Issue and Display Certificate" text
            if (Locale.getDefault().language == "ar")
                "اصدار و عرض الشهادة"
            else "Issue and Display Certificate"
        }
    }

    Button(
        onClick = {
            println("🔘 Issue/View Certificate button clicked (isAlreadyIssued=$isAlreadyIssued)")
            viewModel.issueCertificate(
                requestId = requestDetail.requestId,
                requestTypeId = requestDetail.requestType.id
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = extraColors.blue1
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = !isIssuingCertificate
    ) {
        if (isIssuingCertificate) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = buttonText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * ✅ NEW: Proceed to Payment Button (when isPaid == 0)
 */
@Composable
private fun ProceedToPaymentButton(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    navController: NavController,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    // Accepted/Approved - Continue to Payment
    // ✅ Smart routing: For Temp Registration with ACCEPTED, go to Marine Unit Name first
    Button(
        onClick = {
            val statusId = requestDetail.status.id
            val requestTypeId = requestDetail.requestType.id

            // ✅ Calculate lastCompletedStep based on transaction type and status
            // Each transaction has different number of steps, so review step index varies
            val lastCompletedStep = when {
                statusId == 7 && requestTypeId == 1 -> 7  // Temp Registration ACCEPTED → Marine Unit Name step
                requestTypeId == 3 -> 3  // Issue Navigation Permit → Review step (0=PersonType, 1=MarineUnit, 2=SailingRegions, 3=SailorInfo, 4=Review)
                requestTypeId == 4 -> 2  // Renew Navigation License → Review step (0=PersonType, 1=MarineUnit, 2=Review)
                requestTypeId == 8 -> 3  // Request Inspection → Review step (0=PersonType, 1=MarineUnit, 2=InspectionDetails, 3=Review)
                else -> 8  // Other transactions (Perm Registration, Mortgage, etc.) → Payment step at index 8
            }

            println("🔍 ApiRequestDetailScreen: Navigating with lastCompletedStep=$lastCompletedStep (requestTypeId=$requestTypeId)")

            // ✅ Smart navigation with lastCompletedStep passed through URL
            val route = getTransactionRouteForPayment(
                requestTypeId = requestDetail.requestType.id,
                requestId = requestDetail.requestId,
                statusId = statusId,
                lastCompletedStep = lastCompletedStep
            )
            if (route != null) {
                navController.navigate(route)
            } else {
                // Fallback: show error or unsupported message
                println("⚠️ Payment not supported for requestTypeId: ${requestDetail.requestType.id}")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = extraColors.blue1
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (Locale.getDefault().language == "ar")
                "متابعة الدفع"
            else "Continue to Payment",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Status info messages (non-actionable cards showing status feedback)
 */
@Composable
private fun StatusInfoMessages(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    val statusId = requestDetail.status.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (statusId) {
            4, 5 -> {
                // Pending/Awaiting - Show status info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (Locale.getDefault().language == "ar")
                                "طلبك قيد المراجعة من قبل الجهات المختصة"
                            else "Your request is under review by the authorities",
                            fontSize = 14.sp,
                            color = extraColors.whiteInDarkMode,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            2, 10 -> {
                // Rejected - Show rejection reason
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (Locale.getDefault().language == "ar")
                                    "تم رفض الطلب"
                                else "Request Rejected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        }

                        requestDetail.messageDetails?.let {
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                color = extraColors.whiteInDarkMode,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            3, 7, 11, 12, 13, 14 -> {
                // Approved/Confirmed - Show success message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (Locale.getDefault().language == "ar")
                                "تم قبول الطلب بنجاح"
                            else "Request Approved Successfully",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeaderCard(
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Request Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${requestDetail.requestSerial}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode
                )

                Surface(
                    color = getStatusBackgroundColor(requestDetail.status.id),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = requestDetail.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getStatusTextColor(requestDetail.status.id),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Request Type
            DataRow(
                label = if (Locale.getDefault().language == "ar") "نوع الطلب" else "Request Type",
                value = requestDetail.requestType.name,
                extraColors = extraColors
            )
        }
    }
}

@Composable
private fun MessageCard(
    message: String?,
    messageDetails: String?,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.blue1.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = extraColors.blue1,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (message != null) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.whiteInDarkMode
                    )
                }
                if (messageDetails != null) {
                    Text(
                        text = messageDetails,
                        fontSize = 13.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Expandable section card - matches ReviewStepContent design
 */
@Composable
private fun ExpandableDataSection(
    section: RequestDetailSection,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.whiteInDarkMode
                )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        section.fields.forEach { field ->
                            RenderField(field, extraColors, 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderField(
    field: RequestDetailField,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    indentLevel: Int
) {
    val indentModifier = Modifier.padding(start = (indentLevel * 12).dp)

    when (field) {
        is RequestDetailField.SimpleField -> {
            Column(
                modifier = indentModifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = field.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                )
                Text(
                    text = field.value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )
            }
        }

        is RequestDetailField.NestedObject -> {
            Column(
                modifier = indentModifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = field.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.background.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        field.fields.forEach { nestedField ->
                            RenderField(nestedField, extraColors, 0)
                        }
                    }
                }
            }
        }

        is RequestDetailField.ArrayField -> {
            Column(
                modifier = indentModifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = field.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1
                )

                field.items.forEachIndexed { index, itemFields ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = extraColors.background.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${if (Locale.getDefault().language == "ar") "العنصر" else "Item"} ${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.blue1
                            )
                            itemFields.forEach { itemField ->
                                RenderField(itemField, extraColors, 0)
                            }
                        }
                    }

                    if (index < field.items.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// Helper functions for status colors
private fun getStatusBackgroundColor(statusId: Int): Color {
    return when (statusId) {
        1 -> Color(0xFFF5F5F5) // Draft - Gray
        2, 10 -> Color(0xFFFFE8E8) // Rejected - Red
        3, 7, 11, 12 -> Color(0xFFE8F5E9) // Confirmed/Accepted/Approved - Green
        4, 5 -> Color(0xFFFFF3E0) // Send/Pending - Orange
        6 -> Color(0xFFE3F2FD) // Scheduled - Blue
        8, 9, 15 -> Color(0xFFE8F4FD) // In Review - Blue
        13, 14 -> Color(0xFFE8F5E9) // Action Taken/Issued - Green
        16 -> Color(0xFFFFF3E0) // Waiting Inspection - Orange
        else -> Color(0xFFF0F0F0) // Unknown - Gray
    }
}

private fun getStatusTextColor(statusId: Int): Color {
    return when (statusId) {
        1 -> Color(0xFF9E9E9E) // Draft - Gray
        2, 10 -> Color(0xFFF44336) // Rejected - Red
        3, 7, 11, 12 -> Color(0xFF4CAF50) // Confirmed/Accepted/Approved - Green
        4, 5 -> Color(0xFFFF9800) // Send/Pending - Orange
        6 -> Color(0xFF2196F3) // Scheduled - Blue
        8, 9, 15 -> Color(0xFF4A90E2) // In Review - Blue
        13, 14 -> Color(0xFF4CAF50) // Action Taken/Issued - Green
        16 -> Color(0xFFFF9800) // Waiting Inspection - Orange
        else -> Color(0xFF757575) // Unknown - Gray
    }
}

/**
 * Map request type ID to transaction route with requestId for payment resumption
 * This reuses the existing transaction screens and payment strategy flow
 * Uses TransactionType enum to ensure correct mapping
 */
private fun getTransactionRouteForPayment(requestTypeId: Int, requestId: Int, statusId: Int, lastCompletedStep: Int): String? {
    // Map API request type ID to TransactionType
    val transactionType = TransactionType.fromTypeId(requestTypeId)

    return when (transactionType) {
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE ->
            NavRoutes.ShipRegistrationRoute.createRouteWithResume(requestId.toString(), lastCompletedStep)

        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE ->
            "${NavRoutes.PermanentRegistrationRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.ISSUE_NAVIGATION_PERMIT ->
            "${NavRoutes.IssueNavigationPermitRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.RENEW_NAVIGATION_PERMIT ->
            "${NavRoutes.RenewNavigationPermitRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.MORTGAGE_CERTIFICATE ->
            "${NavRoutes.MortgageCertificateRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.RELEASE_MORTGAGE ->
            "${NavRoutes.ReleaseMortgageRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.CANCEL_PERMANENT_REGISTRATION ->
            "${NavRoutes.ChangeNameOfShipOrUnitRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"

        TransactionType.REQUEST_FOR_INSPECTION ->
            NavRoutes.RequestForInspection.createRouteWithResume(requestId.toString(), lastCompletedStep)

        else -> null // Unsupported or no payment flow for this transaction type
    }
}
