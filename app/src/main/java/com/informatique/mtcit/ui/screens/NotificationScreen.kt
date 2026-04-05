package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.data.model.notification.NotificationResDto
import com.informatique.mtcit.ui.components.CustomToolbar
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    val extraColors    = LocalExtraColors.current
    val context        = LocalContext.current
    val window         = (context as? Activity)?.window

    val notifications  by notificationViewModel.notifications.collectAsStateWithLifecycle()
    val isLoading      by notificationViewModel.isLoading.collectAsStateWithLifecycle()
    val unreadCount    by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

    // Dialog & snackbar state
    var showTestDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }


    // Make status bar transparent
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    // Refresh list every time this screen is visible
    LaunchedEffect(Unit) {
        val userId = TokenManager.getCivilId(context) ?: return@LaunchedEffect
        notificationViewModel.loadNotifications(userId)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {

        // ── Gradient header ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp + statusBarHeight)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(extraColors.blue1, extraColors.iconBlueGrey)
                        )
                    )
            )
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width; val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.62f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.06f))
                val path2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.82f)
                    quadraticTo(w * 0.5f, h * 0.7f, w, h * 0.78f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(path2, color = Color.White.copy(alpha = 0.03f))
            }
        }

        // ── Main scaffold ────────────────────────────────────────────────────
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopProfileBar(navController = navController) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = extraColors.blue1) }
                }

                notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = extraColors.blue1.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = "لا توجد إشعارات",
                                fontSize = 16.sp,
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                    ) {
                        // Section header with Test button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الإشعارات",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extraColors.whiteInDarkMode
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (unreadCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFE53935).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "$unreadCount غير مقروء",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE53935),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(items = notifications, key = { it.id }) { notification ->
                            SwipeableNotificationCard(
                                notification = notification,
                                onTap    = { notificationViewModel.markAsRead(notification.id) },
                                onDelete = { notificationViewModel.deleteNotification(notification.id) }
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom toolbar ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 4.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CustomToolbar(
                navController = navController,
                currentRoute = "notificationScreen",
                unreadNotificationCount = unreadCount
            )
        }
    }
}

// ── Swipeable card ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
    notification: NotificationResDto,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { DeleteBackground(dismissState) },
        modifier = Modifier.fillMaxWidth()
    ) {
        NotificationCard(notification = notification, onTap = onTap)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> Color.Transparent
            else -> Color(0xFFE53935)
        },
        label = "swipe_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "swipe_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "حذف",
            tint = Color.White,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationResDto,
    onTap: () -> Unit
) {
    // ...existing code...
    val extraColors = LocalExtraColors.current
    val isUnread = notification.isRead == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isUnread) onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread)
                extraColors.cardBackground
            else
                extraColors.cardBackground.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnread) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnread) extraColors.blue1.copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isUnread) extraColors.blue1 else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.title ?: "إشعار",
                        fontSize = 14.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = extraColors.whiteInDarkMode,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                    }
                }
                if (!notification.body.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = notification.body,
                        fontSize = 13.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isUnread) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "اضغط للتعليم كمقروء",
                        fontSize = 11.sp,
                        color = extraColors.blue1.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
