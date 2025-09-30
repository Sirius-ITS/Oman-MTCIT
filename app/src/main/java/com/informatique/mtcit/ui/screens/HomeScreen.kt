package com.informatique.mtcit.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.HomeFeatureList
import com.informatique.mtcit.ui.components.HomeHeader
import com.informatique.mtcit.ui.components.HomeHorizontal2
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun HomeScreen(navController: NavController, sharedUserViewModel: SharedUserViewModel) {
    val currentLanguageCode = LocalContext.current.resources.configuration.locales[0].language
    val cardProfile by sharedUserViewModel.cardProfile.collectAsState()
    val name = if (currentLanguageCode == "ar"){
        cardProfile?.fULLNAMEAR.toString()
    }else{
        cardProfile?.fULLNAMEEN.toString()
    }
            Scaffold { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(color = colorResource(id = R.color.background))
                ) {
                    item {
                        HomeHeader(
                            title = "${localizedApp( R.string.hello)} $name",
                            personVictor = Icons.Default.Notifications,
                            notificationVector = Icons.Default.AccountCircle
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HomeHorizontal2(navController = navController)
                        Spacer(modifier = Modifier.height(26.dp))
                        HomeFeatureList(navController = navController)
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }