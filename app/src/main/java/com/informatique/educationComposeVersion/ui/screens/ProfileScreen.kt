package com.informatique.educationComposeVersion.ui.screens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.educationComposeVersion.R
import com.informatique.educationComposeVersion.ui.LandingActivity
import com.informatique.educationComposeVersion.ui.components.ProfileHeader
import com.informatique.educationComposeVersion.ui.components.ProfilePersonalInformation
import com.informatique.educationComposeVersion.ui.components.localizedApp
import com.informatique.educationComposeVersion.ui.viewmodels.SharedUserViewModel


@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun ProfileScreen(
    navController: NavController,
    sharedUserViewModel: SharedUserViewModel
) {
    val currentLanguageCode = LocalContext.current.resources.configuration.locales[0].language
    val cardProfile by sharedUserViewModel.cardProfile.collectAsState()
    var studintame : String?
    var studintid : String?
    var nationality : String?
    var nationalid : String?
    var email : String?
    var mobileno : String?
    var college : String?
    var major : String?
    if (currentLanguageCode == "ar"){
        studintame = cardProfile?.fULLNAMEAR.toString()
        studintid = cardProfile?.sTUDFACULTYCODE.toString()
        nationality = cardProfile?.nATIONDESCRAR.toString()
        nationalid = cardProfile?.nATIONALNUMBER.toString()
        email = cardProfile?.sTUDEMAIL.toString()
        mobileno = cardProfile?.sTUDMOBNO.toString()
        college = cardProfile?.fACULTYDESCRAR.toString()
        major = cardProfile?.mAJORAR.toString()
    }else{
        studintame = cardProfile?.fULLNAMEEN.toString()
        studintid = cardProfile?.sTUDFACULTYCODE.toString()
        nationality = cardProfile?.nATIONDESCREN.toString()
        nationalid = cardProfile?.nATIONALNUMBER.toString()
        email = cardProfile?.sTUDEMAIL.toString()
        mobileno = cardProfile?.sTUDMOBNO.toString()
        college = cardProfile?.fACULTYDESCREN.toString()
        major = cardProfile?.mAJOREN.toString()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background)),
    ) {
        // 🟦 Header Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // الهيدر نفسه
                ProfileHeader(title = localizedApp( R.string.profile))
                // 🪪 كارد البيانات فوق الهيدر باستخدام offset
                ProfilePersonalInformation(
                    navController = navController,
                    studintame = studintame,
                    studintid = studintid ,
                    nationality = nationality ,
                    nationalid = nationalid ,
                    email = email,
                    mobileno = mobileno ,
                    college = college ,
                    major = major
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 46.dp))
            ProfileSettingsCard(
                onChangeLanguageClick = {
                    navController.navigate("languagescreen") // تنتقل لصفحة اللغة
                },
                onLogoutClick = {
                    val context = navController.context
                    val intent = Intent(context, LandingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}



@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 12.sp,
            modifier = Modifier.width(160.dp)
        )
        Text(text = value , color = Color.Gray , fontSize = 12.sp , maxLines = 1)
    }
}

@Composable
fun ProfileSettingItem(text: String ,   onClick: () -> Unit) {
    Text(
        text = text,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp).clickable { onClick() },
    )
}

@Composable
fun ProfileSettingsCard(
    onChangeLanguageClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp , vertical = 10.dp)) {

            // just label
            Text(
                text = localizedApp( R.string.another),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(5.dp))

            // rate app (does nothing now)
            ProfileSettingItem(text = localizedApp( R.string.rate_this_app)) {}

            Divider(modifier = Modifier.padding())

            // change language
            ProfileSettingItem(
                text = localizedApp( R.string.change_language),
                onClick = onChangeLanguageClick
            )

            Divider(modifier = Modifier.padding())

            // log out
            ProfileSettingItem(
                text = localizedApp( R.string.log_out),
                onClick = onLogoutClick
            )
        }
    }
}

