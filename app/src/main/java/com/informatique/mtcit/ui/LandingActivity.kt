package com.informatique.mtcit.ui


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.ui.base.BaseActivity
import com.informatique.mtcit.ui.screens.LoginScreen
import com.informatique.mtcit.ui.screens.MainCategoriesScreen
import com.informatique.mtcit.ui.screens.TransactionListScreen
import com.informatique.mtcit.ui.screens.MarineRegistrationScreen
import com.informatique.mtcit.ui.screens.ShipDataModificationScreen
import com.informatique.mtcit.ui.screens.ComingSoonScreen
import com.informatique.mtcit.ui.screens.FileViewerScreen
import com.informatique.mtcit.ui.screens.LanguageScreen
import com.informatique.mtcit.ui.screens.TransactionRequirementsScreen
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.ui.screens.HomePageScreen
import com.informatique.mtcit.ui.theme.AppTheme
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.LandingViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel
import com.informatique.mtcit.ui.viewmodels.TransactionListViewModel
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.screens.PaymentDetailsScreen
import com.informatique.mtcit.ui.screens.PaymentSuccessScreen
import com.informatique.mtcit.ui.screens.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LandingActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for proper Android 36 support
        enableEdgeToEdge()

        val configuration = resources.configuration

// ناخد قيمة fontScale من الجهاز
        val originalScale = configuration.fontScale

// نحدد الحدود اللي نسمح بيها
        val limitedScale = originalScale.coerceIn(0.85f, 1.15f)

