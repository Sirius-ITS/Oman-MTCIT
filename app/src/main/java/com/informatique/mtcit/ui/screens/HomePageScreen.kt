package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECF0F5))
    ) {
        // Background gradient header with wave overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 16.dp),
        ) {
            item {
                TopProfileBar()
                // Welcome message sits above the bottom of header
                WelcomeSection()
                // Quick Stats (Circular Progress)
                QuickStatsCircular()
                // Available Services Section
                Spacer(modifier = Modifier.height(24.dp))
                AvailableServicesSection(
                    navController = navController,
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
}

@Composable
fun TopProfileBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                    .size(52.dp)
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
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

        }
        // Settings and Notifications
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = localizedApp(R.string.how_can_help),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun QuickStatsCircular() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularStatItem(
                value = "123",
                label = localizedApp(R.string.service_available),
                progress = 0.85f,
                color = Color(0xFF4A90E2),
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
                color = Color(0xFF50C878),
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
    Column(
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
            fontWeight = FontWeight.Bold,
            color = Color.Black
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
fun AvailableServicesSection(navController: NavController, onServiceClick: (Int) -> Unit) {
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
                    fontWeight = FontWeight.Bold,
                    color = extracolors.blue1
                )
                Text(
                    text = localizedApp(R.string.choose_service),
                    fontSize = 12.sp,
                    color = extracolors.blue2
                )
            }
            TextButton(
                onClick = { navController.navigate("mainCategoriesScreen") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = extracolors.blue2
                )
            ) {
                Text(
                    text = localizedApp(R.string.view_all),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        LazyRow (
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        ) {
            item {
                ServiceCard(
                    title = localizedApp(R.string.seafarers_section_title),
                    description = localizedApp(R.string.seafarers_section_desc),
                    icon = painterResource(id = R.drawable.ic_captain),
                    color = extracolors.blue1,
                    onClick = { onServiceClick(1) },
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    title = localizedApp(R.string.registration_section_title),
                    description = localizedApp(R.string.registration_section_desc),
                    icon = painterResource(id = R.drawable.ic_ship_registration),
                    color = extracolors.blue1,
                    onClick = { onServiceClick(0) },
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    title = localizedApp(R.string.registration_section_title),
                    description = localizedApp(R.string.registration_section_desc),
                    icon = painterResource(id = R.drawable.ic_ship_registration),
                    color = extracolors.blue1,
                    onClick = { onServiceClick(0) },
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    title = localizedApp(R.string.registration_section_title),
                    description = localizedApp(R.string.registration_section_desc),
                    icon = painterResource(id = R.drawable.ic_ship_registration),
                    color = extracolors.blue1,
                    onClick = { onServiceClick(0) },
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    title = localizedApp(R.string.registration_section_title),
                    description = localizedApp(R.string.registration_section_desc),
                    icon = painterResource(id = R.drawable.ic_ship_registration),
                    color = extracolors.blue1,
                    onClick = { onServiceClick(0) },
                    modifier = Modifier.weight(1f)
                )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
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
                        extraColors.blue1.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.blue1,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = extraColors.blue2,
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
                    color = color,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = color,
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
                    fontWeight = FontWeight.Bold,
                    color = extracolors.blue1
                )
                Text(
                    text = localizedApp(R.string.dont_miss_opportunity),
                    fontSize = 12.sp,
                    color = extracolors.blue2
                )
            }
            TextButton(
                onClick = { /* View all */ },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = extracolors.blue2
                )
            ) {
                Text(
                    text = localizedApp(R.string.view_all),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        extraColors.blue1.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = extraColors.blue1,
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
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1
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
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}