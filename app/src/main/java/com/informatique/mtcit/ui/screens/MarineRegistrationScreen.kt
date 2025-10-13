package com.informatique.mtcit.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.localizedApp
import androidx.core.net.toUri

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
    transactionType: TransactionType
) {
    val viewModel: MarineRegistrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val fileNavigationEvent by viewModel.fileNavigationEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize transaction type on first composition
    LaunchedEffect(transactionType) {
        viewModel.initializeTransaction(transactionType)
    }

    // State for file operations
    var currentFilePickerField by remember { mutableStateOf("") }
    var currentFilePickerTypes by remember { mutableStateOf(listOf<String>()) }

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

                    val mimeTypes = event.allowedTypes.map { type ->
                        when (type.lowercase()) {
                            "pdf" -> "application/pdf"
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "doc" -> "application/msword"
                            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            "xls" -> "application/vnd.ms-excel"
                            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            else -> "application/*"
                        }
                    }.toTypedArray()

                    filePickerLauncher.launch(mimeTypes)
                }

                is FileNavigationEvent.ViewFile -> {
                    val uri = event.fileUri.toUri()
                    viewModel.openFileOutsideApp(context, uri, event.fileType)
                }

                is FileNavigationEvent.RemoveFile -> {
                    viewModel.onFieldValueChange(event.fieldId, "")
                }
            }
        }
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

    // Main UI
    TransactionFormContent(
        navController = navController,
        uiState = uiState,
        submissionState = submissionState,
        transactionTitle = getMarineRegistrationTitle(transactionType),
        onFieldValueChange = viewModel::onFieldValueChange,
        onFieldFocusLost = viewModel::onFieldFocusLost,
        isFieldLoading = viewModel::isFieldLoading,
        onOpenFilePicker = viewModel::openFilePicker,
        onViewFile = viewModel::viewFile,
        onRemoveFile = viewModel::removeFile,
        goToStep = viewModel::goToStep,
        previousStep = viewModel::previousStep,
        nextStep = viewModel::nextStep,
        submitForm = viewModel::submitForm
    )
}

@Composable
private fun getMarineRegistrationTitle(transactionType: TransactionType): String {
    return when (transactionType) {
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_temporary_registration_certificate)
        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_permanent_registration_certificate)
        TransactionType.SUSPEND_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_suspend_permanent_registration)
        TransactionType.CANCEL_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_cancel_permanent_registration)
        TransactionType.MORTGAGE_CERTIFICATE -> localizedApp(R.string.transaction_mortgage_certificate)
        TransactionType.RELEASE_MORTGAGE -> localizedApp(R.string.transaction_release_mortgage)
        else -> "Unknown Transaction"
    }
}
