package com.informatique.mtcit.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.CustomToolbar
import com.informatique.mtcit.ui.theme.LocalExtraColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.data.model.UserRequest
import com.informatique.mtcit.data.repository.RequestRepository
import androidx.compose.runtime.collectAsState

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: MarineRegistrationViewModel = hiltViewModel()  // ✅ Inject ViewModel
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

    // ✅ KEY: Observe navigation to RequestDetailScreen
    val navigationToDetail by viewModel.navigationToComplianceDetail.collectAsState()

    LaunchedEffect(navigationToDetail) {
        navigationToDetail?.let { action ->
            println("📱 Navigating to RequestDetailScreen from Profile")

            // Build marine unit data string with all details and compliance issues
            val marineData = buildComplianceDetailData(action)

            // Navigate to RequestDetailScreen with proper route
            navController.navigate(
                NavRoutes.RequestDetailRoute.createRoute(
                    RequestDetail.CheckShipCondition(marineData)
                )
            ) {
                // ✅ Don't clear the back stack - keep ProfileScreen so back button works correctly
                launchSingleTop = true
            }

            // Clear the navigation state
            viewModel.clearComplianceDetailNavigation()
        }
    }

    // ✅ NEW: Observe navigation to transaction screen after resuming verified request
    val shouldNavigateToTransaction by viewModel.navigateToTransactionScreen.collectAsState()

    LaunchedEffect(shouldNavigateToTransaction) {
        if (shouldNavigateToTransaction) {
            println("📱 Navigating to transaction screen after resuming")

            // Get the pending request ID from the ViewModel
            val requestId = viewModel.getPendingRequestId()

            // Navigate to the correct transaction form screen with the requestId
            // The requestId will be passed as a navigation argument so it persists
            // across ViewModel recreation
            if (requestId != null) {
                navController.navigate(NavRoutes.ShipRegistrationRoute.createRouteWithResume(requestId)) {
                    launchSingleTop = true
                    // Don't pop profile - allow back navigation
                }
            } else {
                // Fallback to normal navigation if no requestId
                navController.navigate(NavRoutes.ShipRegistrationRoute.route) {
                    launchSingleTop = true
                }
            }

            // Clear the navigation flag
            viewModel.clearNavigationFlag()
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
                item {
                    // Request Statistics Section
                    RequestStatisticsSection()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Forms/Investments Section
                    FormsSection(viewModel = viewModel)  // ✅ Pass ViewModel instance

                    Spacer(modifier = Modifier.height(50.dp))
                }
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
                currentRoute = "profileScreen"
            )
        }
    }
}

@Composable
fun RequestStatisticsSection() {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = extraColors.whiteInDarkMode,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إحصائيات سير الطلبات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Donut Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    totalRequests = 54,
                    completedRequests = 40,
                    processingRequests = 5,
                    actionNeededRequests = 5,
                    rejectedRequests = 4
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(
                    label = "الطلبات المكتملة",
                    value = 40,
                    percentage = 1.47f,
                    color = Color(0xFF6B7FD7)
                )
                LegendItem(
                    label = "الطلبات قيد المعالجة",
                    value = 5,
                    percentage = 4.17f,
                    color = Color(0xFF5DD7A7)
                )
                LegendItem(
                    label = "الطلبات تحتاج إجراء",
                    value = 5,
                    percentage = 4.48f,
                    color = Color(0xFFFF9F6E)
                )
                LegendItem(
                    label = "الطلبات المرفوضة",
                    value = 4,
                    percentage = 10.35f,
                    color = Color(0xFFFF6B8A)
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    totalRequests: Int,
    completedRequests: Int,
    processingRequests: Int,
    actionNeededRequests: Int,
    rejectedRequests: Int
) {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw donut chart
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 45.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val centerX = size.width / 2
            val centerY = size.height / 2

            val total = totalRequests.toFloat()
            var startAngle = -90f

            // Completed (Blue)
            val completedSweep = (completedRequests / total) * 360f
            drawArc(
                color = Color(0xFF6B7FD7),
                startAngle = startAngle,
                sweepAngle = completedSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += completedSweep

            // Processing (Green)
            val processingSweep = (processingRequests / total) * 360f
            drawArc(
                color = Color(0xFF5DD7A7),
                startAngle = startAngle,
                sweepAngle = processingSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += processingSweep

            // Action Needed (Orange)
            val actionSweep = (actionNeededRequests / total) * 360f
            drawArc(
                color = Color(0xFFFF9F6E),
                startAngle = startAngle,
                sweepAngle = actionSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += actionSweep

            // Rejected (Red)
            val rejectedSweep = (rejectedRequests / total) * 360f
            drawArc(
                color = Color(0xFFFF6B8A),
                startAngle = startAngle,
                sweepAngle = rejectedSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$totalRequests",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode
            )
            Text(
                text = "طلب",
                fontSize = 14.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    value: Int,
    percentage: Float,
    color: Color
) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(color, shape = CircleShape)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
            )

        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$value",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                maxLines = 1
            )
            Text(
                text = "($percentage%))",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                maxLines = 1
            )

        }

    }
}

@Composable
fun FormsSection(
    viewModel: MarineRegistrationViewModel = hiltViewModel()  // ✅ Inject ViewModel
) {
    val extraColors = LocalExtraColors.current

    // ✅ Fetch real requests from repository
    var requests by remember { mutableStateOf<List<UserRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Get repository and fetch user requests
        val repository = RequestRepository()
        repository.getUserRequests("currentUserId")
            .onSuccess {
                requests = it
                isLoading = false
                println("✅ Loaded ${it.size} requests from repository")
            }
            .onFailure {
                isLoading = false
                println("❌ Failed to load requests: ${it.message}")
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_documentation),
                contentDescription = null,
                tint = extraColors.whiteInDarkMode,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "الاستمارات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Show real requests or loading state
        if (isLoading) {
            // Show loading indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = extraColors.whiteInDarkMode
                )
            }
        } else if (requests.isEmpty()) {
            // Show empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📋",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد استمارات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = "ستظهر طلباتك هنا",
                        fontSize = 14.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // ✅ Show real requests
            requests.forEach { request ->
                RealRequestCard(
                    request = request,
                    onClick = {
                        println("🔘 User clicked request: ${request.id}")
                        // ✅ This triggers resumeTransaction
                        viewModel.resumeTransaction(request.id)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ✅ NEW: Card for real UserRequest data
@Composable
fun RealRequestCard(
    request: UserRequest,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    // Map RequestStatus to UI properties
    val (statusText, statusColor, statusIcon) = when (request.status) {
        com.informatique.mtcit.data.model.RequestStatus.PENDING ->
            Triple("قيد المراجعة", Color(0xFFFFB74D), "⏳")
        com.informatique.mtcit.data.model.RequestStatus.IN_PROGRESS ->
            Triple("قيد المعالجة", Color(0xFF42A5F5), "⟳")
        com.informatique.mtcit.data.model.RequestStatus.VERIFIED ->
            Triple("تم التحقق", Color(0xFF66BB6A), "✓")
        com.informatique.mtcit.data.model.RequestStatus.REJECTED ->
            Triple("مرفوض", Color(0xFFE74C3C), "✗")
        com.informatique.mtcit.data.model.RequestStatus.COMPLETED ->
            Triple("مكتمل", Color(0xFF26A69A), "✓")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),  // ✅ Clickable!
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Status Badge + ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ID Badge
                Surface(
                    color = Color(0xFF6B7FD7).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7FD7),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusIcon,
                            fontSize = 14.sp,
                            color = statusColor
                        )
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Title
            Text(
                text = request.getDisplayTitle(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Marine Unit Name (if available)
            request.marineUnit?.let { unit ->
                Text(
                    text = "🚢 ${unit.name}",
                    fontSize = 13.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Row 4: Action hint
            Text(
                text = when (request.status) {
                    com.informatique.mtcit.data.model.RequestStatus.VERIFIED ->
                        "✓ اضغط للمتابعة"
                    com.informatique.mtcit.data.model.RequestStatus.PENDING,
                    com.informatique.mtcit.data.model.RequestStatus.IN_PROGRESS ->
                        "⏳ اضغط للتحقق من الحالة"
                    com.informatique.mtcit.data.model.RequestStatus.REJECTED ->
                        "✗ اضغط لعرض السبب"
                    com.informatique.mtcit.data.model.RequestStatus.COMPLETED ->
                        "✓ الطلب مكتمل"
                },
                fontSize = 13.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 5: Last Update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "آخر تحديث: ${formatDate(request.lastUpdatedDate)}",
                    fontSize = 12.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Helper function to format date
fun formatDate(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("dd MMMM yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate.take(10) // Just show date part
    }
}

// Keep the old FormData and FormCard for backward compatibility (if needed elsewhere)
data class FormData(
    val id: String,
    val title: String,
    val status: String,
    val statusColor: Color,
    val statusIcon: ImageVector,
    val statusMessage: String,
    val lastUpdate: String
)

@Composable
fun FormCard(form: FormData) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Status Badge + ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge on the left

                // ID Badge on the right
                Surface(
                    color = Color(0xFF6B7FD7).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = form.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7FD7),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = form.statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = form.statusIcon,
                            contentDescription = null,
                            tint = form.statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = form.status,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = form.statusColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Title (aligned to the right)
            Text(
                text = form.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Status Message (aligned to the right)
            Text(
                text = form.statusMessage,
                fontSize = 13.sp,
                color = form.statusColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 4: Last Update (aligned to the left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = form.lastUpdate,
                    fontSize = 12.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Build compliance detail data string from ShowComplianceDetailScreen action
 * This creates a formatted string with marine unit data and compliance issues
 */
private fun buildComplianceDetailData(action: com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.ShowComplianceDetailScreen): String {
    val unit = action.marineUnit
    val issues = action.complianceIssues

    return buildString {
        appendLine("📋 بيانات الوحدة البحرية")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()

        // Basic Info
        appendLine("🚢 الاسم: ${unit.name}")
        appendLine("🔢 رقم الهوية البحرية: ${unit.maritimeId}")
        appendLine("📍 نوع الوحدة: ${unit.type}")
        appendLine("⚓ ميناء التسجيل: ${unit.registrationPort}")
        appendLine("🎯 النشاط البحري: ${unit.activity}")
        appendLine()

        // Dimensions
        if (unit.totalLength.isNotEmpty()) {
            appendLine("📏 الأبعاد:")
            appendLine("   • الطول الكلي: ${unit.totalLength}")
            if (unit.totalWidth.isNotEmpty()) appendLine("   • العرض الكلي: ${unit.totalWidth}")
            if (unit.draft.isNotEmpty()) appendLine("   • الغاطس: ${unit.draft}")
            appendLine()
        }

        // Compliance Issues Section
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("⚠️ سجل الالتزام - المشاكل المكتشفة")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine()

        if (issues.isEmpty()) {
            appendLine("✅ لا توجد مشاكل")
        } else {
            issues.forEachIndexed { index, issue ->
                val icon = when (issue.severity) {
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING -> "🚫"
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.WARNING -> "⚠️"
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.INFO -> "ℹ️"
                }

                appendLine("$icon ${issue.category}")
                appendLine("   العنوان: ${issue.title}")
                appendLine("   التفاصيل: ${issue.description}")

                if (issue.details.isNotEmpty()) {
                    issue.details.forEach { (key, value) ->
                        appendLine("   • $key: $value")
                    }
                }

                if (index < issues.size - 1) appendLine()
            }
        }

        appendLine()
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("📌 سبب الرفض:")
        appendLine(action.rejectionReason)
    }
}
