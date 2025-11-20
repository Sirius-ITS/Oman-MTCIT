package com.informatique.mtcit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable

@Serializable
sealed interface RequestDetail {
    @Serializable
    data class CheckShipCondition(
        val transactionTitle: String,
        val title: String,
        val description: String,
        val referenceNumber: String,
        val refuseReason: String,
        val shipData: Map<String, String>
    ) : RequestDetail
    @Serializable
    data class Attachments(val requestData: String) : RequestDetail

    @Serializable
    data class AcceptedAndPayment(
        val transactionTitle: String,
        val title: String,
        val referenceNumber: String,
        val dataSubmitted: Map<String, String>
    ) : RequestDetail
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(navController: NavController, requestDetail: RequestDetail){

    val extraColors = LocalExtraColors.current

    BackHandler { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.background)
    ) {

        when (requestDetail) {
            is RequestDetail.CheckShipCondition -> {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = requestDetail.transactionTitle,
                            fontSize = 18.sp,
                            color = extraColors.whiteInDarkMode,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text(
                            text = requestDetail.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = extraColors.whiteInDarkMode,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "الرقم المرجعي: ${requestDetail.referenceNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textSubTitle
                        )

                        Text(
                            text = requestDetail.description, //localizedApp(currentStepData.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textSubTitle
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text(
                            text = "أسباب الرفض",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = extraColors.whiteInDarkMode,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = requestDetail.refuseReason, //localizedApp(currentStepData.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textSubTitle
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "التفاصيل التي قدمتها", //localizedApp(currentStepData.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = extraColors.whiteInDarkMode,
                    )

                    ExpandableBottomSheetSection(
                        title = "بيانات الطلب",
                        isExpand = true,
                        content = {
                            BottomSheetInfoCard(label = "نوع الوحدة البحرية", value = "سفينة صيد")
                            BottomSheetInfoCard(label = "رقم IMO", value = "9990001")
                            BottomSheetInfoCard(label = "رمز النداء", value = "A9BC2")
                            BottomSheetInfoCard(label = "رقم الهوية البحرية", value = "470123456")
                            BottomSheetInfoCard(label = "ميناء التسجيل", value = "صحار")
                            BottomSheetInfoCard(label = "النشاط البحري", value = "صيد")
                            BottomSheetInfoCard(label = "سنة صنع السفينة", value = "2018")
                            BottomSheetInfoCard(label = "نوع الإثبات", value = "شهادة بناء")
                            BottomSheetInfoCard(label = "حوض البناء", value = "Hyundai Shipyard")
                            BottomSheetInfoCard(label = "تاريخ بدء البناء", value = "2014-03-01")
                            BottomSheetInfoCard(label = "تاريخ انتهاء البناء", value = "2015-01-15")
                            BottomSheetInfoCard(label = "تاريخ أول تسجيل", value = "2015-02-01")
                            BottomSheetInfoCard(label = "بلد البناء", value = "سلطنة عمان")
                        }
                    )

                    ExpandableBottomSheetSection(
                        title = "بيانات المعاينة",
                        content = {}
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 26.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate(NavRoutes.MainCategoriesRoute.route) {
                                popUpTo(NavRoutes.MainCategoriesRoute.route) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.startServiceButton,
                            contentColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = localizedApp(R.string.request_detail_back_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            is RequestDetail.Attachments -> {

            }

            is RequestDetail.AcceptedAndPayment -> {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = requestDetail.transactionTitle,
                            fontSize = 18.sp,
                            color = extraColors.whiteInDarkMode,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text(
                            text = requestDetail.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = extraColors.whiteInDarkMode,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "الرقم المرجعي: ${requestDetail.referenceNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textSubTitle
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "التفاصيل التي قدمتها", //localizedApp(currentStepData.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = extraColors.whiteInDarkMode,
                    )

                    ExpandableBottomSheetSection(
                        title = "بيانات الطلب",
                        isExpand = false,
                        content = {
                            BottomSheetInfoCard(label = "نوع الوحدة البحرية", value = "سفينة صيد")
                            BottomSheetInfoCard(label = "رقم IMO", value = "9990001")
                            BottomSheetInfoCard(label = "رمز النداء", value = "A9BC2")
                            BottomSheetInfoCard(label = "رقم الهوية البحرية", value = "470123456")
                            BottomSheetInfoCard(label = "ميناء التسجيل", value = "صحار")
                            BottomSheetInfoCard(label = "النشاط البحري", value = "صيد")
                            BottomSheetInfoCard(label = "سنة صنع السفينة", value = "2018")
                            BottomSheetInfoCard(label = "نوع الإثبات", value = "شهادة بناء")
                            BottomSheetInfoCard(label = "حوض البناء", value = "Hyundai Shipyard")
                            BottomSheetInfoCard(label = "تاريخ بدء البناء", value = "2014-03-01")
                            BottomSheetInfoCard(label = "تاريخ انتهاء البناء", value = "2015-01-15")
                            BottomSheetInfoCard(label = "تاريخ أول تسجيل", value = "2015-02-01")
                            BottomSheetInfoCard(label = "بلد البناء", value = "سلطنة عمان")
                        }
                    )

                    ExpandableBottomSheetSection(
                        title = "بيانات البحارة (5 بحارة)",
                        content = {}
                    )
                    ExpandableBottomSheetSection(
                        title = "بيانات المعاينة",
                        content = {}
                    )
                }

                Row (
                    modifier = Modifier.fillMaxWidth(),
                ){
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 26.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(NavRoutes.MainCategoriesRoute.route) {
                                    popUpTo(NavRoutes.MainCategoriesRoute.route) {
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extraColors.startServiceButton,
                                contentColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = localizedApp(R.string.request_detail_back_title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 26.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate(NavRoutes.PaymentDetailsRoute.route)
                            },
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
                                    text = localizedApp(R.string.process_to_payment_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                }
            }
        }
    }

}

@Composable
private fun BottomSheetInfoCard(label: String, value: String) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(extraColors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode
            )
            Text(text = value, fontSize = 14.sp, color = extraColors.textSubTitle)
        }
    }
}

@Composable
private fun ExpandableBottomSheetSection(
    title: String,
    isExpand: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(isExpand) }
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground2.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = extraColors.whiteInDarkMode
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = extraColors.textBlueSubTitle
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}
