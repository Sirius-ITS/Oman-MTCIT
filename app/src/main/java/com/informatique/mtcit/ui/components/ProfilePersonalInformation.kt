package com.informatique.mtcit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.informatique.mtcit.ui.screens.ProfileInfoRow
import com.informatique.mtcit.ui.screens.ProfileSettingItem

@Composable
fun ProfilePersonalInformation(
    navController: NavController,
    studintame : String,
    studintid : String,
    nationality : String,
    nationalid : String,
    email : String,
    mobileno : String,
    college : String,
    major : String
){

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .offset(y = 80.dp) // يركب فوق الهيدر
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ic_favourite),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(90.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = studintame,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(localizedApp( R.string.general), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(localizedApp( R.string.personal_information), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow( localizedApp( R.string.student_id), studintid)
            ProfileInfoRow( localizedApp( R.string.nationality),  nationality)
            ProfileInfoRow( localizedApp( R.string.national_id),  nationalid)
            ProfileInfoRow(localizedApp( R.string.email), email)
            ProfileInfoRow(localizedApp( R.string.mobile_no), mobileno)
            ProfileInfoRow(localizedApp( R.string.college),  college)
            ProfileInfoRow(localizedApp( R.string.major),  major)

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            ProfileSettingItem(text = localizedApp( R.string.change_password) , onClick = {
                navController.navigate("changepasswordscreen")
            })
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            var isNotificationOn by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(localizedApp( R.string.notification), fontWeight = FontWeight.Medium )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    colors = SwitchDefaults.colors(colorResource(id = R.color.system_bar)),
                    checked = isNotificationOn,
                    onCheckedChange = { isNotificationOn = it }
                )
            }
            Text(
                text = localizedApp( R.string.manage_notification),
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.width(250.dp)
            )
        }
    }
}