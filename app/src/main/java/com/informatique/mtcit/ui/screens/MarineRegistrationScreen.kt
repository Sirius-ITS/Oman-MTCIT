package com.informatique.mtcit.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.localizedApp
import androidx.core.net.toUri
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.informatique.mtcit.ui.viewmodels.StepData
import com.informatique.mtcit.util.UriPermissionManager


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
                    val uri = event.fileUri.toUri()

                    // CRITICAL: Grant permission to the current activity/context before navigation
                    // This ensures the permission persists when navigating to FileViewerScreen
                    try {
                        // First, try to take persistent permission
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d("MarineRegistration", "Took persistent permission for $uri")
                    } catch (e: SecurityException) {
                        // If persistent permission fails, the temporary permission from file picker should still work
                        // But we need to ensure it's granted to the activity
                        android.util.Log.w("MarineRegistration", "Could not take persistent permission, using temporary: ${e.message}")
                    }

                    val fileName = getFileNameFromUri(context, uri)

                    // Navigate to internal file viewer
                    val encodedUri = java.net.URLEncoder.encode(event.fileUri, "UTF-8")
                    val encodedFileName = java.net.URLEncoder.encode(fileName ?: "File", "UTF-8")
                    navController.navigate("file_viewer/$encodedUri/$encodedFileName")

                    viewModel.clearFileNavigationEvent()
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
                navController.navigate("pay")
                viewModel.resetSubmissionState()
            }

            is UIState.Failure -> {
                viewModel.resetSubmissionState()
            }

            else -> { /* No action needed */
            }
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

    // Main UI - Use TransactionFormContent for ALL steps including review
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