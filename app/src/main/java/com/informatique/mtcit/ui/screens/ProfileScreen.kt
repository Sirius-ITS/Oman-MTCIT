@file:Suppress("DEPRECATION")

package com.informatique.mtcit.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.data.model.RequestStatus

@Composable
fun ProfileScreen(
    navController: NavController,
    unreadNotificationCount: Int = 0,
    viewModel: MarineRegistrationViewModel = hiltViewModel()
){
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // ✅ NEW: Check user role to hide bottom bar for engineers
    var userRole by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userRole = com.informatique.mtcit.data.datastorehelper.TokenManager.getUserRole(context)
    }

    val isEngineer = userRole?.equals("engineer", ignoreCase = true) == true

    // ✅ Handle back press for engineers - exit app instead of navigating back
    BackHandler(enabled = isEngineer) {
        // For engineers, exit the app when back is pressed from Profile
        (context as? Activity)?.finish()
    }

    // States for search and filter
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf<FilterType?>(FilterType.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DESCENDING) }

    // ✅ Fetch status counts from API
    val statusCounts by viewModel.statusCounts.collectAsState()
    val statusCountsLoading by viewModel.statusCountsLoading.collectAsState()
    val statusCountsError by viewModel.statusCountsError.collectAsState()

    // ✅ Fetch status counts on screen load
    LaunchedEffect(Unit) {
        viewModel.getStatusCounts()
    }

    // ✅ Show error if any
    LaunchedEffect(statusCountsError) {
        statusCountsError?.let { error ->
            println("❌ Status counts error: $error")
            // You can show a Snackbar here if needed
        }
    }

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
            topBar = {
                Column{
                    TopProfileBar(navController = navController)
                    Search(
                        onFilterSelected = { filterType ->
                            selectedFilter = filterType
                            println("Selected filter: $filterType")
                        },
                        onSortOrderChanged = { order ->
                            sortOrder = order
                            println("🔄 Sort order changed: $order")
                        },
                        currentSortOrder = sortOrder,
                        selectedFilter = selectedFilter,
                        onSearchQueryChanged = { query ->
                        searchQuery = query
                        println("Search query: $query")
                    }, )
                }
                },
            containerColor = Color.Transparent
        ){
            FormsSectionWithLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(bottom = 16.dp),
                navController = navController,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                sortOrder = sortOrder,
                onFilterSelected = { filterType ->
                    selectedFilter = filterType
                    println("Selected filter: $filterType")
                },
                statusCounts = statusCounts,
                statusCountsLoading = statusCountsLoading
            )
        }
        // ✅ UPDATED: Only show bottom bar if user is NOT an engineer
        if (!isEngineer) {
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
                    currentRoute = "profileScreen",
                    unreadNotificationCount = unreadNotificationCount
                )
            }
        }
    }
}

