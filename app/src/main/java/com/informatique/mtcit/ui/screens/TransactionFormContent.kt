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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    viewModel: BaseTransactionViewModel
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
                        nextStep()
                    } else {
                        submitForm()
                    }
                },
                canProceed = uiState.canProceedToNext,
                isSubmitting = submissionState is UIState.Loading
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
            DynamicStepper(
                steps = uiState.steps.map { localizedApp(it.titleRes) },
                currentStep = uiState.currentStep,
                completedSteps = uiState.completedSteps,
                onStepClick = goToStep,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp , bottom = 4.dp)
            )

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
//                    onDeclarationChange = { accepted ->
//                        declarationAccepted = accepted
//                    },
                    onTriggerNext = { viewModel.nextStep() }, // ✅ مرر الـ ViewModel function
                    navController = navController
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
    isSubmitting: Boolean = false
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
                if (currentStep < totalSteps - 1) {
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
                } else {
                    Text(localizedApp(R.string.submit_button))
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

    }
}
