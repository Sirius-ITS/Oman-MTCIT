package com.informatique.mtcit.ui.screens

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

    // Track declaration acceptance state for review step
    var declarationAccepted by remember { mutableStateOf(false) }

    // Check if current step is the review step (last step with no fields)
    val isReviewStep = uiState.steps.getOrNull(uiState.currentStep)?.fields?.isEmpty() == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = extraColors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = transactionTitle,
                        fontSize = 18.sp,
                        color = extraColors.whiteInDarkMode,
                        fontWeight = FontWeight.Medium,
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
                            .background(extraColors.navy18223B)
                            .clickable { navController.navigate(NavRoutes.SettingsRoute.route) },
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
            // Bottom Navigation Bar
            GenericNavigationBottomBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.steps.size,
                onPreviousClick = previousStep,
                onNextClick = {
                    if (uiState.currentStep < uiState.steps.size - 1) {
                        // Not on last step - check if we're on review step
                        if (isReviewStep && viewModel is MarineRegistrationViewModel) {
                            // On review step for Marine Registration - validate before proceeding
                            (viewModel as MarineRegistrationViewModel).validateOnReviewStep()
                        } else {
                            // Regular next step
                            nextStep()
                        }
                    } else {
                        // On last step (final submission)
                        if (viewModel is MarineRegistrationViewModel) {
                            (viewModel as MarineRegistrationViewModel).validateAndSubmit()
                        } else {
                            submitForm()
                        }
                    }
                },
                canProceed = if (isReviewStep) {
                    // On review step, require declaration to be accepted
                    declarationAccepted && uiState.canProceedToNext
                } else {
                    uiState.canProceedToNext
                },
                isSubmitting = submissionState is UIState.Loading,
                isReviewStep = isReviewStep // Pass the review step flag
            )
        }
    ) { paddingValues ->
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = extraColors.whiteInDarkMode,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = localizedApp(currentStepData.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textSubTitle
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
                    onDeclarationChange = { accepted ->
                        declarationAccepted = accepted
                    },
                    onTriggerNext = { viewModel.nextStep() }, // ✅ مرر الـ ViewModel function
                    // Pass only validation state for loading indicator
                    validationState = if (viewModel is MarineRegistrationViewModel) {
                        viewModel.validationState.collectAsState().value
                    } else {
                        ValidationState.Idle
                    },
                    // Pass unit selection callback - errors navigate to RequestDetailScreen
                    onMarineUnitSelected = if (viewModel is MarineRegistrationViewModel) {
                        { unitId -> viewModel.onMarineUnitSelected(unitId) }
                    } else {
                        null
                    }
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
    isReviewStep: Boolean = false // Add parameter to detect review step
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(bottom = 18.dp, top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = extraColors.startServiceButton,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(localizedApp(R.string.back_button))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onNextClick,
                enabled = canProceed && !isSubmitting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.startServiceButton,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder(
                    enabled = canProceed && !isSubmitting
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                // Show "Accept & Send" on review step OR last step
                if (isReviewStep || currentStep >= totalSteps - 1) {
                    Text(localizedApp(R.string.accept_and_send))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(localizedApp(R.string.next_button))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.NavigateNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
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
    }
}
