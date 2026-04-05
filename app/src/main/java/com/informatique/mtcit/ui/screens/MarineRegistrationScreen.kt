package com.informatique.mtcit.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.localizedApp
import androidx.core.net.toUri
import com.informatique.mtcit.ui.screens.RequestDetail
import com.informatique.mtcit.util.UriPermissionManager
import com.informatique.mtcit.ui.components.SuccessDialog
import com.informatique.mtcit.ui.components.SuccessDialogItem
import kotlinx.coroutines.launch
import com.informatique.mtcit.common.util.LocalAppLocale
import androidx.compose.ui.res.stringResource


/**
 * Marine Registration Screen
 *
 * Handles Marine Unit Registration Category (التسجيل):
 * - Temporary Registration Certificate
 * - Permanent Registration Certificate
 * - Suspend Permanent Registration
 * - Cancel Permanent Registration
 * - Mortgage Certificate
 * - Release Mortgage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineRegistrationScreen(
    navController: NavController,
    transactionType: TransactionType,
    requestId: String? = null,  // ✅ NEW: Accept optional request ID for resume
    lastCompletedStep: Int? = null,  // ✅ NEW: Accept lastCompletedStep from navigation to avoid API call
    hasAcceptance: Int? = null  // ✅ NEW: Accept hasAcceptance from navigation
) {
    val viewModel: MarineRegistrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val fileNavigationEvent by viewModel.fileNavigationEvent.collectAsStateWithLifecycle()
    val navigationToComplianceDetail by viewModel.navigationToComplianceDetail.collectAsStateWithLifecycle()
    val isResuming by viewModel.isResuming.collectAsStateWithLifecycle()
    val showToast by viewModel.showToastEvent.collectAsStateWithLifecycle()

    // ✅ NEW: Observe request submission success for showing success dialog
    val requestSubmissionSuccess by viewModel.requestSubmissionSuccess.collectAsStateWithLifecycle()

    // ✅ NEW: Observe file viewer dialog state
    val fileViewerState by viewModel.fileViewerState.collectAsStateWithLifecycle()

    // ✅ NEW: Observe login navigation trigger (when refresh token is also expired)
    val shouldNavigateToLogin by viewModel.shouldNavigateToLogin.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // ✅ Navigate to OAuth login when refresh token is also expired
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            println("🔑 MarineRegistrationScreen: Refresh token expired - navigating to login")
            viewModel.resetNavigateToLogin()
            navController.navigate(NavRoutes.OAuthWebViewRoute.route)
        }
    }

    // ✅ Observe re-auth completion: NavHost sets this flag after the transaction-screen OAuth
    // flow successfully exchanges the code for a new token. We clear any lingering error so
    // the user can immediately retry their last action (e.g. "Accept & Send").
    DisposableEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val observer = androidx.lifecycle.Observer<Boolean> { refreshed ->
            if (refreshed == true) {
                println("✅ MarineRegistrationScreen: Token re-authenticated - clearing lingering error")
                handle?.set("transaction_token_refreshed", false)
                viewModel.clearError()
            }
        }
        handle?.getLiveData<Boolean>("transaction_token_refreshed")?.observeForever(observer)
        onDispose {
            handle?.getLiveData<Boolean>("transaction_token_refreshed")?.removeObserver(observer)
        }
    }

    // ✅ NEW: Show Toast messages
    LaunchedEffect(showToast) {
        showToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearToastEvent()
        }
    }

    // ✅ NEW: Monitor injectInspectionStep flag to trigger inspection step injection
    LaunchedEffect(uiState.formData["injectInspectionStep"]) {
        val shouldInject = uiState.formData["injectInspectionStep"]?.toBoolean() ?: false
        if (shouldInject) {
            println("🔔 Detected injectInspectionStep flag - calling handleInspectionContinue")
            viewModel.handleInspectionContinue()
            // Clear the flag after handling
            viewModel.onFieldValueChange("injectInspectionStep", "false")
        }
    }

    // ✅ NEW: Monitor payment retry trigger
    LaunchedEffect(uiState.formData["_triggerPaymentRetry"]) {
        val shouldRetry = uiState.formData["_triggerPaymentRetry"]?.toBoolean() ?: false
        if (shouldRetry) {
            println("🔔 Detected _triggerPaymentRetry flag - re-submitting payment")
            // Re-submit the current step (payment step) to trigger PaymentManager again
            viewModel.nextStep()
            // ✅ DON'T clear the flag here - let PaymentManager clear it after processing
            // This allows PaymentManager to read the flag and skip submitSimplePayment
        }
    }

    // ✅ NEW: Trigger resume if requestId is provided
    LaunchedEffect(requestId, lastCompletedStep) {
        if (requestId != null) {
            println("🎬 MarineRegistrationScreen mounted with requestId: $requestId, lastCompletedStep: $lastCompletedStep")

            // ✅ If lastCompletedStep is provided, use direct resume (no API call)
            if (lastCompletedStep != null) {
                println("✅ Using direct resume with lastCompletedStep from navigation (avoiding API call)")
                viewModel.resumeDirectlyWithStep(requestId, lastCompletedStep, transactionType)
            } else {
                println("⚠️ No lastCompletedStep provided, falling back to API call")
                viewModel.setRequestIdAndCompleteResume(requestId, transactionType)
            }
        } else {
            println("🎬 MarineRegistrationScreen mounted - no requestId provided")
        }
    }

    // Initialize transaction type on first composition
    // ✅ IMPORTANT: Only initialize if NOT resuming a transaction
    LaunchedEffect(transactionType, isResuming, requestId) {
        // Check if we're currently resuming - if yes, skip normal initialization
        if (!isResuming && requestId == null) {
            // ✅ FIX: Skip re-initialization if the ViewModel already has this transaction type
            // loaded and ready. This prevents a full state reset (including canProceedToNext)
            // when the composable re-enters the composition after returning from the OAuth
            // re-auth flow (OAuthWebViewScreen push → popBackStack).
            if (uiState.isInitialized && uiState.transactionType == transactionType) {
                println("⏭️ Skipping initialization - already initialized for: $transactionType")
            } else {
                println("🆕 Normal initialization for transaction type: $transactionType")
                viewModel.initializeTransaction(transactionType)
            }
        } else {
            println("⏭️ Skipping normal initialization - resume in progress (isResuming=$isResuming, requestId=$requestId)")
        }
    }

    // ✅ NEW: Set hasAcceptance in strategy after initialization completes
    LaunchedEffect(hasAcceptance, uiState.isInitialized) {
        if (hasAcceptance != null && uiState.isInitialized) {
            println("🔧 Setting hasAcceptance=$hasAcceptance in strategy after initialization")
            viewModel.setHasAcceptanceFromApi(hasAcceptance)
        }
    }

    // TODO: Uncomment after backend integration is complete
    // This forwards to RequestDetailScreen when compliance issues are detected
    /*
    LaunchedEffect(navigationToComplianceDetail) {
        navigationToComplianceDetail?.let { action ->
            // Build marine unit data string with all details and compliance issues
            val marineData = buildComplianceDetailData(action)

            // Navigate to RequestDetailScreen
            navController.navigate(
                NavRoutes.RequestDetailRoute.createRoute(
                    RequestDetail.CheckShipCondition(marineData)
                )
            )

            // Clear navigation state
            viewModel.clearComplianceDetailNavigation()
        }
    }
    */

    // State for file operations
    var currentFilePickerField by remember { mutableStateOf("") }
    var currentFilePickerTypes by remember { mutableStateOf(listOf<String>()) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Validate file type - Get actual filename from ContentResolver
            val fileName = getFileNameFromUri(context, it)
            val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""

            if (currentFilePickerTypes.isEmpty() || currentFilePickerTypes.contains(extension)) {
                // CRITICAL: Cache the URI immediately to preserve the permission
                com.informatique.mtcit.util.UriCache.cacheUri(context, it)

                // Use the new UriPermissionManager for proper permission handling
                val result = UriPermissionManager.ensureReadPermission(context, it)

                if (result.isSuccess) {
                    viewModel.onFieldValueChange(currentFilePickerField, it.toString())
                } else {
                    Toast.makeText(
                        context,
                        "Error selecting file: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Invalid file type. Allowed types: ${currentFilePickerTypes.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Handle file navigation events
    LaunchedEffect(fileNavigationEvent) {
        fileNavigationEvent?.let { event ->
            when (event) {
                is FileNavigationEvent.OpenFilePicker -> {
                    currentFilePickerField = event.fieldId
                    currentFilePickerTypes = event.allowedTypes

                    // Launch with "*/*" to show all files, validation happens after selection
                    filePickerLauncher.launch(arrayOf("*/*"))
                    viewModel.clearFileNavigationEvent()
                }

                is FileNavigationEvent.ViewFile -> {
                    println("🔍 ViewFile event received: ${event.fileUri}")

                    // ✅ Check if this is a draft document (from API)
                    // Draft documents come as refNum (not content:// URI)
                    val isDraftDocument = !event.fileUri.startsWith("content://", ignoreCase = true)

                    if (isDraftDocument) {
                        // This is a draft document - fileUri is the refNum
                        val refNum = event.fileUri

                        println("📄 Fetching draft document preview URL for refNo=$refNum")

                        // ✅ Call the API to get the actual MinIO URL
                        viewModel.viewDraftDocument(refNum) { fileUrl, error ->
                            if (error != null) {
                                println("❌ Failed to get file preview: $error")
                                Toast.makeText(
                                    context,
                                    "Unable to load document: $error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (fileUrl != null) {
                                println("✅ Got file URL: $fileUrl")

                                // Extract filename from URL or use default
                                val fileName = try {
                                    fileUrl.toUri().lastPathSegment?.substringBefore('?') ?: "Document"
                                } catch (_: Exception) {
                                    "Document"
                                }

                                println("📄 Opening file viewer with MinIO URL: $fileUrl")

                                // ✅ Open in common file viewer - it will handle URL with WebView
                                viewModel.openFileViewerDialog(fileUrl, fileName, "application/pdf")
                            }
                        }
                        viewModel.clearFileNavigationEvent()
                    } else {
                        // Local URI - use existing logic
                        val uri = event.fileUri.toUri()

                        // ✅ CRITICAL: Re-cache the URI before viewing to restore permissions
                        // This fixes the "corrupted PDF" issue when viewing files after app restart
                        com.informatique.mtcit.util.UriCache.cacheUri(context, uri)

                        // ✅ NEW: Open file viewer dialog instead of navigating to separate screen
                        // This preserves the form state
                        try {
                            // Take persistent permission if possible
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            android.util.Log.d("MarineRegistration", "Took persistent permission for $uri")
                        } catch (e: SecurityException) {
                            android.util.Log.w("MarineRegistration", "Could not take persistent permission: ${e.message}")
                        }

                        val fileName = getFileNameFromUri(context, uri) ?: "File"

                        // ✅ Open dialog instead of navigating
                        viewModel.openFileViewerDialog(event.fileUri, fileName, event.fileType)
                        viewModel.clearFileNavigationEvent()
                    }
                }

                is FileNavigationEvent.RemoveFile -> {
                    viewModel.onFieldValueChange(event.fieldId, "")
                    viewModel.clearFileNavigationEvent()
                }
            }
        }
    }

    // Handle submission result
    LaunchedEffect(submissionState) {
        when (submissionState) {
            is UIState.Success -> {
                // ✅ TODO: Uncomment after backend integration is complete
                // This forwards to RequestDetailScreen (AcceptedAndPayment) after successful submission
                /*
                val shipData = mapOf(
                    "نوع الوحدة البحرية" to "سفينة صيد",
                    "رقم IMO" to "9990001",
                    "رمز النداء" to "A9BC2",
                    "رقم الهوية البحرية" to "470123456",
                    "ميناء التسجيل" to "صحار",
                    "النشاط البحري" to "صيد",
                    "سنة صنع السفينة" to "2018",
                    "نوع الإثبات" to "شهادة بناء",
                    "حوض البناء" to "Hyundai Shipyard",
                    "تاريخ بدء البناء" to "2014-03-01",
                    "تاريخ انتهاء البناء" to "2015-01-15",
                    "تاريخ أول تسجيل" to "2015-02-01",
                    "بلد البناء" to "سلطنة عمان"
                )
                navController.navigate(NavRoutes.RequestDetailRoute.createRoute(
                    RequestDetail.AcceptedAndPayment(
                        transactionTitle = "إصدار تصريح ملاحة للسفن و الوحدات البحرية",
                        title = "قبول الطلب و إتمام الدفع",
                        referenceNumber = "007 24 7865498",
                        dataSubmitted = shipData
                    )
                ))
                */
                // ✅ For now, just reset submission state
                viewModel.resetSubmissionState()
            }

            is UIState.Failure -> {
                viewModel.resetSubmissionState()
            }

            else -> { /* No action needed */
            }
        }
    }

    // ✅ Show loading ONLY when actually loading data
    // Don't show loading just because requestId exists - show it only during the actual resume process
    if (uiState.isLoading || !uiState.isInitialized || isResuming) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Main UI - Use TransactionFormContent for ALL steps including review
    // Note: Error banner is handled inside TransactionFormContent (below stepper)
    Column(modifier = Modifier.fillMaxSize()) {

        // Form content
        TransactionFormContent(
            navController = navController,
            uiState = uiState,
            submissionState = submissionState,
            transactionTitle = getMarineRegistrationTitle(transactionType),
            onFieldValueChange = viewModel::onFieldValueChange,
            onFieldFocusLost = viewModel::onFieldFocusLost,
            isFieldLoading = viewModel::isFieldLoading,
            onOpenFilePicker = { fieldId, allowedTypes ->
                currentFilePickerField = fieldId
                currentFilePickerTypes = allowedTypes
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onViewFile = viewModel::viewFile,
            onRemoveFile = viewModel::removeFile,
            goToStep = viewModel::goToStep,
            previousStep = viewModel::previousStep,
            nextStep = viewModel::nextStep,
            submitForm = viewModel::submitForm,
            viewModel = viewModel
        )
    }

    // ✅ NEW: File Viewer Dialog - Preserves form state
    com.informatique.mtcit.ui.components.FileViewerDialog(
        isOpen = fileViewerState.isOpen,
        fileUri = fileViewerState.fileUri,
        fileName = fileViewerState.fileName,
        mimeType = fileViewerState.mimeType,
        onDismiss = viewModel::closeFileViewerDialog
    )

    // ✅ NEW: Request Submission Success Dialog
    requestSubmissionSuccess?.let { result ->
        val isArabic = LocalAppLocale.current.language == "ar"

        SuccessDialog(
            title = result.message,
            items = listOf(
                SuccessDialogItem(
                    label = stringResource(R.string.request_number),
                    value = result.requestNumber,
                    icon = "📄"
                ),
                SuccessDialogItem(
                    label = stringResource(R.string.status),
                    value = stringResource(R.string.submitted_successfully),
                    icon = "✅"
                ),
                SuccessDialogItem(
                    label = stringResource(R.string.next_step),
                    value = stringResource(R.string.check_my_requests_in_your_profile_to_continue),
                    icon = "👉"
                )
            ),
            qrCode = null,
            onDismiss = {
                viewModel.clearRequestSubmissionSuccess()
                // Navigate back to profile screen
                navController.popBackStack()
            }
        )
    }
}

@Composable
private fun getMarineRegistrationTitle(transactionType: TransactionType): String {
    return when (transactionType) {
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_temporary_registration_certificate)
        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_permanent_registration_certificate)
        TransactionType.REQUEST_FOR_INSPECTION -> localizedApp(R.string.request_for_inspection_title )
        TransactionType.SUSPEND_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_suspend_permanent_registration)
        TransactionType.CANCEL_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_cancel_permanent_registration)
        TransactionType.MORTGAGE_CERTIFICATE -> localizedApp(R.string.transaction_mortgage_certificate)
        TransactionType.RELEASE_MORTGAGE -> localizedApp(R.string.transaction_release_mortgage)
        TransactionType.ISSUE_NAVIGATION_PERMIT -> localizedApp(R.string.transaction_issue_navigation_permit)
        TransactionType.RENEW_NAVIGATION_PERMIT -> localizedApp(R.string.transaction_renew_navigation_permit)
        TransactionType.SHIP_NAME_CHANGE -> localizedApp(R.string.transaction_ship_name_change)
        TransactionType.CAPTAIN_NAME_CHANGE -> localizedApp(R.string.transaction_captain_name_change)
        TransactionType.SHIP_ACTIVITY_CHANGE -> localizedApp(R.string.transaction_ship_activity_change)
        TransactionType.SHIP_DIMENSIONS_CHANGE -> localizedApp(R.string.transaction_ship_dimensions_change)
        TransactionType.SHIP_ENGINE_CHANGE -> localizedApp(R.string.transaction_ship_engine_change)
        TransactionType.SHIP_PORT_CHANGE -> localizedApp(R.string.transaction_ship_port_change)
        TransactionType.SHIP_OWNERSHIP_CHANGE -> localizedApp(R.string.transaction_ship_ownership_change)
        else -> "Unknown Transaction"
    }
}

/**
 * Helper function to get file name from URI
 */
private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}

