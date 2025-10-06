package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.HomeHeader
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import androidx.compose.ui.platform.LocalConfiguration


@Composable
fun HomeScreen(navController: NavController, sharedUserViewModel: SharedUserViewModel) {
    val currentLanguageCode = LocalConfiguration.current.locales[0].language
    val cardProfile by sharedUserViewModel.cardProfile.collectAsState()
    val name = if (currentLanguageCode == "ar"){
        cardProfile?.fULLNAMEAR.toString()
    }else{
        cardProfile?.fULLNAMEEN.toString()
    }

    // Remove Scaffold and use LazyColumn directly for edge-to-edge content
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.white))
    ) {
        item {
            HomeHeader(
                title = "${localizedApp( R.string.hello)} $name",
                personVictor = Icons.Default.Notifications,
                notificationVector = Icons.Default.AccountCircle
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
