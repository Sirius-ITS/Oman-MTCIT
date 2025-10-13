package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Coming Soon Screen - Placeholder for transactions not yet implemented
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    navController: NavController,
    transactionName: String,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(extraColors.background)
    ) {
        // Top Header with Back Button and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = localizedApp(R.string.back_button),
                    tint = extraColors.blue1
                )
            }
            Text(
                text = transactionName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extraColors.blue1,
                modifier = Modifier.weight(1f)
            )
        }

        // Coming Soon Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = extraColors.grayCard
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "Coming Soon",
                        modifier = Modifier.size(80.dp),
                        tint = extraColors.blue1
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Coming Soon",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.blue1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This transaction is currently under development and will be available soon.",
                        fontSize = 14.sp,
                        color = extraColors.blue2,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { navController.navigateUp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.blue1
                        )
                    ) {
                        Text(localizedApp(R.string.back_button))
                    }
                }
            }
        }
    }
}

