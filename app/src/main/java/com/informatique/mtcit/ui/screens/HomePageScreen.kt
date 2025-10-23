package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.theme.LocalExtraColors
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    val categories = LocalCategories.current
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    // Allow drawing behind system bars and make status bar transparent so the gradient can extend into it
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = AndroidColor.TRANSPARENT
            // Ensure status bar icons are light so they remain visible over the dark gradient
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    // Calculate status bar height so the header gradient can cover it
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {
        // Background gradient header with wave overlay extended to include status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp + statusBarHeight)
        ) {
            // Gradient background
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
            // Subtle white wave overlay (like the Swift Path overlay)
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.72f)
                    // Quadratic bezier to create a smooth wave
                    quadraticBezierTo(
                        x1 = w * 0.5f,
                        y1 = h * 0.5f,
                        x2 = w,
                        y2 = h * 0.62f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.06f)
                )

                // Optional: add a second subtle wave for depth
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
        // Scaffold with TopProfileBar as a fixed topBar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // TopProfileBar will include statusBarsPadding to avoid overlap with status bar
                TopProfileBar(
                    isFabMenuExpanded = isFabMenuExpanded,
                    onFabToggle = { isFabMenuExpanded = !isFabMenuExpanded }
                )
            },
            containerColor = Color.Transparent // let the gradient show through
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 16.dp),
            ) {
                item {
                    // Welcome message sits above the bottom of header
                    WelcomeSection()
                    // Quick Stats (Circular Progress)
                    QuickStatsCircular()
                    // Available Services Section
                    Spacer(modifier = Modifier.height(24.dp))
                    AvailableServicesSection(
                        navController = navController,
                        categories = categories,
                        onServiceClick = { serviceId ->
                            // Navigate to service
                        }
                    )
                    // Latest Events Section
                    Spacer(modifier = Modifier.height(24.dp))
                    LatestEventsSection()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        // Scrim/Backdrop when FAB menu is expanded
        if (isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isFabMenuExpanded = false }
            )
        }

        // FAB Menu overlay
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
                HomeExtendedFabMenuItem(
                    icon = Icons.Default.Language,
                    label = localizedApp(R.string.change_language),
                    onClick = {
                        isFabMenuExpanded = false
                        navController.navigate("languagescreen")
                    }
                )

                // Change Theme Item
                HomeExtendedFabMenuItem(
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

@Composable
fun TopProfileBar(
    isFabMenuExpanded: Boolean,
    onFabToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // ensure content is placed below the status bar
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Section
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "الملف الشخصي",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = localizedApp(R.string.hello_label),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = localizedApp(R.string.user_name),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

        }
        // Settings and Notifications
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onFabToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "الإشعارات",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
                Badge(
                    containerColor = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                ) {
                    Text("3", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WelcomeSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = localizedApp(R.string.welcome_message),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = localizedApp(R.string.how_can_help),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun QuickStatsCircular() {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground // شفافية خفيفة
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
//                .background(extraColors.cardBackground),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularStatItem(
                value = "123",
                label = localizedApp(R.string.service_available),
                progress = 0.85f,
                color = Color(0xFF0E62C4),
                icon = Icons.Default.Apps
            )
            Divider(
                color = Color.Gray,
                modifier = Modifier
                    .height(100.dp)
                    .width(1.dp).align(Alignment.CenterVertically)
            )
            CircularStatItem(
                value = "214",
                label = localizedApp(R.string.transaction),
                progress = 0.65f,
                color = Color(0xFF0EBD48),
                icon = Icons.Default.TrendingUp
            )
            Divider(
                color = Color.Gray,
                modifier = Modifier
                    .height(100.dp)
                    .width(1.dp).align(Alignment.CenterVertically)
            )
            CircularStatItem(
                value = "85%",
                label = localizedApp(R.string.electronically),
                progress = 0.85f,
                color = Color(0xFFE74C3C),
                icon = Icons.Default.Computer
            )
        }
    }
}

@Composable
fun CircularStatItem(
    value: String,
    label: String,
    progress: Float,
    color: Color,
    icon: ImageVector
) {
    val extracolor = LocalExtraColors.current
    Column(
//        modifier = Modifier.background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(65.dp)
        ) {
            // Background circle
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.fillMaxSize(),
                color = color.copy(alpha = 0.2f),
                strokeWidth = 6.dp
            )
            // Progress circle
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp
            )
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = extracolor.white
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AvailableServicesSection(navController: NavController, categories: List<MainCategory>, onServiceClick: (Int) -> Unit) {
    val extracolors = LocalExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = localizedApp(R.string.available_services_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = extracolors.white
                )
                Text(
                    text = localizedApp(R.string.choose_service),
                    fontSize = 12.sp,
                    color = extracolors.white
                )
            }
            Surface(
                color = extracolors.blue2.copy(alpha = 0.2f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.height(34.dp).align(Alignment.CenterVertically)
            ) {
                TextButton(
                    onClick = { navController.navigate("mainCategoriesScreen") },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = extracolors.blue2
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Text(
                        text = localizedApp(R.string.view_all),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Display categories dynamically from the categories array
        if (categories.isEmpty()) {
            // Show loading or empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = extracolors.blue1)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    ServiceCard(
                        title = localizedApp(category.titleRes),
                        description = localizedApp(category.descriptionRes),
                        icon = painterResource(id = category.iconRes),
                        color = extracolors.blue1,
                        onClick = {
                            // Navigate to MainCategoriesScreen or specific category
                            navController.navigate("mainCategoriesScreen")
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    description: String,
    icon: Painter,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = modifier
            .height(200.dp)
            .width(250.dp)
            .padding(horizontal = 10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        extraColors.blue4,
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.white,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = extraColors.gray1,
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedApp(R.string.view_details),
                    fontSize = 13.sp,
                    color = extraColors.white,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = extraColors.white,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LatestEventsSection() {
    val extracolors = LocalExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = localizedApp(R.string.upcoming_events_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = extracolors.white
                )
                Text(
                    text = localizedApp(R.string.dont_miss_opportunity),
                    fontSize = 12.sp,
                    color = extracolors.white
                )
            }
            Surface(
                color = extracolors.blue2.copy(alpha = 0.2f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.height(34.dp).align(Alignment.CenterVertically)
            ) {
                TextButton(
                    onClick = { /* View all */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = extracolors.blue2
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = localizedApp(R.string.view_all),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        EventCard(
            title = localizedApp(R.string.event1_title),
            location = localizedApp(R.string.event1_location),
            date = localizedApp(R.string.event1_date),
            time = localizedApp(R.string.event1_time),
            icon = painterResource(id = R.drawable.ic_documentation),
            color = Color(0xFF4A90E2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        EventCard(
            title = localizedApp(R.string.event2_title),
            location = localizedApp(R.string.event2_location),
            date = localizedApp(R.string.event2_date),
            time = localizedApp(R.string.event2_time),
            icon = painterResource(id = R.drawable.ic_documentation),
            color = Color(0xFF4A90E2)
        )
    }
}

@Composable
fun EventCard(
    title: String,
    location: String,
    date: String,
    time: String,
    icon: Painter,
    color: Color
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp , end = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        extraColors.blue4,
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.white
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = location,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = extraColors.blue2,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = extraColors.blue2
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = extraColors.blue2
                    )
                    Text(
                        text = date,
                        fontSize = 12.sp,
                        color = extraColors.blue2
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = extraColors.white,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}