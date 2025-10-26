package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.models.Transaction
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.TransactionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRequirementsScreen(
    transaction: Transaction,
    onStart: () -> Unit,
    onBack: () -> Unit,
    transactionId: String,
    parentTitleRes: Int? = null,
    navController: NavController
) {
    val viewModel: TransactionListViewModel = hiltViewModel()

//    val categories = LocalCategories.current
    val extraColors = LocalExtraColors.current
//
    LaunchedEffect(transactionId) {
        viewModel.getTransactionRequirements(transactionId)
    }
//
    val requirement by viewModel.requirements.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSteps by remember { mutableStateOf(
        transaction.requirements?.serviceInfoList[0]?.data ?: listOf()) }
    val tabs = listOf("المستندات المطلوبة", "الخطوات", "الرسوم")

    // Prepare simple derived data from Transaction model
    val steps = transaction.steps.ifEmpty { List(transaction.stepCount.coerceAtLeast(0)) { index -> "خطوة ${index + 1}" } }
    val documents = transaction.requirements

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = parentTitleRes?.let { localizedApp(it) } ?: localizedApp(transaction.titleRes),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    // Settings/Close Icon Button
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Close Menu",
                            tint = extraColors.white
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.blue1,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ابدأ الخدمة", fontSize = 16.sp , fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        containerColor = extraColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))

                // Info card with icon
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.grayCard
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                    Text(text = localizedApp(transaction.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = extraColors.blue1,
                        modifier = Modifier.padding(bottom = 8.dp))

                    Text(text = localizedApp(transaction.descriptionRes),
                        color = extraColors.blue2)
                }
            }

                Spacer(Modifier.height(16.dp))

                // Summary tiles with icons
                Row (
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    requirement.serviceSummaryList.map{ summary ->
                        SummaryTileWithIcon(
                            label = summary.value,
                            sub = summary.label,
                            icon = Icons.Filled.AttachMoney,
                            iconColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                            // modifier = Modifier.fillMaxWidth(fraction = 1f / transaction.requirements.serviceSummaryList.size.toFloat())
                        )
                    }
//                    SummaryTileWithIcon(
//                        label = transaction.fees,
//                        sub = "الرسوم",
//                        icon = Icons.Filled.AttachMoney,
//                        iconColor = Color(0xFF4CAF50),
//                        modifier = Modifier.weight(1f)
//                    )
//                    SummaryTileWithIcon(
//                        label = (if (transaction.steps.isNotEmpty()) transaction.steps.size else transaction.stepCount).toString(),
//                        sub = "الخطوات",
//                        icon = Icons.AutoMirrored.Filled.List,
//                        iconColor = Color(0xFF2196F3),
//                        modifier = Modifier.weight(1f)
//                    )
//                    SummaryTileWithIcon(
//                        label = transaction.duration,
//                        sub = "مدة التنفيذ",
//                        icon = Icons.Filled.AccessTime,
//                        iconColor = Color(0xFFFF9800),
//                        modifier = Modifier.weight(1f)
//                    )
                }

                Spacer(Modifier.height(16.dp))

                // Tabs
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = extraColors.white
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        val extraColors = LocalExtraColors.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(extraColors.grayCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            requirement.serviceInfoList.forEachIndexed { index, serviceInfo ->
                                CustomTab(
                                    title = serviceInfo.title,
                                    selected = selectedTab == index,
                                    onClick = {
                                        selectedTab = index
                                        selectedSteps = serviceInfo.data
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    selectedSteps.map { step ->
                        DocumentItem(
                            number = step.stepNo,
                            text = step.title,
                            isRequired = false
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }



            // Tab content
            /*when (selectedTab) {
                0 -> {
                    // Documents/requirements
                    item {
                        Text(
                            text = "المستندات المطلوبة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = extraColors.blue1
                        )
                    }

                    if (documents != null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد مستندات محددة لهذه المعاملة.",
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        itemsIndexed(documents) { index, doc ->
                            DocumentItem(
                                number = index + 1,
                                text = doc,
                                isRequired = false
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                1 -> {
                    // Steps
                    item {
                        Text(
                            text = "خطوات طلب الخدمة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = extraColors.blue1
                        )
                    }

                    itemsIndexed(steps) { i, s ->
                        StepItem(number = i + 1, text = s)
                        Spacer(Modifier.height(2.dp))
                    }
                }
                2 -> {
                    // Fees
                    item {
                        Text(
                            text = "رسوم الخدمة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = extraColors.blue1
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "رسوم الخدمة",
                                        fontWeight = FontWeight.Medium,
                                        color = extraColors.blue2
                                    )
                                    Text(
                                        text = transaction.fees,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(
                                    Modifier,
                                    DividerDefaults.Thickness,
                                    color = Color(0xFFEEEEEE)
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "مدة الطلب",
                                        fontWeight = FontWeight.Medium,
                                        color = extraColors.blue2
                                    )
                                    Text(
                                        text = transaction.duration,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
            }*/
        }
    }
}

@Composable
private fun SummaryTileWithIcon(
    label: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(extraColors.white)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = extraColors.blue1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                fontWeight = FontWeight.Medium,
                color = extraColors.blue2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = modifier
            .background(
                color = if (selected) extraColors.white else extraColors.grayCard,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Medium,
            color = if (selected) {
                extraColors.blue1
            } else extraColors.blue2,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun DocumentItem(number: Int, text: String, isRequired: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        val extraColors = LocalExtraColors.current
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // show the document number at the start (right side in RTL will be visually correct)
            Text(
                text = "${number}.",
                style = MaterialTheme.typography.titleMedium,
                color = extraColors.blue1,
                modifier = Modifier.padding(end = 8.dp)
            )

            if (isRequired) {
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFFFEBEE),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "إلزامي",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = extraColors.blue2,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f),
                color = extraColors.blue2
            )
        }
    }
}

@Composable
private fun StepItem(number: Int, text: String) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(extraColors.background )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number circle on the right
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(extraColors.blue1, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            // Step text (takes remaining space) - align to right for Arabic
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                color = extraColors.blue2
            )

        }
    }
}
