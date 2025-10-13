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
import com.informatique.mtcit.ui.viewmodels.ShipDataModificationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.localizedApp
import androidx.core.net.toUri

/**
 * Ship Data Modification Screen
 *
 * Handles Ship Data Modifications Category (تعديل بيانات السفينة):
 * - Ship Registration
 * - Ship Name Change
 * - Ship Dimensions Change
 * - Captain Name Change
 * - Ship Activity Change
 * - Ship Engine Change
 * - Ship Port Change
 * - Ship Ownership Change
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipDataModificationScreen(
    navController: NavController,
    transactionType: TransactionType
) {
    val viewModel: ShipDataModificationViewModel = hiltViewModel()
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
        transactionTitle = getShipDataModificationTitle(transactionType),
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
private fun getShipDataModificationTitle(transactionType: TransactionType): String {
    return when (transactionType) {
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
