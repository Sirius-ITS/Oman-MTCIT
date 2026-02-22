package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.informatique.mtcit.ui.components.FileViewerDialog
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.RequestDetailViewModel
import com.informatique.mtcit.ui.viewmodels.CertificateData
import kotlinx.coroutines.launch
import java.util.Locale

// =====================================================================
// 🔧 CERTIFICATE VIEWER CONFIGURATION
// =====================================================================
// Toggle between WebView (in-app) and External Browser for certificate viewing
//
// 🌐 EXTERNAL BROWSER (USE_EXTERNAL_BROWSER_FOR_CERTIFICATES = true):
//    - Opens certificate URLs in the device's default browser
//    - Pros: Uses the full browser capabilities, more stable for complex web pages
//    - Cons: User leaves the app
//
// 📱 WEBVIEW DIALOG (USE_EXTERNAL_BROWSER_FOR_CERTIFICATES = false):
//    - Opens certificate in an in-app WebView dialog
//    - Pros: User stays within the app, seamless UX
//    - Cons: May have authentication/session issues with complex web flows
//
// 👉 HOW TO SWITCH:
//    1. Change the constant below to 'true' for external browser
//    2. Change it to 'false' for in-app WebView (default)
//    3. Rebuild the app
//
// No other code changes needed! The app automatically uses the configured method.
// =====================================================================
private const val USE_EXTERNAL_BROWSER_FOR_CERTIFICATES = true
// =====================================================================

