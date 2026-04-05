package com.informatique.mtcit.ui.screens

// ✅ NOTE: Intent and Uri imports are needed for External Browser option (see line ~250)
// Uncomment the External Browser code block to use them
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionState
import com.informatique.mtcit.ui.components.*
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData as ViewModelStepData
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.BaseTransactionViewModel
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.ValidationState
import com.informatique.mtcit.ui.components.ErrorBanner
import io.ktor.client.request.forms.formData
import java.util.Locale
import com.informatique.mtcit.common.util.LocalAppLocale
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.res.stringResource


/**
 * Generic Transaction Form Content - Shared UI for all transaction screens
 * This composable contains the common UI structure used by all transaction categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormContent(
    navController: NavController,
    uiState: TransactionState,
    submissionState: UIState<Boolean>,
    transactionTitle: String,
    onFieldValueChange: (String, String) -> Unit,
    onFieldFocusLost: (String, String) -> Unit,
    isFieldLoading: (String) -> Boolean,
    onOpenFilePicker: (String, List<String>) -> Unit,
    onViewFile: (String, String) -> Unit,
    onRemoveFile: (String) -> Unit,
    goToStep: (Int) -> Unit,
    previousStep: () -> Unit,
    nextStep: () -> Unit,
    submitForm: () -> Unit,
    viewModel: BaseTransactionViewModel,
    hideStepperForFirstStep: Boolean = false // New parameter for Login flow
) {
    val extraColors = LocalExtraColors.current

    // ✅ NEW: ScrollState for auto-scrolling to error banner
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Track declaration acceptance state for review step.
    // ✅ Stored in ViewModel (not local composable state) so it survives navigation round-trips
    // such as the OAuth re-auth flow (OAuthWebViewScreen push → popBackStack).
    val declarationAccepted by viewModel.declarationAccepted.collectAsState()

    // ✅ CHANGED: Detect review step by StepType.REVIEW instead of empty fields
    val isReviewStep = uiState.steps.getOrNull(uiState.currentStep)?.stepType == com.informatique.mtcit.business.transactions.shared.StepType.REVIEW

    // Collect processing state from viewModel (to disable Next button and show loader)
    val isProcessingNext by viewModel.isProcessingNext.collectAsState()

    // ✅ NEW: Observe error state from ViewModel
    val errorState by viewModel.error.collectAsState()

    // ✅ NEW: Auto-scroll to top when error appears to show banner
    LaunchedEffect(errorState) {
        if (errorState != null) {
            // Scroll to top to show the error banner
            scrollState.animateScrollTo(0)
        }
    }

    // ✅ NEW: Check if inspection dialog should be shown
    val showInspectionDialog = uiState.formData["showInspectionDialog"]?.toBoolean() ?: false
    val inspectionMessage = uiState.formData["inspectionMessage"] ?: ""
    val canContinueToInspection = uiState.formData["canContinueToInspection"]?.toBoolean() ?: false

    // ✅ NEW: Check if payment retry dialog should be shown (paymentStatus = 1)
    val showPaymentRetryDialog = uiState.formData["showPaymentRetryDialog"]?.toBoolean() ?: false

    // ✅ NEW: Check if payment success dialog should be shown
    val showPaymentSuccessDialog = uiState.formData["showPaymentSuccessDialog"]?.toBoolean() ?: false
    val paymentSuccessMessage = uiState.formData["paymentSuccessMessage"] ?: "تم الدفع بنجاح"
    val paymentReceiptId = uiState.formData["paymentReceiptId"] ?: ""
    val paymentTimestamp = uiState.formData["paymentTimestamp"] ?: ""
    val paymentFinalTotal = uiState.formData["paymentFinalTotal"] ?: "0.0"

    // ✅ NEW: Show payment retry dialog when payment is in progress
    if (showPaymentRetryDialog) {
        PaymentRetryDialog(
            onContinue = {
                println("✅ User chose to retry payment")
                // Clear the retry dialog flag
                onFieldValueChange("showPaymentRetryDialog", "false")
                // Trigger payment again (PaymentManager will use paymentStatus from formData)
                onFieldValueChange("_triggerPaymentRetry", "true")
            },
            onClose = {
                println("❌ User chose to cancel payment retry")
                // Clear the retry dialog flag
                onFieldValueChange("showPaymentRetryDialog", "false")
                // Navigate back
                navController.popBackStack()
            }
        )
    }

    // ✅ NEW: Show inspection dialog when needed
    if (showInspectionDialog) {
        InspectionRequiredDialog(
            message = inspectionMessage,
            text = localizedApp(R.string.done),
            icon = Icons.Default.Done,
            onContinue = if (canContinueToInspection) {
                {
                    // User wants to continue - trigger inspection step injection
                    println("✅ User clicked Continue - injecting inspection step")
                    onFieldValueChange("showInspectionDialog", "false")
                    onFieldValueChange("injectInspectionStep", "true")
                }
            } else null,
            onDismiss = {
                // Clear the dialog flag
                onFieldValueChange("showInspectionDialog", "false")
                // Navigate back to home or request detail screen
                navController.popBackStack()
            }
        )
    }

    // ✅ NEW: Show inspection success dialog when inspection is submitted
    val showInspectionSuccessDialog = uiState.formData["showInspectionSuccessDialog"]?.toBoolean() ?: false
    val inspectionSuccessMessage = uiState.formData["inspectionSuccessMessage"] ?: ""
    val inspectionRequestId = uiState.formData["inspectionRequestId"] ?: ""

    if (showInspectionSuccessDialog) {
        InspectionRequiredDialog(
            message = "$inspectionSuccessMessage\n\nرقم طلب المعاينة: $inspectionRequestId",
            text = localizedApp(R.string.done),
            icon = Icons.Default.Done, // Use Done icon for success
            onContinue = null, // No continue button - just dismiss
            onDismiss = {
                // Clear the dialog flag
                onFieldValueChange("showInspectionSuccessDialog", "false")
                // Navigate back to home
                println("🏠 Inspection success - navigating back to home")
                navController.popBackStack()
            }
        )
    }

    // ✅ NEW: Show payment success dialog when needed
    if (showPaymentSuccessDialog) {
        val hasAcceptance = uiState.formData["hasAcceptance"]?.toBoolean() ?: true

        PaymentSuccessDialog(
            message = paymentSuccessMessage,
            receiptNumber = paymentReceiptId,
            paidAmount = "$paymentFinalTotal ريال عماني ",
            timestamp = paymentTimestamp,
            onDismiss = {
                // Clear the dialog flag
                onFieldValueChange("showPaymentSuccessDialog", "false")

                if (hasAcceptance) {
                    // hasAcceptance = 1: Navigate back to home (user needs to wait for approval)
                    navController.popBackStack()
                } else {
                    // hasAcceptance = 0: Stay in transaction and trigger refresh
                    // Payment is complete, request should now be ISSUED
                    // Set flag to show certificate button instead of pay button
                    onFieldValueChange("paymentCompleted", "true")
                    onFieldValueChange("_triggerRefresh", System.currentTimeMillis().toString())
                }
            }
        )
    }

    // ✅ NEW: Show certificate issued dialog for free services or already paid
    val shouldShowCertificate = uiState.formData["shouldShowCertificate"]?.toBoolean() ?: false
    val certificateIssued = uiState.formData["certificateIssued"]?.toBoolean() ?: false
    val certificateUrl = uiState.formData["certificateUrl"] ?: ""
    val isFreeService = uiState.formData["isFreeService"]?.toBoolean() ?: false
    // Parse multiple affected certificates stored as JSON array by PaymentManager
    val affectedCertificatesJson = uiState.formData["affectedCertificatesList"] ?: ""
    val isArabicLocale = LocalAppLocale.current.language == "ar"

    val affectedCertsList: List<AffectedCert> = remember(affectedCertificatesJson) {
        parseAffectedCertificates(affectedCertificatesJson)
    }

    if (shouldShowCertificate && certificateIssued) {
        val isArabic = isArabicLocale
        val items = buildList {
            if (isFreeService) {
                add(
                    SuccessDialogItem(
                        label = stringResource(R.string.service_type),
                        value = stringResource(R.string.free),
                        icon = "🎁"
                    )
                )
            }
            add(
                SuccessDialogItem(
                    label = stringResource(R.string.certificate_status),
                    value = stringResource(R.string.issued),
                    icon = "✅"
                )
            )
            // Add each affected cert as a summary item
            affectedCertsList.forEach { cert ->
                add(
                    SuccessDialogItem(
                        label = if (isArabic) cert.typeAr.ifBlank { cert.typeEn } else cert.typeEn,
                        value = cert.number,
                        icon = "📄"
                    )
                )
            }
        }

        if (affectedCertsList.isNotEmpty()) {
            // ── Bottom sheet for change-transaction multi-cert issuance ────
            val issuedCertItems = affectedCertsList.map { cert ->
                IssuedCertItem(
                    number = cert.number,
                    typeEn = cert.typeEn,
                    typeAr = cert.typeAr,
                    onView = {
                        val context = navController.context
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cert.url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.openFileViewerDialog(cert.url, cert.typeEn, "application/pdf")
                        }
                    }
                )
            }
            IssuedCertificatesBottomSheet(
                title = stringResource(R.string.certificates_issued_successfully),
                items = issuedCertItems,
                onDismiss = {
                    onFieldValueChange("shouldShowCertificate", "false")
                    onFieldValueChange("certificateIssued", "false")
                    navController.popBackStack()
                }
            )
        } else {
            // ── SuccessDialog for standard single-certificate transactions ─
            SuccessDialog(
                title = stringResource(R.string.certificate_issued_successfully),
                items = items,
                onDismiss = {
                    onFieldValueChange("shouldShowCertificate", "false")
                    onFieldValueChange("certificateIssued", "false")
                    navController.popBackStack()
                },
                onViewCertificate = if (certificateUrl.isNotEmpty()) {
                    {
                        val context = navController.context
                        val intent = Intent(Intent.ACTION_VIEW, certificateUrl.toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.openFileViewerDialog(certificateUrl, "Certificate", "application/pdf")
                        }
                    }
                } else null
            )
        }
    }

    // ✅ NEW: Show Payment WebView when PaymentManager triggers it
    val showPaymentWebView = uiState.formData["_triggerPaymentWebView"]?.toBoolean() ?: false
    val paymentRedirectHtml = uiState.formData["paymentRedirectHtml"] ?: ""
    val paymentRedirectSuccessUrl = uiState.formData["paymentRedirectSuccessUrl"] ?: ""
    val paymentRedirectCanceledUrl = uiState.formData["paymentRedirectCanceledUrl"] ?: ""

    if (showPaymentWebView && paymentRedirectHtml.isNotBlank()) {
        PaymentWebViewDialog(
            html = paymentRedirectHtml,
            successUrl = paymentRedirectSuccessUrl,
            canceledUrl = paymentRedirectCanceledUrl,
            onResult = { success ->
                if (success) {
                    // Mark to show success dialog and trigger a refresh of data
                    onFieldValueChange("showPaymentSuccessDialog", "true")
                    // Trigger a refresh so payment receipt can be reloaded if needed
                    onFieldValueChange("_triggerRefresh", System.currentTimeMillis().toString())
                } else {
                    // Payment canceled - show cancel as failure dialog (reuse success dialog flag set to false)
                    onFieldValueChange("showPaymentSuccessDialog", "false")
                    onFieldValueChange("paymentSuccessMessage", "Payment canceled or failed")
                }

                // Clear webview trigger and html
                onFieldValueChange("_triggerPaymentWebView", "false")
                onFieldValueChange("paymentRedirectHtml", "")
                onFieldValueChange("paymentRedirectSuccessUrl", "")
                onFieldValueChange("paymentRedirectCanceledUrl", "")
            },
            onDismiss = {
                // User closed webview without completing - clear trigger
                onFieldValueChange("_triggerPaymentWebView", "false")
            }
        )
    }

    // ✅ NEW: Handle API errors from centralized error state
    errorState?.let { error ->
        when (error) {
            is com.informatique.mtcit.common.AppError.ApiError -> {
                // ✅ Show ALL API errors as banner (not just 406)
                // Banner will be displayed below stepper in the layout
            }
            is com.informatique.mtcit.common.AppError.Unknown -> {
                // ✅ Show unknown errors as banner too
            }
            else -> {
                // Handle other error types if needed
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = extraColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = transactionTitle,
                        fontSize = 18.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
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
                            .background(extraColors.navy18223B)
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
//                actions = {
//                    Box(
//                        modifier = Modifier
//                            .padding(end = 12.dp)
//                            .size(38.dp)
//                            .clip(CircleShape)
//                            .border(
//                                width = 1.dp,
//                                color = Color(0xFF4A7BA7 ),
//                                shape = CircleShape
//                            )
//                            .shadow(
//                                elevation = 20.dp,
//                                shape = CircleShape,
//                                ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
//                                spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
//                            )
//                            .background(extraColors.navy18223B)
//                            .clickable { navController.navigate(NavRoutes.SettingsRoute.route) },
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Settings,
//                            contentDescription = "Settings",
//                            tint = extraColors.iconBack2
//                        )
//                    }
//                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Bottom Navigation Bar
            GenericNavigationBottomBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.steps.size,
                onPreviousClick = previousStep,
                onNextClick = {
                    // ✅ SIMPLIFIED: Always call nextStep() for ALL steps
                    // Let the strategy's processStepData() handle what happens
                    nextStep()
                },
                canProceed = if (isReviewStep) {
                    // On review step, require declaration to be accepted
                    declarationAccepted && uiState.canProceedToNext
                } else {
                    uiState.canProceedToNext
                },
                // Use viewModel processing flag to disable next button when heavy processing is running
                isSubmitting = submissionState is UIState.Loading || isProcessingNext,
                isReviewStep = isReviewStep,
                isProcessingStep = uiState.isProcessingStep,
                currentStepType = uiState.steps.getOrNull(uiState.currentStep)?.stepType, // ✅ Pass current step type
                formData = uiState.formData // ✅ Pass form data for payment status checks
            )
        }
    ) { paddingValues ->
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Show stepper only if not hiding first step, or if we're past the first step
            // AND ensure we have enough steps to display
            val shouldShowStepper = if (hideStepperForFirstStep) {
                // Only show stepper after first step (step > 0) and if we have at least 2 steps total
                val show = uiState.currentStep > 0 && uiState.steps.size > 1
                println("🔍 Stepper visibility - currentStep: ${uiState.currentStep}, totalSteps: ${uiState.steps.size}, shouldShow: $show")
                show
            } else {
                // Normal behavior: always show if we have steps
                uiState.steps.isNotEmpty()
            }

            if (shouldShowStepper) {

                val stepsToShow = if (hideStepperForFirstStep) {
                    // Exclude first step from stepper display
                    uiState.steps.drop(1).map { localizedApp(it.titleRes) }
                } else {
                    uiState.steps.map { localizedApp(it.titleRes) }
                }

                val currentStepIndex = if (hideStepperForFirstStep) {
                    // Adjust index: actual step 1→display 0, actual step 2→display 1
                    // (We already know currentStep > 0 from shouldShowStepper check)
                    uiState.currentStep - 1
                } else {
                    uiState.currentStep
                }

                val adjustedCompletedSteps = if (hideStepperForFirstStep) {
                    // Adjust completed steps indices (exclude step 0, shift others down)
                    uiState.completedSteps.filter { it > 0 }.map { it - 1 }.toSet()
                } else {
                    uiState.completedSteps
                }

                println("🔍 Stepper data - stepsToShow: ${stepsToShow.size}, currentIndex: $currentStepIndex, completed: $adjustedCompletedSteps")

                DynamicStepper(
                    steps = stepsToShow,
                    currentStep = currentStepIndex,
                    completedSteps = adjustedCompletedSteps,
                    onStepClick = { clickedStep ->
                        if (hideStepperForFirstStep) {
                            // Adjust back to actual step index
                            goToStep(clickedStep + 1)
                        } else {
                            goToStep(clickedStep)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 4.dp)
                )

                // ✅ Show ErrorBanner for ALL API errors (not just 406)
                errorState?.let { error ->
                    when (error) {
                        is com.informatique.mtcit.common.AppError.ApiError -> {
                            ErrorBanner(
                                message = error.message,
                                onDismiss = {
                                    (viewModel as? com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel)?.dismissError()
                                        ?: viewModel.clearError()
                                }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.Unauthorized -> {
                            // ✅ Special handling for 401 errors - show refresh token button
                            ErrorBanner(
                                message = error.message,
                                showRefreshButton = true,
                                onRefreshToken = {
                                    // Call refreshTokenAndRetry() which refreshes token and clears error on success
                                    (viewModel as? com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel)?.refreshTokenAndRetry()
                                },
                                onDismiss = {
                                    // Call dismissError() to manually dismiss the banner
                                    (viewModel as? com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel)?.dismissError()
                                        ?: viewModel.clearError()
                                }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.Unknown -> {
                            ErrorBanner(
                                message = error.message,
                                onDismiss = {
                                    (viewModel as? com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel)?.dismissError()
                                        ?: viewModel.clearError()
                                }
                            )
                        }
                        else -> {
                            // Other error types can be added here if needed
                        }
                    }
                }
            }

            // Form Content
            val currentStepData = uiState.steps.getOrNull(uiState.currentStep)
            if (currentStepData != null) {
                // Step Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = localizedApp(currentStepData.titleRes),
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = localizedApp(currentStepData.descriptionRes),
                            color = extraColors.textSubTitle.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Dynamic Form - Outside Card
                val componentStepData = ViewModelStepData(
                    titleRes = currentStepData.titleRes,
                    descriptionRes = currentStepData.descriptionRes,
                    fields = currentStepData.fields.map { field ->
                        updateFieldWithFormData(field, uiState.formData, uiState.fieldErrors)
                    }
                )

                // ✅ Observe lookup loading states for shimmer effect
                val lookupLoadingStates by viewModel.lookupLoadingStates.collectAsState()
                val loadedLookupData by viewModel.loadedLookupData.collectAsState()
                // ✅ INFINITE SCROLL: observe loading-more state
                val isLoadingMoreShips by viewModel.isLoadingMoreShips.collectAsState()

                DynamicStepForm(
                    stepData = componentStepData,
                    formData = uiState.formData,
                    onFieldChange = { fieldId, value, _ -> onFieldValueChange(fieldId, value) },
                    onFieldFocusLost = { fieldId, value -> onFieldFocusLost(fieldId, value) },
                    isFieldLoading = isFieldLoading,
                    showConditionalFields = { fieldId ->
                        when (fieldId) {
                            "companyName", "companyRegistrationNumber", "companyType" ->
                                uiState.formData["isCompany"] == "true"
                            else -> true
                        }
                    },
                    onOpenFilePicker = onOpenFilePicker,
                    onViewFile = onViewFile,
                    onRemoveFile = onRemoveFile,
                    allSteps = uiState.steps,
                    declarationAccepted = declarationAccepted,
                    onDeclarationChange = { accepted ->
                        viewModel.setDeclarationAccepted(accepted)
                    },
                    onTriggerNext = { viewModel.nextStep() },
                    validationState = if (viewModel is MarineRegistrationViewModel) {
                        viewModel.validationState.collectAsState().value
                    } else {
                        ValidationState.Idle
                    },
                    onMarineUnitSelected = if (viewModel is MarineRegistrationViewModel) {
                        { unitId -> viewModel.onMarineUnitSelected(unitId) }
                    } else {
                        null
                    },
                    // ✅ INFINITE SCROLL: wire load-more callbacks
                    onLoadMore = { viewModel.loadMoreShips() },
                    isLoadingMore = isLoadingMoreShips,
                    hasMore = !viewModel.isLastShipsPage,
                    // ✅ NEW: Pass lookup loading states for automatic shimmer
                    lookupLoadingStates = lookupLoadingStates,
                    loadedLookupData = loadedLookupData,
                    // ✅ NEW: Engine immediate edit/delete callbacks
                    onEditEngineImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { engine -> viewModel.updateEngineImmediate(engine) }
                    } else null,
                    onDeleteEngineImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { engine -> viewModel.deleteEngineImmediate(engine) }
                    } else null,
                    // ✅ NEW: Owner immediate edit/delete callbacks
                    onEditOwnerImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { owner -> viewModel.updateOwnerImmediate(owner) }
                    } else null,
                    onDeleteOwnerImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { owner -> viewModel.deleteOwnerImmediate(owner) }
                    } else null,
                    // ✅ NEW: Sailor immediate save/delete callbacks (Renew Navigation Permit only)
                    onSaveSailorImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { sailor -> viewModel.saveSailorImmediate(sailor) }
                    } else null,
                    onDeleteSailorImmediate = if (viewModel is MarineRegistrationViewModel) {
                        { sailor -> viewModel.deleteSailorImmediate(sailor) }
                    } else null,
                    // ✅ Ship details fetch for "عرض جميع البيانات"
                    fetchShipDetails = { shipId -> viewModel.loadShipCoreInfo(shipId) }
                )
            }
        }
    }
}

@Composable
fun GenericNavigationBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    canProceed: Boolean,
    isSubmitting: Boolean = false,
    isReviewStep: Boolean = false, // Add parameter to detect review step
    isProcessingStep: Boolean = false, // ✅ NEW: Loading state for Next button
    currentStepType: com.informatique.mtcit.business.transactions.shared.StepType? = null, // ✅ NEW: Current step type
    formData: Map<String, String> = emptyMap() // ✅ NEW: Form data for checking payment status
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(  bottom = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding() + 12.dp
            ),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessingStep, // ✅ Disable back button during processing
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = extraColors.startServiceButton,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = localizedApp(R.string.back_button),
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                        )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onNextClick,
                enabled = canProceed && !isSubmitting && !isProcessingStep, // ✅ Disable during processing
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.startServiceButton,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder(
                    enabled = canProceed && !isSubmitting && !isProcessingStep
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            localizedApp(R.string.loading),
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    // Show "Pay" on PAYMENT step, "Accept & Send" on review step, or "Next" otherwise
                    when {
                        currentStepType == com.informatique.mtcit.business.transactions.shared.StepType.PAYMENT -> {
                            // ✅ Check if should issue certificate instead of pay
                            val shouldIssueCertificate = formData["shouldIssueCertificate"]?.toBoolean() ?: false
                            val isFreeService = formData["isFreeService"]?.toBoolean() ?: false
                            val paymentAlreadyCompleted = formData["paymentAlreadyCompleted"]?.toBoolean() ?: false
                            val paymentCompleted = formData["paymentCompleted"]?.toBoolean() ?: false // ✅ NEW: Check if payment just completed

                            val buttonText = when {
                                shouldIssueCertificate || isFreeService || paymentAlreadyCompleted || paymentCompleted -> {
                                    // Show "Issue Certificate" for free services, already paid, or just paid
                                    if (LocalAppLocale.current.language == "ar")
                                        "اصدار الشهادة"
                                    else "Issue Certificate"
                                }
                                else -> {
                                    // Show "Pay" for normal payment flow
                                    localizedApp(R.string.pay_button)
                                }
                            }

                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        // ✅ NEW: Force "Next" button for LOGIN_METHOD_SELECTION
                        currentStepType == com.informatique.mtcit.business.transactions.shared.StepType.LOGIN_METHOD_SELECTION -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = localizedApp(R.string.next_button),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        isReviewStep || currentStep >= totalSteps - 1 -> {
                            // Show "Accept & Send" on review step or last step
                            Text(localizedApp(R.string.accept_and_send) , fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        else -> {
                            // Show "Next" button for regular steps
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = localizedApp(R.string.next_button),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Default.NavigateNext,
//                                    contentDescription = null,
//                                    modifier = Modifier.size(18.dp)
//                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun updateFieldWithFormData(
    field: FormField,
    formData: Map<String, String>,
    fieldErrors: Map<String, String>
): FormField {
    val value = formData[field.id] ?: ""
    val error = fieldErrors[field.id]

    // Localize the label if it has a resource ID
    val localizedLabel = if (field.labelRes != 0) localizedApp(field.labelRes) else field.label

    return when (field) {
        is FormField.TextField -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.DropDown -> field.copy(
            label = localizedLabel,
            options = if (field.optionRes.isNotEmpty()) field.optionRes.map { localizedApp(it) } else field.options,
            selectedOption = value.ifEmpty { null },
            value = value,
            error = error
        )
        is FormField.CheckBox -> field.copy(
            label = localizedLabel,
            checked = value == "true",
            error = error
        )
        is FormField.DatePicker -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.FileUpload -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.OwnerList -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" }, // Ensure valid JSON
            error = error
        )
        is FormField.MarineUnitSelector -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )

        is FormField.SelectableList<*> -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.EngineList -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" },
            error = error
        )
        is FormField.CertificatesList -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" },
            error = error
        )
        is FormField.RadioGroup -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" },
            error = error
        )
        is FormField.InfoCard -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.PaymentDetails -> field.copy(
            // Payment details are populated from formData, just pass through
            value = value,
            error = error
        )
        is FormField.PhoneNumberField -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
        is FormField.OTPField -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )

        is FormField.SailorList -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" },
            error = error
        )

        is FormField.MultiSelectDropDown -> field.copy(
            label = localizedLabel,
            value = value.ifEmpty { "[]" },
            // Normalize decoded selected names and map to actual options so matching works reliably
            selectedOptions = try {
                val decoded = kotlinx.serialization.json.Json.decodeFromString<List<String>>(value.ifEmpty { "[]" })
                val normalized = decoded.map { it.trim() }
                // Preserve option strings from field.options when they match decoded values (trimmed)
                val matched = field.options.filter { opt -> normalized.contains(opt.trim()) }
                println("🔍 MultiSelect field='${field.id}' options=${field.options.size} decoded=${decoded} matched=${matched}")
                matched
            } catch (e: Exception) {
                println("⚠️ Failed to decode MultiSelect value for field='${field.id}': ${e.message}")
                emptyList()
            },
            error = error
        )
        is FormField.CurrentValueCard -> field.copy(
            label = localizedLabel,
            value = value,
            error = error
        )
    }
}

// ── Affected Certificates (multi-cert types 10/11/12/13) ─────────────────────

data class AffectedCert(
    val number: String,
    val typeEn: String,
    val typeAr: String,
    val url: String
)

/**
 * Parses the JSON array stored by PaymentManager as "affectedCertificatesList":
 *   [{"number":"MTCIT-...","typeEn":"Nav license","typeAr":"...","url":"https://..."},...]
 * Uses kotlinx.serialization.json for reliable parsing of all fields including URLs.
 */
fun parseAffectedCertificates(json: String): List<AffectedCert> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val jsonParser = Json { ignoreUnknownKeys = true }
        val array = jsonParser.parseToJsonElement(json).jsonArray
        array.mapNotNull { element ->
            val obj = element.jsonObject
            val number = obj["number"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            AffectedCert(
                number = number,
                typeEn = obj["typeEn"]?.jsonPrimitive?.content ?: "",
                typeAr = obj["typeAr"]?.jsonPrimitive?.content ?: "",
                url = obj["url"]?.jsonPrimitive?.content ?: ""
            )
        }
    } catch (e: Exception) {
        println("⚠️ parseAffectedCertificates failed: ${e.message}")
        emptyList()
    }
}
