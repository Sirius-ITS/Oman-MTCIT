package com.informatique.mtcit.ui.screens

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.MainCategory
import com.informatique.mtcit.ui.models.SubCategory
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.MainCategoriesViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCategoriesScreen(navController: NavController, sharedUserViewModel: SharedUserViewModel) {
    val viewModel: MainCategoriesViewModel = hiltViewModel()
    // Get categories from CompositionLocal instead of ViewModel
    val categories = LocalCategories.current

    // Set categories into ViewModel when they change
    LaunchedEffect(categories) {
        if (categories.isNotEmpty()) {
            viewModel.setCategories(categories)
        }
    }

    val expandedCategories by viewModel.expandedCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // FAB menu state
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
                .height(200.dp + statusBarHeight)
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                        text = localizedApp(R.string.solutions_and_services),
                        fontSize = 22.sp,
                        color = extraColors.white,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp , top = 35.dp))
                            },
                    actions = {
                        // Settings/Close Icon Button
                        Box(
                            modifier = Modifier
                                .padding( 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable{ navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (isFabMenuExpanded) "Close Menu" else "Settings Menu",
                                tint = extraColors.white
                            )
                        }
                    },
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
                // Subtitle
                Text(
                    text = localizedApp(R.string.choose_department_that_suits_your_needs),
                    fontSize = 14.sp,
                    color = extraColors.white.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 24.dp , bottom = 10.dp , top = 4.dp))
                // Filters Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp , end = 16.dp , bottom = 17.dp , top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Institution Filter
                    FilterDropdown(
                        label = localizedApp(R.string.institution),
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f),
                        onSelected = { viewModel.selectInstitution(it) }
                    )

                    // Organization Filter
                    FilterDropdown(
                        label = localizedApp(R.string.organization),
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f),
                        onSelected = { viewModel.selectOrganization(it) }
                    )
                }

                // Categories List
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories.size) { index ->
                            val category = categories[index]
                            CategoryCard(
                                category = category,
                                isExpanded = expandedCategories.contains(category.id),
                                onToggleExpand = { viewModel.toggleCategoryExpansion(category.id) },
                                onSubCategoryClick = { subCategory ->
                                    navController.navigate("transaction_list/${category.id}/${subCategory.id}")
                                },
                                availableServicesCount = viewModel.getAvailableServicesCount(category.id)
                            )
                        }
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
                fontWeight = FontWeight.Medium,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = localizedApp(R.string.select),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: MainCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSubCategoryClick: (SubCategory) -> Unit,
    availableServicesCount: Int
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
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
                                extraColors.blue1.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(category.iconRes),
                            contentDescription = null,
                            tint = extraColors.blue1,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Category Title and Description
                    Column {
                        Text(
                            text = localizedApp(category.titleRes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.blue1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localizedApp(category.descriptionRes),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Expand/Collapse Icon
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }

            // Sub-categories List (Expandable)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extraColors.background.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    category.subCategories.forEach { subCategory ->
                        SubCategoryItem(
                            subCategory = subCategory,
                            onClick = { onSubCategoryClick(subCategory) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Services available badge
                    Text(
                        text = "$availableServicesCount ${localizedApp(R.string.services_available)}",
                        fontSize = 13.sp,
                        color = extraColors.blue1,
                        modifier = Modifier
                            .background(
                                extraColors.blue1.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedApp(subCategory.titleRes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.blue1,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(extraColors.blue1.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