/**
 * API Request Detail Screen - Matches ReviewStep design pattern
 * Fetches and displays request details from API with expandable sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
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
    val fileViewerState by viewModel.fileViewerState.collectAsState()

    // Allow drawing behind system bars and make status bar transparent
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            @Suppress("DEPRECATION")
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    // ✅ Check user role and fetch data
    LaunchedEffect(requestId, requestTypeId) {
        val isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
        println("👷 ApiRequestDetailScreen: User is engineer: $isEngineer")
        println("🔍 ApiRequestDetailScreen: Loading request $requestId (type: $requestTypeId, isEngineer: $isEngineer)")
        viewModel.fetchRequestDetail(requestId, requestTypeId, isEngineer)
    }

    // ✅ NEW: Handle navigation to login when refresh token fails
    val shouldNavigateToLogin by viewModel.shouldNavigateToLogin.collectAsState()
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            println("🔑 ApiRequestDetailScreen: Auto-navigating to login - refresh token failed")
            navController.navigate(com.informatique.mtcit.navigation.NavRoutes.OAuthWebViewRoute.route)
            viewModel.resetNavigationTrigger()
        }
    }

    // ✅ NEW: Observe login completion to reload data
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle

        val observer = androidx.lifecycle.Observer<Boolean> { loginCompleted ->
            if (loginCompleted == true) {
                println("✅ ApiRequestDetailScreen: Login completed, reloading request detail")
                coroutineScope.launch {
                    val isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
                    viewModel.clearAppError()
                    viewModel.fetchRequestDetail(requestId, requestTypeId, isEngineer)
                }
                // Clear the flag
                handle?.set("login_completed", false)
            }
        }

        handle?.getLiveData<Boolean>("login_completed")?.observeForever(observer)

        onDispose {
            handle?.getLiveData<Boolean>("login_completed")?.removeObserver(observer)
        }
    }

    // ✅ Show certificate issuance success dialog ONLY when issuing (not when just viewing)
    // Track if we're in "view mode" vs "issue mode"
    val isViewingCertificate by viewModel.isViewingCertificate.collectAsState()

    // ✅ Handle certificate data
    LaunchedEffect(certificateData) {
        certificateData?.let { certData ->
            if (isViewingCertificate) {
                // ✅ If in view mode, the file viewer should already be open
                // Just clear the certificate data
                println("✅ View mode: Certificate viewer already open, clearing data")
                viewModel.clearCertificateData()
            }
        }
    }

    // Only show dialog if:
    // 1. Certificate data exists
    // 2. FileViewer is NOT open (to avoid showing dialog while viewing)
    // 3. We're NOT in view-only mode (we just issued it, not just viewing)
    if (certificateData != null && !fileViewerState.isOpen && !isViewingCertificate) {
        val certData = certificateData!!
        println("🎉 Showing certificate issuance dialog: ${certData.certificationNumber}")

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
                viewModel.clearCertificateData()
            },
            onViewCertificate = {
                println("🔄 View Certificate clicked in dialog")

                // ✅ Construct URL directly based on request type
                val requestTypeId = requestDetail?.requestType?.id
                if (requestTypeId != null) {
                    println("✅ Constructing certificate URL for requestTypeId: $requestTypeId")

                    // ✅ Use the configuration flag to determine viewing method
                    viewModel.viewCertificate(requestTypeId, useExternalBrowser = USE_EXTERNAL_BROWSER_FOR_CERTIFICATES)

                    // ✅ Clear certificate data to dismiss dialog
                    viewModel.clearCertificateData()
                } else {
                    println("❌ Request type ID not available")
                    android.widget.Toast.makeText(
                        context,
                        if (isArabic) "فشل في عرض الشهادة" else "Failed to view certificate",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // ✅ Handle external browser opening (when USE_EXTERNAL_BROWSER_FOR_CERTIFICATES = true)
    val certificateUrl by viewModel.certificateUrl.collectAsState()
    LaunchedEffect(certificateUrl) {
        certificateUrl?.let { url ->
            println("🌐 Opening certificate in external browser: $url")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
            // Clear the URL after opening
            viewModel.clearCertificateUrl()
        }
    }

    // ✅ Show toast message for non-blocking success notifications
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // Calculate status bar height
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(extraColors.background)) {
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ✅ NEW: Show ErrorBanner for different error types
                appError?.let { error ->
                    when (error) {
                        is com.informatique.mtcit.common.AppError.Unauthorized -> {
                            // ✅ Show refresh token button for 401 errors
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = error.message,
                                showRefreshButton = true,
                                onRefreshToken = {
                                    coroutineScope.launch {
                                        val isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
                                        viewModel.refreshToken(requestId, requestTypeId, isEngineer)
                                    }
                                },
                                onDismiss = { viewModel.clearAppError() }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.ApiError -> {
                            // Other API errors - no refresh button
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = error.message,
                                onDismiss = { viewModel.clearAppError() }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.Unknown -> {
                            // Unknown errors
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = error.message,
                                onDismiss = { viewModel.clearAppError() }
                            )
                        }
                        else -> {
                            // Fallback
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = if (Locale.getDefault().language == "ar") "حدث خطأ" else "An error occurred",
                                onDismiss = { viewModel.clearAppError() }
                            )
                        }
                    }
                }

                // Content area
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState(extraColors)
                        }

                        appError != null -> {
                            ErrorState(
                                error = appError!!,
                                extraColors = extraColors,
                                onRetry = {
                                    coroutineScope.launch {
                                        val isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
                                        viewModel.retry(requestId, requestTypeId, isEngineer)
                                    }
                                }
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

    // ✅ File Viewer Dialog for certificate viewing
    FileViewerDialog(
        isOpen = fileViewerState.isOpen,
        fileUri = fileViewerState.fileUri,
        fileName = fileViewerState.fileName,
        mimeType = fileViewerState.mimeType,
        onDismiss = { viewModel.closeFileViewerDialog() }
    )
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
    val context = LocalContext.current
    var isEngineer by remember { mutableStateOf(false) }
    val checklistItems by viewModel.checklistItems.collectAsState()
    val isLoadingChecklist by viewModel.isLoadingChecklist.collectAsState()

    // Check if user is engineer
    LaunchedEffect(context) {
        isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
    }

    // ✅ Load checklist when purposeId is available (engineer only)
    // ✅ Use checklistItems from workOrderResult if available, otherwise load from API
    LaunchedEffect(requestDetail.purposeId, isEngineer) {
        if (isEngineer && requestDetail.purposeId != null) {
            // Check if we already have checklist items in workOrderResult
            val workOrderResult = requestDetail.workOrderResult
            if (workOrderResult != null && workOrderResult.checklistItems.isNotEmpty()) {
                println("✅ Using checklist items from workOrderResult (${workOrderResult.checklistItems.size} items)")
                // Set checklist items from workOrderResult
                viewModel.setChecklistItems(workOrderResult.checklistItems)

                // ✅ Set work order result ID (crucial for PUT instead of POST)
                workOrderResult.id?.let { id ->
                    viewModel.setWorkOrderResultId(id)
                    println("✅ Set work order result ID: $id")
                }

                // ✅ Initialize answers and answer IDs from workOrderResult
                val answersMap = mutableMapOf<Int, String>()
                val answerIdsMap = mutableMapOf<Int, Int>()

                workOrderResult.checklistAnswers.forEach { answer ->
                    answersMap[answer.checklistItemId] = answer.answer
                    answer.id?.let { answerId ->
                        answerIdsMap[answer.checklistItemId] = answerId
                    }
                    viewModel.updateChecklistAnswer(answer.checklistItemId, answer.answer)
                }

                viewModel.setAnswerIds(answerIdsMap)
                println("✅ Initialized ${workOrderResult.checklistAnswers.size} answers from workOrderResult")
                println("✅ Initialized ${answerIdsMap.size} answer IDs")
            } else {
                println("🔍 Loading checklist from API for purposeId=${requestDetail.purposeId}")
                viewModel.loadChecklistByPurpose(requestDetail.purposeId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp), // Space for fixed bottom button
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

            // ✅ Assigned Engineers Card (shown when engineers are assigned)
            if (requestDetail.engineerWorkOrders.isNotEmpty()) {
                item {
                    AssignedEngineersCard(
                        workOrders = requestDetail.engineerWorkOrders,
                        extraColors = extraColors
                    )
                }
            }

            // ✅ Engineer Checklist Section (Dynamic based on status)
            // ✅ Show existing answers from workOrderResult
            // ✅ Read-only for accepted/rejected (statusId: 2, 3, 7, 10, 11, 12)
            if (isEngineer && requestDetail.purposeId != null) {
                item {
                    val statusId = requestDetail.status.id
                    // ✅ Check if this is accepted/rejected status - make read-only
                    val isReadOnly = statusId in listOf(2, 3, 7, 10, 11, 12)

                    println("📋 Checklist Debug:")
                    println("   - statusId: $statusId")
                    println("   - isReadOnly: $isReadOnly")
                    println("   - checklistItems.size: ${checklistItems.size}")
                    println("   - workOrderResult answers: ${requestDetail.workOrderResult?.checklistAnswers?.size ?: 0}")
                    println("   - isLoadingChecklist: $isLoadingChecklist")

                    EngineerChecklistSection(
                        checklistItems = checklistItems,
                        existingAnswers = requestDetail.workOrderResult?.checklistAnswers,
                        isReadOnly = isReadOnly,
                        isLoadingChecklist = isLoadingChecklist,
                        extraColors = extraColors,
                        onAnswerChanged = { itemId, answer ->
                            viewModel.updateChecklistAnswer(itemId, answer)
                        }
                    )
                }

//                // ✅ NEW: Approve Inspection Button (always visible for engineers)
//                item {
//
//                }
            }

//            // Bottom spacing
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//            }
        }
        if (isEngineer && requestDetail.purposeId != null) {
            val statusId = requestDetail.status.id
            val isReadOnly = statusId in listOf(2, 3, 7, 10, 11, 12)

            ApproveInspectionButton(
                viewModel = viewModel,
                requestDetail = requestDetail,
                isReadOnly = isReadOnly,
                extraColors = extraColors,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }else{
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
    val context = LocalContext.current
    var isEngineer by remember { mutableStateOf(false) }

    // Check if user is engineer
    LaunchedEffect(context) {
        isEngineer = com.informatique.mtcit.data.datastorehelper.TokenManager.isEngineer(context)
    }

    val statusId = requestDetail.status.id
    val isPaid = requestDetail.isPaid

    // ✅ DEBUG: Log status and isPaid values
    println("════════════════════════════════════════════════════════════")
    println("🔍 BottomActionButtons DEBUG")
    println("════════════════════════════════════════════════════════════")
    println("   requestId: ${requestDetail.requestId}")
    println("   requestType: ${requestDetail.requestType.name} (id=${requestDetail.requestType.id})")
    println("   statusId: $statusId")
    println("   statusName: ${requestDetail.status.name}")
    println("   statusNameAr: ${requestDetail.status.nameAr}")
    println("   statusNameEn: ${requestDetail.status.nameEn}")
    println("   isPaid: $isPaid")
    println("   isEngineer: $isEngineer")
    println("════════════════════════════════════════════════════════════")

    // ✅ IMPORTANT: Status 13 (ACTION_TAKEN) and 14 (ISSUED) should ALWAYS show View Certificate
    // This applies to ALL transaction types, not just mortgage
    val isIssued = (statusId == 13 || statusId == 14)
    println("🔍 isIssued: $isIssued (statusId in [13, 14])")

    // ✅ FIX: Engineers should NOT see payment button for accepted requests (statusId 3, 7, 11, 12)
    val shouldShowButton = when {
        isEngineer -> false  // Engineers never see client action buttons
        statusId == 1 -> true  // Draft - Continue Editing
        statusId in listOf(2, 10) -> true  // Rejected - Submit New Request
        statusId in listOf(3, 7, 11, 12) -> true  // Accepted/Approved - Payment or Issue Certificate
        isIssued -> true  // Issued - View Certificate (statusId 13 or 14)
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // ✅ IMPORTANT: Check ISSUED status FIRST (highest priority)
                // Status 13 (ACTION_TAKEN) and 14 (ISSUED) should ALWAYS show View Certificate
                // This applies to ALL transaction types: mortgage, registration, permits, etc.
                when {
                    isIssued -> {
                        println("🔘 BottomActionButtons: ✅ ISSUED STATUS (statusId=$statusId) - Showing View Certificate button")
                        println("   This applies to ALL transaction types when certificate is already issued")
                        // ✅ Certificate Already Issued - Always show View Certificate button
                        // Regardless of isPaid value (0, 1, or null) - if status is ISSUED, certificate exists
                        IssueCertificateButton(
                            requestDetail = requestDetail,
                            navController = navController,
                            extraColors = extraColors,
                            viewModel = viewModel,
                            isIssuingCertificate = isIssuingCertificate,
                            certificateData = certificateData,
                            isAlreadyIssued = true  // ✅ Certificate already issued - show "View Certificate"
                        )
                    }

                    statusId == 1 -> {
                        println("🔘 BottomActionButtons: Showing Continue Editing button (statusId=1)")
                        // ✅ Draft - Continue Editing
                        Button(
                            onClick = {
                                // Extract required values from requestDetail
                                val requestId = requestDetail.requestId
                                val requestTypeId = requestDetail.requestType.id
                                // For drafts, default to lastCompletedStep = 0 (start from beginning)
                                val lastCompletedStep = 0

                                println("===============================================================================")
                                println("📝 CONTINUE EDITING DRAFT")
                                println("===============================================================================")
                                println("📋 Request ID: $requestId")
                                println("📋 Request Type ID: $requestTypeId")
                                println("📋 Last Completed Step: $lastCompletedStep (default for drafts)")
                                println("===============================================================================")

                                // ✅ Navigate using transaction ID routes (7, 8, 4, 5, 21)
                                // These match the NavRoutes configuration in NavGraph
                                val route = when (requestTypeId) {
                                    1 -> NavRoutes.ShipRegistrationRoute.createRouteWithResume(requestId.toString(), lastCompletedStep)
                                    2 -> "${NavRoutes.PermanentRegistrationRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
                                    3 -> "${NavRoutes.IssueNavigationPermitRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
                                    4 -> "${NavRoutes.MortgageCertificateRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
                                    5 -> "${NavRoutes.ReleaseMortgageRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
                                    6 -> "${NavRoutes.RenewNavigationPermitRoute.route}?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
                                    8 -> NavRoutes.RequestForInspection.createRouteWithResume(requestId.toString(), lastCompletedStep)
                                    else -> null
                                }

                                if (route != null) {
                                    navController.navigate(route)
                                } else {
                                    println("⚠️ No route found for requestTypeId: $requestTypeId")
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
                                    "متابعة التعديل"
                                else "Continue Editing",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    statusId == 2 || statusId == 10 -> {
                        println("🔘 BottomActionButtons: Showing Submit New Request button (statusId=$statusId)")
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

                    statusId in listOf(3, 7, 11, 12) -> {
                        println("🔘 BottomActionButtons: Accepted/Approved status (statusId=$statusId, isPaid=$isPaid)")
                        // ✅ NEW: Check isPaid to determine which button to show
                        if (isPaid == 1) {
                            println("   isPaid=1: Showing Issue Certificate button")
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
                            println("   isPaid=$isPaid: Showing Proceed to Payment button")
                            // ✅ Not paid - Show Proceed to Payment button
                            ProceedToPaymentButton(
                                requestDetail = requestDetail,
                                navController = navController,
                                extraColors = extraColors
                            )
                        }
                    }

                    else -> {
                        println("⚠️ BottomActionButtons: Unexpected statusId=$statusId - No button will be shown")
                        println("   This status ID is not handled in the when statement")
                        println("   Supported status IDs: 1 (Draft), 2/10 (Rejected), 3/7/11/12 (Accepted), 13/14 (Issued)")
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
@Suppress("UNUSED_PARAMETER")
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
            println("📋 requestId=${requestDetail.requestId}, requestTypeId=${requestDetail.requestType.id}, statusId=${requestDetail.status.id}")

            if (isAlreadyIssued) {
                // ✅ Certificate already issued - view it using configured method
                viewModel.viewCertificate(
                    requestTypeId = requestDetail.requestType.id,
                    useExternalBrowser = USE_EXTERNAL_BROWSER_FOR_CERTIFICATES
                )
            } else {
                // ✅ Certificate not issued yet - issue it
                viewModel.issueCertificate(
                    requestId = requestDetail.requestId,
                    requestTypeId = requestDetail.requestType.id,
                    statusId = requestDetail.status.id
                )
            }
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
            // For ACCEPTED requests (statusId == 7), navigate to Payment step in their respective strategies
            val lastCompletedStep = when {
                // ✅ ACCEPTED requests - navigate to payment step (using correct typeId from TransactionType enum)
                statusId == 7 && requestTypeId == 1 -> 7  // Temp Registration ACCEPTED → Marine Unit Name step (before Payment at step 8)
                statusId == 7 && requestTypeId == 2 -> 8  // Perm Registration ACCEPTED → Payment step
                statusId == 7 && requestTypeId == 3 -> 4  // Issue Navigation Permit ACCEPTED → Review step (before Payment at step 5)
                statusId == 7 && requestTypeId == 4 -> 8  // Mortgage Certificate ACCEPTED → Payment step
                statusId == 7 && requestTypeId == 5 -> 8  // Release Mortgage ACCEPTED → Payment step
                statusId == 7 && requestTypeId == 6 -> 2  // Renew Navigation Permit ACCEPTED → Payment step (after Review at step 2)
                statusId == 7 && requestTypeId == 7 -> 8  // Cancel Permanent Registration ACCEPTED → Payment step
                statusId == 7 && requestTypeId == 8 -> 3  // Request Inspection ACCEPTED → InspectionPurpose step (before Payment at step 4)

                // ✅ Draft/In-Progress requests - navigate based on last completed step from API
                else -> {
                    // For non-accepted statuses, use default step based on transaction type (using correct typeId)
                    when (requestTypeId) {
                        1 -> 7  // Temp Registration → step before payment
                        2 -> 8  // Perm Registration → payment step
                        3 -> 4  // Issue Navigation Permit → review step
                        4 -> 8  // Mortgage Certificate → payment step
                        5 -> 8  // Release Mortgage → payment step
                        6 -> 2  // Renew Navigation Permit → payment step
                        7 -> 8  // Cancel Permanent Registration → payment step
                        8 -> 3  // Request Inspection → inspection purpose step
                        12 -> 4  // Change Port of Ship → payment step (after affected certificates)
                        else -> 8  // Default to payment step
                    }
                }
            }

            println("🔍 ApiRequestDetailScreen: Navigating with lastCompletedStep=$lastCompletedStep (requestTypeId=$requestTypeId, statusId=$statusId)")

            // ✅ Smart navigation with lastCompletedStep and hasAcceptance passed through URL
            val route = getTransactionRouteForPayment(
                requestTypeId = requestDetail.requestType.id,
                requestId = requestDetail.requestId,
                statusId = statusId,
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = requestDetail.hasAcceptance
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
    val isAr = Locale.getDefault().language == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Row: Request Number (label + value) ── Status badge ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isAr) "رقم الطلب" else "Request Number",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.55f)
                    )
                    Text(
                        text = requestDetail.requestSerial,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = extraColors.whiteInDarkMode
                    )
                }

                Surface(
                    color = getStatusBackgroundColor(requestDetail.status.id),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = requestDetail.status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getStatusTextColor(requestDetail.status.id),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(
                color = extraColors.whiteInDarkMode.copy(alpha = 0.12f),
                thickness = 1.dp
            )

            // ─── Request Type ───
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isAr) "نوع الطلب" else "Request Type",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.55f)
                )
                Text(
                    text = requestDetail.requestType.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode
                )
            }

//            // ─── Ship Name (if available) ───
//            requestDetail.shipName?.let { shipName ->
//                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//                    Text(
//                        text = if (isAr) "اسم السفينة" else "Ship Name",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Normal,
//                        color = extraColors.whiteInDarkMode.copy(alpha = 0.55f)
//                    )
//                    Text(
//                        text = shipName,
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = extraColors.whiteInDarkMode
//                    )
//                }
//            }

            // ─── Message ───
            val displayMessage = requestDetail.message?.takeIf { it.isNotEmpty() }
                ?: when (requestDetail.status.id) {
                    2, 10 -> if (isAr) "تعذر اعتماد الطلب" else "Request could not be approved"
                    3, 7, 11, 12, 13, 14 -> if (isAr) "تم اعتماد الطلب بنجاح" else "Request approved successfully"
                    6 -> if (isAr) "تم تحديد موعد المعاينة" else "Inspection appointment scheduled"
                    else -> null
                }

            displayMessage?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.whiteInDarkMode
                )
            }

//            // ─── Message Details (if available) ───
//            if (!requestDetail.messageDetails.isNullOrEmpty()) {
//                Text(
//                    text = requestDetail.messageDetails,
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.Normal,
//                    color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
//                    lineHeight = 20.sp
//                )
//            }
            // ─── Message Details (if available) ───
            val displayMessageDetails = requestDetail.messageDetails?.takeIf { it.isNotEmpty() }
                ?: when (requestDetail.status.id) {
                    2, 10 -> if (isAr)
                        "نأسف لإبلاغكم بأنه تم رفض الطلب بعد مراجعته، وذلك لعدم استيفاء بعض المتطلبات. يرجى الاطلاع على تفاصيل الرفض أدناه، مع إمكانية تصحيح الملاحظات وإعادة التقديم في حال رغبتكم."
                    else
                        "We regret to inform you that your request has been rejected after review due to unmet requirements."
                    3, 7, 11, 12, 13, 14 -> if (isAr)
                        "نود إحاطتكم علماً بإنه تم تأكيد طلبكم بنجاح بعد استيفاء جميع المتطلبات اللازمة. يمكنكم الآن متابعة الخطوة التالية ضمن مسار الخدمة وفق الإجراءات المعتمدة."
                    else
                        "Your request has been successfully confirmed after meeting all requirements."
                    6 -> if (isAr)
                        "يسرنا إعلامكم بأنه تم تحديد موعد للمعاينة. يرجى التأكد من الالتزام بالحضور في الموعد المحدد لضمان استكمال المعاملة بسلاسة ووفق الأطر الزمنية المعتمدة."
                    else
                        "We are pleased to inform you that an inspection appointment has been scheduled."
                    else -> null
                }

            displayMessageDetails?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.55f),
                    lineHeight = 25.sp
                )
            }
        }
    }
}

@Suppress("unused")
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
 * Expandable section card - matches iOS Swift expandableCard design
 */
