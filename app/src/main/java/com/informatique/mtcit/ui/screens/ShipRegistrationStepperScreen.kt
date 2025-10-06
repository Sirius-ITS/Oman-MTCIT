package com.informatique.mtcit.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@Composable
fun ShipRegistrationStepperScreen(
    navController: NavController,
    sharedUserViewModel: SharedUserViewModel,
    modifier: Modifier = Modifier
) {
    // Delegate to the main ShipRegistrationScreen which has all the step functionality
    ShipRegistrationScreen(
        navController = navController,
        modifier = modifier
    )
}