//@Composable
//fun RequestStatisticsSection() {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.TrendingUp,
//                    contentDescription = null,
//                    tint = extraColors.whiteInDarkMode,
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = localizedApp(R.string.request_statistics_title),
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Normal,
//                    letterSpacing = 1.sp,
//                    color = extraColors.whiteInDarkMode
//                )
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                DonutChart(
//                    totalRequests = 54,
//                    completedRequests = 40,
//                    processingRequests = 5,
//                    actionNeededRequests = 5,
//                    rejectedRequests = 4
//                )
//            }
//
//            Spacer(modifier = Modifier.height(42.dp))
//
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                LegendItem(
//                    label = localizedApp(R.string.completed_requests),
//                    value = 40,
//                    percentage = 1.47f,
//                    color = Color(0xFF6B7FD7)
//                )
//                LegendItem(
//                    label = localizedApp(R.string.processing_requests),
//                    value = 5,
//                    percentage = 4.17f,
//                    color = Color(0xFF5DD7A7)
//                )
//                LegendItem(
//                    label = localizedApp(R.string.action_needed_requests),
//                    value = 5,
//                    percentage = 4.48f,
//                    color = Color(0xFFFF9F6E)
//                )
//                LegendItem(
//                    label = localizedApp(R.string.rejected_requests),
//                    value = 4,
//                    percentage = 10.35f,
//                    color = Color(0xFFFF6B8A)
//                )
//            }
//        }
//    }
//}

//@Composable
//fun DonutChart(
//    totalRequests: Int,
//    completedRequests: Int,
//    processingRequests: Int,
//    actionNeededRequests: Int,
//    rejectedRequests: Int
//) {
//    val extraColors = LocalExtraColors.current
//
//    Box(
//        modifier = Modifier.size(200.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        androidx.compose.foundation.Canvas(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            val strokeWidth = 45.dp.toPx()
//            val total = totalRequests.toFloat()
//            var startAngle = -90f
//
//            val completedSweep = (completedRequests / total) * 360f
//            drawArc(
//                color = Color(0xFF6B7FD7),
//                startAngle = startAngle,
//                sweepAngle = completedSweep,
//                useCenter = false,
//                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
//            )
//            startAngle += completedSweep
//
//            val processingSweep = (processingRequests / total) * 360f
//            drawArc(
//                color = Color(0xFF5DD7A7),
//                startAngle = startAngle,
//                sweepAngle = processingSweep,
//                useCenter = false,
//                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
//            )
//            startAngle += processingSweep
//
//            val actionSweep = (actionNeededRequests / total) * 360f
//            drawArc(
//                color = Color(0xFFFF9F6E),
//                startAngle = startAngle,
//                sweepAngle = actionSweep,
//                useCenter = false,
//                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
//            )
//            startAngle += actionSweep
//
//            val rejectedSweep = (rejectedRequests / total) * 360f
//            drawArc(
//                color = Color(0xFFFF6B8A),
//                startAngle = startAngle,
//                sweepAngle = rejectedSweep,
//                useCenter = false,
//                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
//            )
//        }
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(
//                text = "$totalRequests",
//                fontSize = 36.sp,
//                fontWeight = FontWeight.SemiBold,
//                letterSpacing = 1.sp,
//                color = extraColors.whiteInDarkMode
//            )
//            Text(
//                text = localizedApp(R.string.total_requests_label),
//                fontSize = 14.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
//            )
//        }
//    }
//}

//@Composable
//fun LegendItem(
//    label: String,
//    value: Int,
//    percentage: Float,
//    color: Color
//) {
//    val extraColors = LocalExtraColors.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(16.dp)
//                    .background(color, shape = CircleShape)
//            )
//            Text(
//                text = label,
//                fontSize = 14.sp,
//                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
//            )
//        }
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "$value",
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Normal,
//                letterSpacing = 1.sp,
//                color = extraColors.whiteInDarkMode,
//                maxLines = 1
//            )
//            Text(
//                text = "(%.2f%%)".format(percentage),
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Normal,
//                letterSpacing = 1.sp,
//                color = extraColors.whiteInDarkMode,
//                maxLines = 1
//            )
//        }
//    }
//}

@Composable
fun FormsSectionWithLazyColumn(
    modifier: Modifier = Modifier,
    navController: NavController,
    searchQuery: String = "",
    selectedFilter: FilterType? = null,
    sortOrder: SortOrder = SortOrder.DESCENDING,
    onFilterSelected: (FilterType?) -> Unit = {},
    statusCounts: com.informatique.mtcit.data.model.requests.StatusCountResponse?,
    statusCountsLoading: Boolean
) {
    val extraColors = LocalExtraColors.current
    val isArabic = LocalAppLocale.current.language == "ar"
    val requestsViewModel: com.informatique.mtcit.ui.viewmodels.RequestsViewModel = hiltViewModel()

    // ✅ Listen to sortOrder changes and trigger API call
    LaunchedEffect(sortOrder) {
        val ascending = sortOrder == SortOrder.ASCENDING
        println("🔄 Sort order changed in FormsSectionWithLazyColumn: $sortOrder (ascending=$ascending)")
        requestsViewModel.changeSortOrder(ascending)
    }

    // ✅ NEW: Listen to filter changes and trigger API call with filter
    LaunchedEffect(selectedFilter) {
        val statusId = selectedFilter?.getStatusId()
        println("🔍 Filter changed in FormsSectionWithLazyColumn: $selectedFilter (statusId=$statusId)")
        requestsViewModel.applyFilter(statusId)
    }

    val requests by requestsViewModel.requests.collectAsState()
    val isLoading by requestsViewModel.isLoading.collectAsState()
    val isLoadingMore by requestsViewModel.isLoadingMore.collectAsState()
    val paginationState by requestsViewModel.paginationState.collectAsState()
    val appError by requestsViewModel.appError.collectAsState()

    // ✅ Filter requests based on search query ONLY (status filter now handled by API)
    val filteredRequests = remember(requests, searchQuery) {
        var result = requests

        // ✅ Always exclude draft requests (statusId == 1) from the list
        result = result.filter { request -> request.statusId != 1 }

        // Apply search filter (client-side filtering for search query)
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
                    FilterType.IN_PROGRESS -> request.statusName.contains(if (isArabic) "قيد المعالجة" else "Under Processing", ignoreCase = true) ||
                            request.statusName.contains("in progress", ignoreCase = true) ||
                            request.statusName.contains("processing", ignoreCase = true)
                    FilterType.NEEDS_ACTION -> request.statusName.contains(if (isArabic) "يحتاج إجراء" else "Needs Action", ignoreCase = true) ||
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
                    FilterType.CONFIRMED -> request.statusName.contains(if (isArabic) "مؤكد" else "Confirmed", ignoreCase = true) ||
                            request.statusName.contains("confirmed", ignoreCase = true)
                    FilterType.SEND -> request.statusName.contains("تم الإرسال", ignoreCase = true) ||
                            request.statusName.contains("sent", ignoreCase = true)
                    FilterType.SCHEDULED -> request.statusName.contains("مجدول", ignoreCase = true) ||
                            request.statusName.contains("scheduled", ignoreCase = true)
                    FilterType.IN_REVIEW -> request.statusName.contains("قيد المراجعة", ignoreCase = true) ||
                            request.statusName.contains("in review", ignoreCase = true)
                    FilterType.ISSUED -> request.statusName.contains("مصدر", ignoreCase = true) ||
                            request.statusName.contains("issued", ignoreCase = true)
                    else -> true
                }
            }
        }

        when (sortOrder) {
            SortOrder.ASCENDING -> result.sortedBy { it.modificationDate }
            SortOrder.DESCENDING -> result.sortedByDescending { it.modificationDate }
        }
    }

    // ✅ NEW: Collect navigation trigger for request detail
    val navigationToRequestDetail by requestsViewModel.navigationToRequestDetail.collectAsState()
    val shouldNavigateToLogin by requestsViewModel.shouldNavigateToLogin.collectAsState()

    // ✅ Handle navigation to request detail screen
    LaunchedEffect(navigationToRequestDetail) {
        navigationToRequestDetail?.let { (requestId, requestTypeId) ->
            println("🔍 FormsSectionWithLazyColumn: Navigating to request detail - ID: $requestId, TypeID: $requestTypeId")
            navController.navigate(NavRoutes.ApiRequestDetailRoute.createRoute(requestId, requestTypeId))
            requestsViewModel.clearNavigationTrigger()
        }
    }

    // ✅ Handle navigation to login
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            println("🔑 FormsSectionWithLazyColumn: Navigating to login - token refresh failed")
            navController.navigate(NavRoutes.OAuthWebViewRoute.route)
            requestsViewModel.resetNavigationTrigger()
        }
    }

    // ✅ Observe login completion
    DisposableEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle

        val observer = androidx.lifecycle.Observer<Boolean> { loginCompleted ->
            if (loginCompleted) {
                println("✅ FormsSectionWithLazyColumn: Login completed detected, reloading requests...")
                requestsViewModel.clearAppError()
                requestsViewModel.loadRequests()
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

    LazyColumn(
        modifier = modifier
    ) {
        // Header section
        item {
            UserProfileHeader(
                onFilterSelected = onFilterSelected,
                selectedFilter = selectedFilter,  // ✅ Pass selected filter from parent
                statusCounts = statusCounts,
                isLoading = statusCountsLoading
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Section title and icon
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

                // Error banner
                appError?.let { error ->
                    val isAr = LocalAppLocale.current.language == "ar"
                    when (error) {
                        is com.informatique.mtcit.common.AppError.Unauthorized -> {
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = error.getMessage(isAr),
                                onDismiss = { requestsViewModel.clearAppError() },
                                showRefreshButton = true,
                                onRefreshToken = { requestsViewModel.refreshToken() }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.ApiError -> {
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = "${localizedApp(R.string.error)} ${error.code}: ${error.getMessage(isAr)}",
                                onDismiss = { requestsViewModel.clearAppError() }
                            )
                        }
                        is com.informatique.mtcit.common.AppError.Unknown -> {
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = error.getMessage(isAr),
                                showRefreshButton = true,
                                onRefreshToken = {
                                    requestsViewModel.navigateToLogin()
                                },
                                onDismiss = { requestsViewModel.clearAppError() }
                            )
                        }
                        else -> {
                            com.informatique.mtcit.ui.components.ErrorBanner(
                                message = localizedApp(R.string.an_error_occurred),
                                onDismiss = { requestsViewModel.clearAppError() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Loading state
        if (isLoading && requests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = extraColors.whiteInDarkMode)
                }
            }
        }
        // Empty state
        else if (filteredRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
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
                                localizedApp(R.string.no_results_found)
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
                                localizedApp(R.string.try_different_search_terms)
                            } else {
                                localizedApp(R.string.forms_will_appear_here)
                            },
                            fontSize = 14.sp,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        // Request items using LazyColumn items for proper lazy loading
        else {
            itemsIndexed(filteredRequests) { index, request ->
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NewRequestCard(
                        request = request,
                        onClick = {
                            println("🔘 User clicked request: ID=${request.id}, Status=${request.statusName}")
                            requestsViewModel.onRequestClick(request)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ✅ Trigger load more when user scrolls to the last item
                if (index == filteredRequests.lastIndex &&
                    paginationState.hasMore &&
                    !isLoadingMore) {
                    LaunchedEffect(Unit) {
                        println("📜 Scroll-based Load More: User reached last item, loading more...")
                        requestsViewModel.loadMoreRequests()
                    }
                }
            }

            // Loading indicator for pagination
            if (isLoadingMore && paginationState.hasMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = extraColors.blue1,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

//@Composable
//fun FormsSection(
//    navController: NavController,
//    viewModel: MarineRegistrationViewModel = hiltViewModel(),
//    searchQuery: String = "",
//    selectedFilter: FilterType? = null,
//    sortOrder: SortOrder = SortOrder.DESCENDING,
//    onSortOrderChanged: (SortOrder) -> Unit = {}
//) {
//    val extraColors = LocalExtraColors.current
//
//    // ✅ Use the correct RequestsViewModel (not UserRequestsViewModel)
//    val requestsViewModel: com.informatique.mtcit.ui.viewmodels.RequestsViewModel = hiltViewModel()
//
//    // ✅ Listen to sortOrder changes and trigger API call
//    LaunchedEffect(sortOrder) {
//        val ascending = sortOrder == SortOrder.ASCENDING
//        println("🔄 Sort order changed in FormsSection: $sortOrder (ascending=$ascending)")
//        requestsViewModel.changeSortOrder(ascending)
//    }
//
//    val requests by requestsViewModel.requests.collectAsState()
//    val isLoading by requestsViewModel.isLoading.collectAsState()
//    val isLoadingMore by requestsViewModel.isLoadingMore.collectAsState()
//    val paginationState by requestsViewModel.paginationState.collectAsState()
//    val appError by requestsViewModel.appError.collectAsState()
//
//    // ✅ Filter requests based on search query and selected filter (sorting handled by API)
//    val filteredRequests = remember(requests, searchQuery, selectedFilter) {
//        var result = requests
//
//        // Apply search filter (search in request serial or ship name)
//        if (searchQuery.isNotBlank()) {
//            result = result.filter { request ->
//                request.requestSerial.contains(searchQuery, ignoreCase = true) ||
//                request.shipName.contains(searchQuery, ignoreCase = true)
//            }
//        }
//
//        // Apply status filter
//        if (selectedFilter != null && selectedFilter != FilterType.ALL) {
//            result = result.filter { request ->
//                when (selectedFilter) {
//                    FilterType.COMPLETED -> request.statusName.contains("مكتمل", ignoreCase = true) ||
//                                           request.statusName.contains("completed", ignoreCase = true)
//                    FilterType.IN_PROGRESS -> request.statusName.contains("قيد المعالجة", ignoreCase = true) ||
//                                             request.statusName.contains("in progress", ignoreCase = true) ||
//                                             request.statusName.contains("processing", ignoreCase = true)
//                    FilterType.NEEDS_ACTION -> request.statusName.contains("يحتاج إجراء", ignoreCase = true) ||
//                                              request.statusName.contains("needs action", ignoreCase = true) ||
//                                              request.statusName.contains("action needed", ignoreCase = true)
//                    FilterType.DRAFT -> request.statusName.contains("مسودة", ignoreCase = true) ||
//                                       request.statusName.contains("draft", ignoreCase = true)
//                    FilterType.REJECTED -> request.statusName.contains("مرفوض", ignoreCase = true) ||
//                                          request.statusName.contains("rejected", ignoreCase = true)
//                    FilterType.PENDING -> request.statusName.contains("معلق", ignoreCase = true) ||
//                                         request.statusName.contains("pending", ignoreCase = true)
//                    FilterType.ACCEPTED -> request.statusName.contains("مقبول", ignoreCase = true) ||
//                                          request.statusName.contains("accepted", ignoreCase = true)
//                    FilterType.CONFIRMED -> request.statusName.contains("مؤكد", ignoreCase = true) ||
//                                           request.statusName.contains("confirmed", ignoreCase = true)
//                    FilterType.SEND -> request.statusName.contains("تم الإرسال", ignoreCase = true) ||
//                                      request.statusName.contains("sent", ignoreCase = true)
//                    FilterType.SCHEDULED -> request.statusName.contains("مجدول", ignoreCase = true) ||
//                                           request.statusName.contains("scheduled", ignoreCase = true)
//                    FilterType.IN_REVIEW -> request.statusName.contains("قيد المراجعة", ignoreCase = true) ||
//                                           request.statusName.contains("in review", ignoreCase = true)
//                    FilterType.ISSUED -> request.statusName.contains("مصدر", ignoreCase = true) ||
//                                        request.statusName.contains("issued", ignoreCase = true)
//                    else -> true
//                }
//            }
//        }
//
//        // ❌ DISABLED: Sort order (sort by modification date)
//        // Keep the original order from API without sorting
//        when (sortOrder) {
//            SortOrder.ASCENDING -> result.sortedBy { it.modificationDate }
//            SortOrder.DESCENDING -> result.sortedByDescending { it.modificationDate }
//        }
//    }
//
//    // ✅ Track the last size when load more was triggered to prevent automatic loading
//    var lastLoadTriggeredSize by remember { mutableStateOf(-1) }
//
//    // ✅ NEW: Collect navigation trigger for request detail
//    val navigationToRequestDetail by requestsViewModel.navigationToRequestDetail.collectAsState()
//
//    // ✅ NEW: Collect navigation trigger (like MainCategoriesScreen)
//    val shouldNavigateToLogin by requestsViewModel.shouldNavigateToLogin.collectAsState()
//
//    // ✅ NEW: Handle navigation to request detail screen
//    LaunchedEffect(navigationToRequestDetail) {
//        navigationToRequestDetail?.let { (requestId, requestTypeId) ->
//            println("🔍 ProfileScreen: Navigating to request detail - ID: $requestId, TypeID: $requestTypeId")
//            navController.navigate(NavRoutes.ApiRequestDetailRoute.createRoute(requestId, requestTypeId))
//            requestsViewModel.clearNavigationTrigger()
//        }
//    }
//
//    // ✅ NEW: Handle navigation to login (like MainCategoriesScreen)
//    LaunchedEffect(shouldNavigateToLogin) {
//        if (shouldNavigateToLogin) {
//            println("🔑 ProfileScreen: Navigating to login - token refresh failed")
//            navController.navigate(NavRoutes.OAuthWebViewRoute.route)
//            requestsViewModel.resetNavigationTrigger()
//        }
//    }
//
//    // ✅ FIXED: Use DisposableEffect like MainCategoriesScreen to properly observe login completion
//    DisposableEffect(navController.currentBackStackEntry) {
//        val handle = navController.currentBackStackEntry?.savedStateHandle
//
//        val observer = androidx.lifecycle.Observer<Boolean> { loginCompleted ->
//            if (loginCompleted == true) {
//                println("✅ ProfileScreen: Login completed detected, reloading requests...")
//                // User returned from successful login, reload requests
//                requestsViewModel.clearAppError()
//                requestsViewModel.loadRequests()
//                // Clear the flag
//                handle?.set("login_completed", false)
//            }
//        }
//
//        handle?.getLiveData<Boolean>("login_completed")?.observeForever(observer)
//
//        onDispose {
//            handle?.getLiveData<Boolean>("login_completed")?.removeObserver(observer)
//        }
//    }
//
//    // Load requests on first composition
//    LaunchedEffect(Unit) {
//        requestsViewModel.loadRequests()
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 20.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.ic_documentation),
//                contentDescription = null,
//                tint = extraColors.whiteInDarkMode,
//                modifier = Modifier.size(24.dp)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = localizedApp(R.string.forms_section_title),
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Normal,
//                letterSpacing = 1.sp,
//                color = extraColors.whiteInDarkMode
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // ✅ Show error banner using the same component as other screens
//        appError?.let { error ->
//            when (error) {
//                is com.informatique.mtcit.common.AppError.Unauthorized -> {
//                    // 401 Error - Show banner with refresh token button
//                    com.informatique.mtcit.ui.components.ErrorBanner(
//                        message = error.message,
//                        onDismiss = { requestsViewModel.clearAppError() },
//                        showRefreshButton = true,
//                        onRefreshToken = { requestsViewModel.refreshToken() }
//                    )
//                }
//                is com.informatique.mtcit.common.AppError.ApiError -> {
//                    // Other API errors
//                    com.informatique.mtcit.ui.components.ErrorBanner(
//                        message = "${if (LocalAppLocale.current.language == "ar") "خطأ" else "Error"} ${error.code}: ${error.message}",
//                        onDismiss = { requestsViewModel.clearAppError() }
//                    )
//                }
//                is com.informatique.mtcit.common.AppError.Unknown -> {
//                    // ✅ Token refresh failed - Show with "Go to Login" button
//                    com.informatique.mtcit.ui.components.ErrorBanner(
//                        message = error.message,
//                        showRefreshButton = true,
//                        onRefreshToken = {
//                            // Navigate to login when refresh token is expired
//                            requestsViewModel.navigateToLogin()
//                        },
//                        onDismiss = { requestsViewModel.clearAppError() }
//                    )
//                }
//                else -> {
//                    // Other error types
//                    com.informatique.mtcit.ui.components.ErrorBanner(
//                        message = if (LocalAppLocale.current.language == "ar") "حدث خطأ" else "An error occurred",
//                        onDismiss = { requestsViewModel.clearAppError() }
//                    )
//                }
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//        }
//
//        if (isLoading && requests.isEmpty()) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(color = extraColors.whiteInDarkMode)
//            }
//        } else if (filteredRequests.isEmpty()) {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(16.dp),
//                colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
//                elevation = CardDefaults.cardElevation(0.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(32.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = localizedApp(R.string.empty_state_icon),
//                        fontSize = 48.sp
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = if (searchQuery.isNotBlank() || selectedFilter != null) {
//                            if (LocalAppLocale.current.language == "ar") "لا توجد نتائج" else "No results found"
//                        } else {
//                            localizedApp(R.string.no_forms_available)
//                        },
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Normal,
//                        letterSpacing = 1.sp,
//                        color = extraColors.whiteInDarkMode
//                    )
//                    Text(
//                        text = if (searchQuery.isNotBlank() || selectedFilter != null) {
//                            if (LocalAppLocale.current.language == "ar") "جرب البحث بكلمات مختلفة" else "Try different search terms"
//                        } else {
//                            localizedApp(R.string.forms_will_appear_here)
//                        },
//                        fontSize = 14.sp,
//                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
//                    )
//                }
//            }
//        } else {
//            // Display filtered requests from API
//             filteredRequests.forEachIndexed { index, request ->
//                NewRequestCard(
//                    request = request,
//                    onClick = {
//                        println("🔘 User clicked request: ID=${request.id}, Status=${request.statusName}")
//                        requestsViewModel.onRequestClick(request)
//                    }
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // ✅ Load more ONLY when user reaches the actual LAST item
//                // This triggers when the last card is composed (i.e., visible on screen)
//                if (index == filteredRequests.lastIndex &&
//                    paginationState.hasMore &&
//                    !isLoadingMore &&
//                    requests.size > 0 &&
//                    requests.size != lastLoadTriggeredSize) {
//
//                    // This LaunchedEffect runs when the last item becomes visible
//                    LaunchedEffect(requests.size) {
//                        println("📜 Load More: User reached last item (${requests.size} total), loading more...")
//                        lastLoadTriggeredSize = requests.size
//                        requestsViewModel.loadMoreRequests()
//                    }
//                }
//            }
//
//            // Loading indicator for pagination
//            if (isLoadingMore && paginationState.hasMore) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 16.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(24.dp),
//                        color = extraColors.blue1,
//                        strokeWidth = 2.dp
//                    )
//                }
//            }
//        }
//    }
//}

// ✅ NEW: Request Card using API data with localized status
@Composable
fun NewRequestCard(
    request: com.informatique.mtcit.data.model.requests.UserRequestUiModel,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isArabic = LocalAppLocale.current.language == "ar"

    // ✅ Derive names live from reactive isArabic — updates instantly on language change
    // without requiring a data re-fetch or app restart.
    val displayStatusName = RequestStatus.fromStatusId(request.statusId)
        ?.let { if (isArabic) it.arabicName else it.englishName }
        ?: request.statusName   // fallback to pre-computed if statusId unknown

    val displayTypeName = if (isArabic) request.requestTypeNameAr else request.requestTypeNameEn

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
                        text = displayStatusName,
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
                text = displayTypeName,
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
                        text = localizedApp(R.string.last_update),
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
                    Text(
                        text = localizedApp(R.string.view_details),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5BA3E8)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF5BA3E8),
                        modifier = Modifier.size(18.dp)
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
    } catch (_: Exception) {
        isoDate.take(10)
    }
}

//fun formatDate(isoDate: String): String {
//    return try {
//        val instant = java.time.Instant.parse(isoDate)
//        val formatter = java.time.format.DateTimeFormatter
//            .ofPattern("dd MMMM yyyy")
//            .withZone(java.time.ZoneId.systemDefault())
//        formatter.format(instant)
//    } catch (e: Exception) {
//        isoDate.take(10)
//    }
//}

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
fun Search(
    onSearchQueryChanged: (String) -> Unit = {},
    onSortOrderChanged: (SortOrder) -> Unit = {},
    onFilterSelected: (FilterType?) -> Unit = {},
    currentSortOrder: SortOrder = SortOrder.DESCENDING,
    selectedFilter: FilterType? = FilterType.ALL,  // ✅ NEW: Receive selected filter from parent
){
    var searchQuery by remember { mutableStateOf("") }
    val extraColors = LocalExtraColors.current
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Search Bar with Filter Icon - Functional TextField
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 6.dp),
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
                                    text = localizedApp(R.string.search_by_request_number_or_ship_name),
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
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(22.dp)
                )
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
                            onFilterSelected(filter)
                            showFilterBottomSheet = false
                        },
                        onSortOrderChanged = { order ->
                            onSortOrderChanged(order)
                            showFilterBottomSheet = false
                        },
                        onClearFilter = {
                            onFilterSelected(null)
                            showFilterBottomSheet = false
                        }
                    )
                }
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileHeader(
    onFilterSelected: (FilterType?) -> Unit = {},
    selectedFilter: FilterType? = FilterType.ALL,  // ✅ NEW: Receive selected filter from parent
    statusCounts: com.informatique.mtcit.data.model.requests.StatusCountResponse? = null,
    isLoading: Boolean = false
) {
    // ✅ Extract counts from API response
    val totalCount = statusCounts?.data?.totalCount ?: 0
    val acceptedCount = statusCounts?.data?.statusCounts?.find { it.statusId == 7 }?.count ?: 0
    val sendCount = statusCounts?.data?.statusCounts?.find { it.statusId == 4 }?.count ?: 0
    val rejectedCount = statusCounts?.data?.statusCounts?.find { it.statusId == 2 }?.count ?: 0

    // ✅ REMOVED: LaunchedEffect that was resetting filter on recomposition

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
//        Spacer(modifier = Modifier.height(20.dp))

        // Statistics Grid - 4 Main Cards (Using API Data)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                number = if (isLoading) "..." else totalCount.toString(),
                label = localizedApp(R.string.total),
                numberColor = Color(0xFFE91E63),
                accentColor = Color(0xFFE91E63),
                isSelected = selectedFilter == FilterType.ALL,
                modifier = Modifier.weight(1f),
                onClick = {
                    val newFilter = if (selectedFilter == FilterType.ALL) null else FilterType.ALL
                    onFilterSelected(newFilter)
                }
            )
            StatCard(
                number = if (isLoading) "..." else acceptedCount.toString(),
                label = localizedApp(R.string.accepted),
                numberColor = Color(0xFF4CAF50),
                accentColor = Color(0xFF4CAF50),
                isSelected = selectedFilter == FilterType.ACCEPTED,
                modifier = Modifier.weight(1f),
                onClick = {
                    val newFilter = if (selectedFilter == FilterType.ACCEPTED) null else FilterType.ACCEPTED
                    onFilterSelected(newFilter)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                number = if (isLoading) "..." else sendCount.toString(),
                label = localizedApp(R.string.sent),
                numberColor = Color(0xFF2196F3),
                accentColor = Color(0xFF2196F3),
                isSelected = selectedFilter == FilterType.SEND,
                modifier = Modifier.weight(1f),
                onClick = {
                    val newFilter = if (selectedFilter == FilterType.SEND) null else FilterType.SEND
                    onFilterSelected(newFilter)
                }
            )
            StatCard(
                number = if (isLoading) "..." else rejectedCount.toString(),
                label = localizedApp(R.string.rejected),
                numberColor = Color(0xFFFF9800),
                accentColor = Color(0xFFFF9800),
                isSelected = selectedFilter == FilterType.REJECTED,
                modifier = Modifier.weight(1f),
                onClick = {
                    val newFilter = if (selectedFilter == FilterType.REJECTED) null else FilterType.REJECTED
                    onFilterSelected(newFilter)
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
                text = localizedApp(R.string.filter_sort),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode
            )

            TextButton(onClick = onClearFilter) {
                Text(
                    text = localizedApp(R.string.clear_all),
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sort Order Section
        Text(
            text = localizedApp(R.string.sort_order),
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
                label = localizedApp(R.string.ascending),
                icon = "↑",
                isSelected = currentSortOrder == SortOrder.ASCENDING,
                onClick = { onSortOrderChanged(SortOrder.ASCENDING) },
                modifier = Modifier.weight(1f)
            )

            // Descending Card
            SortOrderCard(
                label = localizedApp(R.string.descending),
                icon = "↓",
                isSelected = currentSortOrder == SortOrder.DESCENDING,
                onClick = { onSortOrderChanged(SortOrder.DESCENDING) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Filter Section
        Text(
            text = localizedApp(R.string.filter_by_status),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // All Status Cards in Grid
        val statusList = listOf(
            StatusFilterItem(FilterType.ALL, localizedApp(R.string.all_requests), "671", Color(0xFFE91E63)),
//            StatusFilterItem(FilterType.DRAFT, localizedApp(R.string.draft), "0", Color(0xFF9E9E9E)),
            StatusFilterItem(FilterType.REJECTED, localizedApp(R.string.rejected), "0", Color(0xFFF44336)),
            StatusFilterItem(FilterType.SEND, localizedApp(R.string.sent_2), "0", Color(0xFFFF9800)),
            StatusFilterItem(FilterType.SCHEDULED, localizedApp(R.string.scheduled), "0", Color(0xFF2196F3)),
            StatusFilterItem(FilterType.ACCEPTED, localizedApp(R.string.accepted), "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.COMPLETED, localizedApp(R.string.completed), "52", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.IN_REVIEW, localizedApp(R.string.in_review), "0", Color(0xFF4A90E2)),
            StatusFilterItem(FilterType.REVIEW_RTA, localizedApp(R.string.rta_review), "0", Color(0xFF673AB7)),
            StatusFilterItem(FilterType.REJECT_AUTHORITIES, localizedApp(R.string.auth_rejected), "0", Color(0xFFE91E63)),
            StatusFilterItem(FilterType.APPROVED_FINAL, localizedApp(R.string.final_approval), "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.ISSUED, localizedApp(R.string.issued), "0", Color(0xFF4CAF50)),
            StatusFilterItem(FilterType.WAITING_INSPECTION, localizedApp(R.string.waiting_inspection), "0", Color(0xFF4CAF50)),
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
    NEEDS_ACTION;

    /**
     * Get the statusId for API filtering
     * Returns null for ALL (no filter)
     */
    fun getStatusId(): Int? {
        return when (this) {
            ALL -> null
            DRAFT -> 1
            REJECTED -> 2
            CONFIRMED -> 3
            SEND -> 4
            PENDING -> 5
            SCHEDULED -> 6
            ACCEPTED -> 7
            IN_REVIEW -> 8
            REVIEW_RTA -> 9
            REJECT_AUTHORITIES -> 10
            APPROVED_AUTHORITIES -> 11
            APPROVED_FINAL -> 12
            ACTION_TAKEN -> 13
            ISSUED -> 14
            WAITING_INSPECTION -> 16
            IN_PROGRESS -> 5 // Map to PENDING
            NEEDS_ACTION -> 5 // Map to PENDING
            COMPLETED -> 14 // Map to ISSUED
        }
    }
}

// Sort Order Types
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

