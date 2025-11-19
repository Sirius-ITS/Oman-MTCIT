package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.informatique.mtcit.ui.components.CustomToolbar
import com.informatique.mtcit.ui.theme.LocalExtraColors


@Composable
fun NotificationScreen(
    navController: NavController,
){
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // Allow drawing behind system bars
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {
        // Background gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp + statusBarHeight)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                extraColors.blue1,
                                extraColors.blue2
                            )
                        )
                    )
            )
            // Wave overlay
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticBezierTo(w * 0.5f, h * 0.5f, w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.06f))

                val path2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.82f)
                    quadraticBezierTo(w * 0.5f, h * 0.7f, w, h * 0.78f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path2, color = Color.White.copy(alpha = 0.03f))
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopProfileBar(navController = navController)
            },
//            floatingActionButton = {
//                CustomToolbar(
//                    navController = navController,
//                    currentRoute = "profileScreen"
//                )
//            },
//            floatingActionButtonPosition = FabPosition.Center,
            containerColor = Color.Transparent
        ){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(bottom = 16.dp)
            ) {
//                item {
//                    // Request Statistics Section
//                    RequestStatisticsSection()
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    // Forms/Investments Section
//                    FormsSection()
//
//                    Spacer(modifier = Modifier.height(50.dp))
//                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(  bottom = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding() + 4.dp
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CustomToolbar(
                navController = navController ,
                currentRoute = "notificationScreen"
            )
        }
    }
}