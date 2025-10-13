package com.informatique.mtcit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.screens.settings.SettingsScreen
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sharedUserViewModel: SharedUserViewModel, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Get dynamic title based on current route
    val currentTitle = getScreenTitle(currentRoute)

    // FAB menu state
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val extraColors = LocalExtraColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Show TopAppBar only for main transaction categories screen
                if (currentRoute == "transaction_categories" || currentRoute == null) {
                    MaterialExpressiveTopAppBar(
                        title = currentTitle,
                        isFabMenuExpanded = isFabMenuExpanded,
                        onFabMenuExpandChange = { isFabMenuExpanded = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {
                NavHost(
                    navController = navController,
                    startDestination = "transaction_categories",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() }
                ) {
                    // Transaction Categories Screen (Main Screen - shows all categories and transactions)
                    composable(
                        route = "transaction_categories",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        TransactionCategoriesScreen(navController = navController)
                    }

                    // Ship Registration Form (Marine Registration Category)
                    composable(
                        route = "ship_registration_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE
                        )
                    }

                    // Permanent Registration Certificate Form
                    composable(
                        route = "permanent_registration_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.PERMANENT_REGISTRATION_CERTIFICATE
                        )
                    }

                    // Suspend Permanent Registration Form
                    composable(
                        route = "suspend_registration_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.SUSPEND_PERMANENT_REGISTRATION
                        )
                    }

                    // Cancel Permanent Registration Form
                    composable(
                        route = "cancel_registration_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.CANCEL_PERMANENT_REGISTRATION
                        )
                    }

                    // Mortgage Certificate Form
                    composable(
                        route = "mortgage_certificate_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.MORTGAGE_CERTIFICATE
                        )
                    }

                    // Release Mortgage Form
                    composable(
                        route = "release_mortgage_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        MarineRegistrationScreen(
                            navController = navController,
                            transactionType = TransactionType.RELEASE_MORTGAGE
                        )
                    }

                    // Ship Registration Renewal Form (placeholder)
                    composable(
                        route = "ship_registration_renewal_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Registration Renewal"
                        )
                    }

                    // Temporary Ship Registration Form (placeholder)
                    composable(
                        route = "temporary_ship_registration_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Temporary Ship Registration"
                        )
                    }

                    // Ship Name Change Form
                    composable(
                        route = "ship_name_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ShipDataModificationScreen(
                            navController = navController,
                            transactionType = TransactionType.SHIP_NAME_CHANGE
                        )
                    }

                    // Ship Dimensions Change Form
                    composable(
                        route = "ship_dimensions_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ShipDataModificationScreen(
                            navController = navController,
                            transactionType = TransactionType.SHIP_DIMENSIONS_CHANGE
                        )
                    }

                    // Ship Color Change Form (placeholder)
                    composable(
                        route = "ship_color_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Color Change"
                        )
                    }

                    // Engine Change Form (placeholder)
                    composable(
                        route = "engine_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Engine Change"
                        )
                    }

                    // Captain Name Change Form
                    composable(
                        route = "captain_name_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ShipDataModificationScreen(
                            navController = navController,
                            transactionType = TransactionType.CAPTAIN_NAME_CHANGE
                        )
                    }

                    // Captain License Update Form (placeholder)
                    composable(
                        route = "captain_license_update_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Captain License Update"
                        )
                    }

                    // Crew Member Addition Form (placeholder)
                    composable(
                        route = "crew_member_addition_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Crew Member Addition"
                        )
                    }

                    // Certificate Request Form (placeholder)
                    composable(
                        route = "certificate_request_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Certificate Request"
                        )
                    }

                    // Document Renewal Form (placeholder)
                    composable(
                        route = "document_renewal_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Document Renewal"
                        )
                    }

                    // License Issuance Form (placeholder)
                    composable(
                        route = "license_issuance_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "License Issuance"
                        )
                    }

                    // ========================================
                    // Navigation Category Routes (ADD THESE)
                    // ========================================

                    // Issue Navigation Permit Form (placeholder)
                    composable(
                        route = "issue_navigation_permit_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Issue Navigation Permit"
                        )
                    }

                    // Renew Navigation Permit Form (placeholder)
                    composable(
                        route = "renew_navigation_permit_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Renew Navigation Permit"
                        )
                    }

                    // Suspend Navigation Permit Form (placeholder)
                    composable(
                        route = "suspend_navigation_permit_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Suspend Navigation Permit"
                        )
                    }

                    // ========================================
                    // Ship Data Modifications Category Routes
                    // ========================================

                    // Ship Activity Change Form (placeholder)
                    composable(
                        route = "ship_activity_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Activity Change"
                        )
                    }

                    // Ship Engine Change Form (placeholder)
                    composable(
                        route = "ship_engine_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Engine Change"
                        )
                    }

                    // Ship Port Change Form (placeholder)
                    composable(
                        route = "ship_port_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Port Change"
                        )
                    }

                    // Ship Ownership Change Form (placeholder)
                    composable(
                        route = "ship_ownership_change_form",
                        enterTransition = { defaultEnterTransition() },
                        exitTransition = { defaultExitTransition() }
                    ) {
                        ComingSoonScreen(
                            navController = navController,
                            transactionName = "Ship Ownership Change"
                        )
                    }

                    // Language Screen
                    composable(
                        route = "languagescreen",
                        enterTransition = { defaultEnterTransition2() },
                        exitTransition = { defaultExitTransition2() }
                    ) {
                        LanguageScreen(navController = navController)
                    }

                    // Settings Screen
                    composable(
                        route = "settings_screen",
                        enterTransition = { defaultEnterTransition2() },
                        exitTransition = { defaultExitTransition2() }
                    ) {
                        SettingsScreen(
                            viewModel = themeViewModel,
                            navController = navController,
                            sharedUserViewModel = sharedUserViewModel
                        )
                    }
                }
            }
        }

        // Scrim/Backdrop when FAB menu is expanded - on top of scaffold
        if (isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isFabMenuExpanded = false }
            )
        }

        // FAB Menu overlay - appears on top of everything, positioned below TopAppBar
        AnimatedVisibility(
            visible = isFabMenuExpanded,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 8.dp),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Change Language Item
                ExtendedFabMenuItem(
                    icon = Icons.Default.Language,
                    label = localizedApp(R.string.change_language),
                    onClick = {
                        isFabMenuExpanded = false
                        navController.navigate("languagescreen")
                    }
                )

                // Change Theme Item
                ExtendedFabMenuItem(
                    icon = Icons.Default.DarkMode,
                    label = localizedApp(R.string.settings_title),
                    onClick = {
                        isFabMenuExpanded = false
                        navController.navigate("settings_screen")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialExpressiveTopAppBar(
    title: String,
    isFabMenuExpanded: Boolean,
    onFabMenuExpandChange: (Boolean) -> Unit
) {
    val extraColors = LocalExtraColors.current
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        actions = {
            // Settings/Close Icon Button
            IconButton(
                onClick = { onFabMenuExpandChange(!isFabMenuExpanded) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Settings,
                    contentDescription = if (isFabMenuExpanded) "Close Menu" else "Settings Menu",
                    tint = extraColors.blue1,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = extraColors.blue1
        )
    )
}

@Composable
fun ExtendedFabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = extraColors.blue1,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Label
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun getScreenTitle(route: String?): String {
    return when (route) {
        "transaction_categories", null -> localizedApp(R.string.categories_title)
        "languagescreen" -> localizedApp(R.string.language_title)
        "settings_screen" -> localizedApp(R.string.settings_title)
        else -> localizedApp(R.string.app_name)
    }
}

// Updated animation functions to work with modern navigation
fun defaultEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = 380,
            easing = { fraction -> fraction * fraction * (3 - 2 * fraction) }
        )
    ) + slideInHorizontally(
        initialOffsetX = { it / 7 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
}

fun defaultExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = 300,
            easing = { fraction -> fraction * fraction * (3 - 2 * fraction) }
        )
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 7 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
}

fun defaultEnterTransition2(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 420,
            easing = { fraction -> fraction * fraction * (3 - 2 * fraction) }
        )
    )
}

fun defaultExitTransition2(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = 380,
            easing = { fraction -> fraction * fraction * (3 - 2 * fraction) }
        )
    )
}
