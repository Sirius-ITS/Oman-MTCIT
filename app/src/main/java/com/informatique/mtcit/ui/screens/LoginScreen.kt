package com.informatique.mtcit.ui.screens

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.LoginViewModel

/**
 * Login/Registration Screen
 *
 * Handles user authentication flow before accessing transactions:
 * - Step 1: Login method selection (Mobile/ID)
 * - Step 2: Mobile phone verification
 * - Step 3: OTP verification
 *
 * After successful login, navigates to the target transaction screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    targetTransactionType: String,
    @Suppress("UNUSED_PARAMETER") categoryId: String,
    @Suppress("UNUSED_PARAMETER") subCategoryId: String,
    @Suppress("UNUSED_PARAMETER") transactionId: String
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()

    // Handle OAuth navigation - Navigate to OAuth WebView
    LaunchedEffect(Unit) {
        viewModel.navigateToOAuth.collect {
            println("🚀 Navigating to OAuth WebView...")
            navController.navigate(NavRoutes.OAuthWebViewRoute.route)
        }
    }

    // Handle login completion - Navigate to target transaction
    LaunchedEffect(Unit) {
        viewModel.loginComplete.collect { isComplete ->
            if (isComplete) {
                println("✅ Login complete! Navigating to transaction: $targetTransactionType")

                // Navigate to the target transaction screen
                val route = when (targetTransactionType) {
                    "TEMPORARY_REGISTRATION_CERTIFICATE", "SHIP_REGISTRATION" -> NavRoutes.ShipRegistrationRoute.route
                    "PERMANENT_REGISTRATION_CERTIFICATE", "PERMANENT_REGISTRATION" -> NavRoutes.PermanentRegistrationRoute.route
                    "REQUEST_FOR_INSPECTION", "REQUEST_INSPECTION" -> NavRoutes.RequestForInspection.route
                    "SUSPEND_REGISTRATION", "SUSPEND_PERMANENT_REGISTRATION" -> NavRoutes.SuspendRegistrationRoute.route
                    "CANCEL_REGISTRATION", "CANCEL_PERMANENT_REGISTRATION" -> NavRoutes.CancelRegistrationRoute.route
                    "MORTGAGE_CERTIFICATE" -> NavRoutes.MortgageCertificateRoute.route
                    "RELEASE_MORTGAGE" -> NavRoutes.ReleaseMortgageRoute.route
                    "ISSUE_NAVIGATION_PERMIT" -> NavRoutes.IssueNavigationPermitRoute.route
                    "RENEW_NAVIGATION_PERMIT" -> NavRoutes.RenewNavigationPermitRoute.route
                    "SUSPEND_NAVIGATION_PERMIT" -> NavRoutes.SuspendNavigationPermitRoute.route
                    "SHIP_NAME_CHANGE" -> NavRoutes.ChangeNameOfShipOrUnitRoute.route
                    "CAPTAIN_NAME_CHANGE" -> NavRoutes.CaptainNameChangeRoute.route
                    "SHIP_ACTIVITY_CHANGE" -> NavRoutes.ShipActivityChangeRoute.route
                    "SHIP_PORT_CHANGE" -> NavRoutes.ShipPortChangeRoute.route
                    "SHIP_OWNERSHIP_CHANGE" -> NavRoutes.ShipOwnershipChangeRoute.route
                    else -> NavRoutes.ShipRegistrationRoute.route
                }

                navController.navigate(route) {
                    // Remove login screen from back stack
                    popUpTo(NavRoutes.LoginRoute.route) { inclusive = true }
                }
            }
        }
    }


    // Show loading during initialization
    if (uiState.isLoading || !uiState.isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    // Reuse TransactionFormContent component (same as MarineRegistrationScreen)
    TransactionFormContent(
        navController = navController,
        uiState = uiState,
        submissionState = submissionState,
        transactionTitle = localizedApp(com.informatique.mtcit.R.string.login),
        onFieldValueChange = viewModel::onFieldValueChange,
        onFieldFocusLost = viewModel::onFieldFocusLost,
        isFieldLoading = viewModel::isFieldLoading,
        onOpenFilePicker = viewModel::openFilePicker,
        onViewFile = viewModel::viewFile,
        onRemoveFile = viewModel::removeFile,
        goToStep = viewModel::goToStep,
        previousStep = viewModel::previousStep,
        nextStep = viewModel::nextStep,
        submitForm = viewModel::submitForm,
        viewModel = viewModel,
        hideStepperForFirstStep = true // Hide stepper in first step, show "1 of 2" starting from step 2
    )
}
