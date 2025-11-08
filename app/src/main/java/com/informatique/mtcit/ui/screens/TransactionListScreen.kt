package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.Transaction
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.viewmodels.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    navController: NavController,
    categoryId: String,
    subCategoryId: String
) {
    val viewModel: TransactionListViewModel = hiltViewModel()
    val categories = LocalCategories.current
    val transactions by viewModel.transactions.collectAsState()
    val subCategory by viewModel.subCategory.collectAsState()
    val mainCategory by viewModel.mainCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val extraColors = LocalExtraColors.current

    LaunchedEffect(categoryId, subCategoryId) {
        viewModel.loadTransactions(categories, categoryId, subCategoryId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = subCategory?.let { localizedApp(it.titleRes) }
                                ?: "Sub Category",
                            fontSize = 22.sp,
                            color = extraColors.whiteInDarkMode,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4A7BA7 ),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                                )
                                .background(extraColors.iconBackBackground2)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = extraColors.iconBack2
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4A7BA7 ),
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                                )
                                .background(extraColors.iconBackBackground2)
                                .clickable { navController.navigate("settings_screen") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = extraColors.iconBack2
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = extraColors.background
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(extraColors.background)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        transactions.forEachIndexed { index, transaction ->
                            TransactionListCard(
                                transaction = transaction,
                                _isSelected = viewModel.isTransactionSelected(transaction.id),
                                _onToggleSelection = { viewModel.toggleTransactionSelection(transaction.id) },
                                // navigate to the requirements page first (includes category and subCategory so viewModel can reload the list)
                                onClick = {
                                    // pass parent subCategory titleRes to the requirements screen so that its TopAppBar can show it
                                    val parentTitleRes = subCategory?.titleRes?.toString() ?: ""
                                    navController.navigate(
                                        "transaction_requirements/$categoryId/$subCategoryId/${transaction.id}/$parentTitleRes"
                                    )
                                },
                                mainCategoryName = mainCategory?.let { localizedApp(it.titleRes) } ?: "",
                                mainCategoryIconRes = mainCategory?.iconRes
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun TransactionListCard(
    transaction: Transaction,
    _isSelected: Boolean,
    _onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    mainCategoryName: String,
    mainCategoryIconRes: Int? = null
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Content: Icon, Title, Description, Category Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row: Icon + Title
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                extraColors.iconLightBlueBackground,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mainCategoryIconRes != null) {
                            Icon(
                                painter = painterResource(id = mainCategoryIconRes),
                                contentDescription = null,
                                tint = extraColors.iconLightBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = extraColors.iconLightBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Transaction Title
                    Text(
                        text = localizedApp(transaction.titleRes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode,
                        lineHeight = 20.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        maxLines = 2
                    )
                }
                // Description (spans full width)
                Text(
                    text = localizedApp(transaction.descriptionRes),
                    fontSize = 14.sp,
                    color = extraColors.textSubTitle,
                    lineHeight = 22.sp,
                    maxLines = 2
                )

                // Category Badge (rounded with blue background) - Now shows MAIN category
                Surface(
                    color = extraColors.iconLightBlueBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = mainCategoryName,
                        fontSize = 12.sp,
                        color = extraColors.iconLightBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Right Column: Metadata (vertically centered)
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Range (use transaction.duration)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = transaction.duration,
                        fontSize = 11.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Steps (derive from transaction.steps when available)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    val stepsCount = if (transaction.steps.isNotEmpty()) transaction.steps.size else transaction.stepCount
                    Text(
                        text = "$stepsCount خطوات",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Fees
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = transaction.fees,
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
