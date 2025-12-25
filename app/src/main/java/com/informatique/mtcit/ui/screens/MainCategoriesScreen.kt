package com.informatique.mtcit.ui.screens

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.R
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.data.model.category.Transaction
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.MainCategoriesViewModel
import com.informatique.mtcit.ui.viewmodels.SubCategoriesUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCategoriesScreen(
    navController: NavController,
    categoryIdToExpand: String = ""
) {

    val locale = LocalAppLocale.current

    val viewModel: MainCategoriesViewModel = hiltViewModel()

    val uiState by viewModel.subCategories.collectAsStateWithLifecycle()

//    LaunchedEffect(Unit) {
//        viewModel.getSubCategoriesApi()
//    }

    // Auto-expand the specified category when navigating from home
    LaunchedEffect(categoryIdToExpand) {
        if (categoryIdToExpand.isNotEmpty()) {
            viewModel.expandCategory(categoryIdToExpand)
        }
    }

    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // FAB menu state
    // var isFabMenuExpanded by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(extraColors.background)) {
        // Background gradient header with wave overlay extended to include status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp + statusBarHeight)
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
            // Subtle whiteInDarkMode wave overlay (like the Swift Path overlay)
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.72f)
                    // Quadratic bezier to create a smooth wave
                    quadraticTo(
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
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        // Back Icon Button
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4A7BA7),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                                )
                                .background(extraColors.iconBackBackground)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = localizedApp(R.string.back_button),
                                tint = extraColors.iconBack
                            )
                        }
                    },
//                    actions = {
//                        // Settings Icon Button
//                        Box(
//                            modifier = Modifier
//                                .padding(12.dp)
//                                .size(38.dp)
//                                .clip(CircleShape)
//                                .border(
//                                    width = 1.dp,
//                                    color = Color(0xFF4A7BA7),
//                                    shape = CircleShape
//                                )
//                                .shadow(
//                                    elevation = 20.dp,
//                                    shape = CircleShape,
//                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
//                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
//                                )
//                                .background(extraColors.iconBackBackground)
//                                .clickable { navController.navigate(NavRoutes.SettingsRoute.route) },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Settings,
//                                contentDescription = localizedApp(R.string.settings_title),
//                                tint = extraColors.iconBack
//                            )
//                        }
//                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent // let the gradient show through
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = localizedApp(R.string.main_category_registration_department),
                    fontSize = 26.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp))
                // Subtitle
                Text(
                    text = localizedApp(R.string.choose_department_that_suits_your_needs),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 24.dp , bottom = 6.dp , top = 6.dp))
                // Filters Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 50.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
//                    // Institution Filter
//                    FilterDropdown(
//                        label = localizedApp(R.string.institution),
//                        icon = Icons.Default.Person,
//                        modifier = Modifier.weight(1f),
//                        onSelected = { viewModel.selectInstitution(it) }
//                    )
                    // Organization Filter
                    FilterDropdown(
                        label = localizedApp(R.string.organization),
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f),
                        onSelected = { viewModel.selectOrganization(it) }
                    )
                }

                // Categories List
                when (uiState) {
                    SubCategoriesUiState.Blank -> {}
                    is SubCategoriesUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is SubCategoriesUiState.Success -> {
                        val subCategories = (uiState as SubCategoriesUiState.Success).subcategories
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(subCategories) { index, subCategory ->
                                CategoryCard(
                                    locale = locale,
                                    category = subCategory,
                                    isExpanded = index == 0,
                                    onSubCategoryClick = { transaction ->
                                        /*navController.navigate(NavRoutes.TransactionListRoute.createRoute(category.id, subCategory.id))*/
                                        navController.navigate(
                                            NavRoutes.TransactionRequirementRoute.createRoute(
                                                transaction = transaction
                                            )
                                        )
                                    },
                                    availableServicesCount = subCategory.transactions.size
                                )
                            }
                        }
                    }
                    is SubCategoriesUiState.Error -> {
                        val error = (uiState as SubCategoriesUiState.Error).message
                        Text(text = error)
                    }
                }

            }
        }

//        // Scrim/Backdrop when FAB menu is expanded
//        if (isFabMenuExpanded) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.4f))
//                    .clickable { isFabMenuExpanded = false }
//            )
//        }
//
//        // FAB Menu overlay
//        AnimatedVisibility(
//            visible = isFabMenuExpanded,
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(top = 64.dp, end = 8.dp),
//            enter = fadeIn() + expandVertically(),
//            exit = fadeOut() + shrinkVertically()
//        ) {
//            Column(
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                horizontalAlignment = Alignment.End
//            ) {
//                // Change Language Item
//                HomeExtendedFabMenuItem(
//                    icon = Icons.Default.Language,
//                    label = localizedApp(R.string.change_language),
//                    onClick = {
//                        isFabMenuExpanded = false
//                        navController.navigate("languagescreen")
//                    }
//                )
//
//                // Change Theme Item
//                HomeExtendedFabMenuItem(
//                    icon = Icons.Default.DarkMode,
//                    label = localizedApp(R.string.settings_title),
//                    onClick = {
//                        isFabMenuExpanded = false
//                        navController.navigate("settings_screen")
//                    }
//                )
//            }
//        }
    }
}

@Composable
fun HomeExtendedFabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = extraColors.blue1,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val extraColors = LocalExtraColors.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground2),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = localizedApp(R.string.select),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}

@Composable
fun CategoryCard(
    locale: Locale,
    category: com.informatique.mtcit.data.model.category.SubCategory,
    isExpanded: Boolean,
    onSubCategoryClick: (Transaction) -> Unit,
    availableServicesCount: Int
) {
    val extraColors = LocalExtraColors.current
    var expanded by remember { mutableStateOf(false) }

    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
            elevation = CardDefaults.cardElevation(1.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    extraColors.iconGreyBackground,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_ship_registration),
                                contentDescription = null,
                                tint = extraColors.iconBlueGrey,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Category Title and Description
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (locale.language == "ar") category.nameAr else category.nameEn,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
//                                letterSpacing = 1.sp,
                                color = extraColors.whiteInDarkMode
                            )
                            if (category.descAr != null || category.descEn != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (locale.language == "ar") (category.descAr ?: "")
                                    else (category.descEn ?: ""),
                                    fontSize = 14.sp,
                                    color = extraColors.textSubTitle
                                )
                            }
                        }
                    }

                    // Expand/Collapse Icon
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(if (expanded) 180f else 0f)
                    )
                }
            }
        }

        // Sub-categories List (Expandable)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extraColors.background.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                category.transactions.forEach { transaction ->
                    SubCategoryItem(
                        locale = locale,
                        transaction = transaction,
                        onClick = { onSubCategoryClick(transaction) }
                    )
                }

                // Spacer(modifier = Modifier.height(8.dp))

                // Services available badge
                /*Text(
                    text = "$availableServicesCount ${localizedApp(R.string.services_available)}",
                    fontSize = 13.sp,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier
                        .background(
                            extraColors.cardBackground2.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )*/
            }
        }
    }
}

@Composable
fun SubCategoryItem(
    locale: Locale,
    transaction: Transaction,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // النقطة الزرقاء
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = extraColors.iconBlueGrey,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (locale.language == "ar") transaction.nameAr else transaction.nameEn,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
