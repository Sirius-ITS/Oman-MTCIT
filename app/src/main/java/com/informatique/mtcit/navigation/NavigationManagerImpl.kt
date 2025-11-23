package com.informatique.mtcit.navigation

import androidx.navigation.NavController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface NavigationManager {
    fun navigate(route: String)
    fun navigateUp()
    fun navigateBack()
    fun navigateWithArgs(route: String, vararg args: Pair<String, Any>)
    fun popBackStack(route: String, inclusive: Boolean)
}

// 2. Implement the Navigation Manager
class NavigationManagerImpl: NavigationManager {

    private val _navigationCommands = MutableSharedFlow<NavigationCommand>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val navigationCommands: SharedFlow<NavigationCommand> = _navigationCommands.asSharedFlow()

    override fun navigate(route: String) {
        _navigationCommands.tryEmit(NavigationCommand.Navigate(route))
    }

    override fun navigateUp() {
        _navigationCommands.tryEmit(NavigationCommand.NavigateUp)
    }

    override fun navigateBack() {
        _navigationCommands.tryEmit(NavigationCommand.NavigateBack)
    }

    override fun navigateWithArgs(route: String, vararg args: Pair<String, Any>) {
        val argsString = args.joinToString("&") { "${it.first}=${it.second}" }
//        navController.navigate("$route?$argsString")
        _navigationCommands.tryEmit(NavigationCommand.NavigateWithArgs(route, argsString))
    }

    override fun popBackStack(route: String, inclusive: Boolean) {
        _navigationCommands.tryEmit(NavigationCommand.PopBackStackTo(route, inclusive))
    }
}