// لو كانت القيمة برا الرينج، نعدلها
        if (originalScale != limitedScale) {
            configuration.fontScale = limitedScale
            val newContext = createConfigurationContext(configuration)
            applyOverrideConfiguration(newContext.resources.configuration)
        }


        installSplashScreen()
        setContent {
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val sharedUserViewModel: SharedUserViewModel = hiltViewModel()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val landingViewModel: LandingViewModel = hiltViewModel()

            val lang by languageViewModel.languageFlow.collectAsState(initial = "ar")
            val currentLocale = Locale(lang)
            val themeOption by themeViewModel.theme.collectAsState(initial = ThemeOption.SYSTEM_DEFAULT)
            val categories by landingViewModel.categories.collectAsState()

            CompositionLocalProvider(
                LocalLayoutDirection provides if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalAppLocale provides currentLocale,
                LocalCategories provides categories
            ) {
                AppTheme(themeOption = themeOption) {
                    // Remove Scaffold padding to allow edge-to-edge content
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navController = rememberNavController()
                        var startDestination by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            val token = TokenManager.getToken(applicationContext)
                            startDestination = if (token.isNullOrEmpty()) "homepage" else "homepage"
                        }

                        if (startDestination == null) {
//                            SplashAnimationScreen()
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            ) {
//                            composable("ship") {
//                                ShipDimensionsChangeScreen(navController)
//                            }
//                                composable ("marineunitsScreen"){
//                                    MarineUnitsScreen(navController)
//                                }
                                composable("paysuc") {
                                    PaymentSuccessScreen(navController = navController)
                                }
                                composable("homepage") {
                                    HomePageScreen(navController = navController)
                                }
                                composable("pay") {
                                    PaymentDetailsScreen(navController = navController)
                                }

                                composable("login") {
                                    val sharedUserViewModel: SharedUserViewModel =
                                        hiltViewModel(LocalActivity.current as ComponentActivity)
                                    LoginScreen(navController, sharedUserViewModel)
                                }

                                // ⚙️ شاشة الإعدادات (اختيار الثيم)
                                composable(
                                    route = "settings_screen",
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
                                    route = "mainCategoriesScreen/{categoryId}",
                                    arguments = listOf(navArgument("categoryId") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    })
                                ) { backStackEntry ->
                                    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                                    MainCategoriesScreen(navController, sharedUserViewModel, categoryId)
                                }

                                // Alternative route without category ID for backward compatibility
                                composable("mainCategoriesScreen") {
                                    MainCategoriesScreen(navController, sharedUserViewModel, "")
                                }

                                // Transaction List Screen - Shows transactions for selected sub-category
                                composable("transaction_list/{categoryId}/{subCategoryId}") { backStackEntry ->
                                    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                                    val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
                                    TransactionListScreen(navController, categoryId, subCategoryId)
                                }

                                // Requirements screen: show transaction requirements before going to form/steps
                                composable("transaction_requirements/{categoryId}/{subCategoryId}/{transactionId}/{parentTitleRes}") { backStackEntry ->
                                    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
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
                                            onStart = { navController.navigate(transaction.route) },
                                            onBack = { navController.popBackStack() },
                                            parentTitleRes = parentTitleRes,
                                            navController = navController,
                                            transactionId = transactionId
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }

                                // ========== TRANSACTION FORMS ==========

                                // Ship Registration Forms
                                composable("ship_registration_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE
                                    )
                                }

                                composable("permanent_registration_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE
                                    )
                                }

                                composable("suspend_registration_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.SUSPEND_PERMANENT_REGISTRATION
                                    )
                                }

                                composable("cancel_registration_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.CANCEL_PERMANENT_REGISTRATION
                                    )
                                }

                                composable("mortgage_certificate_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.MORTGAGE_CERTIFICATE
                                    )
                                }

                                composable("release_mortgage_form") {
                                    MarineRegistrationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.RELEASE_MORTGAGE
                                    )
                                }

                                // Navigation Forms
                                composable("issue_navigation_permit_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Issue Navigation Permit"
                                    )
                                }

                                composable("renew_navigation_permit_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Renew Navigation Permit"
                                    )
                                }

                                composable("suspend_navigation_permit_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Suspend Navigation Permit"
                                    )
                                }

                                // Ship Data Modification Forms
                                composable("ship_name_change_form") {
                                    ShipDataModificationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.SHIP_NAME_CHANGE
                                    )
                                }

                                composable("ship_dimensions_change_form") {
                                    ShipDataModificationScreen(
                                        navController = navController,
                                        transactionType = TransactionType.SHIP_DIMENSIONS_CHANGE
                                    )
                                }

                                composable("ship_engine_change_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Ship Engine Change"
                                    )
                                }

                                composable("captain_name_change_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Captain Name Change"
                                    )
                                }

                                composable("ship_activity_change_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Ship Activity Change"
                                    )
                                }

                                composable("ship_port_change_form") {
                                    ComingSoonScreen(
                                        navController = navController,
                                        transactionName = "Ship Port Change"
                                    )
                                }

                                composable("ship_ownership_change_form") {
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
                                composable("file_viewer/{fileUri}/{fileName}") { backStackEntry ->
                                    val context = LocalContext.current
                                    val fileUri = backStackEntry.arguments?.getString("fileUri") ?: ""
                                    val fileName = backStackEntry.arguments?.getString("fileName")
                                    val decodedFileUri = java.net.URLDecoder.decode(fileUri, "UTF-8")
                                    val decodedFileName = fileName?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                                    FileViewerScreen(
                                        fileUri = decodedFileUri,
                                        fileName = decodedFileName,
                                        onNavigateBack = { navController.navigateUp() },
                                        onOpenExternal = { shareableUri, fileName ->
                                            try {
                                                if (shareableUri != null) {
                                                    val mimeType = android.webkit.MimeTypeMap.getSingleton()
                                                        .getMimeTypeFromExtension(
                                                            fileName?.substringAfterLast('.', "")
                                                        ) ?: "*/*"

                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(shareableUri, mimeType)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }

                                                    val chooser = android.content.Intent.createChooser(intent, "Open with")
                                                    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

                                                    try {
                                                        context.startActivity(chooser)
                                                    } catch (e: android.content.ActivityNotFoundException) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "No app found to open this file type",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "File not ready for external sharing. Please try again.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error: ${e.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    )
                                }

                                // Language Selection Screen
                                composable(
                                    route = "languagescreen",
                                    enterTransition = { defaultEnterTransition() },
                                    exitTransition = { defaultExitTransition() }
                                ) {
                                    LanguageScreen(navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashAnimationScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator() // ممكن تحط لوجو هنا أو انيميشن جاهز
    }
}

// Animation functions for navigation transitions (Navigation Compose 2.8.0+)
fun defaultEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 300)
    ) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(durationMillis = 300)
    )
}

fun defaultExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 300)
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(durationMillis = 300)
    )
}
