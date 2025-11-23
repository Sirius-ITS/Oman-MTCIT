package com.informatique.mtcit.navigation

sealed class NavigationCommand {
    data class Navigate(
        val route: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false
    ) : NavigationCommand()

    object NavigateBack : NavigationCommand()

    object NavigateUp : NavigationCommand()

    data class NavigateWithArgs(
        val route: String,
        val data: String
    ) : NavigationCommand()

    data class PopBackStackTo(
        val route: String,
        val inclusive: Boolean = false
    ) : NavigationCommand()

    data class NavigateAndClearBackStack(
        val route: String
    ) : NavigationCommand()
}