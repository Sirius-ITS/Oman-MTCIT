package com.informatique.mtcit.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.data.model.category.Transaction
import com.informatique.mtcit.ui.defaultEnterTransition
import com.informatique.mtcit.ui.defaultExitTransition
import com.informatique.mtcit.ui.screens.ComingSoonScreen
import com.informatique.mtcit.ui.screens.FileViewerScreen
import com.informatique.mtcit.ui.screens.HomePageScreen
import com.informatique.mtcit.ui.screens.LanguageScreen
import com.informatique.mtcit.ui.screens.LoginScreen
import com.informatique.mtcit.ui.screens.MainCategoriesScreen
import com.informatique.mtcit.ui.screens.MarineRegistrationScreen
import com.informatique.mtcit.ui.screens.NotificationScreen
import com.informatique.mtcit.ui.screens.OAuthWebViewScreen
import com.informatique.mtcit.ui.screens.PaymentDetailsScreen
import com.informatique.mtcit.ui.screens.PaymentSuccessScreen
import com.informatique.mtcit.ui.screens.ProfileScreen
import com.informatique.mtcit.ui.screens.RequestDetail
import com.informatique.mtcit.ui.screens.RequestDetailScreen
import com.informatique.mtcit.ui.screens.ApiRequestDetailScreen
import com.informatique.mtcit.ui.screens.SettingsScreen
import com.informatique.mtcit.ui.screens.ShipDataModificationScreen
import com.informatique.mtcit.ui.screens.TransactionListScreen
import com.informatique.mtcit.ui.screens.TransactionRequirementsScreen
import com.informatique.mtcit.ui.viewmodels.LoginViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@Composable
fun NavHost(themeViewModel: ThemeViewModel, navigationManager: NavigationManagerImpl){

    val sharedUserViewModel: SharedUserViewModel = hiltViewModel()
    val context = LocalContext.current

    val navController = rememberNavController()

    // ✅ NEW: Check user role on app start and load it into SharedUserViewModel
    LaunchedEffect(Unit) {
        val role = com.informatique.mtcit.data.datastorehelper.TokenManager.getUserRole(context)
        if (role != null) {
            sharedUserViewModel.setUserRole(role)
            Log.d("NavHost", "✅ User role loaded: $role")

            // ✅ If user is engineer, navigate directly to profile
            if (role.equals("engineer", ignoreCase = true)) {
                Log.d("NavHost", "🔧 Engineer detected, navigating to profile...")
                navController.navigate(NavRoutes.ProfileScreenRoute.route) {
                    popUpTo(NavRoutes.HomeRoute.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

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

        // ✅ OAuth WebView Screen - handles Keycloak authentication
        composable(route = NavRoutes.OAuthWebViewRoute.route) {
            val parentEntry = remember(navController.currentBackStackEntry) {
                navController.previousBackStackEntry
            }

            // ✅ Detect if parent is LoginScreen, MainCategoriesScreen, or ProfileScreen
            val parentRoute = parentEntry?.destination?.route ?: ""
            val isFromLoginScreen = parentRoute.startsWith("login/")
            val isFromMainCategories = parentRoute.startsWith("mainCategoriesScreen")
            val isFromProfileScreen = parentRoute == "profileScreen"

            println("🔍 OAuth WebView - Parent route: $parentRoute")
            println("🔍 OAuth WebView - isFromLoginScreen: $isFromLoginScreen")
            println("🔍 OAuth WebView - isFromMainCategories: $isFromMainCategories")
            println("🔍 OAuth WebView - isFromProfileScreen: $isFromProfileScreen")

            // ✅ Only get LoginViewModel if parent is LoginScreen
            val loginViewModel: LoginViewModel? = if (isFromLoginScreen && parentEntry != null) {
                hiltViewModel(parentEntry)
            } else {
                null
            }

            // ✅ Get AuthRepository for MainCategoriesScreen flow (properly injected via Hilt)
            val authRepositoryProvider: com.informatique.mtcit.ui.viewmodels.AuthRepositoryProvider = hiltViewModel()
            val authRepository = authRepositoryProvider.authRepository

            // ✅ Track if we should navigate back
            var shouldNavigateBack by remember { mutableStateOf(false) }

            // ✅ Listen for login completion ONLY if coming from LoginScreen
            if (isFromLoginScreen && loginViewModel != null) {
                LaunchedEffect(loginViewModel) {
                    loginViewModel.loginComplete.collect { isComplete ->
                        if (isComplete) {
                            println("✅ OAuth + Login complete! Setting navigation flag...")
                            shouldNavigateBack = true

                            // ✅ CRITICAL: Set flag in savedStateHandle so LoginScreen can detect it
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("oauth_login_completed", true)
                        }
                    }
                }
            }

            // ✅ Handle navigation back when flag is set
            LaunchedEffect(shouldNavigateBack) {
                if (shouldNavigateBack) {
                    println("🔙 Navigating back from OAuth WebView...")
                    kotlinx.coroutines.delay(50) // Small delay to ensure state is updated
                    navController.popBackStack()
                }
            }

            OAuthWebViewScreen(
                navController = navController,
                authUrl = LoginViewModel.OAUTH_AUTH_URL,
                redirectUri = LoginViewModel.OAUTH_REDIRECT_URI,
                onAuthCodeReceived = { code: String ->
                    println("✅ Authorization code received: $code")
                    println("🔍 Handling OAuth code - isFromLoginScreen: $isFromLoginScreen, isFromMainCategories: $isFromMainCategories")

                    CoroutineScope(Dispatchers.Main).launch {
                        if (isFromLoginScreen && loginViewModel != null) {
                            // ✅ LOGIN SCREEN FLOW: Exchange token and submit form
                            println("🔄 OAuth from LoginScreen - calling handleOAuthCode()")
                            val success = loginViewModel.handleOAuthCode(code)

                            if (!success) {
                                // ❌ Token exchange failed - navigate back to allow retry
                                println("❌ Token exchange failed - navigating back to login screen")
                                navController.popBackStack()
                            }
                            // ✅ If success, the loginComplete event will trigger navigation via shouldNavigateBack
                        } else if (isFromMainCategories || isFromProfileScreen) {
                            // ✅ MAIN CATEGORIES / PROFILE SCREEN FLOW: Just exchange token and navigate back
                            println("🔄 OAuth from ${if (isFromMainCategories) "MainCategoriesScreen" else "ProfileScreen"} - exchanging token directly")

                            val result = authRepository.exchangeCodeForToken(code)

                            result.fold(
                                onSuccess = {
                                    println("✅ Token exchanged successfully - checking user role")

                                    // ✅ NEW: Check user role and navigate accordingly
                                    val userRole = com.informatique.mtcit.data.datastorehelper.TokenManager.getUserRole(context)
                                    sharedUserViewModel.setUserRole(userRole)

                                    if (userRole?.equals("engineer", ignoreCase = true) == true) {
                                        println("🔧 Engineer detected - navigating to profile")
                                        // Set flag so the calling screen knows login completed
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("login_completed", true)

                                        // Navigate to profile and clear back stack
                                        navController.navigate(NavRoutes.ProfileScreenRoute.route) {
                                            popUpTo(NavRoutes.HomeRoute.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        println("👤 Client detected - navigating back normally")
                                        // Set flag so the calling screen knows login completed
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("login_completed", true)
                                        navController.popBackStack()
                                    }

                                },
                                onFailure = { error ->
                                    println("❌ Token exchange failed: ${error.message}")
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            // ✅ UNKNOWN FLOW: Log and navigate back
                            println("⚠️ OAuth from unknown screen - navigating back")
                            navController.popBackStack()
                        }
                    }
                }
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
                        4 -> "ISSUE_NAVIGATION_PERMIT"
                        5 -> "RENEW_NAVIGATION_PERMIT"
                        6 -> "SUSPEND_REGISTRATION"
                        7 -> "TEMPORARY_REGISTRATION_CERTIFICATE"
                        8 -> "PERMANENT_REGISTRATION_CERTIFICATE"
                        9 -> "REQUEST_INSPECTION"
                        10 -> "CANCEL_REGISTRATION"
                        12 -> "MORTGAGE_CERTIFICATE"
                        13 -> "RELEASE_MORTGAGE"
                        14 -> "SHIP_NAME_CHANGE"
                        16 -> "SHIP_ACTIVITY_CHANGE"
                        18 -> "CAPTAIN_NAME_CHANGE"
                        19 -> "SHIP_PORT_CHANGE"
                        21 -> "REQUEST_INSPECTION"
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
                transactionId = transaction.id,
                navController = navController
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
        composable(
            route = "${NavRoutes.ShipRegistrationRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType  // ✅ FIX: Use StringType for nullable values
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()  // ✅ Convert to Int if present

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(
            route = NavRoutes.RequestForInspection.routeWithParams,
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getInt("lastCompletedStep")
            val lastCompletedStep = if (lastCompletedStepString != null && lastCompletedStepString != -1) {
                lastCompletedStepString
            } else null

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.REQUEST_FOR_INSPECTION,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }
        composable(
            route = "${NavRoutes.PermanentRegistrationRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType  // ✅ FIX: Use StringType for nullable Int values
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()  // ✅ Convert to Int if present

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(NavRoutes.SuspendRegistrationRoute.route) {
            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.SUSPEND_PERMANENT_REGISTRATION
            )
        }

        composable(
            route = "${NavRoutes.CancelRegistrationRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.CANCEL_PERMANENT_REGISTRATION,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(
            route = "${NavRoutes.MortgageCertificateRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.MORTGAGE_CERTIFICATE,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(
            route = "${NavRoutes.ReleaseMortgageRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.RELEASE_MORTGAGE,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        // Navigation Forms
        composable(
            route = "${NavRoutes.IssueNavigationPermitRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.ISSUE_NAVIGATION_PERMIT,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(
            route = "${NavRoutes.RenewNavigationPermitRoute.route}?requestId={requestId}&lastCompletedStep={lastCompletedStep}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lastCompletedStep") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val lastCompletedStepString = backStackEntry.arguments?.getString("lastCompletedStep")
            val lastCompletedStep = lastCompletedStepString?.toIntOrNull()

            MarineRegistrationScreen(
                navController = navController,
                transactionType = TransactionType.RENEW_NAVIGATION_PERMIT,
                requestId = requestId,
                lastCompletedStep = lastCompletedStep
            )
        }

        composable(NavRoutes.SuspendNavigationPermitRoute.route) {
            ComingSoonScreen(
                navController = navController,
                transactionName = "Suspend Navigation Permit"
            )
        }

        // Ship Data Modification Forms
        composable(NavRoutes.ChangeNameOfShipOrUnitRoute.route) {
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
            ShipDataModificationScreen(
                navController = navController,
                transactionType = TransactionType.SHIP_PORT_CHANGE
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

        // ✅ NEW: API Request Detail Screen - fetches data dynamically from API
        composable(
            route = NavRoutes.ApiRequestDetailRoute.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.IntType },
                navArgument("requestTypeId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getInt("requestId") ?: 0
            val requestTypeId = backStackEntry.arguments?.getInt("requestTypeId") ?: 0

            ApiRequestDetailScreen(
                navController = navController,
                requestId = requestId,
                requestTypeId = requestTypeId
            )
        }

    }

}