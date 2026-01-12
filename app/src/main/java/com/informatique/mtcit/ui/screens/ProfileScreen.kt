package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.CustomToolbar
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel

import java.util.Locale
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: MarineRegistrationViewModel = hiltViewModel()
){
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // States for search and filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<FilterType?>(FilterType.ALL) }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.DESCENDING) }

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
            // Gradient background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                extraColors.blue1,
                                extraColors.iconBlueGrey
                            )
                        )
                    )
            )
            // Subtle whiteInDarkMode wave overlay (like the Swift Path overlay)
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.72f)
                    // Quadratic bezier to create a smooth wave
                    quadraticTo(
                        x1 =  w * 0.5f,
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
                    quadraticTo(w * 0.5f, h * 0.7f, w, h * 0.78f)
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
                    Spacer(modifier = Modifier.height(32.dp))
                    UserProfileHeader(
                        onFilterSelected = { filterType ->
                            selectedFilter = filterType
                            println("Selected filter: $filterType")
                        },
                        onSearchQueryChanged = { query ->
                            searchQuery = query
                            println("Search query: $query")
                        },
                        onSortOrderChanged = { order ->
                            sortOrder = order
                            println("Sort order: $order")
                        },
                        currentSortOrder = sortOrder
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FormsSection(
                        navController = navController,
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        sortOrder = sortOrder
                    )
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
    navController: NavController,
    viewModel: MarineRegistrationViewModel = hiltViewModel(),
    searchQuery: String = "",
    selectedFilter: FilterType? = null,
    sortOrder: SortOrder = SortOrder.DESCENDING
) {
    val extraColors = LocalExtraColors.current

    // ✅ Use the correct RequestsViewModel (not UserRequestsViewModel)
    val requestsViewModel: com.informatique.mtcit.ui.viewmodels.RequestsViewModel = hiltViewModel()

    val requests by requestsViewModel.requests.collectAsState()
    val isLoading by requestsViewModel.isLoading.collectAsState()
    val isLoadingMore by requestsViewModel.isLoadingMore.collectAsState()
    val paginationState by requestsViewModel.paginationState.collectAsState()
    val appError by requestsViewModel.appError.collectAsState()

    // ✅ Filter and sort requests based on search query, selected filter, and sort order
    val filteredRequests = remember(requests, searchQuery, selectedFilter, sortOrder) {
        var result = requests

        // Apply search filter (search in request serial or ship name)
        if (searchQuery.isNotBlank()) {
            result = result.filter { request ->
                request.requestSerial.contains(searchQuery, ignoreCase = true) ||
                request.shipName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply status filter
        if (selectedFilter != null && selectedFilter != FilterType.ALL) {
            result = result.filter { request ->
                when (selectedFilter) {
                    FilterType.COMPLETED -> request.statusName.contains("مكتمل", ignoreCase = true) ||
                                           request.statusName.contains("completed", ignoreCase = true)
                    FilterType.IN_PROGRESS -> request.statusName.contains("قيد المعالجة", ignoreCase = true) ||
                                             request.statusName.contains("in progress", ignoreCase = true) ||
                                             request.statusName.contains("processing", ignoreCase = true)
                    FilterType.NEEDS_ACTION -> request.statusName.contains("يحتاج إجراء", ignoreCase = true) ||
                                              request.statusName.contains("needs action", ignoreCase = true) ||
                                              request.statusName.contains("action needed", ignoreCase = true)
                    FilterType.DRAFT -> request.statusName.contains("مسودة", ignoreCase = true) ||
                                       request.statusName.contains("draft", ignoreCase = true)
                    FilterType.REJECTED -> request.statusName.contains("مرفوض", ignoreCase = true) ||
                                          request.statusName.contains("rejected", ignoreCase = true)
                    FilterType.PENDING -> request.statusName.contains("معلق", ignoreCase = true) ||
                                         request.statusName.contains("pending", ignoreCase = true)
                    FilterType.ACCEPTED -> request.statusName.contains("مقبول", ignoreCase = true) ||
                                          request.statusName.contains("accepted", ignoreCase = true)
                    FilterType.CONFIRMED -> request.statusName.contains("مؤكد", ignoreCase = true) ||
                                           request.statusName.contains("confirmed", ignoreCase = true)
                    FilterType.SEND -> request.statusName.contains("تم الإرسال", ignoreCase = true) ||
                                      request.statusName.contains("sent", ignoreCase = true)
                    FilterType.SCHEDULED -> request.statusName.contains("مجدول", ignoreCase = true) ||
                                           request.statusName.contains("scheduled", ignoreCase = true)
                    FilterType.IN_REVIEW -> request.statusName.contains("قيد المراجعة", ignoreCase = true) ||
                                           request.statusName.contains("in review", ignoreCase = true)
                    FilterType.ISSUED -> request.statusName.contains("تم الإصدار", ignoreCase = true) ||
                                        request.statusName.contains("issued", ignoreCase = true)
                    else -> true
                }
            }
        }

        // Apply sort order (sort by modification date)
        when (sortOrder) {
            SortOrder.ASCENDING -> result.sortedBy { it.modificationDate }
            SortOrder.DESCENDING -> result.sortedByDescending { it.modificationDate }
        }
    }

    // ✅ NEW: Collect navigation trigger for request detail
    val navigationToRequestDetail by requestsViewModel.navigationToRequestDetail.collectAsState()

    // ✅ NEW: Collect navigation trigger (like MainCategoriesScreen)
    val shouldNavigateToLogin by requestsViewModel.shouldNavigateToLogin.collectAsState()

    // ✅ NEW: Handle navigation to request detail screen
    LaunchedEffect(navigationToRequestDetail) {
        navigationToRequestDetail?.let { (requestId, requestTypeId) ->
            println("🔍 ProfileScreen: Navigating to request detail - ID: $requestId, TypeID: $requestTypeId")
            navController.navigate(NavRoutes.ApiRequestDetailRoute.createRoute(requestId, requestTypeId))
            requestsViewModel.clearNavigationTrigger()
        }
    }

    // ✅ NEW: Handle navigation to login (like MainCategoriesScreen)
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            println("🔑 ProfileScreen: Navigating to login - token refresh failed")
            navController.navigate(NavRoutes.OAuthWebViewRoute.route)
            requestsViewModel.resetNavigationTrigger()
        }
    }

    // ✅ FIXED: Use DisposableEffect like MainCategoriesScreen to properly observe login completion
    DisposableEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle

        val observer = androidx.lifecycle.Observer<Boolean> { loginCompleted ->
            if (loginCompleted == true) {
                println("✅ ProfileScreen: Login completed detected, reloading requests...")
                // User returned from successful login, reload requests
                requestsViewModel.clearAppError()
                requestsViewModel.loadRequests()
                // Clear the flag
                handle?.set("login_completed", false)
            }
        }

        handle?.getLiveData<Boolean>("login_completed")?.observeForever(observer)

        onDispose {
            handle?.getLiveData<Boolean>("login_completed")?.removeObserver(observer)
        }
    }

    // Load requests on first composition
    LaunchedEffect(Unit) {
        requestsViewModel.loadRequests()
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

        // ✅ Show error banner using the same component as other screens
        appError?.let { error ->
            when (error) {
                is com.informatique.mtcit.common.AppError.Unauthorized -> {
                    // 401 Error - Show banner with refresh token button
                    com.informatique.mtcit.ui.components.ErrorBanner(
                        message = error.message,
                        onDismiss = { requestsViewModel.clearAppError() },
                        showRefreshButton = true,
                        onRefreshToken = { requestsViewModel.refreshToken() }
                    )
                }
                is com.informatique.mtcit.common.AppError.ApiError -> {
                    // Other API errors
                    com.informatique.mtcit.ui.components.ErrorBanner(
                        message = "${if (Locale.getDefault().language == "ar") "خطأ" else "Error"} ${error.code}: ${error.message}",
                        onDismiss = { requestsViewModel.clearAppError() }
                    )
                }
                is com.informatique.mtcit.common.AppError.Unknown -> {
                    // ✅ Token refresh failed - Show with "Go to Login" button
                    com.informatique.mtcit.ui.components.ErrorBanner(
                        message = error.message,
                        showRefreshButton = true,
                        onRefreshToken = {
                            // Navigate to login when refresh token is expired
                            requestsViewModel.navigateToLogin()
                        },
                        onDismiss = { requestsViewModel.clearAppError() }
                    )
                }
                else -> {
                    // Other error types
                    com.informatique.mtcit.ui.components.ErrorBanner(
                        message = if (Locale.getDefault().language == "ar") "حدث خطأ" else "An error occurred",
                        onDismiss = { requestsViewModel.clearAppError() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading && requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = extraColors.whiteInDarkMode)
            }
        } else if (filteredRequests.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank() || selectedFilter != null) {
                            if (Locale.getDefault().language == "ar") "لا توجد نتائج" else "No results found"
                        } else {
                            localizedApp(R.string.no_forms_available)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedFilter != null) {
                            if (Locale.getDefault().language == "ar") "جرب البحث بكلمات مختلفة" else "Try different search terms"
                        } else {
                            localizedApp(R.string.forms_will_appear_here)
                        },
                        fontSize = 14.sp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Display filtered requests from API
            filteredRequests.forEach { request ->
                NewRequestCard(
                    request = request,
                    onClick = {
                        println("🔘 User clicked request: ID=${request.id}, Status=${request.statusName}")
                        requestsViewModel.onRequestClick(request)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Load More Button (if more pages available)
            if (paginationState.hasMore) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { requestsViewModel.loadMoreRequests() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoadingMore,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.blue1,
                        disabledContainerColor = extraColors.blue1.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (Locale.getDefault().language == "ar") "تحميل المزيد" else "Load More",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ✅ NEW: Request Card using API data with localized status
@Composable
fun NewRequestCard(
    request: com.informatique.mtcit.data.model.requests.UserRequestUiModel,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Row 1: Request ID and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${request.requestSerial}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.whiteInDarkMode
                )

                // Status Badge with localized text
                Surface(
                    color = request.statusBgColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = request.statusName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = request.statusColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Request Type Name
            Text(
                text = request.requestTypeName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Ship Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ship_registration),
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = request.shipName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                thickness = 0.7.dp,
                color = Color(0xFFE8E8E8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Last Update and View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Update
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (Locale.getDefault().language == "ar") "آخر تحديث:" else "Last Update:",
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = formatDateArabic(request.modificationDate),
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = Color(0xFF757575)
                    )
                }

                // View Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = null,
                        tint = Color(0xFF5BA3E8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (Locale.getDefault().language == "ar") "عرض التفاصيل" else "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5BA3E8)
                    )
                }
            }
        }
    }
}

fun formatDateArabic(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("dd-MM-yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate.take(10)
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
//@Composable
//fun UserProfileHeader(
//    onFilterSelected: (FilterType) -> Unit = {}
//) {
//    val extraColors = LocalExtraColors.current
//    var selectedFilter by remember { mutableStateOf<FilterType?>(null) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp)
//    ) {
//        // User Info Row
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Profile Avatar
//            Box(
//                modifier = Modifier
//                    .size(50.dp)
//                    .background(
//                        color = Color(0xFF2196F3),
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Person,
//                    contentDescription = "Profile",
//                    tint = Color.White,
//                    modifier = Modifier.size(28.dp)
//                )
//            }
//            Spacer(modifier = Modifier.width(16.dp))
//            Column {
//                Text(
//                    text = "أحمد محمد",
//                    fontSize = 22.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White
//                )
//                Spacer(modifier = Modifier.height(2.dp))
//                Text(
//                    text = "Civil ID: 123456789",
//                    fontSize = 13.sp,
//                    fontWeight = FontWeight.Normal,
//                    color = Color.White.copy(alpha = 0.7f)
//                )
//            }
//        }
//        Spacer(modifier = Modifier.height(20.dp))
//        // Search Bar - شكل الصورة بالظبط
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(48.dp),
//            shape = RoundedCornerShape(18.dp),
//            colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
//            elevation = CardDefaults.cardElevation(0.dp)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(horizontal = 16.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Search,
//                    contentDescription = "Search",
//                    tint = Color(0xFF9E9E9E),
//                    modifier = Modifier.size(20.dp)
//                )
//                Text(
//                    text = "ابحث برقم الطلب أو اسم السفينة",
//                    fontSize = 14.sp,
//                    color = Color(0xFFBDBDBD)
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        // Statistics Grid
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//            // Total Requests Card (Pink)
//            StatCard(
//                number = "671",
//                label = "عدد الطلبات",
//                numberColor = Color(0xFFE91E63),
//                accentColor = Color(0xFFE91E63),
//                isSelected = selectedFilter == FilterType.ALL,
//                modifier = Modifier.weight(1f),
//                onClick = {
//                    selectedFilter = if (selectedFilter == FilterType.ALL) null else FilterType.ALL
//                    onFilterSelected(FilterType.ALL)
//                }
//            )
//            // Completed Card (Green)
//            StatCard(
//                number = "52",
//                label = "مكتمل",
//                numberColor = Color(0xFF4CAF50),
//                accentColor = Color(0xFF4CAF50),
//                isSelected = selectedFilter == FilterType.COMPLETED,
//                modifier = Modifier.weight(1f),
//                onClick = {
//                    selectedFilter = if (selectedFilter == FilterType.COMPLETED) null else FilterType.COMPLETED
//                    onFilterSelected(FilterType.COMPLETED)
//                }
//            )
//        }
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(10.dp)
//        ) {
//            // In Progress Card (Blue)
//            StatCard(
//                number = "356",
//                label = "قيد المعالجة",
//                numberColor = Color(0xFF2196F3),
//                accentColor = Color(0xFF2196F3),
//                isSelected = selectedFilter == FilterType.IN_PROGRESS,
//                modifier = Modifier.weight(1f),
//                onClick = {
//                    selectedFilter = if (selectedFilter == FilterType.IN_PROGRESS) null else FilterType.IN_PROGRESS
//                    onFilterSelected(FilterType.IN_PROGRESS)
//                }
//            )
//            // Needs Action Card (Orange)
//            StatCard(
//                number = "263",
//                label = "يحتاج إجراء",
//                numberColor = Color(0xFFFF9800),
//                accentColor = Color(0xFFFF9800),
//                isSelected = selectedFilter == FilterType.NEEDS_ACTION,
//                modifier = Modifier.weight(1f),
//                onClick = {
//                    selectedFilter = if (selectedFilter == FilterType.NEEDS_ACTION) null else FilterType.NEEDS_ACTION
//                    onFilterSelected(FilterType.NEEDS_ACTION)
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun StatCard(
//    number: String,
//    label: String,
//    numberColor: Color,
//    accentColor: Color,
//    isSelected: Boolean = false,
//    modifier: Modifier = Modifier,
//    onClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//    Card(
//        modifier = modifier
//            .height(92.dp)
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(18.dp),
//        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
//        border = if (isSelected) {
//            BorderStroke(2.5.dp, accentColor)
//        } else {
//            BorderStroke(0.dp, Color(0xFFE0E0E0))
//        },
//        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Text(
//                text = number,
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold,
//                color = numberColor,
//                lineHeight = 32.sp
//            )
//            Spacer(modifier = Modifier.height(2.dp))
//            Text(
//                text = label,
//                fontSize = 13.sp,
//                fontWeight = FontWeight.Normal,
//                color = Color(0xFF757575),
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//// Filter Types
//enum class FilterType {
//    ALL,
//    COMPLETED,
//    NEEDS_ACTION,
//    IN_PROGRESS
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileHeader(
    onFilterSelected: (FilterType?) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSortOrderChanged: (SortOrder) -> Unit = {},
    currentSortOrder: SortOrder = SortOrder.DESCENDING
) {
    val extraColors = LocalExtraColors.current
    var selectedFilter by remember { mutableStateOf<FilterType?>(FilterType.ALL) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    // Set "All Requests" as selected by default on first composition
    LaunchedEffect(Unit) {
        onFilterSelected(FilterType.ALL)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
//        // User Info Row
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Profile Avatar
//            Box(
//                modifier = Modifier
//                    .size(50.dp)
//                    .background(
//                        color = Color(0xFF2196F3),
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Person,
//                    contentDescription = "Profile",
//                    tint = Color.White,
//                    modifier = Modifier.size(28.dp)
//                )
//            }
//            Spacer(modifier = Modifier.width(16.dp))
//            Column {
//                Text(
//                    text = "أحمد محمد",
//                    fontSize = 22.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White
//                )
//                Spacer(modifier = Modifier.height(2.dp))
//                Text(
//                    text = "Civil ID: 123456789",
//                    fontSize = 13.sp,
//                    fontWeight = FontWeight.Normal,
//                    color = Color.White.copy(alpha = 0.7f)
//                )
//            }
//        }
//        Spacer(modifier = Modifier.height(20.dp))

        // Search Bar with Filter Icon - Functional TextField
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )

                    // Functional TextField for search
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { newQuery ->
                                searchQuery = newQuery
                                onSearchQueryChanged(newQuery)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = extraColors.whiteInDarkMode
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "ابحث برقم الطلب أو اسم السفينة",
                                        fontSize = 14.sp,
                                        color = Color(0xFFBDBDBD)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Filter Icon Button
                IconButton(
                    onClick = { showFilterBottomSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dehaze,
                        contentDescription = "Filter",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics Grid - 4 Main Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                number = "671",
                label = "عدد الطلبات",
                numberColor = Color(0xFFE91E63),
                accentColor = Color(0xFFE91E63),
                isSelected = selectedFilter == FilterType.ALL,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedFilter = if (selectedFilter == FilterType.ALL) null else FilterType.ALL
                    onFilterSelected(selectedFilter)
                }
            )
            StatCard(
                number = "52",
                label = "مكتمل",
                numberColor = Color(0xFF4CAF50),
                accentColor = Color(0xFF4CAF50),
                isSelected = selectedFilter == FilterType.COMPLETED,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedFilter = if (selectedFilter == FilterType.COMPLETED) null else FilterType.COMPLETED
                    onFilterSelected(selectedFilter)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                number = "356",
                label = "قيد المعالجة",
                numberColor = Color(0xFF2196F3),
                accentColor = Color(0xFF2196F3),
                isSelected = selectedFilter == FilterType.IN_PROGRESS,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedFilter = if (selectedFilter == FilterType.IN_PROGRESS) null else FilterType.IN_PROGRESS
                    onFilterSelected(selectedFilter)
                }
            )
            StatCard(
                number = "263",
                label = "يحتاج إجراء",
                numberColor = Color(0xFFFF9800),
                accentColor = Color(0xFFFF9800),
                isSelected = selectedFilter == FilterType.NEEDS_ACTION,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedFilter = if (selectedFilter == FilterType.NEEDS_ACTION) null else FilterType.NEEDS_ACTION
                    onFilterSelected(selectedFilter)
                }
            )
        }
    }

    // Filter Bottom Sheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = sheetState,
            containerColor = extraColors.background,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                )
            }
        ) {
            FilterBottomSheetContent(
                selectedFilter = selectedFilter,
                currentSortOrder = currentSortOrder,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    onFilterSelected(filter)
                    showFilterBottomSheet = false
                },
                onSortOrderChanged = { order ->
                    onSortOrderChanged(order)
                    showFilterBottomSheet = false
                },
                onClearFilter = {
                    selectedFilter = null
                    onFilterSelected(null)
                    showFilterBottomSheet = false
                }
            )
        }
    }
}

