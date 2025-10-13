package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.Transaction
import com.informatique.mtcit.ui.models.TransactionCategory
import com.informatique.mtcit.ui.models.getTransactionCategories
import com.informatique.mtcit.ui.theme.LocalExtraColors

@Composable
fun TransactionCategoriesScreen(navController: NavController) {
    val categories = getTransactionCategories()
    val extraColors = LocalExtraColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        categories.forEach { category ->
            // Category Header
            item {
                CategoryHeader(category = category)
            }

            // Transactions under this category
            items(category.transactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = {
                        navController.navigate(transaction.route)
                    }
                )
            }

            // Spacer between categories
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CategoryHeader(category: TransactionCategory) {
    val extraColors = LocalExtraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Category Icon
        Icon(
            painter = painterResource(id = category.iconRes),
            contentDescription = localizedApp(category.titleRes),
            modifier = Modifier.size(40.dp),
            tint = extraColors.blue1
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Category Title and Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = localizedApp(category.titleRes),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.blue1
            )
            Text(
                text = localizedApp(category.descriptionRes),
                fontSize = 13.sp,
                color = colorResource(id = R.color.grey),
                lineHeight = 16.sp
            )
        }
    }

    // Divider below header
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color = extraColors.background.copy(alpha = 0.2f)
    )
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = localizedApp(transaction.titleRes),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.blue1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = localizedApp(transaction.descriptionRes),
                    fontSize = 12.sp,
                    color = extraColors.blue2,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = extraColors.blue1,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
