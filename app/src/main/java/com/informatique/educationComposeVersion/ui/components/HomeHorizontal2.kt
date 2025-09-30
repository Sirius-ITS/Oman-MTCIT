package com.informatique.educationComposeVersion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.informatique.educationComposeVersion.R


@Composable
fun HomeHorizontal2(
    navController: NavController
) {
    val absence = localizedApp( R.string.absence)
    val student = localizedApp( R.string.student)
    val fees = localizedApp( R.string.fees)
    val payment = localizedApp( R.string.payment)
    val schedule = localizedApp( R.string.schedule)
    val test = localizedApp( R.string.test)
    val result = localizedApp( R.string.result)
    val semester = localizedApp( R.string.semester)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp , vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),

    ) {
        item {
            HomeHorizontalButton(absence,student,  colorResource(id = R.color.absence), R.drawable.baseline_local_fire_department_24){
                navController.navigate("absence_screen")
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            HomeHorizontalButton(fees, payment, colorResource(id = R.color.fees),  R.drawable.baseline_account_balance_wallet_24){
                navController.navigate("fees_screen")
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            HomeHorizontalButton(schedule, test, colorResource(id = R.color.exam),  R.drawable.baseline_calendar_month_24){
                navController.navigate("schedule_screen")
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            HomeHorizontalButton(result, semester, colorResource(id = R.color.transcript),  R.drawable.outline_trophy_24){
                navController.navigate("result_screen")
            }
        }

    }



}
