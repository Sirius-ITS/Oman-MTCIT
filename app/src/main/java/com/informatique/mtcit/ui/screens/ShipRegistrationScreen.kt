package com.informatique.mtcit.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.*
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.ShipRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.viewmodels.StepData as ViewModelStepData
import com.informatique.mtcit.ui.base.UIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipRegistrationScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ShipRegistrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val fileNavigationEvent by viewModel.fileNavigationEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State for file operations
    var showFilePicker by remember { mutableStateOf(false) }
    var showPdfViewer by remember { mutableStateOf(false) }
    var currentFilePickerField by remember { mutableStateOf("") }
    var currentFilePickerTypes by remember { mutableStateOf(listOf<String>()) }
    var currentViewerFileUri by remember { mutableStateOf<Uri?>(null) }
    var currentViewerTitle by remember { mutableStateOf("") }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.onFileSelected(currentFilePickerField, it.toString())
                viewModel.onFieldValueChange(currentFilePickerField, it.toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Error selecting file: ${e.message}", Toast.LENGTH_SHORT).show()
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

                    // Launch file picker directly
                    val mimeTypes = event.allowedTypes.map { type ->
                        when (type.lowercase()) {
                            "pdf" -> "application/pdf"
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "doc" -> "application/msword"
                            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            else -> "application/*"
                        }
                    }.toTypedArray()

                    filePickerLauncher.launch(mimeTypes)
                }

                is FileNavigationEvent.ViewFile -> {
                    val uri = Uri.parse(event.fileUri)
                    when {
                        event.fileType.contains("pdf") -> {
                            currentViewerFileUri = uri
                            currentViewerTitle = "PDF Document"
                            showPdfViewer = true
                        }
                        event.fileType.startsWith("image/") -> {
                            // For images, open with external app or show in full screen
                            openFileOutsideApp(context, uri, event.fileType)
                        }
                        else -> {
                            // For other files, open with external app
                            openFileOutsideApp(context, uri, event.fileType)
                        }
                    }
                }

                is FileNavigationEvent.RemoveFile -> {
                    viewModel.onFileRemoved(event.fieldId)
                    viewModel.onFieldValueChange(event.fieldId, "")
                }
            }
        }
    }

    // Provide context to ViewModel for localization
    LaunchedEffect(Unit) {
        viewModel.setContextProvider { context }
    }

    // Handle submission result
    LaunchedEffect(submissionState) {
        when (submissionState) {
            is UIState.Success -> {
                navController.navigateUp()
                viewModel.resetSubmissionState()
            }
            is UIState.Failure -> {
                viewModel.resetSubmissionState()
            }
            else -> { /* No action needed */ }
        }
    }

    // Show loading during ViewModel initialization
    if (uiState.isLoading || !uiState.isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Main UI - Remove Scaffold and use Column directly for edge-to-edge content
    Column(modifier = Modifier.fillMaxSize()) {
        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header with Back Button and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = localizedApp(R.string.back_button),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = localizedApp(R.string.issuance_of_a_temporary_registration_certificate_for_a_marine_unit_vessel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Modern Stepper using ViewModel data
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                DynamicStepper(
                    steps = uiState.steps.map { localizedApp(it.titleRes) },
                    currentStep = uiState.currentStep,
                    completedSteps = uiState.completedSteps,
                    onStepClick = viewModel::goToStep,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Form Content Card using ViewModel data
            val currentStepData = uiState.steps.getOrNull(uiState.currentStep)
            if (currentStepData != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Step Header
                        Text(
                            text = localizedApp(currentStepData.titleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = localizedApp(currentStepData.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Convert ViewModel StepData to Component StepData for DynamicStepForm
                        // Now we only need to update field values and errors, labels are already correct
                        val componentStepData = ViewModelStepData(
                            titleRes = currentStepData.titleRes,
                            descriptionRes = currentStepData.descriptionRes,
                            fields = currentStepData.fields.map { field ->
                                updateFieldWithFormData(field, uiState.formData, uiState.fieldErrors)
                            }
                        )

                        DynamicStepForm(
                            stepData = componentStepData,
                            onFieldChange = viewModel::onFieldValueChange,
                            onCompanyRegistrationFocusLost = viewModel::onCompanyRegistrationNumberFocusLost,
                            isFieldLoading = viewModel::isFieldLoading,
                            showConditionalFields = { fieldId ->
                                when (fieldId) {
                                    "companyName", "companyRegistrationNumber", "companyType" ->
                                        uiState.formData["isCompany"] == "true"
                                    else -> true
                                }
                            },
                            onOpenFilePicker = { fieldId, allowedTypes ->
                                viewModel.openFilePicker(fieldId, allowedTypes)
                            },
                            onViewFile = { fileUri, fileType ->
                                viewModel.viewFile(fileUri, fileType)
                            },
                            onRemoveFile = { fieldId ->
                                viewModel.removeFile(fieldId)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar using ViewModel
        NavigationBottomBar(
            currentStep = uiState.currentStep,
            totalSteps = uiState.steps.size,
            onPreviousClick = viewModel::previousStep,
            onNextClick = {
                if (uiState.currentStep < uiState.steps.size - 1) {
                    viewModel.nextStep()
                } else {
                    viewModel.submitForm()
                }
            },
            canProceed = uiState.canProceedToNext,
            isSubmitting = submissionState is UIState.Loading
        )
    }

    // PDF Viewer Dialog
    if (showPdfViewer && currentViewerFileUri != null) {
        PdfViewerDialog(
            uri = currentViewerFileUri!!,
            title = currentViewerTitle,
            onDismiss = { showPdfViewer = false }
        )
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
            label = localizedLabel, // Preserve localized label
            value = value,
            error = error
        )
        is FormField.DropDown -> field.copy(
            label = localizedLabel, // Preserve localized label
            options = if (field.optionRes.isNotEmpty()) field.optionRes.map { localizedApp(it) } else field.options,
            selectedOption = if (value.isNotEmpty()) value else null,
            value = value,
            error = error
        )
        is FormField.CheckBox -> field.copy(
            label = localizedLabel, // Preserve localized label
            checked = value == "true",
            error = error
        )
        is FormField.DatePicker -> field.copy(
            label = localizedLabel, // Preserve localized label
            value = value,
            error = error
        )
        is FormField.FileUpload -> field.copy(
            label = localizedLabel, // Preserve localized label
            value = value,
            error = error
        )
    }
}

@Composable
private fun NavigationBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    canProceed: Boolean,
    isSubmitting: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(localizedApp(R.string.back_button))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onNextClick,
                enabled = canProceed && !isSubmitting,
                modifier = Modifier.weight(1f)
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
private fun PdfViewerDialog(
    uri: Uri,
    title: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                PdfViewerScreen(
                    fileUri = uri,
                    title = title,
                    onBack = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
