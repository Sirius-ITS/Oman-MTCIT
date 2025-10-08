package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.CommonButton
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel

@Composable
fun LanguageScreen(navController: NavController,
                   languageViewModel: LanguageViewModel = hiltViewModel()) {
    val isLoading = languageViewModel.isLoading.value

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.background)), // Remove windowInsetsPadding
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Restore original padding
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(16.dp))
                CommonButton(
                    text = "العربية",
                    true,
                    colorResource(id = R.color.login_button)
                ) {
                    // Ensure activity is not null before using it
                    languageViewModel.saveLanguage("ar")
                    navController.popBackStack()
                }
                Spacer(modifier = Modifier.height(16.dp))
                CommonButton(
                    text = "English",
                    true,
                    colorResource(id = R.color.login_button)
                ) {
                    languageViewModel.saveLanguage("en")
                    navController.popBackStack()
                }
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
