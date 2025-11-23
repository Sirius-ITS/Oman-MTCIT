package com.informatique.mtcit.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.data.model.category.Transaction
import com.informatique.mtcit.ui.defaultEnterTransition
import com.informatique.mtcit.ui.defaultExitTransition
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.screens.ComingSoonScreen
import com.informatique.mtcit.ui.screens.FileViewerScreen
import com.informatique.mtcit.ui.screens.HomePageScreen
import com.informatique.mtcit.ui.screens.LanguageScreen
import com.informatique.mtcit.ui.screens.LoginScreen
import com.informatique.mtcit.ui.screens.MainCategoriesScreen
import com.informatique.mtcit.ui.screens.MarineRegistrationScreen
import com.informatique.mtcit.ui.screens.NotificationScreen
import com.informatique.mtcit.ui.screens.PaymentDetailsScreen
import com.informatique.mtcit.ui.screens.PaymentSuccessScreen
import com.informatique.mtcit.ui.screens.ProfileScreen
import com.informatique.mtcit.ui.screens.RequestDetail
import com.informatique.mtcit.ui.screens.RequestDetailScreen
import com.informatique.mtcit.ui.screens.SettingsScreen
import com.informatique.mtcit.ui.screens.ShipDataModificationScreen
import com.informatique.mtcit.ui.screens.TransactionListScreen
import com.informatique.mtcit.ui.screens.TransactionRequirementsScreen
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@Composable
fun NavHost(themeViewModel: ThemeViewModel, navigationManager: NavigationManagerImpl){

    val sharedUserViewModel: SharedUserViewModel = hiltViewModel()

    val navController = rememberNavController()

    val categories = LocalCategories.current

    LaunchedEffect(navController) {
        navigationManager.navigationCommands.collect { command ->
            when (command) {
                is NavigationCommand.Navigate -> {
                    navController.navigate(command.route) {
                        command.popUpTo?.let { route ->
                            popUpTo(route) {
                                inclusive = command.inclusive
                            }
                        }
                        launchSingleTop = command.singleTop
                    }
                }

                NavigationCommand.NavigateBack -> {
                    navController.popBackStack()
                }

                NavigationCommand.NavigateUp -> {
                    navController.navigateUp()
                }

                is NavigationCommand.PopBackStackTo -> {
                    navController.popBackStack(
                        route = command.route,
                        inclusive = command.inclusive
                    )
                }

                is NavigationCommand.NavigateAndClearBackStack -> {
                    navController.navigate(command.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                is NavigationCommand.NavigateWithArgs -> {
                    navController.navigate("${command.route}/${Uri.encode(command.data)}")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HomeRoute.route
    ) {

        composable(NavRoutes.HomeRoute.route) {
            HomePageScreen(navController = navController)
        }

        // ✅ Login Screen - handles authentication before accessing transactions
        composable(
            route = NavRoutes.LoginRoute.route,
            arguments = listOf(
                navArgument("targetTransactionType") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("subCategoryId") { type = NavType.StringType },
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val targetTransactionType = backStackEntry.arguments?.getString("targetTransactionType") ?: ""
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""

            LoginScreen(
                navController = navController,
                targetTransactionType = targetTransactionType,
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                transactionId = transactionId
            )
        }

        // ⚙️ شاشة الإعدادات (اختيار الثيم)
        composable(
            route = NavRoutes.SettingsRoute.route,
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() }
        ) {
            SettingsScreen(
                navController = navController,
                sharedUserViewModel = sharedUserViewModel,
                viewModel = themeViewModel
            )
        }

        // Home Screen - Shows categories with sub-categories
        composable(
            route = NavRoutes.MainCategoriesRoute.route,
            arguments = listOf(navArgument("categoryId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            MainCategoriesScreen(navController, categoryId)
        }
        composable(
            route = NavRoutes.MainCategoriesRouteWithoutID.route)
        { backStackEntry ->
            MainCategoriesScreen(navController, "")
        }
        composable(
            route = NavRoutes.ProfileScreenRoute.route)
        { backStackEntry ->
            ProfileScreen(navController)
        }
        composable(
            route = NavRoutes.NotificationScreen.route)
        { backStackEntry ->
            NotificationScreen(navController)
        }
        // Transaction List Screen - Shows transactions for selected sub-category
        composable(NavRoutes.TransactionListRoute.route) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
            TransactionListScreen(navController, categoryId, subCategoryId)
        }

        // Requirements screen: show transaction requirements before going to form/steps
        composable(NavRoutes.TransactionRequirementRoute.route) { backStackEntry ->
            val data = backStackEntry.arguments?.getString("transactionId") ?: ""
            val transaction = Json.decodeFromString<Transaction>(data)

            TransactionRequirementsScreen(
                onStart = {
                    // ✅ Navigate to LoginScreen first
                    // Map transaction ID to transaction type name
                    val transactionTypeName = when (transaction.id) {
                        7 -> "TEMPORARY_REGISTRATION_CERTIFICATE"
                        8 -> "PERMANENT_REGISTRATION_CERTIFICATE"
                        9 -> "REQUEST_INSPECTION"
                        10 -> "SUSPEND_REGISTRATION"
                        11 -> "CANCEL_REGISTRATION"
                        12 -> "MORTGAGE_CERTIFICATE"
                        13 -> "RELEASE_MORTGAGE"
                        14 -> "ISSUE_NAVIGATION_PERMIT"
                        15 -> "RENEW_NAVIGATION_PERMIT"
                        16 -> "SUSPEND_NAVIGATION_PERMIT"
                        17 -> "SHIP_NAME_CHANGE"
                        18 -> "CAPTAIN_NAME_CHANGE"
                        19 -> "SHIP_ACTIVITY_CHANGE"
                        20 -> "SHIP_PORT_CHANGE"
                        21 -> "SHIP_OWNERSHIP_CHANGE"
                        else -> "TEMPORARY_REGISTRATION_CERTIFICATE"
                    }

                    println("🚀 Navigating to Login with transactionType: $transactionTypeName")

                    navController.navigate(
                        NavRoutes.LoginRoute.createRoute(
                            targetTransactionType = transactionTypeName,
                            categoryId = "0",
                            subCategoryId = "0",
                            transactionId = transaction.id.toString()
                        )
                    )
                },
                onBack = { navController.popBackStack() },
                // parentTitleRes = parentTitleRes,
                transaction = transaction,
                navController = navController,
                transactionId = transaction.id
            )

            /*val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val parentTitleResStr = backStackEntry.arguments?.getString("parentTitleRes") ?: ""

            // Use the same TransactionListViewModel to load the list and find the transaction
            val txListVm: TransactionListViewModel = hiltViewModel()
            LaunchedEffect(categoryId, subCategoryId) {
                txListVm.loadTransactions(categories, categoryId, subCategoryId)
            }
            val transactions by txListVm.transactions.collectAsState()
            val transaction = transactions.find { it.id == transactionId }

            val parentTitleRes: Int? = parentTitleResStr.toIntOrNull()

            if (transaction != null) {
                TransactionRequirementsScreen(
                    transaction = transaction,
                    onStart = {
                        // ✅ Navigate to LoginScreen first, which will redirect to transaction after login
                        // Extract transaction type from route name
                        val transactionTypeName = transaction.route.uppercase()
                            .replace("_ROUTE", "")
                            .replace("_FORM", "")

                        navController.navigate(
                            NavRoutes.LoginRoute.createRoute(
                                targetTransactionType = transactionTypeName,
                                categoryId = categoryId,
                                subCategoryId = subCategoryId,
                                transactionId = transactionId
                            )
                        )
                    },
                    onBack = { navController.popBackStack() },
                    parentTitleRes = parentTitleRes,
                    navController = navController,
                    transactionId = transactionId
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }*/
        }

        // ========== TRANSACTION FORMS ==========

        // Ship Registration Forms
        composable(NavRoutes.ShipRegistrationRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE
            )
        }

        composable(NavRoutes.RequestForInspection.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.REQUEST_FOR_INSPECTION
            )
        }
        composable(NavRoutes.PermanentRegistrationRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE
            )
        }

        composable(NavRoutes.SuspendRegistrationRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.SUSPEND_PERMANENT_REGISTRATION
            )
        }

        composable(NavRoutes.CancelRegistrationRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.CANCEL_PERMANENT_REGISTRATION
            )
        }

        composable(NavRoutes.MortgageCertificateRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.MORTGAGE_CERTIFICATE
            )
        }

        composable(NavRoutes.ReleaseMortgageRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.RELEASE_MORTGAGE
            )
        }

        // Navigation Forms
        composable(NavRoutes.IssueNavigationPermitRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.ISSUE_NAVIGATION_PERMIT
            )
        }

        composable(NavRoutes.RenewNavigationPermitRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.RENEW_NAVIGATION_PERMIT
            )
        }

        composable(NavRoutes.SuspendNavigationPermitRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Suspend Navigation Permit"
            )
        }

        // Ship Data Modification Forms
        composable(NavRoutes.ShipNameChangeRoute.route) {
            ShipDataModificationScreen(
                navController = navController,
                transactionType = TransactionType.SHIP_NAME_CHANGE
            )
        }

        composable(NavRoutes.CaptainNameChangeRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Captain Name Change"
            )
        }

        composable(NavRoutes.ShipActivityChangeRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Ship Activity Change"
            )
        }

        composable(NavRoutes.ShipPortChangeRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Ship Port Change"
            )
        }

        composable(NavRoutes.ShipOwnershipChangeRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Ship Ownership Change"
            )
        }

        // ========== ADDITIONAL ROUTES FROM MAIN SCREEN ==========

        // Ship Registration Renewal Form (placeholder)
        composable("ship_registration_renewal_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Ship Registration Renewal"
            )
        }

        // Temporary Ship Registration Form (placeholder)
        composable("temporary_ship_registration_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Temporary Ship Registration"
            )
        }

        // Ship Color Change Form (placeholder)
        composable("ship_color_change_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Ship Color Change"
            )
        }

        // Engine Change Form (placeholder)
        composable("engine_change_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Engine Change"
            )
        }

        // Captain License Update Form (placeholder)
        composable("captain_license_update_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Captain License Update"
            )
        }

        // Crew Member Addition Form (placeholder)
        composable("crew_member_addition_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Crew Member Addition"
            )
        }

        // Certificate Request Form (placeholder)
        composable("certificate_request_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Certificate Request"
            )
        }

        // Document Renewal Form (placeholder)
        composable("document_renewal_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Document Renewal"
            )
        }

        // License Issuance Form (placeholder)
        composable("license_issuance_form") {
            ComingSoonScreen(
                navController = navController,
                transactionName = "License Issuance"
            )
        }

        // File Viewer Screen
        composable(NavRoutes.FileViewerRoute.route) { backStackEntry ->
            val context = LocalContext.current
            val fileUri = backStackEntry.arguments?.getString("fileUri") ?: ""
            val fileName = backStackEntry.arguments?.getString("fileName")
            val decodedFileUri = URLDecoder.decode(fileUri, "UTF-8")
            val decodedFileName = fileName?.let { URLDecoder.decode(it, "UTF-8") }

            FileViewerScreen(
                fileUri = decodedFileUri,
                fileName = decodedFileName,
                onNavigateBack = { navController.navigateUp() },
                onOpenExternal = { shareableUri, fileName ->
                    try {
                        if (shareableUri != null) {
                            val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(
                                    fileName?.substringAfterLast('.', "")
                                ) ?: "*/*"

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(shareableUri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }

                            val chooser = Intent.createChooser(intent, "Open with")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            try {
                                context.startActivity(chooser)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No app found to open this file type",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "File not ready for external sharing. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        // Language Selection Screen
        composable(
            route = NavRoutes.LanguageScreenRoute.route,
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() }
        ) {
            LanguageScreen(navController = navController)
        }

        composable(NavRoutes.PaymentDetailsRoute.route) {
            PaymentDetailsScreen(navController = navController)
        }

        composable(NavRoutes.PaymentSuccessRoute.route) {
            PaymentSuccessScreen(navController = navController)
        }

        composable(NavRoutes.RequestDetailRoute.route) { backStackEntry ->
            val requestDetail = Json.decodeFromString<RequestDetail>(
                backStackEntry.arguments?.getString("detail") ?: "")
            RequestDetailScreen(navController = navController, requestDetail = requestDetail)
        }

    }

}