@Composable
private fun ExpandableDataSection(
    section: RequestDetailSection,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Chevron rotation: 0° when collapsed, 180° when expanded (same as Swift rotationEffect)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "chevronRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Clickable Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Section icon on trailing side (accent color)
                Icon(
                    imageVector = getSectionIcon(section.title),
                    contentDescription = null,
                    tint = extraColors.iconBlueGrey,
                    modifier = Modifier.size(22.dp)
                )
                // Title — fills remaining space
                Text(
                    text = section.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.weight(1f)
                )
                // Chevron (on the leading side — left in LTR, left in RTL for iOS match)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.blue1,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )

            }

            // ── Expandable Content ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.12f),
                        thickness = 1.dp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
@Suppress("SameParameterValue", "UNUSED_PARAMETER")
private fun RenderField(
    field: RequestDetailField,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    @Suppress("unused") indentLevel: Int
) {
    when (field) {
        // ── Simple field: matches iOS DetailFieldCard ──
        is RequestDetailField.SimpleField -> {
            DetailFieldCard(
                label = field.label,
                value = field.value.ifBlank { "-" },
                extraColors = extraColors
            )
        }

        // ── Nested object: label header + each sub-field as DetailFieldCard ──
        is RequestDetailField.NestedObject -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Section label
                Text(
                    text = field.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                // Each nested field as DetailFieldCard
                field.fields.forEach { nestedField ->
                    RenderField(nestedField, extraColors, 0)
                }
            }
        }

        // ── Array field: label header + numbered groups of DetailFieldCards ──
        is RequestDetailField.ArrayField -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Section label
                Text(
                    text = field.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                field.items.forEachIndexed { index, itemFields ->
                    // Item group header
                    Text(
                        text = "${if (Locale.getDefault().language == "ar") "العنصر" else "Item"} ${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.blue1,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemFields.forEach { itemField ->
                            RenderField(itemField, extraColors, 0)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matches iOS DetailFieldCard exactly:
 * HStack: title (leading, accent-ish) | value (trailing, SemiBold, gray)
 * Background: gray 5% opacity, cornerRadius 10, padding h12 v10
 */
@Composable
private fun DetailFieldCard(
    label: String,
    value: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Gray.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title — leading, wraps up to 2 lines
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = extraColors.whiteInDarkMode,
            lineHeight = 18.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Value — trailing, SemiBold, secondary color
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
            lineHeight = 18.sp,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// Helper: pick an icon per section title (matches Swift sectionIcon logic)
private fun getSectionIcon(title: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = title.lowercase()
    return when {
        lower.contains("ship") || lower.contains("سفينة") || lower.contains("وحدة") || lower.contains("unit") -> Icons.Default.Info
        lower.contains("document") || lower.contains("مستند") || lower.contains("وثيقة") -> Icons.Default.Info
        lower.contains("insurance") || lower.contains("تأمين") -> Icons.Default.CheckCircle
        lower.contains("engine") || lower.contains("محرك") -> Icons.Default.Info
        lower.contains("owner") || lower.contains("مالك") -> Icons.Default.Info
        lower.contains("engineer") || lower.contains("مهندس") -> Icons.Default.Info
        lower.contains("inspection") || lower.contains("معاينة") || lower.contains("فحص") -> Icons.Default.DateRange
        else -> Icons.Default.Info
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
 * Assigned Engineers Card — matches iOS engineersListCard + engineerRow exactly:
 * Expandable card with icon + title + chevron rotation
 * Each engineer row: circle avatar (accent 10%) + name/job VStack + status badge capsule
 * Rows separated by Dividers with horizontal padding
 */
@Composable
private fun AssignedEngineersCard(
    workOrders: List<com.informatique.mtcit.data.model.requests.EngineerWorkOrder>,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    val isAr = Locale.getDefault().language == "ar"
    var isExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "engineersChevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Header Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section icon — trailing (person.2.fill equivalent)
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = extraColors.iconBlueGrey,
                    modifier = Modifier.size(22.dp)
                )
                // Title — fills space
                Text(
                    text = if (isAr) "قائمة المهندسين المعينين" else "Assigned Engineers",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.weight(1f)
                )
                // Chevron — leading
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.blue1,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            // ── Expandable Content ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.12f),
                        thickness = 1.dp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        workOrders.forEachIndexed { index, workOrder ->
                            // ── Engineer Row (matches iOS engineerRow) ──
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circle avatar with person icon (accent 10% fill)
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            color = extraColors.blue1.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(50.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = extraColors.blue1,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Name + job title VStack
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = workOrder.engineerName.ifBlank { "N/A" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = extraColors.whiteInDarkMode
                                    )
                                    workOrder.jobTitle?.let { job ->
                                        if (job.isNotBlank()) {
                                            Text(
                                                text = job,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = extraColors.whiteInDarkMode.copy(alpha = 0.55f)
                                            )
                                        }
                                    }
                                }

                                // Status badge capsule (matches iOS statusBadgeInspection)
                                Surface(
                                    color = getEngineerStatusColor(workOrder.statusId),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = workOrder.statusName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Divider between rows (with horizontal padding — not after last)
                            if (index < workOrders.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = extraColors.whiteInDarkMode.copy(alpha = 0.12f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Maps engineer work order status ID to color — matches iOS statusColorInspection */
private fun getEngineerStatusColor(statusId: String): Color {
    return when (statusId) {
        "2" -> Color(0xFF4CAF50)   // Executed — Green
        "1" -> Color(0xFFFF9800)   // Pending — Orange
        else -> Color(0xFF2196F3)  // Default — Blue (accent)
    }
}

@Composable
private fun EngineerChecklistSection(
    checklistItems: List<com.informatique.mtcit.data.model.ChecklistItem>,
    existingAnswers: List<com.informatique.mtcit.data.model.ChecklistAnswer>?,
    isReadOnly: Boolean,
    isLoadingChecklist: Boolean,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    onAnswerChanged: (itemId: Int, answer: String) -> Unit = { _, _ -> }
) {
    val isArabic = Locale.getDefault().language == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "قائمة الفحص" else "Inspection Checklist",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoadingChecklist -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = extraColors.blue1)
                    }
                }

                checklistItems.isEmpty() -> {
                    Text(
                        text = if (isArabic) "لا توجد قائمة فحص متاحة" else "No checklist available",
                        fontSize = 14.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                else -> {
                    // ✅ Show appropriate message based on read-only status
                    if (isReadOnly) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFE3F2FD),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تم إكمال الفحص - عرض النتائج فقط" else "Inspection completed - View only",
                                fontSize = 13.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFF3E0),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "يمكن ملء وتعديل القائمة" else "Checklist can be filled and edited",
                                fontSize = 13.sp,
                                color = Color(0xFFF57C00)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Dynamic checklist form - editable only if not read-only
                    com.informatique.mtcit.ui.components.DynamicChecklistForm(
                        checklistItems = checklistItems,
                        existingAnswers = existingAnswers,
                        onAnswersChanged = if (isReadOnly) null else { answers ->
                            // ✅ Update ViewModel with answers
                            println("📝 Checklist answers changed: ${answers.size} items")
                            answers.forEach { (itemId, answer) ->
                                onAnswerChanged(itemId, answer)
                                println("   - Item $itemId: $answer")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Map request type ID to transaction route with requestId for payment resumption
 * This reuses the existing transaction screens and payment strategy flow
 * Uses TransactionType enum to ensure correct mapping
 */
@Suppress("UNUSED_PARAMETER")
private fun getTransactionRouteForPayment(
    requestTypeId: Int,
    requestId: Int,
    statusId: Int,
    lastCompletedStep: Int,
    hasAcceptance: Int
): String? {
    // Map API request type ID to TransactionType
    val transactionType = TransactionType.fromTypeId(requestTypeId)

    return when (transactionType) {
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE ->
            NavRoutes.ShipRegistrationRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE ->
            NavRoutes.PermanentRegistrationRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.ISSUE_NAVIGATION_PERMIT ->
            NavRoutes.IssueNavigationPermitRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.RENEW_NAVIGATION_PERMIT ->
            NavRoutes.RenewNavigationPermitRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.MORTGAGE_CERTIFICATE ->
            NavRoutes.MortgageCertificateRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.RELEASE_MORTGAGE ->
            NavRoutes.ReleaseMortgageRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.CANCEL_PERMANENT_REGISTRATION ->
            NavRoutes.CancelRegistrationRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.REQUEST_FOR_INSPECTION ->
            NavRoutes.RequestForInspection.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        TransactionType.SHIP_PORT_CHANGE ->
            NavRoutes.ShipPortChangeRoute.createRouteWithResume(
                requestId = requestId.toString(),
                lastCompletedStep = lastCompletedStep,
                hasAcceptance = hasAcceptance
            )

        else -> null // Unsupported or no payment flow for this transaction type
    }
}

/**
 * ✅ NEW: Approve Inspection Buttons Section
 * Shows two buttons:
 * 1. Save as Draft (حفظ مسودة) - always enabled except for rejected/accepted
 * 2. Submit Inspection (تقديم الفحص) - enabled only when all mandatory fields are filled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApproveInspectionButton(
    viewModel: RequestDetailViewModel,
    requestDetail: com.informatique.mtcit.data.model.requests.RequestDetailUiModel,
    isReadOnly: Boolean,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    modifier: Modifier = Modifier
) {
    val isArabic = Locale.getDefault().language == "ar"
    val checklistItems by viewModel.checklistItems.collectAsState()
    val checklistAnswers by viewModel.checklistAnswers.collectAsState()
    val inspectionDecisions by viewModel.inspectionDecisions.collectAsState()
    val isLoadingDecisions by viewModel.isLoadingDecisions.collectAsState()

    var showDecisionBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDecisionId by remember { mutableStateOf<Int?>(null) }
    var refuseNotes by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // ✅ Calculate if all mandatory fields are filled
    val areAllMandatoryFilled = remember(checklistItems, checklistAnswers) {
        val mandatoryItems = checklistItems.filter { it.isMandatory }
        if (mandatoryItems.isEmpty()) {
            false // No checklist items yet
        } else {
            mandatoryItems.all { item ->
                val answer = checklistAnswers[item.id]
                !answer.isNullOrBlank()
            }
        }
    }

    // ✅ Draft button is always enabled except for read-only (rejected/accepted)
    val isDraftButtonEnabled = !isReadOnly

    // ✅ Submit button is enabled only when all mandatory fields are filled
    val isSubmitButtonEnabled = !isReadOnly && areAllMandatoryFilled

    println("🔘 ApproveInspectionButton state:")
    println("   - isReadOnly: $isReadOnly")
    println("   - checklistItems.size: ${checklistItems.size}")
    println("   - checklistAnswers.size: ${checklistAnswers.size}")
    println("   - areAllMandatoryFilled: $areAllMandatoryFilled")
    println("   - isDraftButtonEnabled: $isDraftButtonEnabled")
    println("   - isSubmitButtonEnabled: $isSubmitButtonEnabled")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ✅ Button 1: Save as Draft (always enabled except for rejected/accepted)
            Button(
                onClick = {
                    println("💾 Save as Draft button clicked")
                    val scheduledRequestId = requestDetail.scheduledRequestId ?: requestDetail.requestId
                    viewModel.saveDraftInspection(scheduledRequestId)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDraftButtonEnabled) extraColors.blue1 else Color.Gray,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isDraftButtonEnabled
            ) {
                Text(
                    text = if (isArabic) "حفظ مسودة" else "Save as Draft",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ✅ Button 2: Submit Inspection (enabled only when mandatory fields are filled)
            Button(
                onClick = {
                    println("✅ Submit Inspection button clicked")
                    // Load decisions and show bottom sheet
                    viewModel.loadInspectionDecisions()
                    showDecisionBottomSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubmitButtonEnabled) extraColors.blue1 else Color.Gray,
                    disabledContainerColor = Color.Gray,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isSubmitButtonEnabled
            ) {
                Text(
                    text = if (isArabic) "اعتماد نتيجة المعاينة" else "Approve inspection result",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // ✅ Decision Bottom Sheet with Refuse Notes and Date Picker
    if (showDecisionBottomSheet) {
        println("🔄 Rendering InspectionDecisionBottomSheet")
        ModalBottomSheet(
            onDismissRequest = {
                println("❌ Decision bottom sheet dismissed")
                showDecisionBottomSheet = false
                selectedDecisionId = null
                refuseNotes = ""
            },
            sheetState = bottomSheetState,
            containerColor = extraColors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isArabic) "اختر القرار" else "Select Decision",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when {
                    isLoadingDecisions -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    inspectionDecisions.isEmpty() -> {
                        Text(
                            text = if (isArabic) "لا توجد قرارات متاحة" else "No decisions available",
                            modifier = Modifier.padding(16.dp),
                            color = extraColors.whiteInDarkMode
                        )
                    }

                    else -> {
                        // Decision options
                        inspectionDecisions.forEach { decision ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDecisionId = decision.id
                                        println("📝 Decision selected: ${decision.id}")
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDecisionId == decision.id,
                                    onClick = {
                                        selectedDecisionId = decision.id
                                        println("📝 Decision selected: ${decision.id}")
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = extraColors.blue1
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) decision.nameAr else decision.nameEn,
                                    fontSize = 16.sp,
                                    color = extraColors.whiteInDarkMode
                                )
                            }
                        }

                        // ✅ Show date picker if Approved (id=1) is selected
                        if (selectedDecisionId == 1) {
                            var showDateDialog by remember { mutableStateOf(false) }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = extraColors.whiteInDarkMode.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isArabic) "تاريخ الانتهاء *" else "Expiry Date *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.whiteInDarkMode,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // ✅ Clickable date field (like transactions)
                            val selectedDateMillis = datePickerState.selectedDateMillis
                            val displayDate = selectedDateMillis?.let {
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(java.util.Date(it))
                            } ?: ""

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        showDateDialog = true
                                    }
                            ) {
                                OutlinedTextField(
                                    value = displayDate,
                                    onValueChange = { },
                                    readOnly = true,
                                    enabled = false,
                                    placeholder = {
                                        Text(
                                            if (isArabic) "اختر تاريخ الانتهاء" else "Select expiry date",
                                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Calendar",
                                            tint = extraColors.blue1,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                                        disabledContainerColor = extraColors.cardBackground,
                                        disabledTextColor = extraColors.whiteInDarkMode,
                                        disabledPlaceholderColor = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // ✅ Show date picker dialog when field is clicked
                            if (showDateDialog) {
                                DatePickerDialog(
                                    onDismissRequest = { showDateDialog = false },
                                    confirmButton = {
                                        TextButton(
                                            onClick = { showDateDialog = false }
                                        ) {
                                            Text(if (isArabic) "تأكيد" else "OK")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDateDialog = false }) {
                                            Text(if (isArabic) "إلغاء" else "Cancel")
                                        }
                                    }
                                ) {
                                    DatePicker(
                                        state = datePickerState,
                                        colors = DatePickerDefaults.colors(
                                            containerColor = extraColors.cardBackground
                                        )
                                    )
                                }
                            }
                        }

                        // ✅ Show refuse notes field if Refused (id=2) is selected
                        if (selectedDecisionId == 2) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = extraColors.whiteInDarkMode.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isArabic) "ملاحظات الرفض" else "Refuse Notes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.whiteInDarkMode,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = refuseNotes,
                                onValueChange = {
                                    refuseNotes = it
                                    println("📝 Refuse notes updated: $it")
                                },
                                placeholder = {
                                    Text(
                                        if (isArabic) "أدخل سبب الرفض" else "Enter refusal reason",
                                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = extraColors.blue1,
                                    unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                                    focusedContainerColor = extraColors.cardBackground,
                                    unfocusedContainerColor = extraColors.cardBackground,
                                    focusedTextColor = extraColors.whiteInDarkMode,
                                    unfocusedTextColor = extraColors.whiteInDarkMode
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 5
                            )
                        }

                        // Submit button
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (selectedDecisionId != null) {
                                    println("✅ Confirmed decision: $selectedDecisionId")

                                    // Get expiry date if decision is Approved (id=1)
                                    val expiredDate = if (selectedDecisionId == 1) {
                                        val selectedDateMillis = datePickerState.selectedDateMillis
                                        if (selectedDateMillis != null) {
                                            val calendar = java.util.Calendar.getInstance()
                                            calendar.timeInMillis = selectedDateMillis
                                            val formattedDate = String.format(
                                                java.util.Locale.US,
                                                "%04d-%02d-%02d",
                                                calendar.get(java.util.Calendar.YEAR),
                                                calendar.get(java.util.Calendar.MONTH) + 1,
                                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                            )
                                            println("📅 Selected expiry date: $formattedDate")
                                            formattedDate
                                        } else {
                                            println("⚠️ No date selected for Approved decision")
                                            null
                                        }
                                    } else {
                                        null
                                    }

                                    // ✅ Use scheduledRequestId (root data.id) not inspectionRequest.id
                                    val idToUse = requestDetail.scheduledRequestId ?: requestDetail.requestId
                                    println("📤 Executing inspection submission:")
                                    println("   - Decision ID: $selectedDecisionId")
                                    println("   - Scheduled Request ID: $idToUse")
                                    println("   - Refuse Notes: $refuseNotes")
                                    println("   - Expired Date: ${expiredDate ?: "N/A"}")

                                    // ✅ Call new executeInspectionSubmission method
                                    viewModel.executeInspectionSubmission(
                                        scheduledRequestId = idToUse,
                                        decisionId = selectedDecisionId!!,
                                        refuseNotes = refuseNotes,
                                        expiredDate = expiredDate
                                    )
                                    showDecisionBottomSheet = false
                                    selectedDecisionId = null
                                    refuseNotes = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = selectedDecisionId != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extraColors.blue1
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isArabic) "تأكيد" else "Confirm",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

