package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.Transaction
import com.informatique.mtcit.ui.providers.LocalCategories
import com.informatique.mtcit.ui.theme.ExtraColors
import com.informatique.mtcit.ui.theme.LocalExtraColors
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
                TopAppBar(
                    title = {
                        Text(
                            text = subCategory?.let { localizedApp(it.titleRes) } ?: "Sub Category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = extraColors.background
                    )
                )
            }
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
                    items(transactions.size) { index ->
                        val transaction = transactions[index]
                        TransactionListCard(
                            transaction = transaction,
                            isSelected = viewModel.isTransactionSelected(transaction.id),
                            onToggleSelection = { viewModel.toggleTransactionSelection(transaction.id) },
                            onClick = { navController.navigate(transaction.route) },
                            mainCategoryName = mainCategory?.let { localizedApp(it.titleRes) } ?: ""
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListCard(
    transaction: Transaction,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    mainCategoryName: String
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                extraColors.blue1,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Transaction Title
                    Text(
                        text = localizedApp(transaction.titleRes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.blue1,
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    )
                }

                // Description (spans full width)
                Text(
                    text = localizedApp(transaction.descriptionRes),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    maxLines = 2
                )

                // Category Badge (rounded with blue background) - Now shows MAIN category
                Surface(
                    color = extraColors.blue2.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = mainCategoryName,
                        fontSize = 12.sp,
                        color = extraColors.blue1,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Right Column: Metadata (vertically centered)
            Column(
                modifier = Modifier.width(90.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Time Range
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
                        text = "1-4 أيام",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Steps
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
                    Text(
                        text = "${transaction.stepCount} خطوات",
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
