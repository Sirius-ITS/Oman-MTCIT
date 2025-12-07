package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.common.util.LocalAppLocale
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.data.model.category.Transaction
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.MainCategoriesViewModel
import com.informatique.mtcit.R
import com.informatique.mtcit.data.model.category.Step
import com.informatique.mtcit.data.model.category.Term
import com.informatique.mtcit.ui.components.localizedPluralsApp
import com.informatique.mtcit.ui.theme.ExtraColors
import com.informatique.mtcit.ui.viewmodels.TransactionDetailUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRequirementsScreen(
    transaction: Transaction,
    onStart: () -> Unit,
    onBack: () -> Unit,
    transactionId: Int,
    parentTitleRes: Int? = null,
    navController: NavController
) {
    // val viewModel: TransactionListViewModel = hiltViewModel()
    val viewModel: MainCategoriesViewModel = hiltViewModel()

    val locale = LocalAppLocale.current
    val extraColors = LocalExtraColors.current

    LaunchedEffect(transactionId) {
        val currentState = viewModel.transactionDetail.value
        if (currentState !is TransactionDetailUiState.Success ||
            currentState.detail.id != transactionId) {
            viewModel.getTransactionDetailApi(transactionId)
        }
    }

    val uiState by viewModel.transactionDetail.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = viewModel.requirementsTabList.collectAsStateWithLifecycle()
    var selectedStep by remember{ mutableStateOf("") }

    // Prepare simple derived data from Transaction model
//    val steps = transaction.steps.ifEmpty { List(transaction.stepCount.coerceAtLeast(0)) { index -> "خطوة ${index + 1}" } }
//    val documents = transaction.requirements

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (locale.language == "ar") transaction.nameAr else transaction.nameEn,
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
                                color = Color(0xFF4A7BA7),
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
                            contentDescription = localizedApp(R.string.back_button),
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
                                color = Color(0xFF4A7BA7),
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = 20.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF4A7BA7).copy(alpha = 0.3f),
                                spotColor = Color(0xFF4A7BA7).copy(alpha = 0.3f)
                            )
                            .background(extraColors.iconBackBackground2)
                            .clickable { navController.navigate(NavRoutes.SettingsRoute.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = localizedApp(R.string.settings_title),
                            tint = extraColors.iconBack2
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 4.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedApp(R.string.start_the_service),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        containerColor = extraColors.background
    ) { innerPadding ->
        if (uiState is TransactionDetailUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else if (uiState is TransactionDetailUiState.Success) {
            val detail = (uiState as TransactionDetailUiState.Success).detail
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
                            containerColor = extraColors.cardBacground3
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row (
                                horizontalArrangement = Arrangement.Center
                            ){
                                Box(
                                    modifier = Modifier
                                        .size(35.dp)
                                        .background(
                                            extraColors.iconLightBlueBackground,
                                            shape = RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ){
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = extraColors.iconLightBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = if (locale.language == "ar") detail.nameAr else detail.nameEn,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center,
                                    color = extraColors.whiteInDarkMode,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (locale.language == "ar") detail.descAr else detail.descEn,
                                color = extraColors.textSubTitle
                            )

                            Spacer(Modifier.height(8.dp))

                            Surface(
                                color = extraColors.iconLightBlueBackground,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = localizedApp(R.string.categories_title),
                                    fontSize = 12.sp,
                                    color = extraColors.iconLightBlue,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Summary tiles with icons
                    Row (
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (detail.duration != null) {
                            SummaryTileWithIcon(
                                label = localizedPluralsApp(
                                    R.plurals.requirements_duration_value,
                                    detail.duration,
                                    detail.duration
                                ),
                                sub = localizedApp(R.string.requirements_duration_title),
                                icon = Icons.Filled.AccessTime,
                                iconColor = Color(0xFFFF9800),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (detail.steps.isNotEmpty()) {
                            SummaryTileWithIcon(
                                label = detail.steps.size.toString(),
                                sub = localizedApp(R.string.requirements_steps_title),
                                icon = Icons.AutoMirrored.Filled.List,
                                iconColor = Color(0xFF2196F3),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (detail.fees != null) {
                            SummaryTileWithIcon(
                                label = localizedApp(R.string.requirements_fees_value, detail.fees),
                                sub = localizedApp(R.string.requirements_fees_title),
                                icon = Icons.Filled.AttachMoney,
                                iconColor = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Tabs
                    if (tabs.value.isNotEmpty()) {
                        Column {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = extraColors.cardBacground3
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    tabs.value.forEachIndexed { index, tab ->
                                        CustomTab(
                                            title = tab,
                                            selected = selectedTab == index,
                                            onClick = {
                                                selectedTab = index
                                                selectedStep = tab
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            when (selectedStep) {
                                localizedApp(R.string.requirements_terms_title) -> {

                                    Text(
                                        text = localizedApp(R.string.requirements_terms_title),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = extraColors.whiteInDarkMode,
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 1
                                    )

                                    detail.terms.forEach { term ->
                                        ServiceTerms(
                                            locale = locale,
                                            extraColors = extraColors,
                                            term = term
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                localizedApp(R.string.requirements_steps_title) -> {
                                    Text(
                                        text = localizedApp(R.string.requirements_steps_heading),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = extraColors.whiteInDarkMode,
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 1
                                    )

                                    detail.steps.forEachIndexed { index, step ->
                                        StepItem(
                                            locale = locale,
                                            extraColors = extraColors,
                                            number = index + 1,
                                            step = step
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }

                                localizedApp(R.string.requirements_fees_title) -> {
                                    Text(
                                        text = localizedApp(R.string.requirements_fees_heading),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = extraColors.whiteInDarkMode,
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 1
                                    )

                                    FeesDurationItem(
                                        title = localizedApp(R.string.requirements_fees_heading),
                                        fees = detail.fees ?: 0,
                                        backgroundColor = Color(0xFF4CAF50)
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    FeesDurationItem(
                                        title = localizedApp(R.string.requirements_duration_title),
                                        duration = detail.duration ?: 0,
                                        backgroundColor = Color(0xFFFF9800)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

            }
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
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(extraColors.cardBacground3)
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
                color = extraColors.whiteInDarkMode
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sub,
                fontWeight = FontWeight.Medium,
                color = extraColors.textSubTitle,
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
                color = if (selected) extraColors.SelectedCustomTab else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Medium,
            color = extraColors.whiteInDarkMode/*if (selected) {
                extraColors.blue1
            } else extraColors.blue2*/,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FeesDurationItem(
    title: String,
    fees: Int? = null,
    duration: Int? = null,
    backgroundColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.03f))
    ) {
        val extraColors = LocalExtraColors.current
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
                color = extraColors.whiteInDarkMode
            )

            Text(
                text = if (fees != null) localizedApp(R.string.requirements_fees_value, fees)
                else localizedPluralsApp(R.plurals.requirements_duration_value, duration!!,
                    duration),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                color = if (fees != null) backgroundColor else extraColors.whiteInDarkMode
            )
        }
    }
}

@Composable
private fun StepItem(
    locale: Locale,
    extraColors: ExtraColors,
    number: Int,
    step: Step
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number circle on the right
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(extraColors.iconLightBlue, CircleShape),
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
            text = if (locale.language == "ar") step.stepNameAr else step.stepNameEn,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = extraColors.whiteInDarkMode
        )

    }
}

@Composable
private fun ServiceTerms(
    locale: Locale,
    extraColors: ExtraColors,
    term: Term
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(extraColors.iconLightBlue.copy(alpha = 0.03f) )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number circle on the right
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(
                        extraColors.iconLightBlue.copy(alpha = 0.10f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = extraColors.iconLightBlue,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            // Step text (takes remaining space) - align to right for Arabic
            Text(
                text = if (locale.language == "ar") term.nameAr else term.nameEn,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                color = extraColors.whiteInDarkMode
            )

        }
    }
}
