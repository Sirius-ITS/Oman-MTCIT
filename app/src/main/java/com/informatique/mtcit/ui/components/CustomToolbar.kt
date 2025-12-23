package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.components.localizedApp

@Composable
fun CustomToolbar(
    navController: NavController,
    currentRoute: String? = null
) {
    val selectedTab = when (currentRoute) {
        "homepage" -> 0
        "profileScreen" -> 1
        "notificationScreen" -> 2
        else -> 0
    }
    Column(
        modifier = Modifier
            .width(320.dp)
            .widthIn( max = 400.dp)
            .background(Color.Transparent)
    ) {
        // الشريط السفلي
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            shape = RoundedCornerShape(35.dp),
            color = LocalExtraColors.current.cardBackground,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                TabItem(
                    icon = Icons.Rounded.Home,
                    label = localizedApp(R.string.home),
                    isSelected = selectedTab == 0,
                    onClick = {
                        navController.navigate("homepage") {
                            // تجنب إنشاء نسخ متعددة من نفس الصفحة
                            popUpTo("homepage") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
                // Profile Tab
                TabItem(
                    icon = Icons.Default.Person,
                    label = localizedApp(R.string.profile),
                    isSelected = selectedTab == 1,
                    onClick = {
                        navController.navigate("profileScreen") {
                            popUpTo("homepage") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Notifications Tab
                TabItem(
                    icon = Icons.Default.Notifications,
                    label = localizedApp(R.string.notification),
                    isSelected = selectedTab == 2,
                    onClick = {
                        navController.navigate("notificationScreen") {
                            popUpTo("homepage") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
    val iconColor = if (isSelected) Color(0xFF2196F3) else LocalExtraColors.current.whiteInDarkMode
    val textColor = if (isSelected) Color(0xFF2196F3) else LocalExtraColors.current.whiteInDarkMode

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = backgroundColor,
        modifier = Modifier.padding(horizontal = 4.dp , vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
//            if (isSelected) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(65.dp)
                )
//            }
        }
    }
}
