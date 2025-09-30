package com.informatique.educationComposeVersion.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.informatique.educationComposeVersion.R

@Composable
fun HomeFeatureList(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // المسافة بين العناصر
    ) {

        HomeFeatureItem("Microsoft Teams",  Color.White,"Microsoft Teams", R.drawable.microsoftteams ){
            openLink(context, "https://teams.microsoft.com/")
        }
        HomeFeatureItem("Moodle",  Color.White,"Moodle", R.drawable.moodle) {
            openLink(context, "https://lms.lum.edu.eg/")
        }
        HomeFeatureItem("SIS",  Color.White,"sis", R.drawable.microsoftteams){
            openLink(context, "https://login.microsoftonline.com/94ee267f-51da-466f-abc7-a3e1cdea78fc/oauth2/authorize?response_type=code&client_id=ffc962f0-741e-46a3-97af-8aaf8287efb4&redirect_uri=https%3a%2f%2fsis.lum.edu.eg%2fDefaultAAD.aspx&prompt=login")
        }
        HomeFeatureItem(localizedApp( R.string.surveypoll),  Color.White,localizedApp( R.string.surveypoll2), R.drawable.microsoftteams)
        {
            navController.navigate("opinion_screen")
        }
        HomeFeatureItem(localizedApp( R.string.coursecatalogue),  Color.White,localizedApp( R.string.coursecatalogue2), R.drawable.microsoftteams){
            navController.navigate("course_list_screen")
        }
        HomeFeatureItem(localizedApp( R.string.academicplans),  Color.White,localizedApp( R.string.academicplans2), R.drawable.microsoftteams){
            navController.navigate("academic_plans_screen")
        }
        HomeFeatureItem(localizedApp( R.string.academiccalender),  Color.White,localizedApp( R.string.academiccalender2), R.drawable.microsoftteams){
            navController.navigate("academic_calendar_screen")
        }

    }

}
fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}