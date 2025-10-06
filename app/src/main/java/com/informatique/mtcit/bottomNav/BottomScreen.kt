package com.informatique.mtcit.bottomNav

import androidx.annotation.StringRes
import com.informatique.mtcit.R

sealed class BottomScreen(val route: String, val icon: Int, @StringRes val titleRes: Int) {
    object Home : BottomScreen("Home", R.drawable.home, R.string.home)
    object Profile : BottomScreen("Profile", R.drawable.user, R.string.profile)
    object ShipRegistration : BottomScreen("Ship Registration", R.drawable.user, R.string.ship_registration)
}
