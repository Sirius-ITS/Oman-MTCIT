package com.informatique.mtcit.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.informatique.mtcit.R
import com.informatique.mtcit.bottomNav.BottomScreen
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(sharedUserViewModel: SharedUserViewModel) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomScreen.Home,
        BottomScreen.Profile,
        BottomScreen.ShipRegistration
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Remove status bar padding
        bottomBar = {
            // Only show main bottom navigation for main screens, not for ship registration
            if (currentRoute in bottomNavItems.map { it.route }) {
                AppBottomNavigation(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomScreen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Use paddingValues to respect the main bottom bar when it's shown
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() }
        ) {
            composable(
                route = BottomScreen.Home.route,
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() }
            ) {
                HomeScreen(navController = navController, sharedUserViewModel = sharedUserViewModel)
            }

            composable(
                route = BottomScreen.Profile.route,
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() }
            ) {
                ProfileScreen(navController = navController, sharedUserViewModel = sharedUserViewModel)
            }

            composable(
                route = BottomScreen.ShipRegistration.route,
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() }
            ) {
                // For ship registration, we pass paddingValues as zero since main bottom bar is hidden
                ShipRegistrationStepperScreen(navController = navController, sharedUserViewModel = sharedUserViewModel)
            }

            composable(
                route = "languagescreen",
                enterTransition = { defaultEnterTransition2() },
                exitTransition = { defaultExitTransition2() }
            ) {
                LanguageScreen(navController = navController)
            }
        }
    }
}

@Composable
fun AppBottomNavigation(
    items: List<BottomScreen>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.DarkGray,
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        onNavigate(screen.route)
                    }
                },
                icon = {
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "IconScale"
                    )
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.route,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                },
                label = {
                    Text(
                        text = localizedApp(screen.titleRes),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorResource(id = R.color.bottom_navigation),
                    selectedTextColor = colorResource(id = R.color.bottom_navigation),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
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