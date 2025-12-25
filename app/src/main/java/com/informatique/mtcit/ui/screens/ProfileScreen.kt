package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.data.model.UserRequest
import com.informatique.mtcit.data.repository.RequestRepository
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.CustomToolbar
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: MarineRegistrationViewModel = hiltViewModel()
){
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }

    val navigationToDetail by viewModel.navigationToComplianceDetail.collectAsState()

    LaunchedEffect(navigationToDetail) {
        navigationToDetail?.let { action ->
            println("📱 Navigating to RequestDetailScreen from Profile")
            val marineData = buildComplianceDetailData(action, context)
            navController.navigate(
                NavRoutes.RequestDetailRoute.createRoute(
                    RequestDetail.CheckShipCondition(marineData)
                )
            ) {
                launchSingleTop = true
            }
            viewModel.clearComplianceDetailNavigation()
        }
    }

    val shouldNavigateToTransaction by viewModel.navigateToTransactionScreen.collectAsState()

    LaunchedEffect(shouldNavigateToTransaction) {
        if (shouldNavigateToTransaction) {
            println("📱 Navigating to transaction screen after resuming")
            val requestId = viewModel.getPendingRequestId()
            if (requestId != null) {
                navController.navigate(NavRoutes.ShipRegistrationRoute.createRouteWithResume(requestId)) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(NavRoutes.ShipRegistrationRoute.route) {
                    launchSingleTop = true
                }
            }
            viewModel.clearNavigationFlag()
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(extraColors.background)) {
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
                            colors = listOf(extraColors.blue1, extraColors.blue2)
                        )
                    )
            )
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
            topBar = { TopProfileBar(navController = navController) },
            containerColor = Color.Transparent
        ){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(bottom = 16.dp)
            ) {
                item {
                    RequestStatisticsSection()
                    Spacer(modifier = Modifier.height(24.dp))
                    FormsSection(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 4.dp
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CustomToolbar(
                navController = navController,
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
                    text = localizedApp(R.string.request_statistics_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    color = extraColors.whiteInDarkMode
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(
                    label = localizedApp(R.string.completed_requests),
                    value = 40,
                    percentage = 1.47f,
                    color = Color(0xFF6B7FD7)
                )
                LegendItem(
                    label = localizedApp(R.string.processing_requests),
                    value = 5,
                    percentage = 4.17f,
                    color = Color(0xFF5DD7A7)
                )
                LegendItem(
                    label = localizedApp(R.string.action_needed_requests),
                    value = 5,
                    percentage = 4.48f,
                    color = Color(0xFFFF9F6E)
                )
                LegendItem(
                    label = localizedApp(R.string.rejected_requests),
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
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 45.dp.toPx()
            val total = totalRequests.toFloat()
            var startAngle = -90f

            val completedSweep = (completedRequests / total) * 360f
            drawArc(
                color = Color(0xFF6B7FD7),
                startAngle = startAngle,
                sweepAngle = completedSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += completedSweep

            val processingSweep = (processingRequests / total) * 360f
            drawArc(
                color = Color(0xFF5DD7A7),
                startAngle = startAngle,
                sweepAngle = processingSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += processingSweep

            val actionSweep = (actionNeededRequests / total) * 360f
            drawArc(
                color = Color(0xFFFF9F6E),
                startAngle = startAngle,
                sweepAngle = actionSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            startAngle += actionSweep

            val rejectedSweep = (rejectedRequests / total) * 360f
            drawArc(
                color = Color(0xFFFF6B8A),
                startAngle = startAngle,
                sweepAngle = rejectedSweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalRequests",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode
            )
            Text(
                text = localizedApp(R.string.total_requests_label),
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
        horizontalArrangement = Arrangement.SpaceBetween,
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
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode,
                maxLines = 1
            )
            Text(
                text = "(%.2f%%)".format(percentage),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FormsSection(
    viewModel: MarineRegistrationViewModel = hiltViewModel()
) {
    val extraColors = LocalExtraColors.current
    var requests by remember { mutableStateOf<List<UserRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
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
                text = localizedApp(R.string.forms_section_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = extraColors.whiteInDarkMode)
            }
        } else if (requests.isEmpty()) {
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
                        text = localizedApp(R.string.empty_state_icon),
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizedApp(R.string.no_forms_available),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = localizedApp(R.string.forms_will_appear_here),
                        fontSize = 14.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            requests.forEach { request ->
                RealRequestCard(
                    request = request,
                    onClick = {
                        println("🔘 User clicked request: ${request.id}")
                        viewModel.resumeTransaction(request.id)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RealRequestCard(
    request: UserRequest,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    val (statusTextRes, statusColor, statusIconText) = when (request.status) {
        com.informatique.mtcit.data.model.RequestStatus.PENDING ->
            Triple(R.string.status_pending, Color(0xFFFFB74D), "⏳")
        com.informatique.mtcit.data.model.RequestStatus.IN_PROGRESS ->
            Triple(R.string.status_in_progress, Color(0xFF42A5F5), "⟳")
        com.informatique.mtcit.data.model.RequestStatus.VERIFIED ->
            Triple(R.string.status_verified, Color(0xFF66BB6A), "✓")
        com.informatique.mtcit.data.model.RequestStatus.REJECTED ->
            Triple(R.string.status_rejected, Color(0xFFE74C3C), "✗")
        com.informatique.mtcit.data.model.RequestStatus.COMPLETED ->
            Triple(R.string.status_completed, Color(0xFF26A69A), "✓")
    }

    val actionHintRes = when (request.status) {
        com.informatique.mtcit.data.model.RequestStatus.VERIFIED ->
            R.string.action_hint_verified
        com.informatique.mtcit.data.model.RequestStatus.PENDING,
        com.informatique.mtcit.data.model.RequestStatus.IN_PROGRESS ->
            R.string.action_hint_pending
        com.informatique.mtcit.data.model.RequestStatus.REJECTED ->
            R.string.action_hint_rejected
        com.informatique.mtcit.data.model.RequestStatus.COMPLETED ->
            R.string.action_hint_completed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF6B7FD7).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        color = Color(0xFF6B7FD7),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

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
                            text = statusIconText,
                            fontSize = 14.sp,
                            color = statusColor
                        )
                        Text(
                            text = localizedApp(statusTextRes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = request.getDisplayTitle(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            request.marineUnit.let { unit ->
                Text(
                    text = "🚢 ${unit?.name}",
                    fontSize = 13.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = localizedApp(actionHintRes),
                fontSize = 13.sp,
                color = statusColor,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    text = "${localizedApp(R.string.last_update_prefix)} ${formatDate(request.lastUpdatedDate)}",
                    fontSize = 12.sp,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun formatDate(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("dd MMMM yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun buildComplianceDetailData(
    action: com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.ShowComplianceDetailScreen,
    context: android.content.Context
): String {
    val unit = action.marineUnit
    val issues = action.complianceIssues

    return buildString {
        appendLine(context.getString(R.string.marine_unit_data_title))
        appendLine(context.getString(R.string.divider_line))
        appendLine()

        appendLine(context.getString(R.string.marine_unit_namee, unit.name))
        appendLine(context.getString(R.string.marine_unit_id, unit.maritimeId))
        appendLine(context.getString(R.string.marine_unit_type, unit.type))
        appendLine(context.getString(R.string.marine_unit_port, unit.registrationPort))
        appendLine(context.getString(R.string.marine_unit_activity, unit.activity))
        appendLine()

        if (unit.totalLength.isNotEmpty()) {
            appendLine(context.getString(R.string.dimensions_title))
            appendLine(context.getString(R.string.dimension_total_length, unit.totalLength))
            if (unit.totalWidth.isNotEmpty()) {
                appendLine(context.getString(R.string.dimension_total_width, unit.totalWidth))
            }
            if (unit.draft.isNotEmpty()) {
                appendLine(context.getString(R.string.dimension_draft, unit.draft))
            }
            appendLine()
        }

        appendLine(context.getString(R.string.divider_line))
        appendLine(context.getString(R.string.compliance_issues_title))
        appendLine(context.getString(R.string.divider_line))
        appendLine()

        if (issues.isEmpty()) {
            appendLine(context.getString(R.string.no_issues_found))
        } else {
            issues.forEachIndexed { index, issue ->
                val iconRes = when (issue.severity) {
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING ->
                        R.string.issue_blocking_icon
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.WARNING ->
                        R.string.issue_warning_icon
                    com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.INFO ->
                        R.string.issue_info_icon
                }

                appendLine("${context.getString(iconRes)} ${issue.category}")
                appendLine(context.getString(R.string.issue_title_label, issue.title))
                appendLine(context.getString(R.string.issue_description_label, issue.description))

                if (issue.details.isNotEmpty()) {
                    issue.details.forEach { (key, value) ->
                        appendLine(context.getString(R.string.issue_detail_item, key, value))
                    }
                }

                if (index < issues.size - 1) appendLine()
            }
        }

        appendLine()
        appendLine(context.getString(R.string.divider_line))
        appendLine(context.getString(R.string.rejection_reason_title))
        appendLine(action.rejectionReason)
    }
}