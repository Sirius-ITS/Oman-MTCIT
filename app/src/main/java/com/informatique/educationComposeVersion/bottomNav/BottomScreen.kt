package com.informatique.educationComposeVersion.bottomNav

import androidx.annotation.StringRes
import com.informatique.educationComposeVersion.R

sealed class BottomScreen(val route: String, val icon: Int, @StringRes val titleRes: Int) {
    object Home : BottomScreen("Home", R.drawable.home, R.string.home)
    object Profile : BottomScreen("Profile", R.drawable.user, R.string.profile)
}