/**
 * NEW: Build compliance detail data string from ShowComplianceDetailScreen action
 * This creates a formatted string with marine unit data and compliance issues
 */
private fun buildComplianceDetailData(action: com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.ShowComplianceDetailScreen): String {
    val unit = action.marineUnit
    val issues = action.complianceIssues

    return buildString {
        appendLine("📋 بيانات الوحدة البحرية")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()

        // Basic Info
        appendLine("🚢 الاسم: ${unit.name}")
        appendLine("🔢 رقم الهوية البحرية: ${unit.maritimeId}")
        appendLine("📍 نوع الوحدة: ${unit.type}")
        appendLine("⚓ ميناء التسجيل: ${unit.registrationPort}")
        appendLine("🎯 النشاط البحري: ${unit.activity}")
        appendLine()

        // Dimensions
        if (unit.totalLength.isNotEmpty()) {
            appendLine("📏 الأبعاد:")
            appendLine("   • الطول الكلي: ${unit.totalLength}")
            if (unit.totalWidth.isNotEmpty()) appendLine("   • العرض الكلي: ${unit.totalWidth}")
            if (unit.draft.isNotEmpty()) appendLine("   • الغاطس: ${unit.draft}")
            appendLine()
        }

        // Compliance Issues Section
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("⚠️ سجل الالتزام - المشاكل المكتشفة")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()

        if (issues.isEmpty()) {
            appendLine("✅ لا توجد مشاكل")
        } else {
            issues.forEachIndexed { index, issue ->
                val icon = when (issue.severity) {
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING -> "🚫"
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.WARNING -> "⚠️"
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.INFO -> "ℹ️"
                }

                appendLine("$icon ${issue.category}")
                appendLine("   العنوان: ${issue.title}")
                appendLine("   التفاصيل: ${issue.description}")

                if (issue.details.isNotEmpty()) {
                    issue.details.forEach { (key, value) ->
                        appendLine("   • $key: $value")
                    }
                }

                if (index < issues.size - 1) appendLine()
            }
        }

        appendLine()
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("📌 سبب الرفض:")
        appendLine(action.rejectionReason)
    }
}
