package com.informatique.mtcit.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.informatique.mtcit.R
import com.informatique.mtcit.bottomNav.BottomScreen
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun MainScreen(sharedUserViewModel: SharedUserViewModel) {
    val navController = rememberAnimatedNavController()

    val bottomNavItems = listOf(
        BottomScreen.Home,
        BottomScreen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val systemUiController = rememberSystemUiController()
    val headerColor = colorResource(id = R.color.system_bar)

    BottomNavigation {
        bottomNavItems.forEach { screen ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                label = { Text(text = localizedApp( screen.titleRes)) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) }
            )
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = headerColor,
            darkIcons = true
        )
    }
    Scaffold(
        bottomBar = {
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
    ) {padding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = BottomScreen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomScreen.Home.route ,
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() }
            ) { HomeScreen(navController = navController, sharedUserViewModel = sharedUserViewModel) }


            composable(BottomScreen.Profile.route ,
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() }
            ) { ProfileScreen(navController = navController,sharedUserViewModel = sharedUserViewModel) }


            composable("languagescreen",
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
    BottomNavigation(
        backgroundColor = Color.White,
        modifier = Modifier.height(70.dp)
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            BottomNavigationItem(
                selected = isSelected,
                alwaysShowLabel = true,
                onClick = {   if (currentRoute != screen.route) {
                    onNavigate(screen.route)
                } },

                icon = {
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.route,
                        modifier = Modifier.size(30.dp)
                            .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                        tint = LocalContentColor.current

                    )
                },
                label = {
                    Text(text = localizedApp(screen.titleRes) , fontSize = 9.sp , maxLines = 1)
                },
                selectedContentColor = colorResource(id = R.color.bottom_navigation),
                unselectedContentColor = Color.DarkGray
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun defaultEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = 380,
            easing = { fraction -> fraction * fraction * (3 - 2 * fraction) } // Smoothstep
        )
    ) + slideInHorizontally(
        initialOffsetX = { it / 7 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
}

@OptIn(ExperimentalAnimationApi::class)
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

@OptIn(ExperimentalAnimationApi::class)
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

@OptIn(ExperimentalAnimationApi::class)
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