@Composable
fun FilterBottomSheetContent(
    selectedFilter: FilterType?,
    currentSortOrder: SortOrder,
    onFilterSelected: (FilterType) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onClearFilter: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تصفية وترتيب",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode
            )

            TextButton(onClick = onClearFilter) {
                Text(
                    text = "مسح الكل",
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sort Order Section
        Text(
            text = "الترتيب",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ascending Card
            SortOrderCard(
                label = "تصاعدي",
                icon = "↑",
                isSelected = currentSortOrder == SortOrder.ASCENDING,
                onClick = { onSortOrderChanged(SortOrder.ASCENDING) },
                modifier = Modifier.weight(1f)
            )

            // Descending Card
            SortOrderCard(
                label = "تنازلي",
                icon = "↓",
                isSelected = currentSortOrder == SortOrder.DESCENDING,
                onClick = { onSortOrderChanged(SortOrder.DESCENDING) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Filter Section
        Text(
            text = "تصفية حسب الحالة",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // All Status Cards in Grid
        val statusList = listOf(
            StatusFilterItem(FilterType.ALL, "عدد الطلبات", "671", Color(0xFFE91E63)),
            StatusFilterItem(FilterType.DRAFT, "مسودة", "0", Color(0xFF9E9E9E)),
            StatusFilterItem(FilterType.REJECTED, "مرفوض", "0", Color(0xFFF44336)),
            StatusFilterItem(FilterType.SEND, "تم الإرسال", "0", Color(0xFFFF9800)),
            StatusFilterItem(FilterType.SCHEDULED, "مجدول", "0", Color(0xFF2196F3)),
            StatusFilterItem(FilterType.ACCEPTED, "مقبول", "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.COMPLETED, "مكتمل", "52", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.IN_REVIEW, "قيد المراجعة", "0", Color(0xFF4A90E2)),
            StatusFilterItem(FilterType.REVIEW_RTA, "مراجعة RTA", "0", Color(0xFF673AB7)),
            StatusFilterItem(FilterType.REJECT_AUTHORITIES, "مرفوض", "0", Color(0xFFE91E63)),
            StatusFilterItem(FilterType.APPROVED_FINAL, "موافقة نهائية", "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.ISSUED, "تم الإصدار", "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.WAITING_INSPECTION, "في انتظار الفحص", "0", Color(0xFF4CAF50)),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 500.dp)
        ) {
            items(statusList.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach{ item ->
                        FilterStatusCard(
                            item = item,
                            isSelected = selectedFilter == item.type,
                            onClick = { onFilterSelected(item.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class StatusFilterItem(
    val type: FilterType,
    val label: String,
    val count: String,
    val color: Color
)

@Composable
fun FilterStatusCard(
    item: StatusFilterItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        border = if (isSelected) {
            BorderStroke(2.5.dp, item.color)
        } else {
            BorderStroke(0.dp, Color.Transparent)
        },
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.count,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = item.color,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SortOrderCard(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current
    val accentColor = Color(0xFF2196F3)

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.1f) else extraColors.cardBackground
        ),
        border = if (isSelected) {
            BorderStroke(2.5.dp, accentColor)
        } else {
            BorderStroke(1.dp, Color(0xFFE0E0E0))
        },
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else extraColors.whiteInDarkMode.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) accentColor else extraColors.whiteInDarkMode.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatCard(
    number: String,
    label: String,
    numberColor: Color,
    accentColor: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        border = if (isSelected) {
            BorderStroke(2.5.dp, accentColor)
        } else {
            BorderStroke(0.dp, Color.Transparent)
        },
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = numberColor,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Filter Types - All Status
enum class FilterType {
    ALL,
    DRAFT,
    REJECTED,
    CONFIRMED,
    SEND,
    PENDING,
    SCHEDULED,
    ACCEPTED,
    COMPLETED,
    IN_REVIEW,
    REVIEW_RTA,
    REJECT_AUTHORITIES,
    APPROVED_AUTHORITIES,
    APPROVED_FINAL,
    ACTION_TAKEN,
    ISSUED,
    WAITING_INSPECTION,
    IN_PROGRESS,
    NEEDS_ACTION
}

// Sort Order Types
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

