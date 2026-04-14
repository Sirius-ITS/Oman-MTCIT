package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun CustomToolbar(
    navController: NavController,
    currentRoute: String? = null,
    unreadNotificationCount: Int = 0,
    hideHome: Boolean = false          // ✅ Hide Home tab for engineers
) {
    val selectedTab = when (currentRoute) {
        "homepage" -> 0
        "profileScreen" -> 1
        "notificationScreen" -> 2
        else -> 0
    }
    Column(
        modifier = Modifier
            .width(360.dp)
            .widthIn(max = 440.dp)
            .background(Color.Transparent)
    ) {
        // الشريط السفلي
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            shape = RoundedCornerShape(35.dp),
            color = LocalExtraColors.current.cardBackground,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (hideHome) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab — hidden for engineers
                if (!hideHome) {
                    TabItem(
                        icon = Icons.Rounded.Home,
                        label = localizedApp(R.string.home),
                        isSelected = selectedTab == 0,
                        onClick = {
                            navController.navigate("homepage") {
                                popUpTo("homepage") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
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

                // Notifications Tab with badge
                TabItemWithBadge(
                    icon = Icons.Default.Notifications,
                    label = localizedApp(R.string.notification),
                    isSelected = selectedTab == 2,
                    badgeCount = unreadNotificationCount,
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
    val contentColor = LocalExtraColors.current.viewAllText
    val backgroundColor = if (isSelected) LocalExtraColors.current.viewAll else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = backgroundColor,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(110.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TabItemWithBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val contentColor = LocalExtraColors.current.viewAllText
    val backgroundColor = if (isSelected) LocalExtraColors.current.viewAll else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = backgroundColor,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(110.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
